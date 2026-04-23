$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$targetDir = Join-Path $repoRoot "target"
$distDir = Join-Path $targetDir "dist"
$packageInputDir = Join-Path $targetDir "package-input"
$jarName = "vibevault-1.0-SNAPSHOT-all.jar"
$iconPath = Join-Path $repoRoot "src\main\resources\assets\vibevault_icon.ico"
$appVersion = "1.0.0"
$appImageDir = Join-Path $distDir "VibeVault"
$portableZip = Join-Path $distDir "VibeVault-portable.zip"

Set-Location $repoRoot

mvn clean package

if (-not (Test-Path (Join-Path $targetDir $jarName))) {
    throw "Expected shaded jar not found: $jarName"
}

if (-not (Test-Path $iconPath)) {
    throw "Expected icon not found: $iconPath"
}

New-Item -ItemType Directory -Force -Path $distDir | Out-Null

if (Test-Path $appImageDir) {
    Remove-Item -Recurse -Force $appImageDir
}

if (Test-Path $portableZip) {
    Remove-Item -Force $portableZip
}

if (Test-Path $packageInputDir) {
    Remove-Item -Recurse -Force $packageInputDir
}

New-Item -ItemType Directory -Force -Path $packageInputDir | Out-Null
Copy-Item (Join-Path $targetDir $jarName) $packageInputDir

jpackage `
  --type app-image `
  --name VibeVault `
  --input $packageInputDir `
  --main-jar $jarName `
  --main-class com.vibevault.Main `
  --dest $distDir `
  --app-version $appVersion `
  --vendor "VibeVault" `
  --description "VibeVault desktop music player" `
  --icon $iconPath `
  --java-options "--enable-native-access=ALL-UNNAMED"

Compress-Archive -Path $appImageDir -DestinationPath $portableZip

jpackage `
  --type exe `
  --name VibeVault `
  --input $packageInputDir `
  --main-jar $jarName `
  --main-class com.vibevault.Main `
  --dest $distDir `
  --app-version $appVersion `
  --vendor "VibeVault" `
  --description "VibeVault desktop music player" `
  --icon $iconPath `
  --win-shortcut `
  --win-menu `
  --java-options "--enable-native-access=ALL-UNNAMED"
