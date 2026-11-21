#!/usr/bin/env python3
"""
`.pen` 设计稿读取工具（Android 版 workaround）。

## 为什么有这个脚本

「Pencil → 小程序」的 skill 用 Pencil MCP 的 `batch_get / snapshot_layout /
get_screenshot` 来读设计稿。本项目没有 MCP，但 `design/desing.pen` 是磁盘上的
JSON —— 直接解析它**比截图目测更严格**：数值是精确的，不存在"看起来差不多"。

## skill 里的 ① 步在这里怎么对应

| skill 的调用 | 本工具 |
| --- | --- |
| `get_editor_state` | `pages` 子命令：列出顶层 Frame（页面）及其尺寸 |
| `batch_get(readDepth:8, resolveVariables:true)` | `dump` 子命令：递归整棵树并**解析设计变量** |
| `snapshot_layout(maxDepth:6)` | `rects` 子命令：输出每个节点的绝对 x/y/w/h |
| `get_variables` | `vars` 子命令 |
| `get_screenshot` | **没有对应物** —— `.pen` 不含渲染图，无法出图。见文件末尾说明 |

## 为什么不存在"截断检测"

skill 的 1.2 步要求检测 `"..."` 截断，那是 MCP 为了省 token 截断返回值造成的。
直接读 JSON 不存在截断，等价于 readDepth 无穷大。但**不完整**是另一回事：
本工具在 `dump` 时会统计「有尺寸但无 fill」「有文字但无 fontSize」这类**缺值**，
缺值必须显式报告，不允许静默补默认值。
"""
import json
import sys

PEN = "design/desing.pen"

FILL_PROPS = ("fill", "color", "stroke", "strokeFill")
GEOM_PROPS = ("x", "y", "width", "height")
TEXT_PROPS = ("content", "text", "fontSize", "fontFamily", "fontWeight")


def load():
    with open(PEN) as f:
        return json.load(f)


def resolve_vars(doc):
    """把 variables 里的 token 展开成 hex，模拟 resolveVariables:true。"""
    out = {}
    for name, v in (doc.get("variables") or {}).items():
        if isinstance(v, dict):
            out[name] = v.get("value") or v.get("hex") or v
        else:
            out[name] = v
    return out


def resolve_color(val, variables):
    """`$brand` → 实际 hex。skill 明确要求颜色不能是变量引用。"""
    if isinstance(val, str) and val.startswith("$"):
        key = val[1:]
        got = variables.get(key)
        if isinstance(got, dict):
            return got.get("value") or got.get("hex") or f"<未解析:{key}>"
        return got if got is not None else f"<未解析:{key}>"
    return val


def walk(node, depth=0, path="", rect=None, out=None, missing=None):
    """递归遍历。rect 累积绝对坐标，对齐 snapshot_layout 的语义。"""
    if out is None:
        out = []
        missing = []

    t = node.get("type", "?")
    name = node.get("name") or node.get("id") or ""
    here = f"{path}/{name}" if name else path
    x = node.get("x", 0) or 0
    y = node.get("y", 0) or 0
    w = node.get("width")
    h = node.get("height")

    ax = (rect[0] + x) if rect else x
    ay = (rect[1] + y) if rect else y

    rec = {
        "depth": depth,
        "path": here,
        "type": t,
        "id": node.get("id"),
        "name": name,
        "x": ax,
        "y": ay,
        "w": w,
        "h": h,
    }
    for k in (
        "layout", "gap", "padding", "cornerRadius",
        "fontSize", "fontWeight", "fontFamily", "content", "text",
        "fill", "color", "stroke", "strokeWidth", "align", "justify",
        "opacity", "effect", "icon", "library", "clip",
    ):
        if k in node and node[k] not in (None, ""):
            rec[k] = node[k]

    # 缺值统计：有尺寸却没填色 / 有文字却没字号 —— skill 不允许静默补默认值
    if t in ("frame", "rectangle", "ellipse") and "fill" not in rec:
        missing.append(f"{here} | {t} 无 fill")
    if t in ("text", "textBlock") and "fontSize" not in rec:
        missing.append(f"{here} | 文本无 fontSize")

    out.append(rec)
    for c in node.get("children") or []:
        walk(c, depth + 1, here, (ax, ay), out, missing)
    return out, missing


def cmd_pages():
    doc = load()
    root = doc["children"]
    print(f"{'idx':<4}{'id':<10}{'name':<28}{'w':>6}{'h':>7}  children")
    for i, n in enumerate(root):
        print(
            f"{i:<4}{str(n.get('id'))[:9]:<10}{(n.get('name') or '')[:27]:<28}"
            f"{n.get('width')!s:>6}{n.get('height')!s:>7}  {len(n.get('children') or [])}"
        )


def cmd_vars():
    doc = load()
    for k, v in resolve_vars(doc).items():
        print(f"{k:<10} {v}")


def find_page(doc, key):
    for n in doc["children"]:
        if key in (str(n.get("id")), n.get("name")):
            return n
    for n in doc["children"]:
        if key and key.lower() in (n.get("name") or "").lower():
            return n
    return None


def cmd_dump(key):
    doc = load()
    page = find_page(doc, key)
    if page is None:
        print(f"未找到页面 {key}；用 pages 子命令看列表", file=sys.stderr)
        return 1
    variables = resolve_vars(doc)
    recs, missing = walk(page)
    for r in recs:
        cols = []
        if r.get("x") or r.get("y"):
            cols.append(f"@{r['x']},{r['y']}")
        if r.get("w") is not None or r.get("h") is not None:
            cols.append(f"{r.get('w')}x{r.get('h')}")
        if "fill" in r:
            cols.append(f"fill={resolve_color(r['fill'], variables)}")
        if "color" in r:
            cols.append(f"color={resolve_color(r['color'], variables)}")
        if "stroke" in r:
            cols.append(f"stroke={resolve_color(r['stroke'], variables)}")
        for k in ("fontSize", "fontWeight", "fontFamily", "layout", "gap",
                  "padding", "cornerRadius", "align", "justify", "opacity"):
            if k in r:
                cols.append(f"{k}={r[k]}")
        for k in ("content", "text"):
            if k in r:
                cols.append(f'"{r[k]}"')
        if "icon" in r:
            cols.append(f"icon={r.get('library')}:{r['icon']}")
        print(f"{'  ' * r['depth']}[{r['type']}] {' '.join(cols)}")

    print(f"\n--- 缺值报告（{len(missing)} 条）---")
    for m in missing:
        print("  " + m)
    return 0


def cmd_rects(key):
    """等价 snapshot_layout：只出有几何信息的节点，供间距交叉计算。"""
    doc = load()
    page = find_page(doc, key)
    if page is None:
        print("未找到页面", file=sys.stderr)
        return 1
    recs, _ = walk(page)
    print(f"{'path':<58}{'x':>6}{'y':>6}{'w':>7}{'h':>6}")
    for r in recs:
        print(f"{r['path'][:57]:<58}{r['x']:>6}{r['y']:>6}{str(r.get('w')):>7}{str(r.get('h')):>6}")
    return 0


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    cmd = sys.argv[1]
    if cmd == "pages":
        cmd_pages()
    elif cmd == "vars":
        cmd_vars()
    elif cmd == "dump":
        return cmd_dump(sys.argv[2] if len(sys.argv) > 2 else "0")
    elif cmd == "rects":
        return cmd_rects(sys.argv[2] if len(sys.argv) > 2 else "0")
    else:
        print(f"未知子命令 {cmd}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())