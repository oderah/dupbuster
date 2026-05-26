1. Run `git status` and review the diff (`git diff` / scoped file review).
2. Stage **only** files that belong to a single coherent change (task scope). Do not use blanket `git add -A` when unrelated edits exist.
3. If the work tree mixes unrelated changes, split into multiple commits with meaningful messages (Conventional Commits where applicable).
4. Stop once the intended scoped changes are committed; leave unrelated work unstaged unless the user explicitly asks to include it.
