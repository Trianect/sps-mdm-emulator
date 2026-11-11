---
name: make-pr
description: Create or update a draft GitHub pull request for the current branch against main, filling in the repo's PR template. Use when the user runs "/make-pr", explicitly asks to open/create a PR, or asks to push/update an existing PR's description after new commits.
---

# Make PR

Create or update a pull request comparing the current branch to `main`, using the repo's PR template at `.github/PULL_REQUEST_TEMPLATE.md`.

## Important: always target the Trianect repo

This repo has an `upstream` remote pointing at the public `googlesamples/android-testdpc`, kept only for fetching Google's updates. PRs belong on `origin` (`Trianect/sps-mdm-emulator`). The `gh` default repo is a per-checkout setting (`gh repo set-default`), and without it `gh` can silently pick `upstream` when run non-interactively, so do not rely on it. Pass `--repo Trianect/sps-mdm-emulator` to **every** `gh` command in this skill (`gh pr list`, `gh pr view`, `gh pr create`, `gh pr edit`). Never create or edit a PR on `googlesamples/android-testdpc`. Push only to `origin`.

## Important: creation requires explicit request

Only run `gh pr create` when the user explicitly asks to create/open a PR (e.g. "create the PR", "open a PR", "/make-pr"). Never create one as a side effect of pushing code, committing, or any other task — check first with `gh pr list --repo Trianect/sps-mdm-emulator --head <branch>` whether a PR already exists for this branch:

- If a PR already exists for the branch, update its body instead (see below) — do not create a duplicate.
- If no PR exists and the user hasn't explicitly asked to create one, do not create one. If the user only asked you to push, just push.

## Steps to create a new PR (only when explicitly requested)

1. Run `git status` and confirm there are no uncommitted changes. If there are, stop and tell the user to commit or stash first — do not commit on their behalf unless asked.
2. Determine the current branch with `git branch --show-current`. Refuse if it is `main`.
3. Confirm no PR already exists for this branch (`gh pr list --repo Trianect/sps-mdm-emulator --head <branch> --json number,url,isDraft`). If one exists, update it instead — see "Updating an existing PR" below.
4. Run `git fetch origin`, then sync the branch with `origin` (never with `upstream`):
   - No remote branch yet: `git push -u origin <branch>`. Confirm with the user before the first push of the branch.
   - Local is ahead of `origin/<branch>`: `git push origin <branch>`.
   - Local and `origin/<branch>` have diverged: the PR automation in this repo rebases open PR branches onto `main` and force-pushes them after every push to `main`, so this is expected. Do not force-push over it. Stop and tell the user; with their go-ahead, run `git pull --rebase origin <branch>` and then push normally.
5. Read `.github/PULL_REQUEST_TEMPLATE.md` and fill it in based on the actual commits/diff since diverging from `origin/main` (`git log origin/main..HEAD`, `git diff origin/main...HEAD`; local `main` may be stale):
   - Fill in `# Summary` with what the PR does and why.
   - On the `Resolves #` line, fill in the issue number only if the user gave one or a commit/branch name references it; otherwise delete the line. The project automation moves the linked issue on the board, so never guess a number.
   - Check the one `## Type of change` box that matches the PR title prefix.
   - Fill in `## Changes made` with the key files/modules touched.
   - Leave `## Checklist` boxes unchecked unless you've actually verified them (e.g. only check "Tests added or updated" if you added/updated tests in this branch).
   - Fill in `## Notes for reviewers` if there's something non-obvious to flag, otherwise leave it empty.
   - Keep the HTML comments intact (they're guidance for humans, not filled-in placeholders to delete).
6. Draft a PR title (short, imperative, under 70 chars) matching the conventional-commit style used by the "Type of change" checklist (e.g. `feat: ...`, `fix: ...`). The org `pr-labeler` workflow checks the title against conventional commits and labels the PR from it, so the prefix must be one of the types in the template, even when the commits themselves are not written that way.
7. Create as a draft:
   ```
   gh pr create --repo Trianect/sps-mdm-emulator --draft --base main --head <branch> --title "<title>" --body "$(cat <<'EOF'
   <filled-in template>
   EOF
   )"
   ```
8. Report the PR URL returned by `gh pr create`.

## Updating an existing PR after new pushes

Whenever the user pushes new commits to a branch that already has an open PR, proactively update the PR description to reflect the current state — this does not require the user to ask each time, since it's just keeping the description in sync (not creating anything new):

1. Confirm a PR exists for the branch: `gh pr view <branch> --repo Trianect/sps-mdm-emulator --json url,body,title,isDraft`.
2. Run `git fetch origin` and re-derive the diff/log against `origin/main` and revise the existing body — keep the same template structure, update `Summary`, `Changes made`, `Type of change`, and `Checklist` to reflect all commits now on the branch (not just the newest one).
3. Update in place: `gh pr edit <branch> --repo Trianect/sps-mdm-emulator --body "$(cat <<'EOF'
<revised body>
EOF
)"`. Update `--title` too if the PR's scope has materially changed.
4. Do not change draft/ready status — leave that as the user has set it.

## Notes

- Never use `--no-verify` or force-push as part of this flow.
- Only create drafts when creating (`--draft`); never mark ready for review unless explicitly asked.
- Never invent content for the template — base every section on the actual commits/diff.
- This repo is public: PR titles and bodies are visible to everyone. Keep customer names, internal hostnames, credentials and other internal details out of them.
- Never update a final PR (i.e. non-draft PR) without explicit consent.
