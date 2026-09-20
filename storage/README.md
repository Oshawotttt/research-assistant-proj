# Storage Management

Section 2 of the research assistant project. Owns all Postgres and file
persistence: `papers`, `notes`, `background_metadata`, `background_text` and
`authors_background`, plus the PDF bytes on disk.

Research Evaluation fetches uploaded PDFs through this service, Updating polls
it on every run and diffs the background-info snapshots it stores, and the
frontend reads papers, notes and background info from it.

- Design and ownership: [../ARCHITECTURE.md](../ARCHITECTURE.md) §2
- Cross-service API contract: [../CONTRACTS.md](../CONTRACTS.md)
- Accounts, keys and the shared env vars: [../SETUP.md](../SETUP.md)

## Stack

Java 25, Spring Boot 4.1.1, Maven, Postgres via Flyway + Spring Data JPA,
springdoc-openapi for Swagger UI. Runs on port **8080**, as
`storage-management` on the Compose network — `backend/.env.example` already
points at `http://storage-management:8080`.

## Prerequisites

- **JDK 25.** Set `JAVA_HOME` if it isn't already:

  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.4"
  ```

  To set it permanently:
  `setx JAVA_HOME "C:\Program Files\Java\jdk-25.0.4"` (reopen the terminal).

- **No Maven install needed.** Use the bundled wrapper — `.\mvnw.cmd` on
  Windows, `./mvnw` elsewhere. Never bare `mvn`.

- **A Postgres to talk to.** Flyway and JPA both need one at startup, so the
  service will not boot without it. For local work, the dev stack in
  `../backend/docker-compose.dev.yml` already has one:

  ```
  docker compose -f ../backend/docker-compose.dev.yml up -d postgres
  ```

## Configuration

Copy `.env.example` to `.env` and fill it in. The app imports `./.env`
automatically on startup, so no exporting by hand; in Docker the same names
arrive as real environment variables instead.

| Var | Purpose |
|---|---|
| `DATABASE_URL` | JDBC URL. **Not** the SQLAlchemy-style URL `backend/.env` uses for the same server. |
| `DATABASE_USER` / `DATABASE_PASSWORD` | Database credentials |
| `JWT_SECRET` | base64 of 32+ random bytes, shared with User Management, decoded before use |
| `RE_BASE_URL` | Research Evaluation's base URL |
| `GROBID_URL` | GROBID's base URL |
| `FILE_STORAGE_PATH` | Directory uploaded PDFs are written to |

All of them are required and the service will not start without them. The
`storage.*` ones report cleanly — `Property: storage.jwtSecret / Reason: must
not be blank`. A missing `DATABASE_URL` is caught by Spring's own datasource
setup instead, which fails with a long stack trace ending in `'url' must start
with "jdbc"`; that almost always means `.env` is missing or wasn't picked up.

## Running

```powershell
.\mvnw.cmd spring-boot:run
```

Swagger UI: <http://localhost:8080/docs>

```powershell
.\mvnw.cmd test              # needs Postgres running
.\mvnw.cmd clean package     # builds target/storage-0.0.1-SNAPSHOT.jar
```

Docker, standalone:

```
docker build -t storage-management .
docker run --rm -p 8080:8080 --env-file .env -v storage_files:/app/files storage-management
```

## State

Scaffolding. The application boots, Swagger UI serves, and configuration is
validated at startup — but **no endpoints, entities or migrations exist yet**.
Security currently opens Swagger UI and denies everything else; the JWT filter
that validates user and service tokens replaces it next, alongside the Flyway
migrations for the five tables.
