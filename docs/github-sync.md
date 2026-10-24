# Put the prepared work on GitHub

The source package includes `git-history.bundle`, preserving the feature branch and its focused commits on top of the repository's original commit. This avoids copying files onto `main` or losing the development history.

## From an authenticated local clone

Run these from an existing clone of `sanuoa/campus-slot`, not from an untracked extracted ZIP folder. Replace the bundle path with its actual extracted location.

```powershell
git fetch origin
git fetch "C:\path\to\extracted\campus-slot\git-history.bundle" feat/campus-slot-mvp
git switch -c feat/campus-slot-mvp FETCH_HEAD
git push -u origin feat/campus-slot-mvp
```

If the feature branch already exists locally, inspect it rather than overwriting it. Do not force-push. If `main` has changed since the original commit, merge `origin/main` into the feature branch and resolve changes before opening the PR.

Open a pull request targeting `main`. Use `docs/pull-request.md` as its description. GitHub Actions should run native PostgreSQL integration tests, frontend tests, the frontend build and browser interaction tests. Check their actual results before merging.

## Connection permissions

The connected GitHub integration could read this repository but branch creation and issue creation each returned HTTP 403: `Resource not accessible by integration`. Those writes have not happened. Configure or reconnect the GitHub app with the required repository write permissions before asking the assistant to push or create issues/PRs. An authenticated Git CLI on your computer is an alternative.

The local task list in `docs/backlog.md` can then become GitHub issues. Branch protection and required checks are repository settings, not settings already applied by this package.
