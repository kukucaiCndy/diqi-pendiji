#!/usr/bin/env python3
"""校验拉下来的 SQLite 文件是否可读且完整。

为什么单独成文件：`tools/e2e.sh` 里已经有一层 heredoc，
再嵌一层 Python heredoc 会导致 bash 解析冲突（实际踩过）。
"""
import sqlite3
import sys


def main() -> int:
    if len(sys.argv) < 2:
        print("用法: dbcheck.py <db 文件>")
        return 2
    path = sys.argv[1]
    try:
        con = sqlite3.connect(f"file:{path}?mode=ro", uri=True)
        result = con.execute("PRAGMA integrity_check").fetchone()[0]
        tables = [
            r[0]
            for r in con.execute(
                "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
            ).fetchall()
        ]
        con.close()
    except Exception as exc:  # noqa: BLE001
        print(f"ERR:{exc}")
        return 1
    if result != "ok":
        print(f"ERR:integrity_check={result}")
        return 1
    print("ok:" + ",".join(tables))
    return 0


if __name__ == "__main__":
    sys.exit(main())