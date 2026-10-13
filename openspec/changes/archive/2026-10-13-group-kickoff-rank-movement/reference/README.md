# Grouped history visual references

- [history-original.png](history-original.png): user-supplied design base, preserved unmodified.
- [history-grouped-icons.png](history-grouped-icons.png): edited design base with recognizable app icons replacing the solid placeholder blocks.

The edited image is a visual direction reference, not a pixel-exact production specification. Its grouped headers, trend pills, match rows, points, timeline rails, and bottom navigation preserve the source composition. It illustrates green upward movement, red downward movement, and the amber steady dash. The original sample text and values were retained; they are illustrative and do not establish scoring or date semantics.

App icon intent:

| Location | Icon intent |
| --- | --- |
| Change-pool action | `swap_horiz` |
| Positive group movement | `arrow_upward` |
| Negative group movement | `arrow_downward` |
| Unchanged group movement | Horizontal dash |
| Scores tab | `trophy` |
| Bets tab | `sports_score` |
| History tab | `history` |

The raster glyphs communicate semantic intent. They are not canonical production vector assets or evidence of exact Material Symbols geometry. Implementation must follow the change's icon requirements: prefer semantically accurate Material Symbols and derive iOS and Android assets from the same committed canonical vector source. The status-bar Wi-Fi replacement is a mockup repair; operating-system controls are excluded from the app icon requirements.

## Provenance

- Requested: 2026-10-12.
- Editing mode: built-in `image_gen.imagegen`, `precise-object-edit`; no CLI/API fallback and no manual pixel editing.
- Edit target: `codex-clipboard-ec0518a4-80d7-405f-bb28-d68774dbc58f.png` supplied by the user.
- Built-in source output: `/Users/felipe/.codex/generated_images/01a130c2-de99-71e2-a665-6157d94e8417/exec-513c5b62-5c31-4d4e-8b61-05aff35fee45.png`.
- Background: opaque, retaining the black design base.
- Visual review: all six broken app icon blocks and the status glyph were replaced; the existing steady dash remains. Team labels, scores, predictions, point values, grouped-header text, and selected History state remain legible. The generated edit resamples the original from 780 × 1688 to 853 × 1844 and has minor raster/style drift, so use the written design and production components for exact layout and behavior.

## Exact edit prompt

```text
Use case: precise-object-edit.
Asset type: repository design-reference screenshot for the Tyche mobile History screen.
Input image 1 is the EDIT TARGET, not a loose style reference. Make an extremely localized icon repair to this exact screenshot. Keep the same portrait aspect ratio, framing and resolution if possible. Do not redesign or regenerate the screen. Preserve every non-icon pixel as closely as possible: black background; outer rounded dark phone border; typography, baseline positions and sizes; user avatar and F; all original literal text including 10:44, History, +8 pts, earned, Sunday, Jul 19, 2:00 PM · 2 matches, Saturday, Jul 18, 4:00 PM, Wednesday, Jul 15, every team name, every prediction, every score and points value; all flag illustrations; separators; timeline vertical rails; top-right circular surface; every pill; bottom translucent capsule and selected green History segment; and all existing cropping/overlap near the bottom.
Change ONLY these broken solid-block icon placeholders, inside their current occupied icon boxes:
1. In the top-right circular button, replace the white square with a crisp white Material Symbols style swap_horiz icon, two parallel horizontal arrows in opposite directions, to represent changing the pool. Keep the circular button and center fixed.
2. In the Sunday group trend pill, replace the pale green square to the left of 3 with a pale green upward arrow (arrow_upward), same visual weight and box. Keep 3 and pill unchanged.
3. In the Saturday group trend pill, replace the red square to the left of 1 with a red downward arrow (arrow_downward), same visual weight and box. Keep 1 and pill unchanged.
4. Keep the Wednesday amber steady dash exactly as-is.
5. Above Scores in the bottom tab bar, replace the pale square with a crisp pale lavender-white Material Symbols style trophy (trophy). Keep label and tab bar unchanged.
6. Above Bets, replace the pale square with a crisp pale lavender-white sports scoreboard/sports_score icon; a simple checkered sports flag is appropriate for sports_score. Keep label and tab bar unchanged.
7. Above History, replace the green square with a crisp green Material Symbols style history icon, circular counterclockwise arrow around clock hands. Use the existing selected green color and keep label/selected capsule unchanged.
8. At the very top right, the small white block immediately before the outlined battery is a broken status glyph: replace that block with a tiny crisp white Wi-Fi status icon. Keep the outlined battery and 10:44 untouched.
All app icons should be coherent, simple 2D vector-like glyphs with consistent stroke/weight, no solid-square placeholders. Do not add text, legends, badges, devices, shadows, watermarks, new UI or new ornament. This is a surgical icon fix, not a new screen.
```

