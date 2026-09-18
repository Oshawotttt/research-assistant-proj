# Frontend

The React app for the whole product. Owned by the User Management team
(Section 1), but it is **not** a User Management UI — the architecture has one
React app talking to several services, so screens for papers, background-info
and change events will live here too.

Backend for this service: [`../user-management/`](../user-management/).

## Status

**Not scaffolded yet.** Needs Node installed first:

```powershell
winget install -e --id OpenJS.NodeJS.LTS
```

Nothing else is blocked on this — the entire backend is built and tested
without it.

## Scope for now: the login page only

Deliberately minimal. Everything else waits.

- `/login` — email + password, posts to `/auth/login`, stores the token, redirects
- An Axios instance with a **request** interceptor attaching the bearer token,
  and a **response** interceptor that clears it and redirects to `/login` on 401,
  so an expired token never renders a broken page
- `AuthContext` holding `{ token, user, login, logout }`, rehydrating from
  `localStorage` on mount

**Deferred:** register page, folder list, folder detail, protected-route
wrapper, and every other section's screens.

## Planned stack

Vite + React 19 + TypeScript · Tailwind v4 · shadcn/ui · react-router 7 ·
react-hook-form + Zod · Axios.

```bash
npm create vite@latest . -- --template react-ts
npm i axios react-router-dom react-hook-form zod @hookform/resolvers
npm i -D tailwindcss @tailwindcss/vite
npx shadcn@latest init
```

## Traps worth knowing before you start

- **Tailwind v4 is not the v3 setup.** Use the `@tailwindcss/vite` plugin and
  `@import "tailwindcss";` in your CSS. There is no `npx tailwindcss init -p`
  and no `tailwind.config.js` — that command was removed in v4, and every
  older tutorial still shows it.
- **Zod 4**: use `z.email()`. The `z.string().email()` form is deprecated.
- **Token goes in `localStorage`**, not an httpOnly cookie. A cookie set by
  User Management would not be sent to Storage Management on a different
  origin without a shared parent domain. See DECISIONS.md.
- **Decode the JWT client-side for display only.** Never branch authorization
  on it — the server re-verifies every request.
- **Keep the API base URL a variable** (`VITE_API_BASE_URL`), not a literal.
  A second client will point at Storage Management.

## API

`http://localhost:8080` in local dev. Request and response shapes are in
[CONTRACTS.md](../CONTRACTS.md) — treat that as the source of truth, not this
file.

## ⚠️ Open question: who builds the other sections' screens

One React app serves all five services, and the brief says Section 1 owns "the
app's frontend" — but not who writes screens for the other four.

| Reading | Consequence |
|---|---|
| Section 1 writes every screen | This team builds UI for papers, background-info, change events and discovery, learning four other people's APIs |
| Section 1 owns the **shell**, each section adds its own screens | This team builds routing, auth, layout, API clients and the design system; others PR their features in |

**Recommendation: the second.** The person who knows an API is the right person
to build its screens, and it stops this team becoming the bottleneck for four
others. Needs a team decision — tracked in [DECISIONS.md](../DECISIONS.md).
