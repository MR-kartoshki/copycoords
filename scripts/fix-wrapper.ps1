#!/usr/bin/env pwsh
<#
Downloads a Gradle distribution zip (tries multiple candidate versions),
extracts the `gradle-wrapper.jar` and copies it into the project's
`gradle/wrapper` directory.

Run in PowerShell (may require `-ExecutionPolicy Bypass`):
  powershell -ExecutionPolicy Bypass -File .\scripts\fix-wrapper.ps1
#>

param(
    [string]$LocalZipPath
)

$candidates = @(
    '9.6.3',
    '9.6.1',
    '9.6.0',
    '9.5.1',
    '9.5.0'
)

$tmpZip = Join-Path $env:TEMP 'gradle-dist.zip'
$extractDir = Join-Path $env:TEMP 'gradle-dist-extract'
$distUrl = $null

if ($LocalZipPath) {
    if (-not (Test-Path $LocalZipPath)) {
        Write-Error "Local zip not found: $LocalZipPath"
        exit 1
    }
    Copy-Item -Path $LocalZipPath -Destination $tmpZip -Force
    $distUrl = "file:///$LocalZipPath"
    Write-Host "Using local zip: $LocalZipPath"
} else {
    foreach ($v in $candidates) {
    $url = "https://services.gradle.org/distributions/gradle-$v-bin.zip"
    Write-Host "Trying $url ..."
    try {
        Invoke-WebRequest -Uri $url -OutFile $tmpZip -UseBasicParsing -ErrorAction Stop
        $distUrl = $url
        break
    } catch {
        Write-Host "Not found: $url"
    }
}
}

if (-not $distUrl) {
    Write-Error "None of the candidate Gradle distributions could be downloaded."
    Write-Host "Open https://services.gradle.org/distributions/ and pick a valid version, then update the script or your wrapper properties."
    exit 2
}

Write-Host "Downloaded $distUrl to $tmpZip"

if (Test-Path $extractDir) { Remove-Item -Recurse -Force $extractDir }

Write-Host "Extracting $tmpZip to $extractDir ..."
try {
    Expand-Archive -Path $tmpZip -DestinationPath $extractDir -Force -ErrorAction Stop
} catch {
    Write-Error "Failed to extract zip: $($_.Exception.Message)"
    exit 3
}

Write-Host "Searching for gradle-wrapper.jar ..."
$jar = Get-ChildItem -Path $extractDir -Recurse -Filter 'gradle-wrapper.jar' | Select-Object -First 1

$destDir = Join-Path (Get-Location) 'gradle\wrapper'
if (-not (Test-Path $destDir)) {
    Write-Host "Creating directory $destDir"
    New-Item -ItemType Directory -Path $destDir | Out-Null
}

if ($jar) {
    $dest = Join-Path $destDir 'gradle-wrapper.jar'
    Copy-Item -Path $jar.FullName -Destination $dest -Force
    Write-Host "Copied gradle-wrapper.jar to $dest"
    Write-Host "You can now run: .\gradlew.bat --version"
} else {
    Write-Host "gradle-wrapper.jar not found inside extracted distribution. Attempting to download from Gradle GitHub..."
    $wrapperUrl = 'https://raw.githubusercontent.com/gradle/gradle/master/gradle-wrapper/src/main/resources/gradle-wrapper.jar'
    $dest = Join-Path $destDir 'gradle-wrapper.jar'
    try {
        Invoke-WebRequest -Uri $wrapperUrl -OutFile $dest -UseBasicParsing -ErrorAction Stop
        Write-Host "Downloaded gradle-wrapper.jar to $dest from $wrapperUrl"
        Write-Host "You can now run: .\gradlew.bat --version"
    } catch {
        Write-Error "Failed to obtain gradle-wrapper.jar from GitHub: $($_.Exception.Message)"
        Write-Host "Manual option: download a gradle-wrapper.jar and place it into the project at gradle\wrapper\gradle-wrapper.jar"
        exit 4
    }
}
