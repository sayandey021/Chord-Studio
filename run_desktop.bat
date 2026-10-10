@echo off
title Chord Studio PC
cd /d "%~dp0"

echo ===================================================
echo            Chord Studio - PC Launcher
echo ===================================================
echo Instructions:
echo   run_desktop.bat              Launch application
echo   run_desktop.bat --recompile  Recompile binary and launch
echo   run_desktop.bat --build      Rebuild standalone exe only
echo   run_desktop.bat --msix       Build Microsoft Store MSIX package
echo   run_desktop.bat --dev        Run via Gradle in dev mode
echo   run_desktop.bat --help       Show this help message
echo ===================================================

set "EXE_DIR=%~dp0desktopApp\build\compose\binaries\main\app\ChordStudio"
set "EXE_PATH=%~dp0desktopApp\build\compose\binaries\main\app\ChordStudio\ChordStudio.exe"

REM Ensure ffmpeg and Python 3.12 are in PATH for Whisper AI transcription
if exist "C:\Ai\ffmpeg" set "PATH=C:\Ai\ffmpeg;%PATH%"
if exist "%~dp0.venv\Scripts" set "PATH=%~dp0.venv\Scripts;%PATH%"
if exist "C:\Program Files\Python312" set "PATH=C:\Program Files\Python312;C:\Program Files\Python312\Scripts;%PATH%"

REM Close any running instance to avoid file locks
taskkill /F /IM ChordStudio.exe >nul 2>&1

if "%1"=="--help" goto show_help
if "%1"=="-h" goto show_help
if "%1"=="help" goto show_help
if "%1"=="--recompile" goto recompile_and_launch
if "%1"=="-r" goto recompile_and_launch
if "%1"=="--build" goto build_only
if "%1"=="-b" goto build_only
if "%1"=="--msix" goto build_msix
if "%1"=="-m" goto build_msix
if "%1"=="msix" goto build_msix
if "%1"=="--dev" goto run_dev
if "%1"=="-d" goto run_dev

REM Default mode: launch binary if present, else build and run
if exist "%EXE_PATH%" (
    if exist "%EXE_DIR%" copy /Y "%~dp0transcribe_lyrics.py" "%EXE_DIR%\transcribe_lyrics.py" >nul 2>&1
    if exist "%~dp0tools\whisper" if not exist "%EXE_DIR%\whisper\whisper-cli.exe" xcopy /E /I /Y "%~dp0tools\whisper" "%EXE_DIR%\whisper" >nul 2>&1
    echo Starting Chord Studio Desktop Application...
    echo (Tip: Run 'run_desktop.bat --recompile' if you made code changes)
    cd /d "%EXE_DIR%"
    start "" "ChordStudio.exe"
    goto :eof
) else (
    echo Binary not found. Compiling and launching application...
    call "%~dp0gradlew.bat" :desktopApp:createDistributable
    if exist "%EXE_PATH%" (
        if exist "%EXE_DIR%" copy /Y "%~dp0transcribe_lyrics.py" "%EXE_DIR%\transcribe_lyrics.py" >nul 2>&1
        if exist "%~dp0tools\whisper" if not exist "%EXE_DIR%\whisper\whisper-cli.exe" xcopy /E /I /Y "%~dp0tools\whisper" "%EXE_DIR%\whisper" >nul 2>&1
        cd /d "%EXE_DIR%"
        start "" "ChordStudio.exe"
    ) else (
        echo Fallback: running via Gradle...
        call "%~dp0gradlew.bat" :desktopApp:run
    )
    goto :eof
)

:recompile_and_launch
echo Recompiling Chord Studio binary (:desktopApp:createDistributable)...
call "%~dp0gradlew.bat" :desktopApp:createDistributable
if %ERRORLEVEL% equ 0 (
    if exist "%EXE_DIR%" copy /Y "%~dp0transcribe_lyrics.py" "%EXE_DIR%\transcribe_lyrics.py" >nul 2>&1
    if exist "%~dp0tools\whisper" if not exist "%EXE_DIR%\whisper\whisper-cli.exe" xcopy /E /I /Y "%~dp0tools\whisper" "%EXE_DIR%\whisper" >nul 2>&1
    echo Compilation successful! Launching Chord Studio...
    cd /d "%EXE_DIR%"
    start "" "ChordStudio.exe"
) else (
    echo Compilation failed with error code %ERRORLEVEL%.
)
goto :eof

:build_only
echo Rebuilding Chord Studio standalone EXE only (:desktopApp:createDistributable)...
call "%~dp0gradlew.bat" :desktopApp:createDistributable
if %ERRORLEVEL% equ 0 (
    if exist "%EXE_DIR%" copy /Y "%~dp0transcribe_lyrics.py" "%EXE_DIR%\transcribe_lyrics.py" >nul 2>&1
    if exist "%~dp0tools\whisper" if not exist "%EXE_DIR%\whisper\whisper-cli.exe" xcopy /E /I /Y "%~dp0tools\whisper" "%EXE_DIR%\whisper" >nul 2>&1
    echo Build complete. Executable ready at:
    echo   %EXE_PATH%
) else (
    echo Build failed with error code %ERRORLEVEL%.
)
goto :eof

:build_msix
echo ===================================================
echo Building Chord Studio Microsoft Store MSIX Package
echo ===================================================
call "%~dp0package_msix.bat"
goto :eof

:run_dev
echo Running via Gradle in development mode (:desktopApp:run)...
call "%~dp0gradlew.bat" :desktopApp:run
goto :eof

:show_help
echo.
echo Compile and Run Commands:
echo   To launch current app:
echo     .\run_desktop.bat
echo.
echo   To recompile and immediately launch the updated app:
echo     .\run_desktop.bat --recompile  (or: .\run_desktop.bat -r)
echo.
echo   To build the standalone exe without launching:
echo     .\run_desktop.bat --build      (or: .\run_desktop.bat -b)
echo.
echo   To build the Microsoft Store MSIX package:
echo     .\run_desktop.bat --msix       (or: .\run_desktop.bat -m)
echo.
echo   To run with live Gradle dev logs:
echo     .\run_desktop.bat --dev        (or: .\run_desktop.bat -d)
echo.
echo   Direct Gradle command:
echo     .\gradlew.bat :desktopApp:createDistributable
echo.
goto :eof
