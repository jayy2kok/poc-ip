@REM Maven Wrapper startup script for Windows
@REM
@REM Required ENV vars:
@REM   JAVA_HOME - location of a JDK home dir

@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set MAVEN_WRAPPER_PROPERTIES="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"

@REM Check if JAVA_HOME is set
if defined JAVA_HOME goto javaHomeSet
set JAVA_EXE=java
goto findJava

:javaHomeSet
set JAVA_EXE="%JAVA_HOME%\bin\java"

:findJava
@REM Download maven-wrapper.jar if not present
if exist %MAVEN_WRAPPER_JAR% goto hasMavenWrapperJar

@REM Try to use PowerShell to download
echo Downloading Maven Wrapper...
for /f "tokens=2 delims==" %%a in ('findstr /r "wrapperUrl" %MAVEN_WRAPPER_PROPERTIES%') do set WRAPPER_URL=%%a
powershell -Command "(New-Object Net.WebClient).DownloadFile('%WRAPPER_URL%', '%MAVEN_WRAPPER_JAR:"=%')" >nul 2>&1
if exist %MAVEN_WRAPPER_JAR% goto hasMavenWrapperJar

@REM Fallback: use java to download
echo Downloading Maven distribution without wrapper jar...

:hasMavenWrapperJar
@REM Find the distribution URL
for /f "tokens=2 delims==" %%a in ('findstr /r "distributionUrl" %MAVEN_WRAPPER_PROPERTIES%') do set DOWNLOAD_URL=%%a

@REM Set up the Maven home directory
set MAVEN_USER_HOME=%USERPROFILE%\.m2\wrapper\dists
for %%i in (%DOWNLOAD_URL%) do set DIST_NAME=%%~ni
set MAVEN_HOME=%MAVEN_USER_HOME%\%DIST_NAME%

@REM Check if Maven is already downloaded
if exist "%MAVEN_HOME%\bin\mvn.cmd" goto runMaven

@REM Download and extract Maven
echo Downloading Maven from %DOWNLOAD_URL%...
set MAVEN_ZIP=%MAVEN_USER_HOME%\%DIST_NAME%.zip
if not exist "%MAVEN_USER_HOME%" mkdir "%MAVEN_USER_HOME%"
powershell -Command "(New-Object Net.WebClient).DownloadFile('%DOWNLOAD_URL%', '%MAVEN_ZIP%')" >nul 2>&1
if not exist "%MAVEN_ZIP%" (
    echo Failed to download Maven distribution
    exit /b 1
)
echo Extracting Maven...
powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%MAVEN_USER_HOME%' -Force" >nul 2>&1
del "%MAVEN_ZIP%" >nul 2>&1

@REM Maven extracts into apache-maven-x.y.z directory
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    @REM Try with apache- prefix
    set MAVEN_HOME=%MAVEN_USER_HOME%\apache-%DIST_NAME%
)

:runMaven
"%MAVEN_HOME%\bin\mvn.cmd" %*
