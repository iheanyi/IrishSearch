# Play release preparation

## Console inspection: October 14, 2026

The existing listing is **Irish Class Search**, package `com.iheanyiekechukwu.irishsearch`. Do not use the separate draft listing with package `edu.nd.swag.irishclasssearch`.

- The correct listing is removed (June 7, 2018) and its last submission was rejected (July 20, 2018).
- Opening the removal's issue details still shows only “Not adhering to Google Play Developer Program policies.” It does not identify a specific violation. The page offers support and appeal routes; neither was submitted.
- Protected with Play says **Releases not signed by Play**. App signing offers enrollment using the existing Java KeyStore. No enrollment or key change was made. Console access alone does not supply the original private key.
- The current policy page requires target API 36. The new app targets 36.
- Latest releases and bundles lists version code **9 / 2.2** as the latest APK (uploaded August 1, 2013). The new version code **10 / 3.0** exceeds it. The legacy release row has an obviously invalid 1969 timestamp; use the artifact's upload metadata, not that timestamp.
- App content reports **9 declarations need attention**: Data safety, Financial features, Health apps, Privacy policy, Sign in details, Content ratings, Target audience and content, Advertising ID, and Government apps.

## Signing and internal testing

**Legacy listing limitation:** the owner no longer has the original keystore. Google's [Play App Signing documentation](https://support.google.com/googleplay/android-developer/answer/9842756) states that a self-managed app signing key cannot be reset after loss. Upload-key resets apply to apps already enrolled in Play App Signing and do not solve this case. Unless a backup is recovered, publication requires a new application ID/listing and fresh Play-managed signing. Existing installs will not receive that as an update. The owner approved a new listing and chose `com.iheanyi.notredame.classsearch`; this is now the application ID. The Kotlin namespace remains unchanged. Existing tester data can be transferred using the manual backup/restore flow; the two packages can coexist while migrating. Resolve policy compliance for the rebuilt product before submission; a new identity does not waive the old policy concerns.

1. Create the new listing for `com.iheanyi.notredame.classsearch` and enroll it in Play App Signing with a new Play-managed app signing key. Review the enrollment and agreements with the account owner before submitting them. The old listing cannot receive this build.
2. Generate and securely back up a new upload keystore. Supply all four environment variables outside Git: `IRISHSEARCH_KEYSTORE`, `IRISHSEARCH_KEYSTORE_PASSWORD`, `IRISHSEARCH_KEY_ALIAS`, `IRISHSEARCH_KEY_PASSWORD`. Use an absolute keystore path and a secret manager; keep passwords out of shell history and logs. With none supplied, builds are explicitly unsigned. Partial configuration fails.
3. Current source uses version code 11 / name 3.0. Check the new listing before final packaging so any previously uploaded version code is not reused.
4. Build and verify the signed artifact, then upload to an internal track after owner approval. Validate installation, backup/restore migration, exports, alerts, and notification navigation on the delivered build. The new package can coexist with the old app but cannot directly read its private storage.
5. Complete policy declarations and resolve any applicable policy issues before requesting production review. A build or internal test does not establish policy approval.

## New listing and signing setup: October 14, 2026

- New listing: **IrishSearch**, `com.iheanyi.notredame.classsearch`, app ID `4973169011298502435`. Created as a free app in en-US after the owner explicitly approved the policy/export declarations.
- Play Console reports **Releases signed by Play** and an app signing key **In use**. Google generated the signing keys during new-app creation. This setting applies to this app; it does not enroll other apps in the developer account.
- Google-held app-signing certificate SHA-256 shown in the Digital Asset Links example: `BF:C0:61:CA:4F:78:1F:43:B8:32:55:FB:AA:BA:00:7E:98:66:78:5D:03:A7:87:8D:0F:2A:63:A2:44:DD:12:09`. Console also shows a quantum-ready certificate. Re-read the current signing screen when configuring any future API fingerprints.
- A separate RSA-4096 PKCS12 upload key is stored outside Git under `~/.android/keystores/irishsearch/`, with directory mode 0700 and key/credential file mode 0600. Its alias is `irishsearch-upload`.
- Upload certificate SHA-256: `87:19:CA:18:BF:CF:17:E7:E8:8D:23:1D:75:C2:FB:C2:30:AE:4D:D4:C5:56:F9:0C:ED:42:58:DF:FE:81:30:B4`.
- The password and keystore/certificate attachments are backed up in the owner's Private 1Password vault as **IrishSearch — Google Play upload key**. A readback verified the stored password and both attachments. No secret values belong in repository files or command output.
- Signed version 3.0 (10) built successfully; JAR signature verification passed. AAB SHA-256: `f0ba541b9d945c06093277ddc6cb8cc6ec3e1edffb72542ce33753c69190f4a4`.
- Google Play processed and accepted bundle 10 (3.0), min API 24 / target API 36, into the new internal-test draft. Review showed no blocking errors and three warnings: no testers selected yet, no deobfuscation mapping (this build is not obfuscated), and no native debug symbols for dependency libraries. Final rollout status must be confirmed separately.

## Internal rollout completed: October 14, 2026

