param(
    [string]$RemoteHost = "dd-karimi@192.168.70.162",
    [string]$RemoteReleasesDir = "~/BankPoker/releases",
    [string]$ApkPath = ""
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

$versionObj = [ordered]@{
    versionName = $versionName
    versionCode = $versionCode
    fileName    = "bankpoker-latest.apk"
    apkUrl      = "/releases/bankpoker-latest.apk"
    sizeBytes   = $sizeBytes
    sizeMB      = $sizeMB
    builtAt     = $builtAt
    publishedAt = $publishedAt
}

$tmpVersionJson = Join-Path ([System.IO.Path]::GetTempPath()) "bankpoker-version-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()).json"
try {
    $jsonText = $versionObj | ConvertTo-Json -Depth 3
    [System.IO.File]::WriteAllText($tmpVersionJson, $jsonText, (New-Object System.Text.UTF8Encoding($false)))

    Write-Host "Publishing BankPoker APK v$versionName (code $versionCode, $sizeMB MB / $sizeBytes bytes) to ${RemoteHost}:${RemoteReleasesDir}..."
    ssh -o BatchMode=yes -o ConnectTimeout=10 $RemoteHost "mkdir -p $RemoteReleasesDir"
    if ($LASTEXITCODE -ne 0) { throw "Failed to create remote releases directory." }

    scp -o BatchMode=yes -o ConnectTimeout=10 $ApkPath "${RemoteHost}:${RemoteReleasesDir}/bankpoker-latest.apk"
    if ($LASTEXITCODE -ne 0) { throw "Failed to upload APK via scp." }

    scp -o BatchMode=yes -o ConnectTimeout=10 $tmpVersionJson "${RemoteHost}:${RemoteReleasesDir}/version.json"
    if ($LASTEXITCODE -ne 0) { throw "Failed to upload version.json via scp." }

    Write-Host "Successfully published bankpoker-latest.apk and version.json."
}
finally {
    if (Test-Path $tmpVersionJson) {
        Remove-Item -Force $tmpVersionJson
    }
}
