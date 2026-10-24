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


def frame_visibility_targets() -> list[tuple[str, bool]]:
    """(relative_path, is_full_device) for device frames whose fit we can check.

    is_full_device=True means the whole device must be inside the canvas (no
    crop on any edge) — the landscape tablet-10 frames. The portrait frames may
    crop off the bottom, so only their top/left/right margins are checked.
    """
    targets: list[tuple[str, bool]] = []
    android_src = ROOT / "android-screenshots"
    ios_src = ROOT / "ios-screenshots"
    for stem, full in (("phone", False), ("tablet-7", False), ("tablet-10", True)):
        for index in source_indices(android_src, stem):
            targets.append((f"android-store/{stem}-{index}.png", full))
    for stem in ("iphone", "ipad"):
        for index in source_indices(ios_src, stem):
            targets.append((f"ios-store/{stem}-{index}.png", False))
    return targets


def check_frame_visibility(failures: list[str]) -> None:
    """Verify the device frame is completely visible and properly fitted.

    Detects the near-black device bezel and asserts it is not clipped by the
    canvas where it should not be: every frame must keep a margin on the top and
    both sides (so the headline gap and side margins survive and no UI is pushed
    off-canvas), full-device (landscape tablet-10) frames must also keep a bottom
    margin, and the device must fill a sensible fraction of the canvas. Catches
    regressions like a landscape tablet cropped at the bottom or a device that
    overflows/shrinks. Decorative edge stickers are ignored because they never
    form a full bezel-width dark band.
    """
    try:
        import numpy as np  # noqa: PLC0415
        from PIL import Image  # noqa: PLC0415
    except ImportError as exc:  # pragma: no cover - optional dependency
        missing = getattr(exc, "name", "numpy/Pillow")
        print(f"Frame-visibility checks skipped: install {missing} (numpy + Pillow) to enable them.")
        return

    dark_threshold = 40
    for relative, is_full_device in frame_visibility_targets():
        path = ROOT / relative
        if not path.exists():
            continue  # missing files are already reported by the size checks
        arr = np.asarray(Image.open(path).convert("RGB")).astype(int)
        height, width, _ = arr.shape
        dark = (
            (arr[:, :, 0] < dark_threshold)
            & (arr[:, :, 1] < dark_threshold)
            & (arr[:, :, 2] < dark_threshold)
        )
        row_dark = dark.sum(axis=1)
        col_dark = dark.sum(axis=0)
        if row_dark.max() < 0.2 * width:
            failures.append(f"no device frame detected: {relative}")
            continue
        # The vertical side rails are the columns that are dark over most of the
        # device's height; they give the left/right edges directly and, crucially,
        # the true bottom edge — when a device is cropped at the bottom its bottom
        # bezel is gone but the rails run to the canvas edge.
        cols = np.where(col_dark >= 0.5 * col_dark.max())[0]
        left, right = int(cols.min()), int(cols.max())
        rail_rows = np.where(dark[:, cols].any(axis=1))[0]
        top, bottom = int(rail_rows.min()), int(rail_rows.max())
        margin = max(2, round(width * 0.004))
        if top < margin:
            failures.append(f"device touches top edge (no headline gap): {relative}")
        if left < margin:
            failures.append(f"device clipped at left edge: {relative}")
        if right > width - 1 - margin:
            failures.append(f"device clipped at right edge: {relative}")
        if is_full_device and bottom > height - 1 - margin:
            failures.append(
                f"landscape tablet cropped at bottom (must be fully visible): {relative}"
            )
        device_width = right - left
        if device_width < 0.35 * width:
            failures.append(
                f"device too small / poor fit: {relative} "
                f"({device_width}px = {device_width / width:.0%} of canvas width)"
            )


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

    check_frame_visibility(failures)

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
