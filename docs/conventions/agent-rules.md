# Agent Rules (full rulebook)

These mirror and expand [`../../CLAUDE.md`](../../CLAUDE.md) §2. Any agent or
contributor working in this repo must follow them.

## 1. Secrets & credentials — hard stop
- Never commit passwords, API keys, JWT secrets, tokens, keystores, or real DB
  connection strings — not in code, YAML, docs, tests, or commit messages.
- Committed config uses **placeholders only** (`${DB_PASSWORD}`, `${JWT_SECRET}`).
  Real values come from environment variables or a **git-ignored**
  `application-local.yml` / `.env`.
- Before **every** commit, run `git diff --cached` and confirm no secret is staged.
- If a secret is ever committed: rotate it immediately (treat as leaked), remove
  it, and flag it. Deleting it later does **not** remove it from history.

## 2. No AI / agent attribution
- Do **not** add `Co-Authored-By` trailers, "Generated with …" footers, or any
  model/agent/tool name to commits, PRs, branch names, code comments, or docs.
- Commit only as the repository's configured git identity. Do not change it.
- Write in a neutral, human voice. Do not sign or watermark work.

## 3. Git discipline
- `main` stays buildable. Branch for features (`feat/<scope>-<desc>`).
- Conventional Commits (see [git-and-collaboration.md](git-and-collaboration.md)).
- Never force-push shared branches or rewrite published history.
- Never commit build output (`target/`), IDE folders, or the wrapper jar.

## 4. Testing is mandatory
- No feature is done without passing tests (see
  [testing-strategy.md](testing-strategy.md)). `./mvnw clean verify` must be green
  before you consider a task complete or open a PR. Every bug fix ships with a
  regression test.

## 5. Architecture boundaries
- A service touches **only its own database**. All cross-service data via Feign.
- Internal endpoints (`/inventory/reserve`, `/wallets/*/debit`, …) are never
  exposed through the gateway.
- Money is `BigDecimal`; identity comes from the validated JWT, never the body.

## 6. Scope & process
- Follow the phased plan; finish and verify a phase before the next.
- Don't add new frameworks/dependencies without justifying it in the relevant doc.
- Keep docs in sync: change a contract → update its doc in the same commit.

## 7. Safety
- Don't run destructive commands against real data. Test DBs are disposable
  (Testcontainers). Confirm before deleting/overwriting anything you didn't create.
