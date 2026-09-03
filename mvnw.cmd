@echo off
setlocal
set MAVEN_VERSION=3.9.9
set BASE_DIR=%~dp0
set MAVEN_HOME=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%

where mvn >nul 2>nul
if %errorlevel%==0 (
  mvn %*
  exit /b %errorlevel%
)

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  echo Maven nao encontrado. Baixando Maven %MAVEN_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $url='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip'; $zip='%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%-bin.zip'; Invoke-WebRequest -Uri $url -OutFile $zip; Expand-Archive -Path $zip -DestinationPath '%BASE_DIR%.mvn' -Force; Remove-Item $zip"
  if errorlevel 1 (
    echo Falha ao baixar Maven. Verifique a internet ou instale Maven 3.9+.
    exit /b 1
  )
)

call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %errorlevel%
