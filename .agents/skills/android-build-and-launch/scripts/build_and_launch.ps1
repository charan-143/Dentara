# Build and Launch Android App Automation Script
[CmdletBinding()]
param (
    [string]$DeviceId,
    [switch]$BuildOnly,
    [switch]$LaunchOnly
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..\..")
$AndroidDir = Join-Path $ProjectRoot "android"
$AppPackage = "com.dentara.dental"
$MainActivity = "com.example.thornburydental.MainActivity"
$ApkPath = Join-Path $AndroidDir "app\build\outputs\apk\debug\app-debug.apk"

Write-Host "== Dentara Android Build & Launch ==" -ForegroundColor Cyan
Write-Host "Project Root: $ProjectRoot"

# 1. Resolve JAVA_HOME
if (-not $env:JAVA_HOME -or -not (Test-Path $env:JAVA_HOME)) {
    $StudioJbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path $StudioJbr) {
        $env:JAVA_HOME = $StudioJbr
        Write-Host "Using Android Studio JBR: $env:JAVA_HOME" -ForegroundColor Green
    } else {
        $foundJava = Get-Command java -ErrorAction SilentlyContinue
        if ($foundJava) {
            $env:JAVA_HOME = Split-Path (Split-Path $foundJava.Source -Parent) -Parent
            Write-Host "Detected Java from PATH: $env:JAVA_HOME" -ForegroundColor Green
        } else {
            Write-Error "JAVA_HOME is not set and no suitable JDK/JBR was detected."
            exit 1
        }
    }
} else {
    Write-Host "Using JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green
}

# 2. Resolve ADB & Android SDK
$AdbPath = $null
$LocalProps = Join-Path $AndroidDir "local.properties"
if (Test-Path $LocalProps) {
    $SdkLine = Get-Content $LocalProps | Where-Object { $_ -match "^sdk\.dir\s*=" }
    if ($SdkLine) {
        $RawSdk = ($SdkLine -replace "^sdk\.dir\s*=", "").Trim().Replace("\\", "\")
        $CandidateAdb = Join-Path $RawSdk "platform-tools\adb.exe"
        if (Test-Path $CandidateAdb) {
            $AdbPath = $CandidateAdb
        }
    }
}

if (-not $AdbPath) {
    $CommonAdb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $CommonAdb) {
        $AdbPath = $CommonAdb
    } elseif (Get-Command adb -ErrorAction SilentlyContinue) {
        $AdbPath = (Get-Command adb).Source
    } else {
        Write-Error "Could not locate adb.exe in Android SDK or PATH."
        exit 1
    }
}
Write-Host "Using ADB: $AdbPath" -ForegroundColor Green

# 3. Detect Connected Device
if (-not $BuildOnly) {
    $deviceListRaw = & $AdbPath devices
    $attachedDevices = @()
    foreach ($line in ($deviceListRaw -split "`r?`n")) {
        if ($line -match "^([^\s]+)\s+device$") {
            $attachedDevices += $matches[1]
        }
    }

    if ($attachedDevices.Count -eq 0) {
        Write-Warning "No connected Android devices/emulators found in 'device' state."
        if (-not $LaunchOnly) {
            Write-Host "Proceeding with build only..." -ForegroundColor Yellow
            $BuildOnly = $true
        } else {
            Write-Error "Cannot launch app: no device connected."
            exit 1
        }
    } else {
        if (-not $DeviceId) {
            $DeviceId = $attachedDevices[0]
        }
        $DeviceModel = & $AdbPath -s $DeviceId shell getprop ro.product.model
        Write-Host "Target Device: $DeviceId ($DeviceModel)" -ForegroundColor Green
    }
}

# 4. Build Step
if (-not $LaunchOnly) {
    Write-Host "`nBuilding Android app with Gradle..." -ForegroundColor Cyan
    Push-Location $AndroidDir
    try {
        & .\gradlew.bat assembleDebug
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Gradle build failed with exit code $LASTEXITCODE"
            exit $LASTEXITCODE
        }
    } finally {
        Pop-Location
    }
    Write-Host "Build complete: $ApkPath" -ForegroundColor Green
}

# 5. Install & Launch Step
if (-not $BuildOnly -and $DeviceId) {
    if (-not (Test-Path $ApkPath)) {
        Write-Error "APK not found at $ApkPath. Please run with build enabled."
        exit 1
    }

    Write-Host "`nInstalling APK to $DeviceId..." -ForegroundColor Cyan
    & $AdbPath -s $DeviceId install -r $ApkPath
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to install APK onto $DeviceId."
        exit $LASTEXITCODE
    }

    Write-Host "Launching $AppPackage/$MainActivity..." -ForegroundColor Cyan
    & $AdbPath -s $DeviceId shell am start -n "$AppPackage/$MainActivity"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to start activity on $DeviceId."
        exit $LASTEXITCODE
    }

    Start-Sleep -Seconds 1
    $PidMatch = & $AdbPath -s $DeviceId shell pidof $AppPackage
    if ($PidMatch) {
        Write-Host "App is running successfully! Process ID: $PidMatch" -ForegroundColor Green
    } else {
        Write-Host "App launched (verify on device screen)." -ForegroundColor Yellow
    }
}

Write-Host "`nDone!" -ForegroundColor Green
