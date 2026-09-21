# Frontend

The React app for the whole product. Owned by the User Management team
(Section 1), but it is **not** a User Management UI — the architecture has one
React app talking to several services, so screens for papers, background-info
and change events will live here too.

Backend for this service: [`../user-management/`](../user-management/).

## Status — parked

**Deferred until the backends are settled** (DECISIONS.md, 2026-09-18).
Nothing here is started, and nothing else is blocked on it.

When work does start, the first prerequisite is Node, which is not installed:

```powershell
winget install -e --id OpenJS.NodeJS.LTS
```

## Scope when it resumes: the login page only

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


## Ownership: Section 1 builds the shell, each section builds its own screens

Settled 2026-09-18 — see [DECISIONS.md](../DECISIONS.md).

**Section 1 provides the shell** — the parts every screen sits inside:

| Shell (Section 1 writes and maintains) | What it does |
|---|---|
| `src/router.tsx` | Which URL shows which screen |
| `src/auth/AuthContext.tsx` | Who is logged in; login and logout |
| `src/auth/ProtectedRoute.tsx` | Bounces logged-out users to `/login` |
| `src/api/*.ts` | One Axios client per service, token already attached |
| `src/layout/` | Nav bar and page chrome |
| `src/components/ui/` | Button, Input, Card, Dialog (shadcn) |
| `src/pages/` | Login, register, folders — Section 1 screens |

**Each other section builds its own screens.** If you own Storage Management,
Research Evaluation or Updating, the UI for your data is yours. You know what
your fields mean; Section 1 does not.

### What the shell already does for you

Do not rebuild any of this:

- **Auth is handled.** If your screen renders at all, the user is logged in.
  Get them with `const { user } = useAuth()`.
- **The token is attached automatically.** Just call the API client. Never read
  `localStorage` or set an `Authorization` header yourself.
- **401s are handled.** An expired token clears itself and redirects to login.
- **UI components exist.** Import from `@/components/ui/` instead of styling
  raw buttons, so every screen looks like one product.

### Adding your feature

Everything of yours lives in one folder:

```
src/features/<your-section>/
├── api.ts          calls to YOUR service
├── types.ts        YOUR DTOs
├── routes.tsx      exports your routes
└── PaperPage.tsx   your screens
```

Export your routes from `routes.tsx` and the shell composes them in. **Do not
edit `router.tsx` directly** — if four people edit one file, you get four merge
conflicts. Export a route array and Section 1 wires it up once.

Request and response shapes come from [CONTRACTS.md](../CONTRACTS.md), the
source of truth. If your screen needs a field the API does not return, that is
a CONTRACTS.md change plus a backend change — not something to work around in
the component.

### Conventions

- **Always handle three states**: loading, error, and empty. A screen that
  renders nothing while fetching looks broken.
- **Show the message the server sent on error.** Every endpoint returns the
  same `ApiError` shape and its `message` is written to be read by a human.
- **Keep API calls in `api.ts`**, not inside components.
- **No new Axios instances.** Use the shell client for the service you are
  calling, or ask Section 1 to add one.

### You will need to write some React

Not a lot — a feature folder is mostly copying the patterns the shell
establishes — but it is not zero. If that is a problem for your section, say so
now rather than in week 6.
