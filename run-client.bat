@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "IRONCOMPASS_JDK="

if defined IRONCOMPASS_JAVA_HOME call :consider_jdk "%IRONCOMPASS_JAVA_HOME%"
if defined IRONCOMPASS_JDK goto :run

if defined JAVA_HOME call :consider_jdk "%JAVA_HOME%"
if defined IRONCOMPASS_JDK goto :run

for /d %%D in (
    "%ProgramFiles%\Eclipse Adoptium\jdk-*"
    "%ProgramFiles%\Java\jdk-*"
    "%LocalAppData%\Programs\Eclipse Adoptium\jdk-*"
    "%UserProfile%\.jdks\*"
) do (
    if not defined IRONCOMPASS_JDK call :consider_jdk "%%~fD"
)

for /d %%R in ("%TEMP%\iron-compass-jdk11-*") do (
    for /d %%D in ("%%~fR\jdk-*") do (
        if not defined IRONCOMPASS_JDK call :consider_jdk "%%~fD"
    )
)

if not defined IRONCOMPASS_JDK (
    echo Iron Compass requires a JDK 11 or newer, but only a Java runtime was found.
    echo.
    echo Install Eclipse Temurin JDK 11 from:
    echo https://adoptium.net/temurin/releases/?version=11
    echo.
    echo Then reopen Command Prompt and run:
    echo run-client.bat
    echo.
    echo You can also set IRONCOMPASS_JAVA_HOME to an existing JDK directory.
    exit /b 1
)

:run
echo Using JDK: %IRONCOMPASS_JDK%
set "JAVA_HOME=%IRONCOMPASS_JDK%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
"%JAVA_HOME%\bin\java.exe" "-Dorg.gradle.appname=gradlew" -classpath "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain run %*
exit /b %ERRORLEVEL%

:consider_jdk
set "CANDIDATE_JDK=%~1"
if not exist "%CANDIDATE_JDK%\bin\javac.exe" exit /b 0

set "JAVAC_VERSION="
for /f "tokens=2" %%V in ('"%CANDIDATE_JDK%\bin\javac.exe" -version 2^>^&1') do set "JAVAC_VERSION=%%V"
if not defined JAVAC_VERSION exit /b 0

set "JDK_MAJOR="
for /f "tokens=1,2 delims=." %%A in ("!JAVAC_VERSION!") do (
    if "%%A"=="1" (
        set "JDK_MAJOR=%%B"
    ) else (
        set "JDK_MAJOR=%%A"
    )
)
if not defined JDK_MAJOR exit /b 0
if !JDK_MAJOR! LSS 11 exit /b 0

set "IRONCOMPASS_JDK=%CANDIDATE_JDK%"
exit /b 0
