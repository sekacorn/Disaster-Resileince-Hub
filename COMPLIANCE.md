# Compliance status

What has been implemented against GDPR, the European Accessibility Act / EN 301 549,
Section 508 / WCAG 2.1 AA, and NIST SP 800-53 — and, equally, what has not.

This is a status record, not a conformance claim. Nothing here has been assessed by an
external auditor. Where a control is partial, it says so; where a requirement needs a
person rather than code, it says that too.

---

## Scope of this work

Three phases of code remediation, applied to the `disaster-integrator`, `user-session`
and `api-gateway` services and the React frontend. The project is a demo: no real
personal data is processed today, so the work prioritises controls that are verifiable
in code over operational procedures that would need an organisation behind them.

---

## GDPR

### Implemented

| Requirement | Where |
|---|---|
| **Art. 5(1)(f), 32** — security of processing | AES-256-GCM field encryption for special category health data, applied by a JPA converter so no code path can bypass it (`privacy/crypto/`) |
| **Art. 5(1)(c)** — data minimisation | Client addresses truncated, actors pseudonymised, and log statements stripped of usernames and emails |
| **Art. 5(1)(e)** — storage limitation | Nightly retention sweep for expired location history (`privacy/retention/`) |
| **Art. 6(1)(a), 7** — consent | Append-only, purpose-scoped consent records storing the lawful basis and the exact wording shown at decision time (`privacy/consent/`) |
| **Art. 7(3)** — withdrawal as easy as granting | One endpoint handles both directions |
| **Art. 15, 20** — access and portability | `GET /privacy/export` returns structured JSON |
| **Art. 16** — rectification | `PUT /data/health/me` |
| **Art. 17** — erasure | `DELETE /privacy/me`, with a receipt recording what was kept and why |
| **Art. 9(2)(a)** — explicit consent for health data | `EMERGENCY_HEALTH_RESPONSE` carries `EXPLICIT_CONSENT` as its basis |

### The specific defect this replaced

`IndividualHealthData` carried `dataEncrypted = true` as a hardcoded boolean with no
encryption behind it. Medical conditions, allergies, medications, mobility and oxygen
requirements, and insurance policy numbers were stored in plaintext while the record
asserted otherwise. That assertion is now backed by an actual cipher.

### Not implemented

- **Data subject rights cover `disaster-integrator` only.** Account data in
  `user-session` and messages in `collaboration-service` have no export or erasure
  route. The export output names these gaps explicitly rather than implying it is
  complete, but a subject cannot yet exercise Art. 15 or 17 against them.
- **No Records of Processing Activities (Art. 30)** or DPIA. The health data processing
  would require a DPIA before going live — Art. 35(3)(b), large-scale special category
  processing.
- **No breach notification procedure (Art. 33/34).** This is a process, not code.
- **No DPO appointment, and no processor agreements (Art. 28)** for NOAA, USGS, Mapbox
  or any LLM provider the chat feature calls.
- **`mbti_type` is still collected** with no consent gate wired to it. The
  `UX_PERSONALISATION` purpose exists to cover it; the field is not yet checked against
  that purpose before use.
- **Encryption key management.** The key comes from configuration. A real deployment
  needs a KMS, and there is no key rotation path — re-keying today means decrypting and
  re-encrypting every row.

---

## European Accessibility Act / EN 301 549 / Section 508 / WCAG 2.1 AA

### Verified

`axe-core` reports **zero violations across all nine routes**. `eslint-plugin-jsx-a11y`
reports **zero errors**. Both run against the real application, not a static sample.

### Starting point

One `aria-label` across all 31 frontend source files. No `role`, no `alt`, no
`tabIndex`. No skip link, no focus management, no live regions.

### Implemented

| Criterion | What changed |
|---|---|
| **1.1.1** Non-text Content | The WebGL canvas exposes no accessibility tree, so it is hidden from assistive technology and `DisasterDataTable` presents the same incidents as a real table — shown to everyone, not gated behind a toggle |
| **1.3.1** Info and Relationships | 20 labels associated with their controls; `<label>` elements misused for static text replaced with `<dl>`; table headers scoped |
| **1.3.5** Identify Input Purpose | `autoComplete` tokens on identity and credential fields |
| **1.4.1** Use of Colour | Active nav marked with `aria-current`; severity and selection carried in text, not only colour |
| **1.4.11** Non-text Contrast | `forced-colors` rules restoring focus and current-page state under Windows High Contrast |
| **2.1.1** Keyboard | The account menu was hover-only and unreachable without a pointer; saved routes and collaboration rooms were click-only `<div>`s. All now native controls |
| **2.2.1** Timing Adjustable | Toast auto-dismiss raised from 5s to 20s, with pause on hover and focus loss |
| **2.3.3** Animation from Interactions | `prefers-reduced-motion` honoured |
| **2.4.1** Bypass Blocks | Skip link, first in the tab order |
| **2.4.2** Page Titled | Route announcer sets a distinct title per route; previously every route shared one static title |
| **2.4.3** Focus Order | Focus moves to `main` on navigation; Escape closes the menu and returns focus to its trigger; the closed menu no longer leaks three links into the tab order |
| **2.4.7** Focus Visible | `:focus-visible` indicators on every interactive element |
| **3.3.1 / 3.3.2** Error Identification and Labels | `FormField` wires errors to controls via `aria-describedby` and announces them in a live region |
| **4.1.2 / 4.1.3** Name, Role, Value / Status Messages | Icon-only buttons named; loading states given text equivalents |

