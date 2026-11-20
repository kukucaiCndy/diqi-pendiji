#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# HerRhythm 端到端驱动脚本
#
# 为什么需要它：本机 adb 是**远程 TCP**（192.168.1.26:41015），会掉线；
# Compose 节点默认没有 resource-id；设备无 root/sqlite3 也经常处于睡眠。
# 每次手工敲 adb 既慢又不可复现，且容易静默读到上一轮的过期节点树。
#
# 用法：
#   tools/e2e.sh install            构建并安装 debug APK
#   tools/e2e.sh launch             冷启动 app 并等待首帧
#   tools/e2e.sh shot <name>        截图到 docs/evidence/<name>.png
#   tools/e2e.sh dump               打印带 resource-id / text 的节点树
#   tools/e2e.sh has <tag>          断言节点存在
#   tools/e2e.sh tap <tag>          点击节点中心
#   tools/e2e.sh text <tag>         打印节点 text
#   tools/e2e.sh attr <tag> <name>  打印节点属性（content-desc / enabled / checked）
#   tools/e2e.sh wait <tag> [sec]   等待节点出现
#   tools/e2e.sh scrollto <tag>     向上滚动直到节点出现（最多 15 次）
#   tools/e2e.sh nomatch <text>     断言当前界面**不含**该文本
#   tools/e2e.sh db                 拉取 Room 数据库（含 WAL）并打印路径
#   tools/e2e.sh sql <query>        对拉下来的库执行查询
#   tools/e2e.sh grant / revoke     授予 / 收回相机与通知权限
#   tools/e2e.sh cleardata          清除 app 数据
#   tools/e2e.sh fontscale <v>      设置系统字体缩放并重启进程
#   tools/e2e.sh wake               唤醒屏幕、解除锁屏、关闭超时休眠
#   tools/e2e.sh logs [filter]      清空并跟踪 logcat
#   tools/e2e.sh ensure             自检设备连通，掉线自动重连
# ─────────────────────────────────────────────────────────────────────────────
set -uo pipefail

SERIAL="${HR_SERIAL:-192.168.1.26:41015}"
PKG="${HR_PKG:-com.diqi.demo}"
ACTIVITY="${HR_ACTIVITY:-.MainActivity}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
EVIDENCE="$ROOT/docs/evidence"
TMPXML="/tmp/hr_dump.xml"
DBDIR="/tmp/hr_db"

adbs() { adb -s "$SERIAL" "$@"; }

ensure() {
    local state
    state="$(adb devices | awk -v s="$SERIAL" '$1==s {print $2}')"
    if [[ "$state" != "device" ]]; then
        echo "[e2e] 设备未就绪（state=${state:-none}），尝试重连…" >&2
        adb connect "$SERIAL" >/dev/null 2>&1 || true
        for _ in 1 2 3; do
            sleep 2
            state="$(adb devices | awk -v s="$SERIAL" '$1==s {print $2}')"
            [[ "$state" == "device" ]] && break
        done
    fi
    if [[ "$state" != "device" ]]; then
        echo "[e2e] 错误：设备 $SERIAL 无法连接（state=${state:-none}）" >&2
        return 1
    fi
    # 设备睡眠会让 dump 抓到锁屏/通知面板而不是 app —— 这是最容易静默出错的地方
    local wake
    wake="$(adbs shell dumpsys power | grep -o 'mWakefulness=[A-Za-z]*' | head -1)"
    if [[ "$wake" != "mWakefulness=Awake" ]]; then
        adbs shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1
        sleep 1
        adbs shell wm dismiss-keyguard >/dev/null 2>&1
    fi
    return 0
}

# 抓取当前界面的语义树。
# 三个必须的防御（都是曾静默出错的地方）：
#   1. 先删旧文件 —— 否则 pull 失败时会读到上一轮的树并"通过"
#   2. 校验 pull 成功且非空
#   3. 校验抓到的是 app 自己的窗口，不是锁屏/通知面板
do_dump() {
    rm -f "$TMPXML"
    adbs shell rm -f /data/local/tmp/hr_ui.xml >/dev/null 2>&1
    adbs shell uiautomator dump /data/local/tmp/hr_ui.xml >/dev/null 2>&1
    adbs pull /data/local/tmp/hr_ui.xml "$TMPXML" >/dev/null 2>&1
    adbs shell rm -f /data/local/tmp/hr_ui.xml >/dev/null 2>&1
    [[ -s "$TMPXML" ]] || return 1
    if ! grep -q "package=\"$PKG\"" "$TMPXML"; then
        return 1
    fi
    return 0
}

