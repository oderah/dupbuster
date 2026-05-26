This command receives an option argument: the milestone to implement (example: `M1`, `M2`, `M3`, `M4`).

## Input

- Milestone argument: `{{args}}`

## Workflow (must follow in order)

1. Inspect and understand `docs/architecture.md`, `docs/implementation-plan.md`, and `docs/requirements.md`.
2. Determine the target milestone:
   - If `{{args}}` is missing/empty, ask the user which milestone to focus on.
   - Optionally recommend a “next milestone” based on dependencies in `docs/implementation-plan.md`, but do not start unless the user explicitly confirms a milestone choice.
   - If `{{args}}` is present, parse it as `M0`/`M1`/`M2`/`M3`/`M4` (only those expected v1 milestones).
3. Locate the matching milestone section in `docs/implementation-plan.md` (the one with the **Tasks** and **Exit gate** subsections).
4. Implement tasks **one at a time** to avoid overloading the agent:
   - Start at the milestone task numbered `{{args}}-01`.
   - Implement **ONLY** that one task (do not start `{{args}}-02` in the same run).
   - When the task is complete, notify the user that `{{args}}-01` is done.
   - Wait for the user prompt to proceed. On the next prompt, implement exactly the next task in order (`{{args}}-02`, then `{{args}}-03`, …).
   - Keep repeating until the last task listed under that milestone is complete.
5. After all tasks under the milestone are complete, proceed to the milestone **“Exit gate”**:
   - Use the Exit gate bullets as the progress tracker.
   - For each bullet that starts with square brackets, replace:
     - `[ ]` → `✅` when the criterion is met.
     - Leave it as `[ ]` if not met yet.
   - If any exit-gate criteria are not met, notify the user which bullets remain `[ ]` and provide what still must be done to reach the ✅ state.
6. The milestone is **done only when**:
   - All tasks listed under that milestone are complete, and
   - Every Exit gate criterion has been verified and marked with `✅`.
   - Once complete:
     - Update `docs/README.md` with any necessary new information learned/added during this milestone (new run/build steps, new directories, new entry points, updated milestone status, or other non-obvious “how to work with the repo” context).
     - Notify the user that the milestone is finished and that `docs/README.md` was updated, then ask whether to proceed to the next milestone or stop.

## Rules that must be enforced while implementing

- Follow `.cursor/rules/task.mdc` (standards → project law → codebase inspection → reuse/extend single source of truth).
- Prefer **re-use/extension** over re-implementation; avoid parallel helpers or duplicate sources of truth.
- Respect `docs/requirements.md` and `docs/architecture.md` as higher authority than implementation notes.
