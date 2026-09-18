# research-assistant-proj

A tool for researchers to track papers over time. The core loop: **detect a
meaningful change in a tracked paper → assess its impact → recommend an
action → the researcher decides.**

Five services sit behind one React frontend and talk to each other over
REST, trusting a single JWT issued at login. See
[ARCHITECTURE.md](ARCHITECTURE.md) for the full design.

## Where to look

| Doc | What's in it |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Services, ownership, data flow, and the week-7 vs week-13 scope split |
| [CONTRACTS.md](CONTRACTS.md) | Cross-service API contracts: endpoints, request/response shapes, auth |
| [DECISIONS.md](DECISIONS.md) | Dated log of design decisions and why they were made |
| [SETUP.md](SETUP.md) | Accounts, API keys, software, and how to run the stack locally |
| [DEMO.md](DEMO.md) | The week-7 demo runbook |

## Services

| Service | Stack | Folder |
|---|---|---|
| User Management | Spring Boot | `user-management/` |
| Frontend (User Management team) | React + Vite | `frontend/` |
| Storage Management | Java + Spring Boot | `storage/` |
| Research Evaluation | Python | `backend/` |
| Updating | Python | `backend/` |

Research Evaluation and Updating share one Python project in `backend/`
(two FastAPI apps, one image) — see `backend/` for details.

## Keeping the docs current

**Every PR that changes code updates the docs it affects, in the same PR.**
A doc that lies is worse than no doc: the next person trusts it, builds on it,
and loses an afternoon. Reviewers should reject a PR that skips this.

| If your PR changes... | Update |
|---|---|
| An endpoint, payload, JWT claim, or anything another service calls | [CONTRACTS.md](CONTRACTS.md) |
| A database schema, service boundary, or folder layout | [ARCHITECTURE.md](ARCHITECTURE.md) |
| A choice with a trade-off someone may later question | [DECISIONS.md](DECISIONS.md) (dated entry, newest first) |
| Required accounts, env vars, or how to run a service | [SETUP.md](SETUP.md) |
| The demo path or its seed data | [DEMO.md](DEMO.md) |
| Anything inside one service only | That service's own README |

Two rules of thumb:

- **Interface changes are breaking changes.** If another service calls it,
  CONTRACTS.md is not optional and the affected owner should be told directly,
  not left to discover it at integration time.
- **Write the reasoning, not just the change.** "Switched to X" ages badly;
  "Switched to X because Y broke under Z" stops the next person re-litigating it.

## Status

Week 7 (midterm) scope is in progress. See ARCHITECTURE.md for what's in
and out of scope for this milestone.
