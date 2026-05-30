# Manual QA

Executable checklists for screen-reader and store smoke that automated Jest cannot cover.

| Document | Milestone | When to run |
|----------|-----------|-------------|
| [m2-voiceover-talkback-smoke-matrix.md](./m2-voiceover-talkback-smoke-matrix.md) | M2-11 | Before M2 exit gate sign-off; re-run when catalog a11y components change |

**Prerequisite:** `npm run test:a11y` green (M2-10 automated gate) before manual smoke.

**Authority:** Requirement IDs (`A11Y-*`, `AC-a11y-*`) live in `docs/requirements.md` §10–11. Frozen announcement strings live in `src/tokens/tokens.ts` (`a11y.*`).
