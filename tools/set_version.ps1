<#
.SYNOPSIS
    Updates the Chord Studio version across all project files, Compose Desktop configurations, and MSIX installer definitions.
.PARAMETER NewVersion
    The new version number (e.g., 0.8.1 or 0.8.1.0). If omitted, you will be prompted interactively.
#>

param(
    [Parameter(Position=0, Mandatory=$false)]
    [string]$NewVersion
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Resolve-Path "$PSScriptRoot\..").Path

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "        Chord Studio - Version Manager             " -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

# 1. Detect current version from AppConfig.kt
$AppConfigFile = Join-Path $RepoRoot "desktopApp\src\main\kotlin\com\gbros\tabslite\data\AppConfig.kt"
$currentVersion = "Unknown"
if (Test-Path $AppConfigFile) {
    $content = Get-Content $AppConfigFile -Raw
    if ($content -match 'const val VERSION = "([^"]+)"') {
        $currentVersion = $matches[1]
    }
}

Write-Host "Current Application Version: " -NoNewline
Write-Host "$currentVersion" -ForegroundColor Yellow

# 2. Prompt if version not supplied
if ([string]::IsNullOrWhiteSpace($NewVersion)) {
    Write-Host ""
    $inputVersion = Read-Host "Enter new version (e.g. 0.8.1 or 1.0.0)"
    if ([string]::IsNullOrWhiteSpace($inputVersion)) {
        Write-Host "Version update cancelled (empty input)." -ForegroundColor Yellow
        exit 0
    }
    $NewVersion = $inputVersion
}

# Clean input (strip whitespace, strip leading 'v' / 'V')
$NewVersion = $NewVersion.Trim()
if ($NewVersion.StartsWith("v", [System.StringComparison]::OrdinalIgnoreCase)) {
    $NewVersion = $NewVersion.Substring(1).Trim()
}

# Validate format (e.g., 0.8.1 or 0.8.1.0)
if ($NewVersion -notmatch '^\d+(\.\d+){2,3}$') {
    Write-Host "[ERROR] Invalid version format: '$NewVersion'" -ForegroundColor Red
    Write-Host "Expected format: X.Y.Z (e.g., 0.8.1) or X.Y.Z.W (e.g., 0.8.1.0)" -ForegroundColor Red
    exit 1
}

# Derive SemVer (X.Y.Z) and QuadVer (X.Y.Z.0 for MSIX AppxManifest)
$parts = $NewVersion.Split('.')
if ($parts.Length -eq 3) {
    $semVer = $NewVersion
    $quadVer = "$NewVersion.0"
} elseif ($parts.Length -eq 4) {
    $semVer = "$($parts[0]).$($parts[1]).$($parts[2])"
    $quadVer = $NewVersion
} else {
    $semVer = $NewVersion
    $quadVer = "$NewVersion.0"
}

Write-Host ""
Write-Host "Updating version to:" -ForegroundColor Cyan
Write-Host "  SemVer (App & Gradle) : " -NoNewline
Write-Host "$semVer" -ForegroundColor Green
Write-Host "  MSIX Quad Version      : " -NoNewline
Write-Host "$quadVer" -ForegroundColor Green
Write-Host ""

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$updatedFiles = 0

function Update-FileText {
    param(
        [string]$FilePath,
        [scriptblock]$TransformBlock,
        [string]$Description
    )

    if (-not (Test-Path $FilePath)) {
        Write-Host "  [-] Skipped (not found): $Description" -ForegroundColor DarkGray
        return
    }

    $raw = [System.IO.File]::ReadAllText($FilePath, [System.Text.Encoding]::UTF8)
    $newContent = & $TransformBlock $raw

    if ($raw -ne $newContent) {
        [System.IO.File]::WriteAllText($FilePath, $newContent, $utf8NoBom)
        Write-Host "  [OK] Updated $Description" -ForegroundColor Green
        $script:updatedFiles++
    } else {
        Write-Host "  [--] Already up to date: $Description" -ForegroundColor Gray
    }
}

