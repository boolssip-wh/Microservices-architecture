@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

if not "%JAVA_HOME%"=="" (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_EXE=java"
)

set "APP_JAR=target\smartgrade-monolith-1.0.0.jar"

if exist "%APP_JAR%" (
    echo [1/1] 启动已打包的 Demo：%APP_JAR%
    "%JAVA_EXE%" -jar "%APP_JAR%"
) else (
    echo 未找到 %APP_JAR%，改用 Maven 直接启动（首次会下载依赖）...
    call mvn spring-boot:run
)

endlocal
