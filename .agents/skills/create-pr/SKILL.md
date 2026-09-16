---
name: create-pr
description: >-
  Creates a new branch, stages and commits all changes with a descriptive message, pushes to GitHub origin,
  and opens a Pull Request with a full description. Use whenever asked to branch, commit, push, or create a PR.
---

# Create PR Skill

Automates creating a new Git branch, staging all working directory changes, creating a clean conventional commit, pushing to origin, and creating a GitHub Pull Request with a comprehensive description.

---

## Fast Automated Execution

Run the helper script from the workspace root:

```powershell
powershell -ExecutionPolicy Bypass -File .agents/skills/create-pr/scripts/create_pr.ps1 -BranchName "feature/my-feature" -Title "feat: my feature description"
```

If `-BranchName` or `-Title` are omitted, the script inspects current changes with `git status` and interactively prompts or automatically infers appropriate branch and PR details.

---

## Step-by-Step Procedure

### 1. Analyze Working Changes
Inspect modified, added, and deleted files:
```powershell
git status
git diff
```

### 2. Create and Checkout New Branch
Formulate a descriptive kebab-case branch name (e.g. `feature/<name>`, `fix/<name>`, `ui/<name>`):
```powershell
git checkout -b <branch-name>
```

### 3. Stage All Changes
```powershell
git add -A
```

### 4. Commit with Conventional Format
Structure the commit with a concise title and a detailed bulleted summary:
```powershell
git commit -m "<type>(<scope>): <summary>" -m "- Bullet 1`n- Bullet 2`n- Bullet 3"
```

### 5. Push to Remote Origin
Set upstream tracking and push the branch:
```powershell
git push -u origin <branch-name>
```

### 6. Create Pull Request
Use the GitHub CLI (`gh`):
```powershell
& "C:\Program Files\GitHub CLI\gh.exe" pr create --title "<Title>" --body "<Detailed Body>" --base master
```

If GitHub CLI requires login or interactive review, provide the GitHub comparison URL:
```
https://github.com/<owner>/<repo>/compare/master...<branch-name>?expand=1
```

---

## PR Description Standard Template

```markdown
## Summary of Changes
- **<Component/Area>**: High-level explanation of what was added or refactored.

## Key Motivations & UX Improvements
- Why this change was needed.
- How user-facing issues were resolved.

## Verification
- Automated build / tests passing.
- Physical device verification details and screenshots.
```
