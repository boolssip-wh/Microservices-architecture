@echo off
chcp 65001 >nul
setlocal

rem ===========================================================
rem  SmartGrade (single-app demo) launcher - keep this file as ASCII
rem ===========================================================

set "DEMO_HOME=%~dp0"
set "REPO=%DEMO_HOME%smartgrade-monolith"
set "APP_JAR=%REPO%\target\smartgrade-monolith-1.0.0.jar"

if not exist "%APP_JAR%" goto nojar

rem --- pick a Java: bundled JDK 17 first, then JAVA_HOME, then PATH ---
set "JAVA_EXE="
for /d %%D in ("%DEMO_HOME%tmp\_buildtools\jdk-*") do set "JAVA_EXE=%%D\bin\java.exe"
if not defined JAVA_EXE if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE set "JAVA_EXE=java"

rem --- some security suites block the JVM loopback pipe; give it a writable dir ---
if not exist "%DEMO_HOME%tmp\unixdomain" mkdir "%DEMO_HOME%tmp\unixdomain" >nul 2>nul
set "JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=%DEMO_HOME%tmp\unixdomain"

echo ============================================================
echo   SmartGrade demo (week 1 minimum closed loop)
echo ------------------------------------------------------------
echo   Java    : %JAVA_EXE%
echo   Jar     : %APP_JAR%
echo   URL     : http://localhost:8080/
echo   Accounts: teacher1 / student1 / student2 / admin
echo   Password: 123456
echo ------------------------------------------------------------
echo   Waiting for the service to start (about 20 seconds).
echo   The browser opens automatically when it is ready.
echo   Spring Boot log is printed below - keep this window open.
echo   Close this window (or press Ctrl+C) to stop the service.
echo ============================================================
echo.

rem --- background helper: wait for port 8080, then open the browser ---
start "smartgrade-browser" /min powershell -NoProfile -Command "for($i=0;$i -lt 120;$i++){ try{ $c=New-Object Net.Sockets.TcpClient; $c.Connect('127.0.0.1',8080); $c.Close(); Start-Process 'http://localhost:8080/'; exit } catch { Start-Sleep -Seconds 2 } }"

"%JAVA_EXE%" -jar "%APP_JAR%"

echo.
echo ============================================================
echo   Service stopped (exit code %ERRORLEVEL%).
echo   If you saw an error above, send a screenshot of this window.
echo ============================================================
pause
endlocal
exit /b 0

:nojar
echo ============================================================
echo   ERROR: packaged demo not found:
echo   %APP_JAR%
echo.
echo   Please build it first:  mvn package
echo ============================================================
pause
endlocal
exit /b 1
