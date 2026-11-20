#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# AC-14 文案红线扫描
#
# 为什么必须扫 .kt：本项目的用户可见文案目前是**硬编码在 Compose 字面量**里的
# （strings.xml 只有 app_name），只扫 XML 会一无所获 —— 这正是首版
# BannedWordsTest 的盲区（它断言的只是测试自己写的局部字面量）。
#
# 用法：
#   tools/scan-banned.sh                扫全部源码
#   tools/scan-banned.sh --include-tests  连测试文件一起扫
#
# 退出码：0 = 无命中；1 = 有命中（CI 可直接用）
# ─────────────────────────────────────────────────────────────────────────────
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INCLUDE_TESTS=0
[[ "${1:-}" == "--include-tests" ]] && INCLUDE_TESTS=1

# 与 core/text/BannedWords.kt 的 WORDS 保持一致。
# 用更宽的词干覆盖（例如「治愈」而非「治愈率」），避免只抓到复合词。
PATTERN='盆底肌力|肌力|动作正确率|收缩正确率|动作标准|治愈|显著改善|疗效|血压|血氧|情绪|心理评分|HRV|已学会'

EXCLUDE_ARGS=(
  --exclude-dir=build
  --exclude-dir=.gradle
  --exclude-dir=.git
  --exclude-dir=screens/dev          # 设计系统自检页会刻意提及 token 名
)

if [[ $INCLUDE_TESTS -eq 0 ]]; then
  EXCLUDE_ARGS+=(--exclude-dir=test --exclude-dir=androidTest)
fi

echo "── 扫描范围 ─────────────────────────────────────────────"
echo "根目录：$ROOT/android"
echo "词表  ：$PATTERN"
echo "测试文件：$([ $INCLUDE_TESTS -eq 1 ] && echo 纳入 || echo 排除)"
echo

# 只扫**用户可见文案**的位置：字符串字面量与结论文本。
# 注释里出现「肌力」等词是允许的（例如解释为什么禁用该词），
# 因此先剔除以 * 或 // 开头的行，再匹配。
RAW="$(grep -rnE "$PATTERN" "$ROOT/android" \
        --include='*.kt' --include='*.xml' \
        "${EXCLUDE_ARGS[@]}" 2>/dev/null || true)"

# 过滤掉纯注释行与 BannedWords.kt 自身的词表定义
FILTERED="$(printf '%s\n' "$RAW" \
  | grep -vE ':[[:space:]]*(//|\*|/\*)' \
  | grep -v 'core/text/BannedWords.kt' \
  | grep -v 'tools/scan-banned.sh' \
  | grep -v '^\s*$' || true)"

if [[ -z "$FILTERED" ]]; then
  echo "✅ 未发现禁词（AC-14 字面检查通过）"
  exit 0
fi

echo "❌ 发现疑似禁词命中："
echo "────────────────────────────────────────────────────────"
printf '%s\n' "$FILTERED"
echo "────────────────────────────────────────────────────────"
echo
echo "说明："
echo "  · 注释中提及禁词是允许的，本脚本已剔除注释行；"
echo "  · 语义级违规（如把心率描述成肌肉状态）无法用正则发现，"
echo "    必须靠固定文案白名单 + 内容/临床评审 —— 这一点在验收记录中如实标注。"
exit 1