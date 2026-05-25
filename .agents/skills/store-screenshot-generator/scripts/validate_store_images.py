#!/usr/bin/env python3
from __future__ import annotations

import re
import struct
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[4]

EXPECTED_EXACT = {
    "android-store/feature-graphic.png": (1024, 500),
    "android-store/logo-512.png": (512, 512),
}

IPHONE_SIZES = {
    (1242, 2688),
    (2688, 1242),
    (1284, 2778),
    (2778, 1284),
}

IPAD_SIZES = {
    (2064, 2752),
    (2752, 2064),
    (2048, 2732),
    (2732, 2048),
}


def source_indices(source_dir: Path, stem: str) -> list[int]:
    """Return contiguous 1..N indices found for files named '<stem>-<N>.<ext>'."""
    if not source_dir.exists():
        return []
    pattern = re.compile(rf"^{re.escape(stem)}-(\d+)$")
    indices: set[int] = set()
    for child in source_dir.iterdir():
        if not child.is_file():
            continue
        match = pattern.match(child.stem)
        if match:
            indices.add(int(match.group(1)))
    return sorted(indices)


def build_dynamic_expectations() -> tuple[dict[str, tuple[int, int]], dict[str, set[tuple[int, int]]]]:
    android_src = ROOT / "android-screenshots"
    ios_src = ROOT / "ios-screenshots"

    android_constraints: dict[str, tuple[int, int]] = {}
    for stem, bounds in (("phone", (320, 3840)), ("tablet-7", (320, 3840)), ("tablet-10", (1080, 7680))):
        for index in source_indices(android_src, stem):
            android_constraints[f"android-store/{stem}-{index}.png"] = bounds

    ios_allowed: dict[str, set[tuple[int, int]]] = {}
    for stem, sizes in (("iphone", IPHONE_SIZES), ("ipad", IPAD_SIZES)):
        for index in source_indices(ios_src, stem):
            ios_allowed[f"ios-store/{stem}-{index}.png"] = sizes

    return android_constraints, ios_allowed


def png_size(path: Path) -> tuple[int, int]:
    with path.open("rb") as handle:
        header = handle.read(24)
    if len(header) < 24 or header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
        raise ValueError("not a PNG with an IHDR header")
    return struct.unpack(">II", header[16:24])


def has_store_aspect_ratio(size: tuple[int, int]) -> bool:
    width, height = size
    # Allow one-pixel rounding for dimensions such as 941x1672.
    return min(abs(width * 9 - height * 16), abs(width * 16 - height * 9)) <= max(width, height)


def main() -> int:
    failures: list[str] = []
    android_constraints, ios_allowed = build_dynamic_expectations()

    if not android_constraints and not ios_allowed:
        print(
            "Store image validation skipped: no source screenshots found in android-screenshots/ or ios-screenshots/."
        )

    for relative, expected in sorted(EXPECTED_EXACT.items()):
        path = ROOT / relative
        if not path.exists():
            failures.append(f"missing: {relative}")
            continue
        try:
            actual = png_size(path)
        except ValueError as exc:
            failures.append(f"invalid: {relative}: {exc}")
            continue
        if actual != expected:
            failures.append(
                f"wrong size: {relative}: expected {expected[0]}x{expected[1]}, got {actual[0]}x{actual[1]}"
            )

    for relative, (minimum, maximum) in sorted(android_constraints.items()):
        path = ROOT / relative
        if not path.exists():
            failures.append(f"missing: {relative}")
            continue
        try:
            actual = png_size(path)
        except ValueError as exc:
            failures.append(f"invalid: {relative}: {exc}")
            continue
        width, height = actual
        if not (minimum <= width <= maximum and minimum <= height <= maximum):
            failures.append(
                f"wrong size: {relative}: each side must be between {minimum}px and {maximum}px, got {width}x{height}"
            )
        if not has_store_aspect_ratio(actual):
            failures.append(f"wrong aspect ratio: {relative}: expected 16:9 or 9:16, got {width}x{height}")

    for relative, allowed in sorted(ios_allowed.items()):
        path = ROOT / relative
        if not path.exists():
            failures.append(f"missing: {relative}")
            continue
        try:
            actual = png_size(path)
        except ValueError as exc:
            failures.append(f"invalid: {relative}: {exc}")
            continue
        if actual not in allowed:
            allowed_text = ", ".join(f"{width}x{height}" for width, height in sorted(allowed))
            failures.append(f"wrong size: {relative}: expected one of {allowed_text}, got {actual[0]}x{actual[1]}")

    if failures:
        print("Store image validation failed:")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print(
        f"Store image validation passed: {len(EXPECTED_EXACT) + len(android_constraints) + len(ios_allowed)} PNG files checked."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
