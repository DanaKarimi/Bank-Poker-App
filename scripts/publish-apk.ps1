param(
    [string]$RemoteHost = "dd-karimi@192.168.70.162",
    [string]$RemoteReleasesDir = "~/BankPoker/releases",
    [string]$ApkPath = "",
    [bool]$ForceUpdate = $true
)

$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
if ([string]::IsNullOrWhiteSpace($ApkPath)) {
    $ApkPath = Join-Path $RepoRoot "app\build\outputs\apk\debug\app-debug.apk"
}
$GradleFile = Join-Path $RepoRoot "app\build.gradle.kts"

if (-not (Test-Path $ApkPath)) {
    throw "APK not found at $ApkPath. Run '.\gradlew.bat assembleDebug --no-daemon' first."
}
if (-not (Test-Path $GradleFile)) {
    throw "Gradle file not found at $GradleFile"
}

$gradleContent = Get-Content -Raw -Path $GradleFile
$versionCode = 1
$versionName = "1.0"

if ($gradleContent -match 'versionCode\s*=\s*(\d+)') {
    $versionCode = [int]$Matches[1]
}
if ($gradleContent -match 'versionName\s*=\s*"([^"]+)"') {
    $versionName = $Matches[1]
}

$apkItem = Get-Item -Path $ApkPath
$sizeBytes = $apkItem.Length
$sizeMB = [math]::Round($sizeBytes / 1MB, 2)
$builtAt = $apkItem.LastWriteTimeUtc.ToString("o")
$publishedAt = (Get-Date).ToUniversalTime().ToString("o")
$versionedFileName = "BankPoker-v$versionName.apk"

$versionObj = [ordered]@{
    versionName = $versionName
    versionCode = $versionCode
    fileName    = $versionedFileName
    apkUrl      = "/releases/$versionedFileName"
    forceUpdate = $ForceUpdate
    whatsNew    = @(
        "Unified Edit Buy-In & Exit sheets to match Buy-In / Exit design",
        "Cleaner Group screen without unnecessary Pending Requests",
        "Restricted Password Reset management exclusively to Super Admin",
        "Fixed mobile Web Notification panel alignment"
    )
    sizeBytes   = $sizeBytes
    sizeMB      = $sizeMB
    builtAt     = $builtAt
    publishedAt = $publishedAt
}

$tmpVersionJson = Join-Path ([System.IO.Path]::GetTempPath()) "bankpoker-version-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()).json"
try {
    $jsonText = $versionObj | ConvertTo-Json -Depth 4
    [System.IO.File]::WriteAllText($tmpVersionJson, $jsonText, (New-Object System.Text.UTF8Encoding($false)))

    Write-Host "Publishing $versionedFileName (code $versionCode, $sizeMB MB / $sizeBytes bytes, forceUpdate=$ForceUpdate) to ${RemoteHost}:${RemoteReleasesDir}..."
    ssh -o BatchMode=yes -o ConnectTimeout=10 $RemoteHost "mkdir -p $RemoteReleasesDir"
    if ($LASTEXITCODE -ne 0) { throw "Failed to create remote releases directory." }

    scp -o BatchMode=yes -o ConnectTimeout=10 $ApkPath "${RemoteHost}:${RemoteReleasesDir}/${versionedFileName}"
    if ($LASTEXITCODE -ne 0) { throw "Failed to upload $versionedFileName via scp." }

    # Maintain bankpoker-latest.apk copy for backwards compatibility
    ssh -o BatchMode=yes -o ConnectTimeout=10 $RemoteHost "cp -f ${RemoteReleasesDir}/${versionedFileName} ${RemoteReleasesDir}/bankpoker-latest.apk"
    if ($LASTEXITCODE -ne 0) { throw "Failed to update bankpoker-latest.apk copy." }

    scp -o BatchMode=yes -o ConnectTimeout=10 $tmpVersionJson "${RemoteHost}:${RemoteReleasesDir}/version.json"
    if ($LASTEXITCODE -ne 0) { throw "Failed to upload version.json via scp." }

    Write-Host "Successfully published $versionedFileName, bankpoker-latest.apk, and version.json."
}
finally {
    if (Test-Path $tmpVersionJson) {
        Remove-Item -Force $tmpVersionJson
    }
}
