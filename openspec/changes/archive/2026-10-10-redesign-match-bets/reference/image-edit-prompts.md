# Reference image edit prompts

Built-in imagegen edits, 2026-10-07. Each request used its corresponding preserved original as the single referenced image and `transparent_background: false`.

## finished

Input: `originals/match-bets-finished.png`

Use case: precise-object-edit. Image 1 is the edit target: a dark mobile match-bets UI reference. Repair only missing icons, preserving the portrait dimensions, composition, black canvas, device outline, all text, team flags, participant order, prediction values and point pills exactly. Replace the solid white square inside the top-left circular control with a crisp white back chevron. Replace the white square inside the top-right circular control with a crisp white outlined home icon. Replace the solid white status-bar block just left of the existing battery with small recognizable white signal/Wi-Fi status icons. Keep the existing battery and 11:43 time. Do not alter text, crop, rows, or scores. This is an icon-repaired reference screenshot, not a new design. No solid placeholder squares should remain in icon positions.

## revealed

Input: `originals/match-bets-revealed.png`

Use case: precise-object-edit. Image 1 is the edit target, a dark mobile match-bets UI with an In progress pill, Spain and Argentina and a participant list. Repair the missing top chrome and icons only. At the top restore the 11:43 clock, white signal/Wi-Fi and battery indicators. Above the In progress pill restore a centered Score title, with a dark circular Back control on the left containing a crisp white back chevron and a matching dark circular Home control on the right containing a crisp white outlined house. If those elements already exist, keep them and replace only any solid white missing-icon blocks. Preserve all other pixels and design as closely as possible: dimensions, black background, rounded device outline, In progress pill, flags, names, predictions, all list order and separators. Do not add points pills. Do not crop or redesign. No placeholder squares.

## hidden

Input: `originals/match-bets-hidden.png`

Use case: precise-object-edit. Image 1 is the edit target, a dark mobile match-bets hidden-state reference. Repair only missing icons. Replace the white square inside the top-left circular control with a white back chevron; replace the white square in the top-right circle with a white outlined home icon; replace the white status block left of the battery with small white signal/Wi-Fi icons. Replace the large gray square directly above Bets are hidden with a centered, recognizable gray outlined closed-padlock icon, similar overall occupied size, no square background. Preserve exact portrait dimensions, outer label 1a Apuestas de un partido, device outline, time, Score title, Starts Sunday, Jul 19 · 2:00 PM pill, flags, team names, VS, every existing word of body copy, Your bet 3 – 2, all spacing and black canvas. Do not change reveal wording or score values. No missing-icon rectangles remain.
