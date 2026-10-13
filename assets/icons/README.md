# Icons

Canonical vector sources for app-rendered icons that iOS and Android share. Each platform's asset is derived from the file here, so the glyph geometry is the same on both. Tint, size, and the surrounding control belong to each platform.

## Sources

### `add.svg`

- Icon: Material Symbols "add".
- Style: Outlined, fill 0, weight 400, grade 0, optical size 24.
- Source: <https://github.com/google/material-design-icons/blob/bd8cb85bd4bad964fe6918f79665bb40c3a8efef/symbols/web/add/materialsymbolsoutlined/add_24px.svg> (commit `bd8cb85bd4bad964fe6918f79665bb40c3a8efef`), downloaded 2026-09-27. The file is unmodified. The same glyph is served at <https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsoutlined/add/default/24px.svg>.
- SHA-256: `e9532de2b9be01ce951e899e98ee673bb97acd574f0db118feb72fe074a1c376`
- License: Apache License 2.0, copyright Google. The full text is in [`MATERIAL_SYMBOLS_LICENSE.txt`](MATERIAL_SYMBOLS_LICENSE.txt), copied from the same commit.

### `refresh.svg`

- Icon: Material Symbols "refresh".
- Style: Outlined, fill 0, weight 400, grade 0, optical size 24.
- Source: <https://github.com/google/material-design-icons/blob/bd8cb85bd4bad964fe6918f79665bb40c3a8efef/symbols/web/refresh/materialsymbolsoutlined/refresh_24px.svg> (commit `bd8cb85bd4bad964fe6918f79665bb40c3a8efef`), downloaded 2026-10-04. The file is unmodified. The same glyph is served at <https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsoutlined/refresh/default/24px.svg>.
- SHA-256: `e56d7ee330abd6d410cca7778419c524cebacf300c3b6e1b97ca9d0f3c3c1f7c`
- License: Apache License 2.0, copyright Google. The full text is in [`MATERIAL_SYMBOLS_LICENSE.txt`](MATERIAL_SYMBOLS_LICENSE.txt).

### `home.svg`

- Icon: Material Symbols "home".
- Style: Outlined, fill 0, weight 400, grade 0, optical size 24.
- Source: <https://github.com/google/material-design-icons/blob/bd8cb85bd4bad964fe6918f79665bb40c3a8efef/symbols/web/home/materialsymbolsoutlined/home_24px.svg> (commit `bd8cb85bd4bad964fe6918f79665bb40c3a8efef`), downloaded 2026-10-10. The file is unmodified. The same glyph is served at <https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsoutlined/home/default/24px.svg>. Its path is the geometry both platforms already used for Home.
- SHA-256: `41735f056fa43ed570585a294210f4c32dad1481bdf0a617b86b1007b2a8a913`
- License: Apache License 2.0, copyright Google. The full text is in [`MATERIAL_SYMBOLS_LICENSE.txt`](MATERIAL_SYMBOLS_LICENSE.txt).

### `lock.svg`

- Icon: Material Symbols "lock".
- Style: Outlined, fill 0, weight 400, grade 0, optical size 24.
- Source: <https://github.com/google/material-design-icons/blob/bd8cb85bd4bad964fe6918f79665bb40c3a8efef/symbols/web/lock/materialsymbolsoutlined/lock_24px.svg> (commit `bd8cb85bd4bad964fe6918f79665bb40c3a8efef`), downloaded 2026-10-10. The file is unmodified. Its path is the geometry both platforms already used for the lock. The fonts.gstatic.com copy of this symbol draws the keyhole with differently split arcs, so the repository copy is the source.
- SHA-256: `148a3da500ee74920d4f80984e8f73830b1f4e613bac095502d854b9c700315c`
- License: Apache License 2.0, copyright Google. The full text is in [`MATERIAL_SYMBOLS_LICENSE.txt`](MATERIAL_SYMBOLS_LICENSE.txt).

### `arrow_upward.svg`, `arrow_downward.svg`, `horizontal_rule.svg`

- Icons: Material Symbols "arrow_upward", "arrow_downward", and "horizontal_rule": the upward, downward, and steady rank-movement symbols that `TrendIndicator` draws on the leaderboard, My pools, and the History and Timeline group headers.
- Style: Outlined, fill 0, weight 400, grade 0, optical size 24.
- Source: the iOS image sets' existing `<name>_FILL0_wght400_GRAD0_opsz24.svg` files, the file names Material Symbols gives these exports, copied unchanged on 2026-10-12. They were not compared byte for byte with the upstream files in <https://github.com/google/material-design-icons> when they were moved here. The same glyphs are served at `https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsoutlined/<name>/default/24px.svg`.
- SHA-256:
  - `arrow_upward.svg`: `a902b4447496b87a48b3fe7be338a2e2b68ecf63341e0b90545f133b87207e87`
  - `arrow_downward.svg`: `480a48a9a11fcfe380851816636315f1e3b3ffc391fcd3f5f5135f6d46a62ee8`
  - `horizontal_rule.svg`: `e909c03a7cad4ca53b942f92ef9a7b47a79bc0aaa526684562536ac94d4bb385`
