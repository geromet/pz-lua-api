#!/usr/bin/env python3
"""
split_api.py — Split lua_api.json into a lightweight index and per-class detail files.

Produces:
  lua_api_index.json          — top-level metadata + summary entry per class
  lua_api_detail/<fqn>.json   — full class object per class (1 file per FQN)
"""
import json
import os
import pathlib
import sys

SRC = "lua_api.json"
INDEX_OUT = "lua_api_index.json"
DETAIL_DIR = pathlib.Path("lua_api_detail")

# Keys to preserve verbatim at the top level of the index
TOP_LEVEL_PRESERVE = [
    "global_functions",
    "_source_index",
    "_class_by_simple_name",
    "_source_only_paths",
    "_extends_map",
    "_interface_extends",
    "_interface_paths",
    "unresolved",
    "_meta",
]

# Per-class keys to keep in the index (summary only)
INDEX_CLASS_KEYS = {"set_exposed", "lua_tagged", "is_enum", "source_file", "simple_name"}


def main():
    print(f"Loading {SRC}…", flush=True)
    with open(SRC, encoding="utf-8") as f:
        api = json.load(f)

    classes = api.get("classes", {})
    print(f"  {len(classes)} classes found")

    # Build index: top-level keys + stripped class entries
    index = {}
    for key in TOP_LEVEL_PRESERVE:
        if key in api:
            index[key] = api[key]

    index_classes = {}
    for fqn, cls in classes.items():
        entry = {k: cls[k] for k in INDEX_CLASS_KEYS if k in cls}
        entry["method_count"] = len(cls.get("methods") or [])
        entry["field_count"] = len(cls.get("fields") or [])
        index_classes[fqn] = entry
    index["classes"] = index_classes

    print(f"Writing {INDEX_OUT}…", flush=True)
    with open(INDEX_OUT, "w", encoding="utf-8") as f:
        json.dump(index, f, separators=(",", ":"))
    idx_size = os.path.getsize(INDEX_OUT)
    src_size = os.path.getsize(SRC)
    print(f"  index size: {idx_size/1024:.1f} KB  ({idx_size/src_size*100:.1f}% of source)")

    # Write per-class detail files
    DETAIL_DIR.mkdir(exist_ok=True)
    print(f"Writing {len(classes)} detail files to {DETAIL_DIR}/…", flush=True)
    for fqn, cls in classes.items():
        dest = DETAIL_DIR / f"{fqn}.json"
        # Ensure parent directory exists (FQNs use dots, not slashes, so no subdirs needed)
        with open(dest, "w", encoding="utf-8") as f:
            json.dump(cls, f, separators=(",", ":"))

    written = len(list(DETAIL_DIR.glob("*.json")))
    print(f"  {written} detail files written")

    # Verify
    assert idx_size < src_size * 0.15, f"Index too large: {idx_size} >= 15% of {src_size}"
    assert written == len(classes), f"File count mismatch: {written} != {len(classes)}"
    print("Done. All assertions passed.")


if __name__ == "__main__":
    main()
