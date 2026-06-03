# DupBuster Privacy Policy

**Effective date:** [Set when published]  
**Canonical URL:** https://dupbuster.app/privacy  
**App:** DupBuster (com.dupbuster) — duplicate detection on your device

---

## Summary

DupBuster finds duplicate photos, videos, and other files **on your device only**. We do not upload your files, file paths, content hashes, or perceptual fingerprints to DupBuster servers in version 1. Optional anonymous crash reports are **off by default** and only sent if you turn them on in Settings.

---

## 1. On-device processing

All scanning, hashing, indexing, and duplicate grouping run **locally on your phone or tablet**. Your media and documents are read only to detect duplicates and, if you choose, to delete copies you do not want. Processing does not require a DupBuster account and does not sync your library to the cloud.

---

## 2. What we access on your device

With your permission, DupBuster may access:

- **Photos and videos** — to read content for comparison and to delete items you confirm removing (iOS Photos / Android MediaStore).
- **Other files** — folders you select (for example via the system document picker or storage access you grant).
- **Local catalog** — a SQLite database on the device that stores scan metadata (sizes, hashes, duplicate groups). This stays on the device.

We show file names and paths **in the app** so you can review duplicates. Those paths are **not** included in optional crash reports.

---

## 3. What we do not do (v1)

- Upload, sell, or share your files or file contents with DupBuster or third-party backends.
- Use your library for advertising or cross-app tracking.
- Automatically delete files without your explicit keeper choice and two-step confirmation.
- Claim forensic or secure erasure beyond what the operating system provides when you delete a file.

---

## 4. Optional crash analytics

You can enable **Send anonymous crash reports** in Settings. This setting is **off by default**.

When enabled, only allowlisted diagnostic fields may be sent (for example scan phase and schema version). Reports are redacted so they do not contain file paths, hashes, unscannable reason codes tied to specific files, or file content. When disabled, the app does not send crash analytics through DupBuster’s telemetry layer.

If we add a third-party crash SDK in a future version, this policy will be updated and store disclosures will be re-filed before collection changes.

---

## 5. Permissions

DupBuster requests storage or photo-library access only to scan and delete files you authorize. Partial or limited library access is disclosed in the app; we do not claim a full-device scan without the access you granted.

---

## 6. Data retention

Scan results and settings are stored on your device until you delete the app or clear app data. Uninstalling DupBuster removes the local catalog from your device.

---

## 7. Children

DupBuster is not directed at children under 13. We do not knowingly collect personal information from children.

---

## 8. Changes

We may update this policy when product behavior or legal requirements change. Material changes (for example enabling upload of file data or default-on analytics) will be reflected in the app, store listings, and an updated effective date on this page.

---

## 9. Contact

For privacy questions about DupBuster, contact: **[privacy contact email — set before prod publish]**

---

## Operator note (remove from hosted page)

Source of truth in repo: `docs/store/privacy-policy.md`. Publish this document at **https://dupbuster.app/privacy** before production store submit (M4-14). Align with `docs/store/privacy-store-disclosure.md` (M4-09).