Three of these were violations only `axe` found — a static linter cannot tell that an
icon-only button has no accessible name.

### Not implemented

- **No screen reader testing.** axe is static analysis in a real DOM; it does not tell
  you whether a NVDA or VoiceOver user can complete a task. EN 301 549 conformance
  needs manual testing with actual assistive technology.
- **No formal ACR / VPAT.** The evidence exists; the document does not.
- **`prefers-contrast` is not handled**, only `forced-colors`.
- **The README advertises "Multi-language support".** There is no i18n library and no
  translation infrastructure. That claim is not currently true.
- **Contrast is verified on the default theme only.** Dark mode was not audited
  systematically.

---

## NIST SP 800-53

### Implemented

| Control | What |
|---|---|
| **AU-2** Event Logging | `AuditEventType` — authentication outcomes, lockouts, credential and MFA changes, role changes, data subject rights |
| **AU-3** Content of Audit Records | Each record carries what, when (UTC), where, source, outcome and pseudonymised identity |
| **AU-6, AU-7** Review and Reporting | `/audit/events`, filterable by severity, actor and type over a time window |
| **AU-8** Time Stamps | `Instant`, UTC, so records from services in different zones stay totally ordered |
| **AU-9** Protection of Audit Information | SHA-256 hash chain; entity callbacks reject update and delete; `/audit/integrity` returns 500 on a broken chain |
| **AU-10** Non-repudiation | Chained hashes bind each record to its predecessor |
| **AC-7** Unsuccessful Logon Attempts | Failures audited, including against non-existent accounts, so credential stuffing is visible |
| **SC-8** Transmission Confidentiality | HSTS, CSP, `upgrade-insecure-requests` |
| **SC-18** Mobile Code | `script-src 'self'`, `object-src 'none'`, Permissions-Policy denying geolocation, camera, microphone, payment, USB |
| **SC-28** Protection at Rest | AES-256-GCM for special category data |
| **SI-11** Error Handling | Log redaction prevents secrets and personal data reaching logs via error messages |

### Known limits, stated plainly

- **Tail truncation of the audit chain is not detectable.** Hash chaining proves nothing
  about records removed from the end — there is no later hash committing to them.
  Detecting that needs an external anchor (AU-9(2)), such as shipping hashes to
  write-once storage. There is a test asserting this limit so the passing tests above
  cannot be mistaken for full protection.
- **Chain appends are serialised per instance only.** `AuditService.record` is
  `synchronized`, which is correct for one JVM. A multi-instance deployment needs a
  database-level lock or a single writer, or two instances will claim the same sequence
  number.
- **The JWT is still in `localStorage`** and readable by any script on the origin. All
  eight call sites now route through `services/tokenStorage.js`, which documents the
  weakness and makes the move to an `HttpOnly` cookie a single-file change. **This is
  not resolved** — IA-5 and SC-28 are not fully met for the token.
- **`/api/v1/**` is `permitAll` in the gateway.** Not changed here, because altering
  routing without knowing what depends on it risks breaking the demo — but it warrants
  review.
- **No AU-4** (audit storage capacity), **AU-5** (response to audit processing failure)
  beyond a log line, or **AU-11** (audit retention policy).
- **No SI-2 dependency scanning.** `npm audit` reports findings that were not triaged as
  part of this work.

---

## Verification

```bash
cd frontend && npx eslint src --ext js,jsx
```

```bash
cd backend/disaster-integrator && mvn test -Dtest='FieldEncryptionServiceTest,ConsentServiceTest'
```

```bash
cd backend/user-session && mvn test -Dtest=AuditServiceTest
```

Accessibility was verified by running `axe-core` against the live application across
all nine routes.

**Pre-existing test failures**, present on the baseline before this work and unrelated
to it: two in `disaster-integrator`'s `DataValidatorTest` (a `@Builder.Default` bug in
`EnvironmentalData`), and fourteen in `user-session` (`AuthControllerTest`,
`JwtServiceTest`, `UserSessionIntegrationTest`).

---

## Required configuration

Both default to a safe-but-loud fallback rather than a hardcoded value, because a
default secret is indistinguishable from no secret.

| Variable | Purpose | If unset |
|---|---|---|
| `PRIVACY_ENCRYPTION_KEY` | Base64 32-byte AES-256 key for health data | An ephemeral key is generated and a warning logged; encrypted data will not survive a restart |
| `AUDIT_PSEUDONYM_SALT` | Salt for audit actor pseudonyms (`user-session`) | Pseudonyms become dictionary-reversible |
| `PRIVACY_LOG_PSEUDONYM_SALT` | Salt for user references in gateway logs | Pseudonyms become dictionary-reversible |
| `PRIVACY_LOCATION_RETENTION_DAYS` | Days of inactive location history kept | Defaults to 90 |

```bash
openssl rand -base64 32
```
