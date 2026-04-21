Analyze the current git state and create conventional commits, grouping related files together intelligently.

## Steps

1. Run `git status` and `git diff HEAD` in parallel to understand all changes
2. Plan the commit groups (see grouping rules below) — show the plan to the user before committing
3. For each group, stage only those files explicitly, then commit
4. Repeat until `git status` is clean

## Grouping Rules

Files belong in the **same commit** when they are directly coupled:
- An entity and its DTO that were changed together
- A new enum and the entity/DTO that uses it
- A service and its corresponding repository
- A CI/config file change that is one logical change
- A deleted file and the file that replaced it (e.g. rename/move)

Files must be in **separate commits** when they are independent concerns:
- Changes in `common/` vs changes in `server/` — always separate
- A new feature vs a refactor, even in the same package
- Model changes vs DTO changes (unless they are a direct pair)
- Multiple unrelated entities changed for different reasons

When unsure whether two files are related, keep them separate.

## Commit Type

Pick one per commit based on what changed:
- `feat` — new file or new functionality added
- `fix` — bug fix
- `update` — change to existing feature (not a bug fix)
- `refactor` — restructure without behavior change
- `chore` — build config, CI, dependencies, tooling, docs

## Commit message format

```
type(scope): short description

Optional body if the why is not obvious.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
Co-Authored-By: Phạm Trà <phamvantra.dev@gmail.com>
```

- Subject line: lowercase, no period, under 72 chars
- Scope: the folder or module affected, e.g. `server/model`, `common`, `ci`
- Use a HEREDOC to pass the message so newlines are preserved

## Rules

- Never use `git add .` or `git add -A` — always stage files explicitly by name
- Never skip hooks (`--no-verify`)
- If nothing to commit, say so and stop
