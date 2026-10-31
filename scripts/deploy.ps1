# Build AdShield and install it on a connected device or emulator (Windows).
#
# Usage:
#   .\scripts\deploy.ps1              # first connected device
#   .\scripts\deploy.ps1 -d           # physical USB device
#   .\scripts\deploy.ps1 -e           # running emulator
#   .\scripts\deploy.ps1 -s SERIAL    # a specific serial
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

$Package = "com.local.adshield"
$ApkPath = "app\build\outputs\apk\debug\app-debug.apk"
$Target = $args

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Error "adb not found on PATH (install Android SDK platform-tools)"
}

& adb @Target get-state *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "No authorized device found. Enable USB debugging and accept the RSA prompt." -ForegroundColor Red
    & adb devices
    exit 1
}

Write-Host "[1/4] Building debug APK..." -ForegroundColor Yellow
& .\gradlew.bat :app:assembleDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "[2/4] Installing $ApkPath..." -ForegroundColor Yellow
& adb @Target install -r $ApkPath
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "[3/4] Granting notification permission..." -ForegroundColor Yellow
& adb @Target shell pm grant $Package android.permission.POST_NOTIFICATIONS 2>$null

Write-Host "[4/4] Launching AdShield..." -ForegroundColor Yellow
& adb @Target shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Out-Null

Write-Host "Done. Tap the toggle on the dashboard and accept the one-time VPN prompt." -ForegroundColor Green