- With the owner's explicit approval, created the reusable **IrishSearch internal testers** email list containing one tester and selected it only for this app's internal track. The existing license-testers list remains unselected.
- Published **3.0 (10) — First internal test**. Console reports the track **Active** and the release **Available to internal testers**, **Not reviewed**. This is an internal release, not production publication or policy approval.
- Tester enrollment: https://play.google.com/apps/internaltest/4701731374736793458
- Console uses the temporary name `com.iheanyi.notredame.classsearch (unreviewed)`. Publishing confirmation says propagation usually takes up to one hour, occasionally longer.
- The missing-testers warning is resolved. Two nonblocking warnings remain: no deobfuscation file and no native debug symbols.
- Play-delivered installation has not yet been validated. The installed debug build uses a different signing certificate; export the current library before removing it to install the Play-signed build, then restore the backup. Do not clear or uninstall either existing package without protecting its data.

## Declaration preparation

### Production preparation update: October 14, 2026

- Owner requested a production upload. Promoted the internal artifact into production track `4697998478832069569`, then replaced it with **3.0 (11)** to include the complete in-app privacy policy and contact mechanism. Console processed and accepted the signed bundle and confirms the draft **3.0 (11) — First production release** is saved. It has not been submitted for production review or published.
- Build 11 AAB SHA-256: `21a1af3590412e4eda299c947d6a4d043b40727133ee561c826e2d2732d42d65`. Release build, release vital lint, debug build and JAR signature verification passed.
- Saved the default store listing with descriptions, accepted shamrock icon, feature graphic and three unchanged native screenshots from `docs/play-store/`. Console reports **Ready to send for review**. The icon and feature graphic are labeled AI-created; the authentic screenshots are not.
- Saved financial features (none), health features (none), ads (none), unrestricted sign-in access, advertising ID (not used), government affiliation (none), and the public privacy policy URL: https://github.com/iheanyi/IrishSearch/blob/codex/play-policy-2026/PRIVACY.md . This branch must remain accessible until the URL is migrated to a permanent site/default-branch page.
- Data safety is saved and ready for review after completing the target audience. Disclosures: app interactions and in-app search history collected/shared for app functionality, required for catalog requests, without claiming ephemeral server processing; HTTPS in transit; no accounts; no verified remote deletion mechanism. Local schedules and user-selected exports are distinguished from PATH request data. Review reasoning against Google's [Data safety definitions](https://support.google.com/googleplay/android-developer/answer/10787469) if server practices or features change.
- Owner approved worldwide distribution, public support email `hello@iheanyi.com`, and intended ages **16–17 and 18+**. Saved all 178 country/region entries (177 named entries plus Rest of World), Education category, and audience. Contact details were published immediately by Console.
- Owner explicitly approved IARC terms and sharing the support email with rating authorities. Questionnaire is saved: no bundled mature content or native user communication; live online course catalog; possible textual drug references, not a focus and no imagery; no restricted-product promotion, precise-location sharing, purchases, rewards, or general web browser/search engine. Neutral educational/factual information is declared. Notre Dame's [HHS catalog](https://catalog.nd.edu/undergraduate/courses/hhs/) includes HHS 30153, Drugs in American Life, supporting the text-reference answer. Assigned ratings include ESRB Everyone 10+, PEGI 3, USK 6+, and IARC 3+; ratings are distinct from intended target ages.
- Production preview has **zero blocking errors** and two nonblocking warnings (deobfuscation file and dependency native debug symbols). Publishing overview contains 11 changes, including build 11 full rollout and worldwide availability. Automatic approval review blocked final submission because managed publishing is off and the upload request did not explicitly authorize automatic publication. Asked the owner to approve submission and worldwide publication after Google approval; no submission occurred. Quick checks are running independently.
- Submission-ready evidence: `/private/tmp/irishsearch-production-ready-for-review.jpg`. Preserve the new app's Console tab for the pending approval.
- Evidence: `/private/tmp/irishsearch-production-build11.jpg`; production bundle is saved in Console, not merely built locally. Internal testing remains on build 10.

These are review notes based on the current source, not submitted declarations:

| Declaration | Evidence / proposed answer |
| --- | --- |
| Sign in details | App has no account or login. All in-app functions use public class data. University sign-in-only instructor/location fields remain unavailable. |
| Advertising ID | No advertising ID permission, advertising SDK, or advertising feature. Verify merged release manifest. |
| Financial features | No financial services or financial transactions. |
| Health apps | No health features. Personal commitments are arbitrary user-entered schedule text. |
| Government apps | Independent app, not a government service. |
| Privacy policy | Publish the current `PRIVACY.md` at an accessible stable URL and enter that URL. Include exports, local commitments, and periodic PATH requests. |
| Target audience | College course planning. Account owner should confirm intended age groups, including any prospective/dual-enrollment students. |
| Content ratings | Complete the current questionnaire for a public course catalog and private local planning tool. No social publishing, advertising, purchases, or user-to-user messaging is implemented. |
| Data safety | Review the final binary and Google's definitions. No developer server, analytics, ads, or account. Local schedules/commitments remain on-device unless users export them. Searches and watched-section lookups go directly to the university, including query parameters and normal network metadata; assess this processing and university practices rather than assuming that “no developer server” means no disclosure is needed. Files are sent only to the user-selected provider/app. |

## Release verification

- Debug-only seat simulation lives under `app/src/debug` and must be absent from the release manifest and DEX.
- Notification checks are battery/network constrained and hourly; Android can delay them. Avoid promises of instant or guaranteed registration alerts.
- Calendar exports are snapshots; they do not create a Google account integration or register students for courses.
- Share images label the represented week and include an inventory of all saved schedule entries, including entries without calendar geometry.
- Backups merge without overwriting existing IDs. Newly imported watches start off.
- Keep the public store copy clear that this app is independent of Notre Dame and CourseLeaf.
