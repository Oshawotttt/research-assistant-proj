# Decisions log

Dated log of decisions and why they were made, so settled questions stay
settled and anyone (including the TA) can see the reasoning. Newest first.

---

## 2026-09-18 — User Management (Section 1) build

### Forced by the toolchain, not chosen

- **Spring Boot 4.1.1 / Spring Security 7.** Spring Initializr no longer
  offers the Boot 3.x line at all, so this was not a choice. It matters
  because essentially every JWT tutorial online targets Security 6 and
  will not compile. Boot 4 also renamed the starters: `-web` became
  `-webmvc`, `-oauth2-resource-server` became
  `-security-oauth2-resource-server`, Flyway got its own starter, and the
  single `spring-boot-starter-test` split into per-slice test starters.
  Copying a Boot 3 pom will fail to resolve.
- **springdoc 3.1.1**, not the 2.x line, which is Boot 3 only.

### Decisions

- **JWT via the Spring Security resource server, not a custom
  `OncePerRequestFilter`.** Security 7 added
  `NimbusJwtEncoder/Decoder.withSecretKey(key)`, which defaults to HS256,
  so issuing and validating are one line each. Other services need only
  the key bean, the decoder bean and one `oauth2ResourceServer` line.
- **`JWT_SECRET` is base64-decoded before use**, per CONTRACTS.md. The
  first implementation used raw `getBytes(UTF_8)` and was corrected
  before merge — raw bytes here would have derived a different key from
  the same string than the Python services do, and every token this
  service issued would have failed their signature check. This is the
  exact Java/Python bug CONTRACTS.md warns about; there are now three
  unit tests specifically to stop it regressing.
- **UUID primary keys, not `bigserial`.** `users.id` crosses a service
  boundary into Storage Management, so it needs to be globally unique and
  non-enumerable. It also keeps the JWT `sub` from leaking the user count.
- **Its own `usermgmt` Postgres schema, not `public`.** Two config lines,
  and it makes the "bare references, not enforced FKs" rule physical:
  Storage Management in its own schema cannot accidentally FK into
  `usermgmt.users`. One Supabase project can still host every service.
- **Flyway owns the schema, with `ddl-auto: validate`.** The schema is a
  contract other services read, so it is a reviewed file in git rather
  than a side effect of entity annotations. `validate` fails fast on
  entity drift instead of silently altering a table.
- **`folders.owner_id` is a plain `UUID` field, not a `@ManyToOne User`.**
  The database still enforces the FK; JPA stays trivial and folder
  queries carry no lazy-loading or N+1 risk.
- **Another user's folder returns 404, not 403.** A 403 confirms the id
  exists. `/auth/login` likewise returns an identical 401 for an unknown
  email and a wrong password, so it cannot enumerate registered addresses.
- **No Lombok.** It patches `javac` internals and historically breaks on
  brand-new JDKs; the dev machine runs JDK 26, GA in July 2026, whose
  Lombok support is unverified. With two entities the cost of hand-written
  accessors is near zero, and every DTO is a `record`.
- **`<java.version>21</java.version>`**, not 26. Compiles fine under a
  JDK 26 toolchain via `--release`, and matches the `eclipse-temurin:21-jre`
  image Deployment will use, so local and container agree.
- **Token lives in `localStorage`** on the frontend, not an httpOnly
  cookie. A cookie set by User Management would not be sent to Storage
  Management on a different origin without a shared parent domain, which
  fights the multi-service design.
- **Dev database is Supabase, one free project per developer**, rather
  than local Postgres in Docker. Removes Docker and WSL from Section 1's
  critical path entirely. Use the **session pooler** string: the direct
  connection is IPv6-only on the free plan and times out on IPv4 campus
  wifi. Free projects also pause after a week idle — resume before the demo.

### Open — needs a team answer

- ~~**Folder layout mismatch.**~~ **Resolved 2026-09-18.** Backend and
  frontend are now separate top-level folders: `user-management/` holds the
  Spring Boot service, `frontend/` holds the React app. The earlier plan to
  put the whole service in `frontend/` was dropped - a Spring Boot API in a
  path called `frontend/` misleads every new reader, and the two halves have
  different build tools, different CI steps and different Dockerfiles. The
  backend could not go in `backend/` either: that is the Python project for
  Research Evaluation and Updating. ARCHITECTURE.md and README.md updated.
- **Frontend is deferred until the backends are settled.** Decided
  2026-09-18. Backend work is unblocked and frontend work is not (Node is
  not installed, and screens need APIs to call), so there is nothing to gain
  from interleaving them.

  Still open, and cheap to settle before the work starts: **who writes the
  screens for the other four sections.** One React app serves all five
  services. The proposal is that Section 1 owns the shell - routing, auth
  context, API clients, layout, design system - and each section PRs its own
  feature screens, since the person who knows an API is the right person to
  render it. The alternative is Section 1 building every screen.

  Worth noting against the demo: DEMO.md steps 1-3 are all Swagger UI, and
  the only frontend moment is step 4, a **paper detail page with a changes
  panel**. That screen belongs to Storage Management and Updating data, not
  User Management, and nobody is currently assigned to it. A login page
  alone does not cover the demo.
