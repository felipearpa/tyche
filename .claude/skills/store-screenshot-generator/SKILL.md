---
name: store-screenshot-generator
description: Use when generating or refreshing this repository's Android/Google Play and iOS/App Store screenshot assets in android-store/ and ios-store/ from source screenshots placed in android-screenshots/ and ios-screenshots/, especially when matching the existing Tyche store-image style or the Facebook iOS App Store reference pattern with a full-bleed color background, large top headline, app screenshot/device crop, and optional side stickers.
---

# Store Screenshot Generator

Use this skill to create or update store listing images for this repo.

## Inputs to inspect first

Before generating, inspect the current assets:

- Source Android screenshots in `android-screenshots/`
- Source iOS screenshots in `ios-screenshots/`
- `android-store/`
- `ios-store/`

Generate store images from screenshots placed in `android-screenshots/` and `ios-screenshots/`. Treat `android-store/` and `ios-store/` as output folders, not source folders. If a needed screen is missing from the screenshot folders, ask for it or capture it before inventing UI.

Use always this pattern:

- Full-bleed solid brand-green background (#4CAF50, the light-theme primary), in the flat single-color style of the Facebook iOS reference — not a gradient.
- Large, bold, centered headline near the top.
- Main phone screenshot/device crop below the headline, with rounded device corners.
- The screenshot is oversized and cropped by the bottom edge when useful.
- Sticker-like motifs peek in from the left/right edges — vary them per image (like the random stickers in the Facebook reference), using football/competition motifs such as ⚽ 🏆 🥇 🎯 🎉. Do not repeat the same app icon on every frame.
- Clean composition: one product message per image, minimal copy, no explanatory paragraphs.

Bundled examples live in `assets/`:

- `assets/facebook-ios-1.webp`: Marketplace layout with headline, tall white app screen, and side sticker.
- `assets/facebook-ios-2.webp`: Groups layout with full-width image header inside the device and side stickers.
- `assets/facebook-ios-3.webp`: Events layout with top headline, large rounded app screen, and bottom-left sticker.
- `assets/facebook-ios-4.webp`: Home feed layout with top headline, app brand visible inside the device, and right-side sticker.

Inspect these assets visually when matching that style; do not require the original Desktop files.

## Required output files

Preserve these names and dimensions unless the user asks otherwise.

The number of output images per device category is **driven by the source folder**: for every `phone-N.*` in `android-screenshots/` produce a `phone-N.png` in `android-store/`, and the same one-to-one rule applies to `tablet-7-N`, `tablet-10-N`, `iphone-N`, and `ipad-N`. Do not cap at 4 — generate as many as exist in the source folders, and do not generate outputs for indices that have no source. Indices must be contiguous starting at 1.

Android:

- `android-store/feature-graphic.png`: 1024 x 500. Compose it from two app screenshots, usually side by side or overlapping, with enough headline/brand space to remain readable. **Round the corners of every screenshot panel** — clip each screenshot to a rounded rectangle (no square/sharp screenshot edges), even when no full device bezel is drawn.
- `android-store/logo-512.png`: 512 x 512
- `android-store/phone-N.png` for each `android-screenshots/phone-N.*`: 16:9 or 9:16 aspect ratio; each side must be between 320 px and 3,840 px.
- `android-store/tablet-7-N.png` for each `android-screenshots/tablet-7-N.*`: 16:9 or 9:16 aspect ratio; each side must be between 320 px and 3,840 px.
- `android-store/tablet-10-N.png` for each `android-screenshots/tablet-10-N.*`: 16:9 or 9:16 aspect ratio; each side must be between 1,080 px and 7,680 px. The tablet-10 source is **landscape**, so this output is landscape (e.g. 1920×1080). Show the **whole device, fully visible — do not crop it at the bottom** (unlike the portrait frames). Size the landscape device to fit within the canvas height that remains beneath the headline; a centered device with green side margins is expected and fine. Fill its screen edge-to-edge so a bottom navigation bar sits flush at the bottom (see Device frame).

iOS:

- `ios-store/iphone-N.png` for each `ios-screenshots/iphone-N.*`: any valid iPhone App Store screenshot size:
  - 1242 x 2688
  - 2688 x 1242
  - 1284 x 2778
  - 2778 x 1284
- `ios-store/ipad-N.png` for each `ios-screenshots/ipad-N.*`: any valid iPad App Store screenshot size:
  - 2064 x 2752
  - 2752 x 2064
  - 2048 x 2732
  - 2732 x 2048

Keep existing QR SVG files unless the user explicitly asks to regenerate them:

- `android-store/play-store-qr.svg`
- `ios-store/app-store-qr.svg`

## Design Direction

Match the current Tyche visual system:

- Use a flat, solid brand-green background (`#4CAF50`, the light-theme primary) so the background stays consistent across runs — match the Facebook iOS reference's single-color treatment, not a gradient.
- Use white bold headline text for strong contrast on the green. Use a title only — no subtitle.
- Feature dark-mode app UI screenshots inside realistic device frames when available.
- Favor focused sports-prediction messages such as "Make your predictions", "Create your pools", "Compete with friends", and "Track every result".
- Use Spanish UI screenshots if the current source screenshots are Spanish.
- Keep text large enough to remain readable in store thumbnails.
- Place the headline in the top 10-18% of the canvas.
- Use a two-line headline when it improves rhythm.
- Position the device's top by measuring the actual bottom of the headline and leaving a clear gap (≈3-4% of canvas height) before the device — do not use a fixed canvas fraction, or the headline ends up touching/overlapping the bezel.
- Allow the device to crop off the bottom edge for **portrait** frames only; the landscape tablet-10 frame shows the full device with no crop (see the tablet-10 note above).
- Use decorative side stickers sparingly and never over the headline or critical UI; pick a different, content-relevant sticker per image rather than reusing the app icon.

## Device frame

Every screenshot is shown with **rounded borders, never square corners** — whether inside a full device bezel (the phone/tablet/iPad frames) or as a bare panel (the feature graphic). Clip the screenshot to a rounded rectangle in all cases.

Present each screenshot inside a flat black bezel with rounded corners and a soft drop shadow — the frame used across the current store assets. All values are relative to the device's outer width `Wd`, so frames scale cleanly from phone to iPad:

- Bezel color: near-black `#111113`; bezel thickness `round(Wd * 0.022)` (min 6 px).
- Default outer corner radius `round(Wd * 0.085)`; inner (screenshot) radius `outer_radius - thickness`. Treat this as a starting point and reduce it per *Size the corner radius to the content* below.
- Resize the screenshot to the inner width and clip it to a rounded rectangle at the inner radius; its height follows the source aspect ratio.
- **Fill the screen edge-to-edge.** The clipped screenshot must reach the bezel on every side — never pad it with a background-coloured margin to make corners or edges "safe". A background border makes the screenshot float instead of filling the screen, and lifts any bottom-anchored UI (a navigation bar) off the bottom so it reads as a stray divider above empty space.
- **Size the corner radius to the content.** Rounded corners must not cut into UI. The default radius suits sources that have a status-bar / safe-area margin; when the source content runs into its corners (a header, a corner action button, edge-aligned rows, a full-width bottom bar) shrink the radius until the corner arc clears the nearest content pixel.
- **Trim stray edge lines.** Crop any thin full-width border hairline at the very top or bottom edge of the source before framing; once the screen is filled edge-to-edge such a line otherwise sits at the screen edge as a false divider.
- Drop shadow: a black silhouette of the framed device at ~35% opacity (`alpha 90`), offset down `round(Wd * 0.02)`, Gaussian-blurred by ~1.2% of the canvas width, composited behind the device.
- Build the frame at final pixel size, paste it onto the solid-green canvas, and let it crop off the bottom edge (portrait frames only; the landscape tablet-10 device must fit fully inside the canvas — never crop a landscape tablet). Place the device top with a clear gap below the headline (see Design Direction).

## Workflow

1. Inventory source screenshots with `find android-screenshots ios-screenshots -maxdepth 2 -type f`.
2. Inventory current output assets with `file android-store/*.png ios-store/*.png`.
3. Inspect source screenshots visually and map them to the four store story frames.
4. Inspect representative existing store images before changing them.
5. Decide the story frames for phone/iPhone first (one per source screenshot); derive tablet/iPad compositions from the matching frames.
6. Compose the Android feature graphic from two source screenshots.
7. Generate or compose images at final pixel dimensions, not by scaling a smaller final.
8. Size each device frame to match its source screenshot's aspect ratio, not a fixed phone aspect (see Device frame).
9. Preserve output filenames exactly.
10. **Validate dimensions and frame fit.** Run the validator — it checks every output's size/aspect **and** that each device frame is completely visible and properly fitted: not clipped by the canvas (top + both sides for every frame, all four sides for the landscape tablet-10), and large enough to fill a sensible share of the canvas. Decorative edge stickers are ignored. (The frame-fit checks need `numpy` + `Pillow`; the validator prints a notice and skips them if either is missing.)

```bash
python3 .agents/skills/store-screenshot-generator/scripts/validate_store_images.py
```

11. **Inspect that the screenshot fits the frame** — the part automation can't see. Open at least one Android phone, one 7" tablet, one 10" tablet, one iPhone, one iPad, and the feature graphic, and for each confirm:
    - The app screenshot **fills the screen edge-to-edge** and is **completely visible** — it reaches the bezel on every side (no background border), and no UI is clipped by the rounded corners. Look hardest at whatever hugs the edges/corners (headers, corner buttons, edge-aligned rows, a bottom navigation bar); if anything is clipped, shrink the corner radius (see Device frame).
    - Bottom-anchored UI (a navigation bar) sits **flush at the bottom**, and no **stray edge hairline/divider** floats inside the frame (see Device frame → *Trim stray edge lines*).
    - Full-device frames (the landscape tablet-10) are shown in full with all four sides inside the canvas; portrait frames may crop only the bottom.
    - The screenshot **fits properly**: correct aspect ratio (no stretching), no awkward empty bands, and the headline does not touch the bezel.

## Quality Bar

- Every screenshot has rounded corners — device frames and bare screenshot panels (including the feature graphic) are clipped to rounded rectangles, never square/sharp edges.
- Each app screenshot is completely visible and properly fitted: no UI clipped by the rounded corners or pushed off-canvas, and no stray edge hairline/divider floating inside the frame.
- The landscape tablet-10 device is fully visible (never cropped on any edge); portrait frames crop only the bottom.
- No clipped headline text.
- No unreadable UI text caused by excessive downscaling.
- No low-resolution screenshots stretched beyond their useful size.
- No browser chrome, simulator controls, cursors, or capture artifacts.
- No accidental transparency for screenshots; PNGs should be RGB or fully opaque RGBA.
- Store assets should look like final marketing screenshots, not raw simulator captures.

## When using image generation

Use generated bitmap assets only for backgrounds, decorative side stickers, or final marketing composites. Do not invent inaccurate app UI if real screenshots are available. If UI must be generated because screenshots are missing, make it clearly consistent with the app's existing screens and verify the result visually.
