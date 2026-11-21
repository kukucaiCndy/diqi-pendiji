"""紧凑打印某个页面 Frame 的规格（调试用）。"""
import sys
sys.path.insert(0, 'tools')
from pen import load, find_page, walk, resolve_vars, resolve_color

doc = load(); V = resolve_vars(doc)
page = find_page(doc, 'W7XDia')
keys = sys.argv[1].split(',')
for row in page['children']:
    if row.get('type') != 'frame':
        continue
    for f in row.get('children') or []:
        nm = f.get('name') or ''
        if not any(k in nm for k in keys):
            continue
        print(f"===== {nm} =====")
        print("说明:", (f['children'][1].get('content') or '')[:110])
        recs, _ = walk(f['children'][2])
        for r in recs:
            a = []
            for k in ('layout', 'gap', 'padding', 'cornerRadius', 'fontSize', 'fontWeight'):
                if k in r:
                    a.append(f"{k}={r[k]}")
            for k in ('fill', 'color', 'stroke'):
                if k in r:
                    a.append(f"{k}={resolve_color(r[k], V)}")
            if 'icon' in r:
                a.append("icon=" + str(r.get('icon')))
            t = ''
            if 'content' in r:
                t = ' <<' + str(r['content']) + '>>'
            print('  ' * (r['depth'] - 1) + f"{r['type']}:{r['name']} {r.get('w')}x{r.get('h')} " + ' '.join(a) + t)
