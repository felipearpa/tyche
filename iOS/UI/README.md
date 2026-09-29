# UI

Shared SwiftUI components, styles, and design tokens used by the Fortuna feature packages and the app target.

## Liquid Glass and control surfaces

Fortuna supports iOS and iPadOS 16 and later. Liquid Glass (iOS 26 and later) is reserved for navigation and standalone actions that float above content; content that people read or edit uses standard, opaque controls. Every iOS 26 API lives behind an `#available(iOS 26.0, *)` check in this package, so feature code picks a style by the control's role and never checks the OS version itself.

### Choosing a surface

| Surface | Treatment | Shared API |
| --- | --- | --- |
| Navigation bars, pool-home tab bar, ordinary toolbar actions | System material and scroll-edge effect. Add no glass modifier or bar background. | None: use `ToolbarItem` and native controls |
| Accent-filled toolbar action (create pool) | An accent-filled circle of at least 44 points in a `PlainToolbarItem`, with a hit area covering the whole circle. On iOS 26 and later it draws its own accent-tinted interactive glass circle over the hidden shared toolbar background: the one app-drawn glass exception in a bar, because the native prominent toolbar item only takes taps on a 32 × 36-point area. Below 26 it is an accent-filled circle. The glyph is the button's label: the image sized by `ToolbarProminentButtonStyle.glyphSize`, drawn in the on-accent color, with the title as the accessibility label. | `PlainToolbarItem` with `.toolbarProminent` |
| Toolbar account avatar | Keep the circular photo or letter fallback without the shared toolbar background. | `PlainToolbarItem` |
| Standalone action groups (welcome sign-in choices, email-link continuation and recovery, join confirmation and recovery) | Glass buttons; wrap adjacent glass buttons in one local container. Keep provider-supplied sign-in buttons outside it. | `.liquidGlassProminent`, `.liquidGlass`, `LiquidGlassContainer` |
| Text inputs: email, password, pool name, username, prediction scores | Standard opaque input. | `.textFieldStyle(.standard)`, `InputMetrics` |
| Inline form actions: sign-in, pool creation, username save and retry | Standard bordered controls: `.standardProminent` for the accent-filled primary action, `.bordered` for secondary ones. | `.standardProminent`, native `.bordered` |
| Actions inside list rows: prediction edit, save, cancel, retry; pool-row invite | Standard bordered controls that keep the row's existing geometry. The accent-filled prediction save uses `.standardProminent`. A secondary `.bordered` action without its own tint (prediction cancel, pool-row invite) uses `.tint(.primary)`: accent text lacks contrast on the bordered fill. Error-tinted actions (prediction failure cancel and retry) keep `.bordered` / `.borderedProminent` with the error tint. | `.standardProminent`, native SwiftUI button styles |
| Paging-error retry inside lists (My pools, leaderboard, manage gamblers) | Standard accent-filled control at the regular size, below the error message. | `LazyPagingVStackConcatenateError` (`.standardProminent`) |
| List rows, rank tiles, summaries, previews, drawer body | Existing surfaces and highlights; never glass. | None |
| System sheets (share and invitation) | Native presentation background and detents. Do not set `presentationBackground`. | None |

### Glass buttons

`LiquidGlassProminentButtonStyle` (`.liquidGlassProminent`) and `LiquidGlassButtonStyle` (`.liquidGlass`) use `.glassProminent` and `.glass` on iOS 26 and later, and fall back to `.borderedProminent` and `.bordered`. Primary-versus-secondary emphasis, the enabled state, and the action are the same on every version. The `.bordered` fallback of `.liquidGlass` uses `.tint(.primary)`, so its label is black or white like the glass label; accent text measures below 4.5:1 on the bordered fill. That tint is set inside the style, so a caller's `.tint` doesn't change the fallback. Both use the large control size, so a standalone action is at least 44 points tall at every Dynamic Type size (47 points at the smallest). Use them only for the standalone action groups in the table above. `.liquidGlassProminent` is accent-filled and uses the on-accent label described below.

