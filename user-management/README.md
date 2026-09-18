# User Management — Section 1

Accounts, research folders, and JWT issuance. Spring Boot only — the React app this team also owns lives in [`../frontend/`](../frontend/).

**Other sections depend on this one.** Storage Management's `papers.owner_id` / `folder_id` are bare references into tables owned here, and both Storage Management and Research Evaluation validate the JWT this service issues *without calling back to it*. If you own another section, the part you need is **[§6 The JWT contract](#6-the-jwt-contract)**.

---

## 1. Status

| | |
|---|---|
| ✅ Done | Backend complete: scaffold · Flyway `V1__init` · entities + repos · `SecurityConfig` (base64 JWT per CONTRACTS.md) · `/auth/register` + `/auth/login` · `/folders` CRUD · error handling · Swagger · **6 passing tests**. Merged with `main`. |
| 🔜 Next | Fill in `application-local.yml`, then run — the first live Flyway migration against Supabase. Nothing else is blocked on it |
| ⏸ Later | Frontend — **login page only** for now (§8). Needs Node installed; blocks nothing else |

Branch: `feat/user-management`.

---

## 2. Setup

### Prerequisites

Only **JDK 21+** is required for the backend. Everything else is deliberately avoided:

| Not needed | Why |
|---|---|
| Maven | `mvnw` / `mvnw.cmd` download it themselves. **Never run a bare `mvn`.** |
| Docker / WSL | No local database — we use Supabase |
| Postgres / psql | Same; use the Supabase dashboard's SQL editor |
| Node | Frontend only (§8), not the backend |

### Database — one Supabase project per developer

The free plan allows **2 active projects per organization**, so create your own rather than sharing. That's the point: nobody else's Flyway run touches your schema, and `V1__init.sql` stays editable until it's merged. Only the deployed demo needs a shared project.

Free plan: 500 MB database, 1 GB file storage.

### ⚠️ Getting the connection string right

This is the step that goes wrong. Click **Connect** in the dashboard. The tabs are *Framework · Server · MCP · Direct connection · ORM* — there is **no tab labelled "Session pooler"**. Look under **Server**.

Shortcut: session and transaction mode are **the same host on different ports**, so take any pooler string and set the port to **5432**.

| Mode | Port | Use? |
|---|---|---|
| Direct (`db.<ref>.supabase.co`) | 5432 | ❌ **IPv6-only** on the free plan. On IPv4 campus wifi this hangs, then fails with a timeout that looks like bad credentials |
| **Session pooler** | 5432 | ✅ **This one.** IPv4 everywhere, supports prepared statements, so HikariCP and Flyway behave normally |
| Transaction pooler | 6543 | ❌ No prepared statements. Needs `prepareThreshold=0` and fights Flyway |

Tell them apart at a glance:

| | Direct (avoid) | Pooler (use) |
|---|---|---|
| Host | `db.<ref>.supabase.co` | `aws-N-<region>.pooler.supabase.com` |
| Username | `postgres` | `postgres.<ref>` ← **has a dot** |

The `aws-N-<region>` index can't be derived from your region — copy it from the dialog.

### Your credentials file

Fill in `src/main/resources/application-local.yml` (already created, already gitignored):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://aws-N-<region>.pooler.supabase.com:5432/postgres
    username: postgres.<PROJECT-REF>
    password: <your database password>

jwt:
  # base64, NOT raw text. Generate: openssl rand -base64 32
  secret: <base64 of 32+ random bytes>
```

Then run with the `local` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Three things that bite here:

- **The `jdbc:` prefix.** Supabase shows `postgresql://...`; Spring needs `jdbc:postgresql://...`.
- **Spring Boot does not read `.env` files.** A `.env` in the repo does nothing on its own — hence `application-local.yml`.
- **Three separate properties, not one URI.** Supabase passwords often contain characters that need percent-encoding inside a URL, and the resulting failure looks exactly like a wrong password.

`<PROJECT-REF>` is the id in your dashboard URL. The **database** password is the one set at project creation — not your Supabase login. Lost it? Settings → Database → Reset database password.

### ⚠️ Free projects pause after 1 week idle

A direct risk to the week 7 demo. Set this up, don't touch it for a week, and it's asleep when you present. **Resume it and re-run §9 the day before the demo**, not that morning.

---

## 3. Architecture decisions

**JWT via Spring Security's OAuth2 Resource Server**, not a hand-rolled filter. Security 7 added `NimbusJwtEncoder.withSecretKey(key).build()`, which defaults to HS256 — so issuing and validating are one line each, with no `JwsHeader` or `ImmutableSecret` plumbing. Other services need only the key bean, the decoder bean, and one `oauth2ResourceServer` line.

