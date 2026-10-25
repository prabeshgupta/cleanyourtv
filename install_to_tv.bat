@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   Install CleanYourTV to Android TV / Set-Top Box
echo ========================================================

cd /d "%~dp0"

:: 1. Determine Target TV IP Address
set "TV_IP=%~1"
if "%TV_IP%"=="" (
    echo.
    echo Please enter your Android TV IP address.
    echo (Make sure Developer Options and Network Debugging are enabled on your TV)
    echo.
    set /p "TV_IP=Enter TV IP Address (e.g. 10.0.0.50): "
)

if "%TV_IP%"=="" (
    echo [ERROR] No TV IP address provided. Exiting.
    pause
    exit /b 1
)

set "TV_PORT=5555"

:: 2. Locate ADB binary
set "ADB_BIN=adb"
where adb >nul 2>&1
if %ERRORLEVEL% neq 0 (
    if defined ANDROID_HOME if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
        set "ADB_BIN=%ANDROID_HOME%\platform-tools\adb.exe"
    ) else if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set "ADB_BIN=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    ) else (
        echo [ERROR] adb was not found in PATH or standard Android SDK directories.
        echo Please ensure Android platform-tools is installed.
        pause
        exit /b 1
    )
)

:: 3. Locate APK
set "APK_PATH=%~dp0release\CleanYourTV.apk"
if not exist "%APK_PATH%" (
    set "APK_PATH=%~dp0app\build\outputs\apk\release\app-release.apk"
)
if not exist "%APK_PATH%" (
    set "APK_PATH=%~dp0app\build\outputs\apk\debug\app-debug.apk"
)

if not exist "%APK_PATH%" (
    echo [ERROR] APK not found! Please run build.bat first to compile the app.
    pause
    exit /b 1
)

echo.
echo [INFO] Target Device: %TV_IP%:%TV_PORT%
echo [INFO] APK File     : %APK_PATH%
echo [INFO] ADB Command  : %ADB_BIN%
echo.

echo [1/3] Connecting to Android TV via ADB...
"%ADB_BIN%" connect %TV_IP%:%TV_PORT%

echo.
echo [2/3] Installing APK (replacing existing)...
"%ADB_BIN%" -s %TV_IP%:%TV_PORT% install -r "%APK_PATH%"

if %ERRORLEVEL% equ 0 (
    echo.
    echo [3/3] Launching CleanYourTV Launcher...
    "%ADB_BIN%" -s %TV_IP%:%TV_PORT% shell monkey -p com.gathin.tvhome -c android.intent.category.LAUNCHER 1
    echo.
    echo ========================================================
    echo   INSTALLATION COMPLETED SUCCESSFULLY!
    echo ========================================================
) else (
    echo.
    echo [ERROR] Installation failed. Please check TV connection and permissions.
)

if "%CI%"=="" pause
