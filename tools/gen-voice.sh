#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# 生成语音素材（决策 D2′）
#
# 为什么在构建期生成而不是用设备 TTS：
#   实测目标设备（NX729J / Android 15）**没有任何 TTS 引擎** ——
#   `cmd package services android.intent.action.TTS_SERVICE` 返回 No services found。
#   所以「用设备 TTS 兜底」在验收机上根本跑不起来。
#   改为用 macOS 中文语音在开发机生成 AAC 素材打进 assets，
#   既回到需求 §8.1 的原始设计（预录音频），又完全离线、可重复生成。
#
# 文案的**唯一来源**是 app 里的 `audio/VoiceScript.kt`，本脚本从那里读取，
# 避免"文案改了、音频还是旧的"这种演示事故。
#
# 用法：
#   tools/gen-voice.sh          # 重新生成缺失的素材（默认跳过已存在的）
#   tools/gen-voice.sh --force  # 全部重新生成
# ─────────────────────────────────────────────────────────────────────────────
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT_KT="$ROOT/android/app/src/main/java/com/diqi/demo/audio/VoiceScript.kt"
OUT_DIR="$ROOT/android/app/src/main/assets/voice"
VOICE="${HR_VOICE:-Tingting}"
RATE="${HR_VOICE_RATE:-165}"
FORCE=0
[[ "${1:-}" == "--force" ]] && FORCE=1

if ! command -v say >/dev/null 2>&1; then
    echo "[voice] 需要 macOS 的 say 命令（本脚本只能在 macOS 上跑）" >&2
    exit 1
fi

if [[ ! -f "$SCRIPT_KT" ]]; then
    echo "[voice] 找不到 $SCRIPT_KT" >&2
    exit 1
fi

mkdir -p "$OUT_DIR"

# 从 VoiceScript.kt 里抽取 (assetPath, text) 对。
# 用 python 而不是正则 sed：文案里有中文标点与全角逗号，shell 处理容易出错。
python3 - "$SCRIPT_KT" > /tmp/hr_voice_plan.tsv <<'PY'
import re, sys
src = open(sys.argv[1], encoding='utf-8').read()
# 匹配  assetPath = "voice/xxx.m4a",\n  text = "……",
pattern = re.compile(
    r'assetPath\s*=\s*"([^"]+)"\s*,\s*\n\s*text\s*=\s*"((?:[^"\\]|\\.)*)"',
    re.S,
)
for m in pattern.finditer(src):
    path, text = m.group(1), m.group(2)
    text = text.replace('\\"', '"').replace('\\n', ' ')
    print(f"{path}\t{text}")
PY

if [[ ! -s /tmp/hr_voice_plan.tsv ]]; then
    echo "[voice] 未能从 VoiceScript.kt 解析出任何文案（正则可能失效）" >&2
    exit 1
fi

# 关键自检：源码里每个条目都有 assetPath，解析条数必须与之相等。
# 否则新增条目一旦写成 """原始字符串""、参数变序或 "a" + "b" 拼接，
# 会被静默跳过，而末尾的"全部素材就绪"仍然打印 ✅ —— 正是本脚本要防的事故。
declared=$(grep -c 'assetPath =' "$SCRIPT_KT")
parsed=$(wc -l < /tmp/hr_voice_plan.tsv | tr -d ' ')
if [[ "$declared" != "$parsed" ]]; then
    echo "[voice] 条目数不一致：源码 $declared 条，解析 $parsed 条" >&2
    echo "[voice] 说明 VoiceScript.kt 的写法变化导致解析遗漏，请同步更新本脚本的解析规则" >&2
    exit 1
fi
echo "[voice] 解析到 $parsed 条文案（与源码条目数一致）"

count=0
skipped=0
while IFS=$'\t' read -r relpath text; do
    out="$OUT_DIR/$(basename "$relpath")"
    # 用"文案哈希"做边车文件，而不是"文件是否存在"：
    # 文案改了而不带 --force 重跑时，旧音频必须被替换掉，
    # 否则就是"屏幕上写的新文案、耳朵里听的是旧文案"。
    stamp="$OUT_DIR/.$(basename "$out").textsha"
    now_sha="$(printf '%s' "$text" | shasum -a 256 | cut -d' ' -f1)"
    if [[ -f "$out" && -f "$stamp" && $FORCE -eq 0 ]]; then
        if [[ "$(cat "$stamp")" == "$now_sha" ]]; then
            skipped=$((skipped + 1))
            continue
        fi
        echo "[voice] 文案已变更，重新生成：$(basename "$out")"
    fi
    tmp_aiff="$(mktemp -t hrvoice).aiff"
    if ! say -v "$VOICE" -r "$RATE" -o "$tmp_aiff" "$text"; then
        echo "[voice] 生成失败：$text" >&2
        rm -f "$tmp_aiff"
        continue
    fi
    # 22.05kHz 单声道 AAC 64kbps：老年用户听人声足够，体积小
    if ! afconvert -f m4af -d aac -b 64000 "$tmp_aiff" "$out"; then
        echo "[voice] 转码失败：$out" >&2
        rm -f "$tmp_aiff"
        continue
    fi
    rm -f "$tmp_aiff"
    printf '%s' "$now_sha" > "$stamp"
    count=$((count + 1))
    printf '[voice] %-28s %6s bytes  %s\n' "$(basename "$out")" "$(wc -c < "$out" | tr -d ' ')" "$text"
done < /tmp/hr_voice_plan.tsv

echo
echo "[voice] 生成 $count 个，跳过已存在 $skipped 个 → $OUT_DIR"
echo "[voice] 语音：$VOICE  语速：$RATE wpm"

# 自检：必须有产物，且每个都非空
missing=0
while IFS=$'\t' read -r relpath _; do
    f="$OUT_DIR/$(basename "$relpath")"
    if [[ ! -s "$f" ]]; then
        echo "[voice] 缺失或为空：$f" >&2
        missing=$((missing + 1))
    fi
done < /tmp/hr_voice_plan.tsv

if [[ $missing -gt 0 ]]; then
    echo "[voice] 有 $missing 个素材缺失" >&2
    exit 1
fi

echo "[voice] 全部素材就绪 ✅"