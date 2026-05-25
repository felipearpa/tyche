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

- Full-bleed bright brand-color background ( #1B5E20 and #FFC107 ).
- Large, bold, centered headline near the top.
- Main phone screenshot/device crop below the headline, with rounded device corners.
- The screenshot is oversized and cropped by the bottom edge when useful.
- Optional sticker-like app icons or UI motifs peek from the left/right edges.
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

- `android-store/feature-graphic.png`: 1024 x 500. Compose it from two app screenshots, usually side by side or overlapping, with enough headline/brand space to remain readable.
- `android-store/logo-512.png`: 512 x 512
- `android-store/phone-N.png` for each `android-screenshots/phone-N.*`: 16:9 or 9:16 aspect ratio; each side must be between 320 px and 3,840 px.
- `android-store/tablet-7-N.png` for each `android-screenshots/tablet-7-N.*`: 16:9 or 9:16 aspect ratio; each side must be between 320 px and 3,840 px.
- `android-store/tablet-10-N.png` for each `android-screenshots/tablet-10-N.*`: 16:9 or 9:16 aspect ratio; each side must be between 1,080 px and 7,680 px.

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

- Use a green brand background, preferably a subtle green radial or vignette treatment. Use a center stop of `#2E7D32` and an edge stop of `#0C2F0E` so the background stays consistent across runs.
- Use black or white bold headline text with strong contrast.
- Feature dark-mode app UI screenshots inside realistic device frames when available.
- Favor focused sports-prediction messages such as "Make your predictions", "Create your pools", "Compete with friends", and "Track every result".
- Use Spanish UI screenshots if the current source screenshots are Spanish.
- Keep text large enough to remain readable in store thumbnails.
- Place the headline in the top 10-18% of the canvas.
- Use a two-line headline when it improves rhythm.
- Place the device/screenshot between roughly 18-100% of canvas height.
- Allow the device to crop off the bottom edge.
- Use decorative side elements sparingly and never over the headline or critical UI.

## Workflow

1. Inventory source screenshots with `find android-screenshots ios-screenshots -maxdepth 2 -type f`.
2. Inventory current output assets with `file android-store/*.png ios-store/*.png`.
3. Inspect source screenshots visually and map them to the four store story frames.
4. Inspect representative existing store images before changing them.
5. Decide the story frames for phone/iPhone first (one per source screenshot); derive tablet/iPad compositions from the matching frames.
6. Compose the Android feature graphic from two source screenshots.
7. Generate or compose images at final pixel dimensions, not by scaling a smaller final.
8. Size each device frame to match its source screenshot's aspect ratio, not a fixed phone aspect.
9. Preserve output filenames exactly.
10. Run the validator:

```bash
python3 .agents/skills/store-screenshot-generator/scripts/validate_store_images.py
```

11. Visually inspect at least one Android phone, one iPhone, one tablet, one iPad, and the feature graphic.

## Quality Bar

- No clipped headline text.
- No unreadable UI text caused by excessive downscaling.
- No low-resolution screenshots stretched beyond their useful size.
- No browser chrome, simulator controls, cursors, or capture artifacts.
- No accidental transparency for screenshots; PNGs should be RGB or fully opaque RGBA.
- Store assets should look like final marketing screenshots, not raw simulator captures.

## When using image generation

Use generated bitmap assets only for backgrounds, decorative side stickers, or final marketing composites. Do not invent inaccurate app UI if real screenshots are available. If UI must be generated because screenshots are missing, make it clearly consistent with the app's existing screens and verify the result visually.
