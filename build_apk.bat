@echo off
setlocal EnableExtensions
cd /d "%~dp0"

title EhViewer Build

echo ========================================
echo   EhViewer build helper
echo ========================================
echo.

set "MODE=Debug"
set "TASKS=app:assembleAppReleaseDebug"

if /I "%~1"=="release" (
    set "MODE=Release"
    set "TASKS=app:assembleAppReleaseRelease"
) else if /I "%~1"=="clean" (
    set "MODE=Clean + Debug"
    set "TASKS=clean app:assembleAppReleaseDebug"
) else if /I "%~1"=="clean-release" (
    set "MODE=Clean + Release"
    set "TASKS=clean app:assembleAppReleaseRelease"
)

echo Build mode: %MODE%
echo Tasks     : %TASKS%
echo.

if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat was not found.
    echo Put this BAT file in the project root folder.
    goto fail
)

where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java was not found in PATH.
    echo Please install JDK 21 and make sure java works in cmd.
    echo Example: java -version
    goto fail
)

if exist "local.properties" (
    echo [INFO] local.properties found.
) else (
    echo [WARN] local.properties not found.
    echo Open the project once in Android Studio, or configure Android SDK first.
    if "%ANDROID_HOME%"=="" if "%ANDROID_SDK_ROOT%"=="" (
        echo [WARN] ANDROID_HOME / ANDROID_SDK_ROOT is also empty.
    )
)

echo.
echo [START] Building, please wait...
echo.
call "%~dp0gradlew.bat" %TASKS% --stacktrace
if errorlevel 1 goto fail

echo.
echo [OK] Build finished.
echo.
if exist "%cd%\app\build\outputs\apk" (
    echo APK output folder:
    echo   %cd%\app\build\outputs\apk
    echo.
    for /r "%cd%\app\build\outputs\apk" %%i in (*.apk) do echo   %%~fi
) else (
    echo APK output folder was not created yet.
)

echo.
echo Usage:
echo   Double click          = build Debug APK
echo   build_apk.bat clean   = clean + Debug
echo   build_apk.bat release = Release

echo   build_apk.bat clean-release = clean + Release

echo.
pause
exit /b 0

:fail
echo.
echo [FAILED] Build did not finish.
echo Common reasons:
echo   1. JDK 21 is missing or java is not in PATH
echo   2. Android SDK / NDK / CMake is missing
echo   3. First build needs network to download Gradle or dependencies
echo   4. Gradle wrapper or dependency download failed

echo.
pause
exit /b 1
