# Design reference

Exported from the [Pool Gamblers Redesign canvas](https://claude.ai/artifact/D3jPJxiMWR6ThWnP7Wzig2) on 2026-10-16. The artboards are drawn as iOS dark-mode mockups at 390 × 844 and are the same set the Android change used; iOS follows them with its existing native bars, `.alert`, and shared theme colors. Light mode uses the same tokens' light values. Example names, emails, and avatar colors are illustrative.

| File | State it shows |
| --- | --- |
| `1-gamblers-list.png` | Loaded list: scrolling "Gamblers" heading, count line, owner row highlighted as the current user with "You" and "Owner", flat rows with separators. No Edit action, no swipe hint. |
| `2-swipe-to-remove.png` | A non-owner row dragged toward the leading edge revealing the error-colored background with the person-remove glyph and "Remove". |
| `3-confirm-removal.png` | The confirmation dialog over the list. Android keeps its Material `AlertDialog` and the existing title/message strings; the rounded sheet styling is iOS. |
| `4-removing-and-failure.png` | Two states at once: Jaime removing (row dimmed, ball spinner in place of the avatar, email kept) and Santiago failed (full opacity, error glyph + "Couldn't remove" in place of the email, "Retry" pill and ✕ dismiss button at the trailing edge). No banner. |
| `5-loading.png` | First load: placeholder count line and eight placeholder rows with the shared pulse. |
| `6-no-other-gamblers.png` | Only the owner in the pool: owner row followed by the centered "No other gamblers yet" state with the group glyph. |
| `current-1-list.webp`, `current-2-edit-mode.webp`, `current-3-swipe.webp` | The screen before this change (iOS), kept for comparison: blank top bar with Edit/Done, solid green owner row, minus controls in edit mode, trash swipe background. |
