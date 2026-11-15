# Nooklin Photos: design audit (30 Oct 2026)

Checked against the App Ontwerp-Handleiding (updated 30 Oct 2026). Source reading only, nothing measured on a device. Applied in v192 unless noted.

## Applied in v192
| Guide | Finding | Change |
| --- | --- | --- |
| 1 Tokens | Only radius, grid, accent were tokens; 28 color literals | `ui/theme/Tokens.kt`: Spacing, AppText, Motion, AppColors, MinTouchTarget; color literals replaced (the user color palette in `AppPreferences` stays, it is the palette itself) |
| 2 Type and spacing | Text 11/13 sp, spacing 5/6/10/14/20/28 dp | Text sizes map to Caption 12, Body 14, BodyLarge 16, Headline 28. Padding and spacedBy values off the grid mapped to the grid |
| 3 Contrast | White on #FF3B30 is 3.55:1 | #D70015 (5.38:1) for buttons with white text; destructive text uses it on light grounds; hint alpha 0.6 to 0.7 |
| 4 Touch | 32 dp chevrons (Home, Hidden Home), 28 dp plus (People), 22 dp (Discovered), 40 dp swatches, 44 dp video buttons | `Modifier.touchTarget()` and 48 dp boxes; visuals unchanged |
| 5.8 State | 3 `rememberSaveable` in 354 states | Editor mode, filter, rotation, straighten, perspective, flips and aspect preset survive the app being closed by Android |
| 5.10 Consistency | Edit mode bar differed from the viewer toolbar; two red buttons in the video editor | One `EditorModeBar` in both editors; one red Revert button |
| 8 Motion | Durations 150 to 350 ms picked per place | `Motion.Short` 150 and `Motion.Medium` 300. Slideshow keeps 600 and 700 ms on purpose |
| 9 Empty and error | 10 empty screens had text only; error toasts gave no step | `EmptyState` (text and one Back button); five error texts got a next step |
| 10 Accessibility | 109 clickables without a role | `Role.Button` on 58 (the two with custom interaction sources were left) |

## Not changed, and why
- Haptics: long press already gives feedback; no decorative haptics (guide 8).
- Black mode uses pure black: a user option.
- `Color.White` and `Color.Black` over photos and video: legitimate.
- Hairlines of 1 dp.

## Still open (needs a device)
- Maximum system text size (guide 2, release gate): not tested. Rows with one line and fixed heights may break.
- TalkBack walkthrough of the core flows (guide 10).
- Contrast of every color, only the red was calculated.
- Deep links: none besides VIEW, PICK and GET_CONTENT for media. A removed item falls back to the previous screen, other failing paths were not walked through.
- `contentDescription = null` appears 79 times; many are decorative, not reviewed one by one.
- The 3-tap rule is marked unconfirmed in the guide; Hidden with PIN may take 4.
- Primary action (save) sits top right in the editors; the guide prefers the lower third, but marks the thumb zones as too rigid. Left as is.