**UUID primary keys, not `bigserial`.** `users.id` crosses a service boundary into Storage Management. A UUID is globally unique, non-enumerable, and won't collide if services get seeded independently — and it keeps the JWT `sub` from leaking how many users exist.

**Flyway owns the schema, with `ddl-auto: validate`.** The schema is a contract three other services read, so it lives in a reviewed file in git rather than emerging as a side effect of entity annotations. `validate` fails fast at startup if an entity drifts, instead of silently altering the table the way `update` would.

**Its own `usermgmt` schema, not `public`.** Two config lines, and it makes the "bare references, not enforced FKs" rule physical — Section 2 in a `storage` schema *cannot* accidentally FK into `usermgmt.users`. It also lets one Supabase project host every service when you consolidate for the demo.

**`folders.owner_id` is a plain `UUID` field, not a `@ManyToOne User`.** The DB still enforces the FK from the migration, but JPA stays trivial and `findAllByOwnerId` carries no lazy-loading or N+1 risk. Every folder query is by owner anyway.

**No Lombok.** It patches `javac` internals and historically breaks on brand-new JDKs; JDK 26 is two months old and its Lombok support is unverified. With two entities the cost is near zero — hand-written accessors, and every DTO is a `record`.

**Java 21, not 26.** `<java.version>21</java.version>` compiles fine under a JDK 26 toolchain via `--release`, and matches the `eclipse-temurin:21-jre` image Section 5 will deploy on, so local and container agree.

---

## 4. ⚠️ Spring Boot 4 renamed the starters

If you copy a `pom.xml` from any Boot 3 tutorial, it will not resolve. Verified against our generated pom:

| Boot 3 | **Boot 4** |
|---|---|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `spring-boot-starter-oauth2-resource-server` | `spring-boot-starter-security-oauth2-resource-server` |
| `flyway-core` (added manually) | `spring-boot-starter-flyway` |
| `spring-boot-starter-test` (one jar) | **per-slice**: `-webmvc-test`, `-data-jpa-test`, `-security-test`, … |

Boot 4 also ships **Spring Security 7**, so essentially every JWT tutorial online targets an API that no longer compiles.

---

## 5. Schema

`src/main/resources/db/migration/V1__init.sql`:

```sql
create table users (
  id            uuid primary key default gen_random_uuid(),
  email         varchar(255) not null unique,
  password_hash varchar(255) not null,
  created_at    timestamptz  not null default now()
);

create table folders (
  id         uuid primary key default gen_random_uuid(),
  owner_id   uuid not null references users(id) on delete cascade,
  name       varchar(255) not null,
  created_at timestamptz not null default now()
);
create index idx_folders_owner on folders(owner_id);
create unique index uq_folders_owner_name on folders(owner_id, lower(name));
```

The table is `users`, not `user` — `user` is reserved in Postgres. `gen_random_uuid()` is built into Postgres 13+, no pgcrypto needed.

---

## 6. The JWT contract

**[CONTRACTS.md](../CONTRACTS.md) is the source of truth** for this and
every other cross-service interface. Repeated here only as a pointer:

| Item | Value |
|---|---|
| Header | `Authorization: Bearer <token>` |
| Algorithm | HS256 |
| `sub` | the user **UUID** as a string — `jwt.getSubject()` |
| Other claims | `iss: user-management`, `email`, `iat`, `exp` |
| TTL | 2 hours |
| Secret | env var `JWT_SECRET`, **base64 of 32+ random bytes** |

### ⚠️ The secret is base64 — decode it

`JWT_SECRET` is base64-encoded, and **every service base64-decodes it before
building the key**:

```java
byte[] bytes = Base64.getDecoder().decode(secret.trim());   // NOT secret.getBytes()
return new SecretKeySpec(bytes, "HmacSHA256");
```

Using the raw characters instead derives a completely different key from the
same string, so tokens issued here would fail validation in the Python
services — the Java/Python bug CONTRACTS.md warns about. `JwtSecretKeyTest`
exists specifically to stop that regressing. Generate a secret with
`openssl rand -base64 32`.

To validate in another Spring service, that is the whole job:

```java
@Bean JwtDecoder jwtDecoder(SecretKey k) { return NimbusJwtDecoder.withSecretKey(k).build(); }
// + .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults())) in your SecurityFilterChain
```

**Never commit the secret.** Any service holding it can mint a valid token for
any user. Local dev via `application-local.yml`; deployment via GitHub Actions
secrets.

