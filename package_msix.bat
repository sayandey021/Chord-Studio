@echo off
title Build Chord Studio MSIX Package
cd /d "%~dp0"

echo ===================================================
echo       Chord Studio - MSIX Package Generator
echo ===================================================
echo Identity:  Saayan.ChordStudio
echo Publisher: CN=37E2AF47-D2FC-489C-BDC1-02C989A7B989
echo Store ID:  9P71JGK9JB2Q
echo ===================================================

REM 1. Build Distributable App
echo [1/4] Building standalone Compose Desktop binaries...
call "%~dp0gradlew.bat" :desktopApp:createDistributable
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Gradle build failed.
    exit /b %ERRORLEVEL%
)

REM 2. Prepare staging directory
set "STAGING=%~dp0desktopApp\build\msix-staging"
set "DIST_DIR=%~dp0desktopApp\build\compose\binaries\main\app\ChordStudio"
set "OUTPUT_DIR=%~dp0desktopApp\build\msix"

if exist "%STAGING%" rmdir /S /Q "%STAGING%"
mkdir "%STAGING%"
mkdir "%STAGING%\app\ChordStudio"
mkdir "%STAGING%\Assets"
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

echo [2/4] Copying application files to MSIX staging layout...
xcopy /E /I /Y "%DIST_DIR%" "%STAGING%\app\ChordStudio" >nul
copy /Y "%~dp0desktopApp\package\msix\AppxManifest.xml" "%STAGING%\AppxManifest.xml" >nul

REM Copy assets
if exist "%~dp0desktopApp\src\main\resources\app_icon_48.png" copy /Y "%~dp0desktopApp\src\main\resources\app_icon_48.png" "%STAGING%\Assets\Square44x44Logo.png" >nul
if exist "%~dp0desktopApp\src\main\resources\app_icon_128.png" copy /Y "%~dp0desktopApp\src\main\resources\app_icon_128.png" "%STAGING%\Assets\Square150x150Logo.png" >nul
if exist "%~dp0desktopApp\src\main\resources\app_icon_48.png" copy /Y "%~dp0desktopApp\src\main\resources\app_icon_48.png" "%STAGING%\Assets\StoreLogo.png" >nul
if exist "%~dp0desktopApp\src\main\resources\app_icon_256.png" copy /Y "%~dp0desktopApp\src\main\resources\app_icon_256.png" "%STAGING%\Assets\Wide310x150Logo.png" >nul
if exist "%~dp0desktopApp\src\main\resources\app_icon_512.png" copy /Y "%~dp0desktopApp\src\main\resources\app_icon_512.png" "%STAGING%\Assets\SplashScreen.png" >nul

REM 3. Search for MakeAppx.exe in Windows SDK
echo [3/4] Locating MakeAppx.exe...
set "MAKEAPPX="
for /d %%D in ("C:\Program Files (x86)\Windows Kits\10\bin\10.*") do (
    if exist "%%D\x64\makeappx.exe" set "MAKEAPPX=%%D\x64\makeappx.exe"
)

if not defined MAKEAPPX (
    echo [WARNING] Windows SDK MakeAppx.exe not found in default paths.
    echo Staging layout prepared successfully at:
    echo   %STAGING%
    echo You can package this directory with MSIX Packaging Tool or MakeAppx.
    goto :done
)

echo [4/4] Creating MSIX Package with %MAKEAPPX%...
"%MAKEAPPX%" pack /d "%STAGING%" /p "%OUTPUT_DIR%\ChordStudio-0.8.0.msix" /nv /o
if %ERRORLEVEL% equ 0 (
    echo.
    echo ===================================================
    echo MSIX Package created successfully:
    echo %OUTPUT_DIR%\ChordStudio-0.8.0.msix
    echo ===================================================
) else (
    echo [ERROR] MakeAppx pack failed with code %ERRORLEVEL%.
)

:done
echo Done.