- License: Apache License 2.0, copyright Google. The full text is in [`MATERIAL_SYMBOLS_LICENSE.txt`](MATERIAL_SYMBOLS_LICENSE.txt).
- Android: `Android/ui/src/main/res/drawable/arrow_upward.xml`, `arrow_downward.xml`, and `horizontal_rule.xml`, derived the same way as `add.xml`. They replaced `ic_arrow_upward.xml`, `ic_arrow_downward.xml`, and `ic_horizontal_rule.xml`, which drew a different, rounded geometry.

## Platform assets

| Canonical source | Platform | Asset | Derivation |
| --- | --- | --- | --- |
| `add.svg` | iOS | `iOS/UI/Sources/UI/Assets/Assets.xcassets/Icons/add.imageset/add.svg` (`SharedImageResource.add`) | Byte-identical copy. The image set renders as a template and preserves vector data. |
| `add.svg` | Android | `Android/ui/src/main/res/drawable/add.xml` (`com.felipearpa.tyche.ui.R.drawable.add`) | Vector drawable with the SVG's path data copied unchanged. A 960 × 960 viewport and a group with `translateY="960"` stand in for the SVG's `0 -960 960 960` viewBox. The fill is a placeholder; the consumer's `Icon` tint colours it. |
| `refresh.svg` | iOS | `iOS/UI/Sources/UI/Assets/Assets.xcassets/Icons/refresh.imageset/refresh.svg` (`SharedImageResource.refresh`) | Byte-identical copy. The image set renders as a template and preserves vector data. |
| `refresh.svg` | Android | `Android/ui/src/main/res/drawable/refresh.xml` (`com.felipearpa.tyche.ui.R.drawable.refresh`) | Vector drawable with the SVG's path data copied unchanged. A 960 × 960 viewport and a group with `translateY="960"` stand in for the SVG's `0 -960 960 960` viewBox. The fill is a placeholder; the consumer's `Icon` tint colours it. |
| `home.svg` | iOS | `iOS/UI/Sources/UI/Assets/Assets.xcassets/Icons/home.imageset/home.svg` (`SharedImageResource.home`) | Byte-identical copy. The image set renders as a template and preserves vector data. |
| `home.svg` | Android | `Android/ui/src/main/res/drawable/home.xml` (`com.felipearpa.tyche.ui.R.drawable.home`) | Vector drawable with the SVG's path data copied unchanged. A 960 × 960 viewport and a group with `translateY="960"` stand in for the SVG's `0 -960 960 960` viewBox. It replaced a file that drew the same outline with the path pre-translated by 960, so the geometry did not change. The fill is a placeholder; the consumer's `Icon` tint colours it. |
| `lock.svg` | iOS | `iOS/UI/Sources/UI/Assets/Assets.xcassets/Icons/lock.imageset/lock.svg` (`SharedImageResource.lock`) | Byte-identical copy. The image set renders as a template and preserves vector data. |
| `lock.svg` | Android | `Android/ui/src/main/res/drawable/lock.xml` (`com.felipearpa.tyche.ui.R.drawable.lock`) | Vector drawable derived the same way as `home.xml`. It replaced a file that drew the same outline with the path pre-translated by 960. |
| `arrow_upward.svg` | iOS | `iOS/UI/Sources/UI/Assets/Assets.xcassets/Icons/arrow_upward.imageset/arrow_upward.svg` (`ImageResource.arrowUpward`, used by `TrendIndicator`) | Byte-identical copy. The image set renders as a template and preserves vector data. |
| `arrow_downward.svg` | iOS | `iOS/UI/Sources/UI/Assets/Assets.xcassets/Icons/arrow_downward.imageset/arrow_downward.svg` (`ImageResource.arrowDownward`, used by `TrendIndicator`) | Byte-identical copy. The image set renders as a template and preserves vector data. |
| `horizontal_rule.svg` | iOS | `iOS/UI/Sources/UI/Assets/Assets.xcassets/Icons/horizontal_rule.imageset/horizontal_rule.svg` (`ImageResource.horizontalRule`, used by `TrendIndicator`) | Byte-identical copy. The image set renders as a template and preserves vector data. |
| `arrow_upward.svg` | Android | `Android/ui/src/main/res/drawable/arrow_upward.xml` (`com.felipearpa.tyche.ui.R.drawable.arrow_upward`, used by `TrendIndicator`) | Vector drawable with the SVG's path data copied unchanged. A 960 × 960 viewport and a group with `translateY="960"` stand in for the SVG's `0 -960 960 960` viewBox. The fill is a placeholder; `TrendIndicator`'s `Icon` tint colours it. |
| `arrow_downward.svg` | Android | `Android/ui/src/main/res/drawable/arrow_downward.xml` (`com.felipearpa.tyche.ui.R.drawable.arrow_downward`, used by `TrendIndicator`) | Derived the same way as `arrow_upward.xml`. |
| `horizontal_rule.svg` | Android | `Android/ui/src/main/res/drawable/horizontal_rule.xml` (`com.felipearpa.tyche.ui.R.drawable.horizontal_rule`, used by `TrendIndicator`) | Derived the same way as `arrow_upward.xml`. |
