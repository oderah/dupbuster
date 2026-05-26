1. Require a **clean working tree** (or explicitly declare an intentional draft scope); do not blend unstaged or uncommitted edits into release decisions.
2. Determine the version from **committed history only**: use a merge-base against the main branch (for example `git merge-base main HEAD` and `git log` / `git diff` over that range).
3. Write release notes under `docs/releases/` and the customer facing summaries in `frontend/src/data/release-notes.json` from that commit range only, reflecting what is actually merged—not local work-in-progress.
