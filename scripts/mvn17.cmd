@echo off
setlocal
chcp 65001 >nul

rem A1 build entry: pin JDK 17 and the project-local Maven repository.
if not defined SMARTGRADE_TOOLS set "SMARTGRADE_TOOLS=D:\SmartGradeTools"
set "JAVA_HOME=%SMARTGRADE_TOOLS%\jdk17"
set "PROJECT_DIR=%~dp0.."
set "MAVEN_OPTS=-Dmaven.repo.local=%PROJECT_DIR%\.m2\repository -Dfile.encoding=UTF-8"
set "MAVEN_SETTINGS=%~dp0maven-settings.xml"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [ERROR] JDK 17 not found: %JAVA_HOME%
  exit /b 1
)
if not exist "%SMARTGRADE_TOOLS%\maven\bin\mvn.cmd" (
  echo [ERROR] Maven not found: %SMARTGRADE_TOOLS%\maven
  exit /b 1
)

call "%SMARTGRADE_TOOLS%\maven\bin\mvn.cmd" -s "%MAVEN_SETTINGS%" %*
exit /b %ERRORLEVEL%
