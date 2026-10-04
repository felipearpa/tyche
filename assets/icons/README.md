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

## Platform assets

| Canonical source | Platform | Asset | Derivation |
| --- | --- | --- | --- |
| `add.svg` | iOS | `iOS/UI/Sources/UI/Assets/Assets.xcassets/Icons/add.imageset/add.svg` (`SharedImageResource.add`) | Byte-identical copy. The image set renders as a template and preserves vector data. |
| `add.svg` | Android | `Android/ui/src/main/res/drawable/add.xml` (`com.felipearpa.tyche.ui.R.drawable.add`) | Vector drawable with the SVG's path data copied unchanged. A 960 × 960 viewport and a group with `translateY="960"` stand in for the SVG's `0 -960 960 960` viewBox. The fill is a placeholder; the consumer's `Icon` tint colours it. |
| `refresh.svg` | iOS | `iOS/UI/Sources/UI/Assets/Assets.xcassets/Icons/refresh.imageset/refresh.svg` (`SharedImageResource.refresh`) | Byte-identical copy. The image set renders as a template and preserves vector data. |
| `refresh.svg` | Android | `Android/ui/src/main/res/drawable/refresh.xml` (`com.felipearpa.tyche.ui.R.drawable.refresh`) | Vector drawable with the SVG's path data copied unchanged. A 960 × 960 viewport and a group with `translateY="960"` stand in for the SVG's `0 -960 960 960` viewBox. The fill is a placeholder; the consumer's `Icon` tint colours it. |