Note that Updating mints **service tokens** with `sub=svc:updating` and
`role=service`. This service never accepts them — it only issues user tokens —
but Storage Management must handle a `sub` that is not a UUID.

## 7. API

All endpoints Swagger-documented at `/swagger-ui.html`.

| Method | Path | Auth | Notes |
|---|---|---|---|
| `POST` | `/auth/register` | — | 409 if email taken |
| `POST` | `/auth/login` | — | Returns token + expiry |
| `GET` | `/folders` | Bearer | Caller's folders only |
| `POST` | `/folders` | Bearer | |
| `PUT` | `/folders/{id}` | Bearer | |
| `DELETE` | `/folders/{id}` | Bearer | |

Two rules that matter more than the CRUD:

- **Owner always comes from the token**, never the request body — `@AuthenticationPrincipal Jwt jwt` → `UUID.fromString(jwt.getSubject())`. A client-supplied `ownerId` would let any user write into another's account.
- **404, not 403**, for a folder belonging to someone else. A 403 confirms the id is real and leaks other users' folder ids.

Likewise `/auth/login` returns the **same generic 401** for an unknown email and a wrong password, so it can't be used to enumerate registered addresses.

`OpenApiConfig` registers a `bearer-jwt` security scheme so you can paste a token into Swagger UI's **Authorize** box. Without it every protected endpoint returns 401 in the UI and the demo stalls.

---

## 8. Frontend — moved

The React app is a **separate deliverable in [`../frontend/`](../frontend/)**,
with its own README, build tooling and Dockerfile. This team owns it, but it is
not part of the Spring Boot build and nothing here depends on it.

Scope there for now is the **login page only**. See
[`../frontend/README.md`](../frontend/README.md).

## 9. Verification

1. Supabase project is **awake**, `application-local.yml` filled in.
2. `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` — Flyway should log `V1__init` applied. A hang ending in a connection timeout almost always means the direct string instead of the session pooler (§2).
3. `http://localhost:8080/swagger-ui.html` → `POST /auth/register` → copy token → **Authorize** → `POST /folders` → `GET /folders` returns it.
4. **Check the contract:** decode the token at jwt.io; confirm `sub` is the user's UUID and `iss` is `user-management`. This is the check that matters most — it's what Sections 2–4 depend on.
5. Negative paths: `GET /folders` with no token → 401; with an expired token → 401.
6. Rows landed: Supabase → SQL Editor → `select id, email, created_at from usermgmt.users;` (never select `password_hash` onto a shared screen).

**Week 7 demo path:** register → login showing the JWT come back → create a folder.

---

## 10. Gotchas

| Trap | Correct move |
|---|---|
| `JWT_SECRET` used as raw text | It is **base64** — decode it first, or Python services reject every token |
| Property named `app.jwt.secret` | CONTRACTS.md says `JWT_SECRET`, so the property is `jwt.secret` |
| Supabase **direct** connection string | IPv6-only on free; times out on IPv4 wifi. Use the session pooler, port 5432, user `postgres.<ref>` |
| Transaction pooler (6543) | No prepared statements; fights Flyway. Session mode only |
| Free project **pauses after 1 week idle** | Resume and re-verify the *day before* the demo |
| Boot 3 starter names | Renamed in Boot 4 — see §4 |
| Security 6 JWT tutorials | Won't compile on Security 7; use `NimbusJwtEncoder/Decoder.withSecretKey` |
| springdoc 2.x | Boot 3 only — we use **3.1.1** |
| `.env` file for secrets | Spring Boot doesn't read them; use `application-local.yml` |
| JWT secret under 32 chars | HS256 needs 256 bits; app fails at startup |
| Lombok on JDK 26 | Unverified; skip it |
| Table named `user` | Reserved in Postgres; use `users` |
| 403 on someone else's folder | Leaks that the id exists; return 404 |
| `tailwindcss init -p` | Removed in v4 |
| `z.string().email()` | Deprecated in Zod 4 — use `z.email()` |

---

## 11. PR breakdown

Seven PRs onto `feat/user-management`, each independently reviewable:

1. Scaffold + datasource config + Flyway `V1__init`
2. Entities, repositories, `application.yml`
3. `SecurityConfig` + `TokenService` — **JWT issuance/validation**
4. `/auth/register` + `/auth/login` + error handler
5. `/folders` CRUD with ownership enforcement
6. OpenAPI config + controller tests
7. Frontend: scaffold, auth context, login/register, folders page

**PR 3 unblocks Sections 2 and 3.** Land it early and tell those owners the moment the claim contract is merged.