# 精确匹配 tag。**不再使用 endswith** —— 曾导致
# "p04.demo_watermark" 被全局 "common.demo_watermark" 误命中。
node_query() {
    local tag="$1" field="$2"
    python3 - "$TMPXML" "$tag" "$field" <<'PY'
import sys, xml.etree.ElementTree as ET
path, tag, field = sys.argv[1], sys.argv[2], sys.argv[3]
try:
    root = ET.parse(path).getroot()
except Exception:
    sys.exit(2)
for n in root.iter('node'):
    if (n.get('resource-id') or '') == tag:
        if field == '__center__':
            b = (n.get('bounds') or '').strip()
            try:
                nums = b.replace('][', ',').strip('[]').split(',')
                x1, y1, x2, y2 = map(int, nums)
            except Exception:
                sys.exit(3)
            print((x1 + x2) // 2, (y1 + y2) // 2)
        elif field == '__exists__':
            print('1')
        else:
            print(n.get(field) or '')
        sys.exit(0)
sys.exit(1)
PY
}

node_center() { node_query "$1" '__center__'; }
node_exists() { node_query "$1" '__exists__'; }

dbpull() {
    rm -rf "$DBDIR"; mkdir -p "$DBDIR"
    local names
    names="$(adbs shell "run-as $PKG ls databases 2>/dev/null" | tr -d '\r')"
    if [[ -z "$names" ]]; then
        echo "[e2e] databases/ 为空 —— 可能尚未创建数据库" >&2
        return 1
    fi
    # 按**原始文件名**拉取，这样 .db / .db-wal / .db-shm 三个伴生文件
    # 名字关系保持一致，sqlite3 打开时能自动应用 WAL。
    # （旧实现把 .db 改名后再 checkpoint，伴生名对不上 → WAL 永远不合并
    #   → 查不到最新写入 → AC-01/07/08/12/13/19/23 会被判成假 FAIL）
    local n
    for n in $names; do
        adbs exec-out "run-as $PKG cat databases/$n" > "$DBDIR/$n" 2>/dev/null
    done
    # 排除 WorkManager 等第三方库自带的库；优先认主库名
    local main
    main="$(printf '%s\n' $names | grep -E '^herrhythm.*\.db$' | head -1)"
    if [[ -z "$main" ]]; then
        main="$(printf '%s\n' $names | grep -E '\.db$' | grep -v -E 'work|google|androidx' | head -1)"
    fi
    if [[ -z "$main" ]]; then
        echo "[e2e] 未找到主数据库，现有文件：$names" >&2
        return 1
    fi
    echo "$DBDIR/$main"
}

cmd="${1:-help}"; shift || true

case "$cmd" in
  ensure) ensure ;;
  wake)
    ensure >/dev/null 2>&1 || true
    adbs shell input keyevent KEYCODE_WAKEUP
    sleep 1
    adbs shell wm dismiss-keyguard
    adbs shell settings put system screen_off_timeout 2147483647
    adbs shell settings put global stay_on_while_plugged_in 3
    echo "awake" ;;
  install)
    ensure || exit 1
    ( cd "$ROOT/android" && ./gradlew :app:assembleDebug --console=plain -q ) || exit 1
    adbs install -r "$APK" | tail -1 ;;
  launch)
    ensure || exit 1
    adbs shell am start -W -n "$PKG/$ACTIVITY" | grep -E "TotalTime|Status" ;;
  relaunch)
    ensure || exit 1
    adbs shell am force-stop "$PKG"
    sleep 1
    adbs shell am start -W -n "$PKG/$ACTIVITY" | grep -E "TotalTime|Status" ;;
  shot)
    ensure || exit 1
    mkdir -p "$EVIDENCE"
    name="${1:-shot_$(date +%H%M%S)}"
    adbs exec-out screencap -p > "$EVIDENCE/$name.png"
    echo "$EVIDENCE/$name.png" ;;
  dump)
    ensure || exit 1
    do_dump || { echo "[e2e] dump 失败（设备卡顿/锁屏/或当前不显示本 app）" >&2; exit 1; }
    python3 - "$TMPXML" <<'PY'
import sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
for n in root.iter('node'):
    rid = (n.get('resource-id') or '').strip()
    txt = (n.get('text') or '').strip()
    desc = (n.get('content-desc') or '').strip()
    if rid or txt or desc:
        print(f"id={rid or '-':40} text={txt[:30]:32} desc={desc[:28]}")
PY
    ;;
  has)
    ensure >/dev/null || exit 1
    do_dump || { echo "MISSING(dump失败)"; exit 1; }
    if node_exists "${1:?tag 必填}" >/dev/null; then echo "OK"; else echo "MISSING"; exit 1; fi ;;
  wait)
    tag="${1:?tag 必填}"; secs="${2:-10}"
    for _ in $(seq 1 "$secs"); do
        if do_dump && node_exists "$tag" >/dev/null; then echo "OK"; exit 0; fi
        sleep 1
    done
    echo "TIMEOUT waiting for $tag"; exit 1 ;;
  scrollto)
    ensure >/dev/null || exit 1
    tag="${1:?tag 必填}"
    for _ in $(seq 1 15); do
        if do_dump && node_exists "$tag" >/dev/null; then echo "OK"; exit 0; fi
        adbs shell input swipe 558 1750 558 700 260
        sleep 0.4
    done
    echo "TIMEOUT scrollto $tag"; exit 1 ;;
  tap)
    ensure >/dev/null || exit 1
    tag="${1:?tag 必填}"
    picked=""
    for _ in $(seq 1 10); do
        if do_dump; then
            if picked="$(node_center "$tag")"; then
                [[ -n "$picked" ]] && break
            fi
        fi
        # 找不到就先滚一下（折叠线以下的主按钮很常见）
        adbs shell input swipe 558 1750 558 700 260
        sleep 0.4
    done
    [[ -z "$picked" ]] && { echo "[e2e] 未找到节点: $tag" >&2; exit 1; }
    read -r x y <<< "$picked"
    adbs shell input tap "$x" "$y"
    echo "tapped $tag @ $x,$y" ;;
  text)
    ensure >/dev/null || exit 1
    do_dump || exit 1
    node_query "${1:?tag 必填}" text ;;
  attr)
    ensure >/dev/null || exit 1
    do_dump || exit 1
    node_query "${1:?tag 必填}" "${2:?attr 必填}" ;;
  nomatch)
    ensure >/dev/null || exit 1
    do_dump || exit 1
    if grep -q "${1:?text 必填}" "$TMPXML"; then
        echo "FOUND(不应出现)"; exit 1
    else
        echo "ABSENT"; fi ;;
  db)
    ensure >/dev/null || exit 1
    dbpull ;;
  sql)
    ensure >/dev/null || exit 1
    f="$(dbpull)" || exit 1
    sqlite3 -header -column "$f" "${1:?query 必填}" ;;
  cleardata)
    ensure >/dev/null || exit 1
    adbs shell pm clear "$PKG" ;;
  grant)
    ensure >/dev/null || exit 1
    adbs shell pm grant "$PKG" android.permission.CAMERA 2>/dev/null
    adbs shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null
    echo "granted" ;;
  revoke)
    ensure >/dev/null || exit 1
    adbs shell pm revoke "$PKG" android.permission.CAMERA 2>/dev/null
    echo "revoked" ;;
  fontscale)
    ensure >/dev/null || exit 1
    adbs shell settings put system font_scale "${1:?value 必填}"
    adbs shell am force-stop "$PKG"
    echo "font_scale=${1}" ;;
  logs)
    ensure >/dev/null || exit 1
    adbs logcat -c
    adbs logcat -v brief "${1:-*:W}" ;;
  logcatch)
    ensure >/dev/null || exit 1
    adbs logcat -d -v brief "${1:-*:W}" ;;
  force-stop)
    ensure >/dev/null || exit 1
    adbs shell am force-stop "$PKG" ;;
  *)
    sed -n '2,45p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//' ;;
esac