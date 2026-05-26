# Docs

This folder contains the **source of truth** documentation for DupBuster v1.

## Read order (authority)

1. `docs/requirements.md` — product scope, acceptance criteria, and non-negotiables
2. `docs/architecture.md` — system boundaries, bridge contracts, schema, and security rules
3. `docs/implementation-plan.md` — milestone plan (M1–M5), tasks, and exit gates

If there is any conflict, follow the authority order above.

## Project invariants (v1 quick recap)

- **On-device only**: no accounts, backend sync, or cloud upload of user files
- **Duplicates = same content only** (by hash / defined equivalence rules), not name/path/metadata
- **Review before delete**: destructive actions require explicit keeper selection + two-step confirm
- **Partial coverage honesty**: never claim full-device scan without grants

## Milestones

Implementation proceeds in milestones as defined in `docs/implementation-plan.md`.

- **M1**: Native ScanEngine spike (discovery, hashing, SQLite index, progress throttle, checkpoint, video fingerprinting)
- **M2**: React Native shell + UX catalog (can stub progress until bridge is live)
- **M3**: Actions + integrity (two-step delete, TOCTOU, permission-revoke pause/resume)
- **M4**: Background + release hardening (Android FGS, redaction gate, store checklist)
- **M5**: Deliverable docs (this folder)

## Working on a milestone

When implementing a milestone, treat the milestone’s **Exit gate** section in `docs/implementation-plan.md` as the definition of done:

- Track exit-gate bullets as a checklist.
- Mark an item complete only when it is **actually** satisfied (ideally with tests/fixtures where required).

