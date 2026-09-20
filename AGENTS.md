# Tyche

## OpenSpec Spec Authoring

When authoring future OpenSpec specifications that introduce or change app-rendered icons:

- Prefer Material Symbols when a semantically accurate representation exists.
- Otherwise, allow a custom icon without requiring or recording a justification.
- Require iOS and Android to use assets derived from the same committed canonical vector source.
- Do not prescribe SF Symbols on iOS or Compose Material `Icons.*` on Android.
- Exclude controls rendered entirely by the operating system.
- Do not retroactively revise existing specifications solely to apply this rule.

When authoring future mobile UI specifications that introduce or change model-backed loading placeholders:

- Require each affected platform to populate the production item or row component with a placeholder model.
- Apply the existing shared platform shimmer treatment.
- Do not prescribe a separately maintained skeleton-only layout.
- Allow placeholder-specific behavior to suppress remote loading, navigation, and accessibility exposure, but not to replace the production layout.
- Do not expose placeholder values as real user content.
- Do not retroactively revise existing specifications solely to apply this rule.