---

## 2026-09-18 — Research Evaluation + Updating scope and design

### Team decisions (via Q&A)

- **LLM provider: DeepSeek**, not Claude/OpenAI. Chosen for cost — under
  1¢ per stance/claims call — with demo reliability handled by
  pre-warming the cache rather than by provider choice.
- **Updating's stack: Python**, sharing one codebase/image with Research
  Evaluation (two FastAPI apps). Avoids duplicating the
  `BackgroundInfoDTO`, JWT validation and HTTP client code across a
  Python and a Java project, which the original "Updating: TBD" left
  unresolved.
- **Updating's change data (`change_events`, `tracked_papers`,
  `poll_runs`) lives in Updating's own Postgres schema**, not in Storage
  Management's database, even though Section 2 of the original doc says
  Storage Management owns "every background-info/change snapshot." That
  line is read as covering the background-info snapshots themselves
  (which Storage Management does own); the derived diff/event data is
  Updating's own bookkeeping and putting it in Updating's schema avoids
  making every Updating write depend on Storage Management's uptime.
- **New-related-paper detection is out of scope for week 7.** The
  original brief listed two independent detection sources (status diff
  on a tracked paper, and new papers appearing that relate to it). Only
  the first is built for the midterm; the second moves to week 13
  alongside topic-based discovery, which needs similar infrastructure
  (OpenAlex topic/citation queries) anyway.
- **Repo layout:** Research Evaluation + Updating live in `backend/`;
  `frontend/` and `storage/` start as empty placeholder folders for the
  other two owners.

### Deviations from the original brief, found while verifying API facts live

- **Crossref retraction signal comes from `updated-by`, not `relation`.**
  The original brief marked this "UNVERIFIED — needs confirming against
  a live retracted DOI." Checked live against two retracted papers
  (`10.1016/S0140-6736(20)31180-6` and
  `10.1016/j.ijantimicag.2020.105949`): the `relation` object only ever
  held unrelated things like `has-review`. The real signal is the
  `updated-by` array, where each entry has `type` (`retraction`,
  `expression_of_concern`, `correction`, `erratum`, ...), `label`,
  `source` (`publisher` / `retraction-watch`), the notice's own `DOI`,
  and an `updated` date. Column renamed from `update_to` to
  `crossref_updates` to match.
- **`journal_legitimate` (DOAJ) dropped as a separate API call.**
  OpenAlex's work record already carries
  `primary_location.source.is_in_doaj`, sourced from DOAJ's own journal
  list, for free alongside data Research Evaluation fetches anyway. A
  separate DOAJ client would add a call, a 2 req/s rate limit, and a new
  failure mode for no new information. Caveat carried forward: DOAJ
  lists only fully open-access journals, so `in_doaj=false` for a
  subscription journal (e.g. The Lancet) is not a legitimacy signal —
  only a **true→false flip (delisting)** is treated as a change event.
- **Semantic Scholar snippets are stored and reused, not fetched fresh
  every time.** Section 2 said snippets are query-driven and fetched at
  comparison time, not stored per paper — confirmed correct. But
  re-fetching on every stance call would waste calls when nothing
  relevant changed. Research Evaluation now caches snippet results keyed
  by (candidate DOI, claim-text hash), and re-pulls only when: the claim
  text changed, the candidate paper's Crossref `updated-by` list changed
  (checked via one free Crossref lookup), or the caller passes
  `refresh=true`. Snippets still never land in Storage Management's
  per-paper `background_text` row — they belong to a (claim, candidate)
  pair, not to one paper.
- **`/evaluate/stance` takes DOIs, not Storage Management paper ids.** A
  candidate paper a new snippet/citation points at may not exist in
  Storage Management at all, and this keeps Research Evaluation free of
  runtime calls back into Storage Management just to resolve an id.
- **OpenAlex now requires an API key** for its full $1/day free usage
  budget (keyless calls get 1/10 of that). This wasn't true when the
  original brief was written. See SETUP.md.
- **No `GET /health` endpoints.** Considered and dropped — not needed
  for this project's scope; Docker Compose's own container health can
  stand in if ever needed.
- **DeepSeek `reasoning_effort` defaults to `low`**, not `high`. Chosen
  for latency and cost in the common case; raise per-call only if a demo
  pair's stance/claims output comes out wrong under low effort.
- **Research Evaluation's local sqlite cache (`cache.py`) is capped** at
  `CACHE_MAX_ENTRIES` (default 5000) with least-recently-used eviction,
  so it can't grow unbounded across a semester of development and demo
  runs. It was never a source of truth (Storage Management owns
  persistence), so evicting an entry only costs a re-fetch, not data
  loss.

### Rejected / not pursued

- A dedicated DOAJ API client — see above.
- Storing Semantic Scholar snippets in Storage Management's schema — see
  above.
- Giving Updating a hard dependency on Storage Management's database
  (shared schema) instead of its own — rejected for the reason given
  above under "Updating's change data."
