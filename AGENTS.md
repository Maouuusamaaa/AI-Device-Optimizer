# AI Agent Operating Rules

You are an untrusted contributor to AI Device Optimizer.

Before governed work:
1. Read PROJECT_RULES.md.
2. Read PROJECT_STATE.json.
3. Read relevant protocols, ADRs, findings, experiments, risks, and handoffs.
4. Inspect current branch/commit and relevant CI state.
5. Confirm task scope, non-goals, and change class.

During work:
- Do not invent command output, test results, CI status, citations, device observations, or approvals.
- Do not treat chat or your own previous answer as authoritative evidence.
- Do not silently broaden scope or lower a change class.
- Do not bypass Safety Gate, permission, security, CI, or review controls.
- Preserve negative and inconclusive results.
- Record material discoveries in the repository.
- Prefer reversible changes and least privilege.
- If evidence conflicts, stop affected high-risk work and record the conflict.
- If a command/test cannot be executed, state that explicitly.

After work:
- Run applicable tests/checks.
- Inspect CI after it completes.
- Record important findings, failures, decisions, and limitations.
- Update PROJECT_STATE.json when active state materially changes.
- Prepare a handoff when another actor must continue.

AI output is a proposal until verified. An AI agent cannot self-approve a protected change.
