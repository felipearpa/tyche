---
name: store-changelog
description: Use when adding a CHANGELOG.md entry or writing App Store / Google Play "What's New" release notes for a Tyche release (iOS or Android). Standardizes the changelog entry format and the store release-notes text derived from it — version headers, bullet voice/tone, and per-store character limits. Trigger on requests like "add a changelog entry", "release notes for the store", "what's new text".
---

# Store Changelog & Release Notes

One standard for (1) the per-platform `CHANGELOG.md` and (2) the store "What's New" text derived from it. Use it whenever you ship a version bump.

## Files (source of truth)

- iOS: `iOS/CHANGELOG.md`
- Android: `Android/CHANGELOG.md`

The changelog is the single source of truth; store release notes are **derived** from the entry for the version being shipped. This repo keeps no fastlane/metadata files — paste the store text directly into App Store Connect / Play Console.

## CHANGELOG.md format

```
# Changelog

## <version>

- <user-facing change>
- <user-facing change>

## <previous version>
...
```

Rules:
- First line is always `# Changelog`.
- Newest version on top. One `## <version>` header per release — **version only, no date, no build number** (e.g. `## 1.7.2`).
- The version **must match** the shipped `MARKETING_VERSION` (iOS, `iOS/Tyche/Tyche.xcodeproj/project.pbxproj`) / `versionName` (Android, `Android/app/build.gradle.kts`).
- Plain dash bullets (`- `). No nested bullets, no `Added/Fixed/Changed` section headings, no emoji, no markdown bold.
- Each bullet is one user-facing change written as a benefit: present tense, plain language, no class/file/API names and no jargon ("refactor", "null", "GSI", "view model"…).
- Order by importance: headline features first, smaller fixes last.
- Only list what a user can notice. Internal/infra changes get no bullet.
- **Bug fixes:** write the resolved symptom as a benefit sentence — do **not** prefix with `Fix:` or a bug id. (Older Android entries use a legacy `Fix:` prefix; drop it if you edit them, keep new entries prefix-free on both platforms.)

Voice:
- Good — "Swipe a gambler to remove them, with a confirmation prompt before they leave the pool"
- Good — "The Save button stays disabled until your prediction is complete, so you can't submit a bet with a missing score"
- Bad — "Fix PendingBetItemViewModel validation" (jargon, names code)
- Bad — "🐛 Bugfixes and improvements" (emoji, vague, not user-facing)

## Store release notes ("What's New")

Derive store notes from the version's changelog entry:
1. Start from that version's bullets.
2. Lead with the biggest change; you may merge several tiny fixes into one line.
3. **Plain text only** — stores don't render markdown. Use `•` or `- ` as a literal bullet and short lines.
4. Same tone as the changelog: present tense, benefit-first, no emoji, no code names.

Per-store limits & placement:
- **App Store** — "What's New in This Version" in App Store Connect, localized per language. Limit ~4000 chars. A 1–3 line note is fine for a maintenance release.
- **Google Play** — release notes in Play Console (or `changelogs/<versionCode>.txt`, wrapped in `<en-US>…</en-US>` tags, if fastlane is ever added). **Max 500 chars per language** — keep it tight.

Template (paste into the store console):
```
What's new in <version>

• <change 1>
• <change 2>
```

**Platform truth:** if a change only shipped on one platform (a fix that landed only in iOS or only in Android), write store notes only for that platform. Never claim a change that didn't ship there. Check the diff/commit scope before copying a bullet across platforms.

## Checklist before shipping
- [ ] `## <version>` added to the correct platform `CHANGELOG.md`, newest on top
- [ ] Version matches `MARKETING_VERSION` (iOS) / `versionName` (Android)
- [ ] Bullets are user-facing, present tense, no emoji, no code names, no `Fix:` prefix
- [ ] Store "What's New" derived from the entry; Google Play copy ≤ 500 chars
- [ ] Notes mention only changes that actually shipped on that platform
