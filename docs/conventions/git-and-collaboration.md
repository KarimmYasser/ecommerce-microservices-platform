# Git & Collaboration

## Golden rules (see also [../../CLAUDE.md](../../CLAUDE.md) §2)
1. **No secrets in history** — ever. Check `git diff --cached` before committing.
2. **No AI/agent attribution** — no `Co-Authored-By`, no "Generated with…", no
   model/agent names in commits, code, or docs. Commit as the repo's git user only.
3. **Never rewrite published history**; never force-push shared branches.

## Branching
- `main` is always buildable.
- Feature work on branches: `feat/<service>-<short-desc>`, e.g.
  `feat/inventory-product-crud`. Also `fix/…`, `docs/…`, `chore/…`.
- Merge to `main` via PR/merge once the phase's checklist items pass.

## Commit messages (Conventional Commits)
```
<type>(<scope>): <summary in imperative, lower-case>

<optional body: what & why, not how>
```
- **types:** `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`.
- **scope:** the module — `inventory`, `wallet`, `shop`, `gateway`, `eureka`,
  `config`, `infra`, `docs`.
- Examples:
  - `feat(inventory): add product search and category filter`
  - `feat(shop): implement checkout saga with wallet debit`
  - `docs(api): document inter-service feign contracts`
- One logical change per commit. Keep them small and reviewable.

## What must never be committed
`target/`, `*.class`, `*.jar` (incl. the Maven wrapper jar), IDE folders
(`.idea/`, `.vscode/`, `.settings/`), OS files, and **any** file containing real
credentials: `.env`, `*-local.yml`, `*.local`, keystores, `*.pem`. These are in
`.gitignore` — keep them there.

## If a secret is ever committed
Rotate the secret immediately (assume it's compromised), remove it, and tell the
team. Do not just delete it in a later commit — it stays in history.

## Pull requests
- Title = the main commit summary. Body: what changed, why, how to test, which
  phase/checklist items it completes.
- No agent attribution in PR descriptions.
