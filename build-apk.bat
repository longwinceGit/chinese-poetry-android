@echo off
chcp 65001 >nul
echo ================================
echo   中华诗词库 - Android 构建脚本
echo ================================
echo.

REM 设置项目根目录
set PROJECT_DIR=%~dp0
set GRADLE_VERSION=7.5
set GRADLE_DIR=%USERPROFILE%\.gradle\wrapper\dists\gradle-%GRADLE_VERSION%-bin
set GRADLE_EXE=

REM 查找 Gradle 7.5
if exist "%GRADLE_DIR%\*\bin\gradle.bat" (
    for /f "delims=" %%i in ('dir /s /b "%GRADLE_DIR%\*\bin\gradle.bat" 2^>nul') do set GRADLE_EXE=%%i
)

if "%GRADLE_EXE%"=="" (
    echo [1/3] Gradle %GRADLE_VERSION% 未找到，正在下载...
    set DIST_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip
    set TMP_ZIP=%TEMP%\gradle-%GRADLE_VERSION%.zip
    
    REM 使用 PowerShell 下载
    powershell -Command "& { Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%TMP_ZIP%' }"
    
    if not exist "%TMP_ZIP%" (
        echo 下载失败，请手动下载 Gradle %GRADLE_VERSION% 并配置 PATH
        pause
        exit /b 1
    )
    
    echo [2/3] 解压 Gradle...
    powershell -Command "& { Expand-Archive -Path '%TMP_ZIP%' -DestinationPath '%USERPROFILE%\.gradle\wrapper\dists\' -Force }"
    del "%TMP_ZIP%"
    
    REM 重新查找
    for /f "delims=" %%i in ('dir /s /b "%GRADLE_DIR%\*\bin\gradle.bat" 2^>nul') do set GRADLE_EXE=%%i
)

echo [3/3] 使用 Gradle 构建 APK...
echo Gradle: %GRADLE_EXE%
echo.

cd /d "%PROJECT_DIR%"
"%GRADLE_EXE%" assembleDebug --stacktrace

if exist "%PROJECT_DIR%app\build\outputs\apk\debug\app-debug.apk" (
    echo.
    echo ================================
    echo   构建成功！
    echo   APK: %PROJECT_DIR%app\build\outputs\apk\debug\app-debug.apk
    echo ================================
) else (
    echo.
    echo [!!] 构建失败，请检查错误信息
)

pause
