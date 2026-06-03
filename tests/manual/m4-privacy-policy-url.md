# M4 — Privacy policy URL sign-off

**Task:** M4-14  
**Owner:** Release / Legal publishes; Release signs off before prod store submit  
**Authority:** `docs/implementation-plan.md` §12 (privacy policy URL), NFR-06

DupBuster requires a **public HTTPS privacy policy** that states on-device-only processing. The policy text lives in the repo; operators host it at the canonical URL and paste that URL into Play and App Store listings.

---

## 1. Prerequisites

| Item | Detail |
|------|--------|
| Policy source | `docs/store/privacy-policy.md` (user-facing; aligned with M4-09 `privacy-store-disclosure.md`) |
| Canonical URL | `https://dupbuster.app/privacy` — must match `src/config/privacyPolicyUrl.ts` |
| Automated preflight | `npm run test:privacy-policy` green on release branch |
| M4-09 | Play Data safety + App Privacy filed (claims must match policy) |
| M4-13 | Crash analytics opt-in default off (policy §4) |

---

## 2. Publish hosted policy

1. Replace placeholders in `docs/store/privacy-policy.md`:
   - **Effective date**
   - **Contact email** (§9) — remove the operator note section from the hosted page.
2. Publish HTML or rendered Markdown at **https://dupbuster.app/privacy** (HTTPS, no auth wall).
3. Verify in a browser: page loads, states **on-device** processing, **no upload** of user files in v1, optional crash reports **off by default**.

Record in sign-off (§5): publish date, who verified the live URL, and screenshot or archive link.

---

## 3. Store listing URLs

| Store | Console path | Field |
|-------|--------------|-------|
| Google Play | Play Console → App content → **Privacy policy** | `https://dupbuster.app/privacy` |
| App Store | App Store Connect → App Information → **Privacy Policy URL** | `https://dupbuster.app/privacy` |

Both listings must use the **same** URL as `PRIVACY_POLICY_URL` in the app. Re-file M4-09 forms if policy claims change.

---

## 4. In-app link smoke

On a release build (internal or prod candidate):

| Step | Expected |
|------|----------|
| Open scan screen | **Privacy policy** link visible below crash analytics setting |
| Tap link | System browser opens `https://dupbuster.app/privacy` |
| VoiceOver / TalkBack | Announces link label + opens-in-browser hint |

Row ID: **M4-PRIVACY-01**

---

## 5. Sign-off

| Check | Result | Notes | Owner / Date |
|-------|--------|-------|--------------|
| Hosted URL live (HTTPS) | | | |
| Policy states on-device-only (v1) | | | |
| Play Console privacy policy URL set | | | |
| App Store Connect privacy policy URL set | | | |
| In-app link opens same URL (M4-PRIVACY-01) | | | |
| `npm run test:privacy-policy` on release tag | | | |

**Pass rule:** All rows **Pass** before `DUPBUSTER_PROD_PROMOTE_APPROVED=1` and Fastlane **prod** submit.

---

## 6. When to re-run

- Policy text or URL changes
- Enabling default-on analytics or new off-device data types
- Store rejection citing privacy policy mismatch

Update `docs/store/privacy-policy.md`, `privacyPolicyUrl.ts`, and re-publish the hosted page in the same release.
