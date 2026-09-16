[CmdletBinding()]
param(
    [string]$BranchName = "",
    [string]$Title = "",
    [string]$Body = "",
    [string]$BaseBranch = "master"
)

$ErrorActionPreference = "Stop"

Write-Host "=== Create PR Automation Script ===" -ForegroundColor Cyan

# 1. Verify working directory has changes or is on a feature branch
$status = git status --porcelain
$currentBranch = (git branch --show-current).Trim()

if (-not $status -and ($currentBranch -eq "master" -or $currentBranch -eq "main")) {
    Write-Warning "No working changes detected and currently on default branch '$currentBranch'. Nothing to commit or PR."
    exit 0
}

# 2. Determine / create branch
if ($BranchName -and $currentBranch -ne $BranchName) {
    Write-Host "Checking out branch: $BranchName" -ForegroundColor Yellow
    git checkout -B $BranchName
    $currentBranch = $BranchName
} elseif (-not $BranchName -and ($currentBranch -eq "master" -or $currentBranch -eq "main")) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmm"
    $BranchName = "feature/update-$timestamp"
    Write-Host "Auto-generating branch name: $BranchName" -ForegroundColor Yellow
    git checkout -b $BranchName
    $currentBranch = $BranchName
}

# 3. Stage and commit if there are uncommitted changes
if ($status) {
    Write-Host "Staging changes..." -ForegroundColor Yellow
    git add -A
    
    if (-not $Title) {
        $Title = "chore: update project components"
    }
    
    Write-Host "Committing changes..." -ForegroundColor Yellow
    if ($Body) {
        git commit -m "$Title" -m "$Body"
    } else {
        git commit -m "$Title"
    }
} else {
    Write-Host "Working tree already clean on branch $currentBranch." -ForegroundColor Green
}

# 4. Push branch
Write-Host "Pushing $currentBranch to origin..." -ForegroundColor Yellow
git push -u origin $currentBranch

# 5. Create Pull Request using gh CLI if available
$ghPath = "C:\Program Files\GitHub CLI\gh.exe"
if (-not (Test-Path $ghPath)) {
    $candidate = Get-Command gh -ErrorAction SilentlyContinue
    if ($candidate) { $ghPath = $candidate.Source }
}

if (Test-Path $ghPath) {
    Write-Host "Creating GitHub Pull Request via gh CLI..." -ForegroundColor Cyan
    if (-not $Title) { $Title = "Updates for $currentBranch" }
    if (-not $Body) { $Body = "Automated Pull Request for $currentBranch." }

    try {
        & $ghPath pr create --title "$Title" --body "$Body" --base $BaseBranch
        Write-Host "Pull Request successfully created!" -ForegroundColor Green
    } catch {
        Write-Warning "Could not automatically create PR via gh CLI: $_"
        $repoUrl = (git remote get-url origin).Trim() -replace "\.git$", ""
        Write-Host "Open PR manually at: $repoUrl/compare/$BaseBranch...${currentBranch}?expand=1" -ForegroundColor Cyan
    }
} else {
    $repoUrl = (git remote get-url origin).Trim() -replace "\.git$", ""
    Write-Host "Pull Request compare URL: $repoUrl/compare/$BaseBranch...${currentBranch}?expand=1" -ForegroundColor Cyan
}
