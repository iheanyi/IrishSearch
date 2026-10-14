# Google Play visual assets

Prepared October 14, 2026 for `com.iheanyi.notredame.classsearch`, version 3.0 (11).

| Asset | Dimensions | Source and scope |
| --- | --- | --- |
| `feature-graphic.png` | 1024 × 500 | Rendered from `feature-graphic.svg`; extends the accepted `../brand/shamrock-icon.svg` without changing its geometry or palette. |
| `phone-01-course-details.png` | 1344 × 2992 | Unchanged Husky screenshot `/private/tmp/irish-exclusion-after.png`: genuine CSE 10101 course, meeting times, enrollment, and linked exclusion requirements. |
| `phone-02-prerequisites.png` | 1344 × 2992 | Unchanged Husky screenshot `/private/tmp/irish-final-parser-prereq.png`: genuine CSE 40657 prerequisite groups. |
| `phone-03-seat-alerts.png` | 1080 × 2400 | Unchanged native emulator screenshot `/private/tmp/irish-new-registration-dark.png`: current four-tab navigation and genuine saved course identifiers. Notifications are visibly off; this does not claim delivery. |

All PNGs are fully opaque (native screenshots retain an unused alpha channel) and below 250 KB. No screenshot pixels, course data, labels, or status bars were edited. Course availability is historical data from the capture, not a current guarantee. Screenshots were visually inspected; no account identifiers, personal commitments, or synthetic notification-test courses appear.

Older Browse screenshots were excluded because they show an obsolete three-tab navigation. Schedule captures with QA plan names were excluded. These assets show actual app screens and do not represent a comprehensive final production-binary validation.

## Feature graphic

Standalone vector brand composition; rasterized at its native 1024 × 500 size using `rsvg-convert`, with Inter loaded from the existing Coda Android font asset. No generative screenshot or fabricated product mockup was used. The PNG is the upload artifact; SVG text requires Inter for an identical future render.

Design references inspected through Mobbin:

- [Partiful hero](https://mobbin.com/sites/sections/5472e9d8-de81-40dc-ba9e-a2d6f4e71fe1): concise left-aligned product promise with a separate visual anchor.
- [Kalstore hero](https://mobbin.com/sites/sections/940a348a-e6d4-4299-8b1b-2dca4907f506): large restrained typography and clear calendar-product identity.

No reference imagery was copied. The graphic uses the app's existing navy and shamrock colors, a clear class-planning promise, and an independent-app disclosure.
