@echo off
echo ==================================
echo   诗词乐园 - APK 构建脚本
echo ==================================
echo.

REM 检查 Java 17
java -version 2>&1 | find "17" >nul
if %errorlevel% neq 0 (
    echo [错误] 需要 JDK 17，当前 Java 版本：
    java -version 2>&1
    exit /b 1
)

echo [信息] 清理旧构建...
if exist app\build rmdir /s /q app\build

echo [信息] 生成 Gradle Wrapper...
call gradle wrapper --gradle-version 8.5

echo [信息] 开始构建 Debug APK...
call gradlew assembleDebug

if %errorlevel% equ 0 (
    echo.
    echo ==================================
    echo   构建成功！
    echo   APK 位置: app\build\outputs\apk\debug\app-debug.apk
    echo ==================================
) else (
    echo.
    echo [错误] 构建失败，请检查上方错误信息
    exit /b 1
)