# A. Update desktopApp/build.gradle.kts
$gradleFile = Join-Path $RepoRoot "desktopApp\build.gradle.kts"
Update-FileText -FilePath $gradleFile -Description "desktopApp/build.gradle.kts" -TransformBlock {
    param($text)
    # Update top-level packageVersion under nativeDistributions
    $text = [regex]::Replace($text, '(?m)(^\s*packageName\s*=\s*"ChordStudio"[\s\S]*?packageVersion\s*=\s*)"[^"]+"', {
        param($m) $m.Groups[1].Value + "`"$semVer`""
    })
    # Update windows block packageVersion
    $text = [regex]::Replace($text, '(?m)(windows\s*\{[\s\S]*?packageVersion\s*=\s*)"[^"]+"', {
        param($m) $m.Groups[1].Value + "`"$semVer`""
    })
    # For macOS: Apple Dmg/Pkg packaging requires Major > 0. Only update macOS version if Major >= 1.
    $major = [int]$semVer.Split('.')[0]
    if ($major -ge 1) {
        $text = [regex]::Replace($text, '(?m)(macOS\s*\{[\s\S]*?packageVersion\s*=\s*)"[^"]+"', {
            param($m) $m.Groups[1].Value + "`"$semVer`""
        })
    }
    return $text
}

# B. Update desktopApp/package/msix/AppxManifest.xml
$manifestFile = Join-Path $RepoRoot "desktopApp\package\msix\AppxManifest.xml"
Update-FileText -FilePath $manifestFile -Description "desktopApp/package/msix/AppxManifest.xml" -TransformBlock {
    param($text)
    [regex]::Replace($text, '(<Identity[\s\S]*?\bVersion=")[^"]+(")', {
        param($m)
        $m.Groups[1].Value + $quadVer + $m.Groups[2].Value
    })
}

# C. Update desktopApp/build/msix-staging/AppxManifest.xml if it exists
$stagingManifestFile = Join-Path $RepoRoot "desktopApp\build\msix-staging\AppxManifest.xml"
if (Test-Path $stagingManifestFile) {
    Update-FileText -FilePath $stagingManifestFile -Description "desktopApp/build/msix-staging/AppxManifest.xml" -TransformBlock {
        param($text)
        [regex]::Replace($text, '(<Identity[\s\S]*?\bVersion=")[^"]+(")', {
            param($m)
            $m.Groups[1].Value + $quadVer + $m.Groups[2].Value
        })
    }
}

# D. Update desktopApp/src/main/kotlin/com/gbros/tabslite/data/AppConfig.kt
$appConfigFile = Join-Path $RepoRoot "desktopApp\src\main\kotlin\com\gbros\tabslite\data\AppConfig.kt"
Update-FileText -FilePath $appConfigFile -Description "desktopApp/.../data/AppConfig.kt" -TransformBlock {
    param($text)
    [regex]::Replace($text, 'const val VERSION\s*=\s*"[^"]+"', "const val VERSION = `"$semVer`"")
}

# E. Update package_msix.bat
$msixBatFile = Join-Path $RepoRoot "package_msix.bat"
Update-FileText -FilePath $msixBatFile -Description "package_msix.bat" -TransformBlock {
    param($text)
    [regex]::Replace($text, 'ChordStudio-[\d\.]+\.msix', "ChordStudio-$semVer.msix")
}

# F. Update docs/STORE_LISTING.md
$storeDocFile = Join-Path $RepoRoot "docs\STORE_LISTING.md"
Update-FileText -FilePath $storeDocFile -Description "docs/STORE_LISTING.md" -TransformBlock {
    param($text)
    [regex]::Replace($text, '(\|\s*\*\*Version\*\*\s*\|\s*`)[^`]+(`\s*\|)', {
        param($m)
        $m.Groups[1].Value + $quadVer + $m.Groups[2].Value
    })
}

Write-Host ""
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host " Version updated successfully to $semVer ($quadVer)" -ForegroundColor Green
Write-Host " Files modified: $updatedFiles" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  - To launch with updated version: run_desktop.bat --recompile"
Write-Host "  - To build MSIX package:          run_desktop.bat --msix"
Write-Host "  - Remember to add release notes in CHANGELOG.md if publishing."
Write-Host ""
