# Section 1 — what is left

Two lists: things needing a conversation with the team, and things that are
just work. Last reviewed 2026-09-19.

For how the service works, see [README.md](README.md). For cross-service
promises, see [../CONTRACTS.md](../CONTRACTS.md).

---

## Part 1 — to discuss with the team

### 1.1 Nobody owns the demo's only frontend screen ⚠️

[DEMO.md](../DEMO.md) steps 1–3 are all Swagger UI. Step 4 is the single
moment a real UI appears:

> **Frontend:** show the changes panel on the paper detail page, then
> acknowledge one event.

That screen shows **Storage Management** and **Updating** data. Under the
ownership split settled on 2026-09-18 — this team builds the shell, each
section builds its own screens — it belongs to those owners, not to you.

**Nobody currently holds that ticket.** Raise it at the next sync. If it is
still unassigned in week 6, the demo has no frontend.

A login page alone does not cover the demo. That is worth saying out loud,
because "Section 1 is doing the frontend" is easy to hear as "Section 1 has the
demo screen covered."

### 1.2 The other owners need the `JWT_SECRET` value

[SETUP.md](../SETUP.md) tells them to *"confirm the `JWT_SECRET` format with the
User Management owner"* — that is you.

The format question is settled and implemented: **base64 of 32+ random bytes,
and every service base64-decodes it before use.** What they still need is the
actual value, so that tokens issued here validate in their services.

Send it over a private channel, never in the repo. It is in your
`application-local.yml`, which is gitignored. Anyone holding it can mint a valid
token for any user.

### 1.3 The ownership decision assumes your teammates will write React

The split means the Storage Management, Research Evaluation and Updating owners
each build their own screens. Two of those are Python developers.

It is not much React — a feature folder mostly copies patterns the shell sets up
— but it is not zero. Confirm they are willing. **If they are not, the real
answer is that this team builds every screen**, and you need to know that now
rather than in week 6, because it is roughly the difference between one week of
work and three.

---

## Part 2 — left to do

### 2.1 Controller tests — the biggest gap 🔴

There are two controllers and **zero tests covering them**. Every security rule
in the service is currently proven only by a manual run someone did once:

| Rule | Why it matters |
|---|---|
| Another user's folder returns **404, not 403** | 403 would confirm the id exists |
| Unknown email and wrong password return an **identical 401** | Otherwise the endpoint reveals which emails are registered |
| Owner comes from the **token**, never the request body | Otherwise any user could write into another account |
| No token → **401** | The whole point of the auth |

Someone could break any of these in a refactor and all 6 existing tests would
still pass. The dependencies are already in `pom.xml`
(`spring-boot-starter-webmvc-test`, `-security-test`).

Suggested: `AuthControllerTest` and `FolderControllerTest`, one test per row
above, plus duplicate-email → 409 and validation → 400.

### 2.2 Frontend — login page

Parked until the backends are settled. Node is installed. When it resumes:
scaffold Vite in [`../frontend/`](../frontend/), build the shell (routing, auth
context, Axios client), then the login page. Conventions are already written up
in [`../frontend/README.md`](../frontend/README.md).

### 2.3 Dockerfile

This service has none; Section 5 will need one.

⚠️ It **must** use a Java 25+ base image (`eclipse-temurin:25-jre`). Bytecode
compiled for 25 will not run on an older JRE, and it fails *only inside the
container* — never on a dev machine. Coordinate with whoever owns Deployment.

### 2.4 Add this service to SETUP.md

[SETUP.md](../SETUP.md) is the team's "how to run the stack locally" doc, and it
mentions User Management only in passing. It lists no JDK requirement and no run
command, so a teammate cannot start this service by following it.

Needs: **JDK 25+**, no Maven install required (`mvnw` handles it), the Supabase
session-pooler note, and:

```bash
cd user-management
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

(Separately, SETUP.md is written for Linux — `sudo systemctl`, `usermod` — while
at least one machine on this team is Windows.)

### 2.5 CORS origin is hardcoded

`application.yml` allows `http://localhost:5173`, the Vite dev port. Correct for
development; deployment needs the real frontend origin. It is overridable via the
`APP_CORS_ALLOWEDORIGINS` env var — undocumented until now, so worth noting
before deployment week.

### 2.6 Wake Supabase before the demo ⏰

Free Supabase projects **pause after one week of inactivity**. Set this up, do
not touch it for a week, and it is asleep when you present.

Resume it and re-run the §9 verification **the day before the demo**, not that
morning.

---

## Done

- Backend built and **verified running against Supabase** — Flyway applied
  `V1__init`; register, login, folder CRUD, cross-user isolation and input
  validation all exercised live.
- JWT issuance matching CONTRACTS.md — HS256, `sub` = user UUID, base64 secret.
- Swagger UI with a working **Authorize** button.
- 6 tests guarding the claim contract and base64 key derivation.
- Docs: interfaces in CONTRACTS.md, decisions in DECISIONS.md, a
  doc-maintenance rule in the root README.
- Backend and frontend split into separate top-level folders; frontend
  ownership settled.
