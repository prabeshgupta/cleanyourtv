@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   Building CleanYourTV Launcher
echo ========================================================

cd /d "%~dp0"

:: 1. Validate / Locate Java
if not defined JAVA_HOME (
    for /f "tokens=*" %%i in ('where java 2^>nul') do (
        if not defined JAVA_HOME (
            for %%j in ("%%~dpi..") do set "JAVA_HOME=%%~fj"
        )
    )
)

if not defined JAVA_HOME (
    echo [ERROR] JAVA_HOME is not set and Java was not found in PATH.
    echo Please install JDK 17+ and set JAVA_HOME:
    echo   set JAVA_HOME=C:\Path\To\Jdk-17
    exit /b 1
)

:: 2. Validate / Locate Android SDK
if not defined ANDROID_HOME (
    if defined ANDROID_SDK_ROOT (
        set "ANDROID_HOME=%ANDROID_SDK_ROOT%"
    ) else if exist "%LOCALAPPDATA%\Android\Sdk" (
        set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
    )
)

if not defined ANDROID_HOME (
    echo [WARNING] ANDROID_HOME is not set. Gradle will attempt to locate it via local.properties.
)

:: 3. Run Gradle Wrapper
if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat not found in "%~dp0"!
    exit /b 1
)

echo [INFO] JAVA_HOME   : %JAVA_HOME%
if defined ANDROID_HOME echo [INFO] ANDROID_HOME: %ANDROID_HOME%
echo [INFO] Running Gradle assembleRelease...
echo.

call gradlew.bat :app:assembleRelease

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================================
    echo   BUILD SUCCESSFUL!
    echo   Release APK:
    echo   %~dp0app\build\outputs\apk\release\app-release.apk
    echo ========================================================
    if exist "%~dp0app\build\outputs\apk\release\app-release.apk" (
        if not exist "%~dp0release" mkdir "%~dp0release"
        copy /y "%~dp0app\build\outputs\apk\release\app-release.apk" "%~dp0release\CleanYourTV.apk" >nul
        echo   Copied to: %~dp0release\CleanYourTV.apk
    )
) else (
    echo.
    echo [ERROR] Build failed with exit code %ERRORLEVEL%.
)

if "%~1"=="--no-pause" goto :eof
if "%CI%"=="" pause