### Accent-filled actions

The app accent (`AccentColor` in the app's asset catalog) is #2E7D32 in light appearance and #4CAF50 in dark appearance, with high-contrast variants of the same values. With Increase Contrast, the native glass and bordered styles still darken the drawn fill in light appearance and lighten it in dark appearance; the on-accent label measures at least 7:1 there. The system's white label measures 2.78:1 on #4CAF50, so accent-filled prominent actions draw their label in `onPrimary` (`Color(sharedResource: .onPrimary)`): white in light appearance (5.13:1 on #2E7D32) and black in dark appearance (7.56:1 on #4CAF50).

- `.standardProminent` (`StandardProminentButtonStyle`) is `.borderedProminent` with that label, at the inherited control size. Use it for accent-filled actions in forms and list content instead of `.borderedProminent`.
- `.liquidGlassProminent` applies the same label to its glass and bordered branches.
- `.toolbarProminent` (`ToolbarProminentButtonStyle`) applies the same label to the create-pool toolbar circle. Put it in a `PlainToolbarItem`: on iOS 26 the style draws its own glass, and the shared toolbar background would otherwise sit behind it.
- While disabled, both keep the system's disabled label and fill.
- Use them only for accent-filled actions. An action with another tint, such as an error-tinted retry, keeps `.borderedProminent` with the system label.

### Grouping adjacent glass controls

Wrap one layout container of adjacent glass buttons in `LiquidGlassContainer`:

```swift
LiquidGlassContainer {
    VStack(spacing: boxSpacing.small) {
        Button(action: onJoinPool) {
            Text(.joinPoolAction).frame(maxWidth: .infinity)
        }
        .buttonStyle(.liquidGlassProminent)

        Button(action: onAbort) {
            Text(.goToMyPoolsAction).frame(maxWidth: .infinity)
        }
        .buttonStyle(.liquidGlass)
    }
}
```

- On iOS 26 and later the content is placed in a `GlassEffectContainer` with a blending spacing of zero, so the buttons share one rendering pass but stay separate at rest, with their own full-size hit areas, as long as the stack leaves any gap between them.
- Below iOS 26 the content is returned unchanged: same layout, bindings, actions, and order.
- Pass a single stack, so the layout is decided by that stack on every OS version.
- Wrap only the adjacent glass controls. Don't wrap a whole screen, a native toolbar or tab bar, a single button, or a provider-supplied sign-in button.
- Don't add `glassEffectID`, namespaces, or morphing transitions; no screen needs them.
- The container adds no accessibility element, so VoiceOver still reaches each button directly.

### Standard text inputs

Apply `.textFieldStyle(.standard)` to `TextField` and `SecureField` inputs, or to a container of them. `StandardTextFieldStyle` pads the text by `InputMetrics.contentPadding` inside a capsule, on every OS version:

- Enabled: a `tertiarySystemFill` fill and a 1-point gray boundary. The boundary has at least 3:1 contrast with the screen background in light and dark appearances. The fill is the system's translucent input-field fill, so the style needs no information about the screen: on a plain background (white or black) and on a grouped background (`systemGroupedBackground`, such as the username screen), the field reads as a step darker in light appearance and a step lighter in dark appearance.
- Disabled: the same geometry and full-contrast text, so a draft stays readable while it is being saved, with no fill and a quiet `separator` boundary. The system announces the field as dimmed.

A tap anywhere inside the capsule, including the padding, focuses the field. The style attaches its own additional focus binding for this, so a consumer's `focused(_:)` binding still sets initial focus and reports the same state. Taps on the text line itself still go straight to the system field for caret placement and selection. Focus ownership, selection, bindings, keyboard type, and submit handling stay with the view that owns the field.

Layouts that size a field from its content use `InputMetrics` instead of hard-coding the padding. For example, prediction score cells are three digits wide plus `InputMetrics.contentPadding` on each side.
