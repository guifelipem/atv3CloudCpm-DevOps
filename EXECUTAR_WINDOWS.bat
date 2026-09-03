@echo off
cd /d "%~dp0"
echo ==========================================
echo Biblioteca Cloud - Spring Boot + Java 21
echo ==========================================
echo.
java -version
if errorlevel 1 (
  echo.
  echo ERRO: Java nao encontrado. Instale Java 21 e configure o PATH/JAVA_HOME.
  pause
  exit /b 1
)
echo.
echo Iniciando aplicacao em http://localhost:8080/login
call mvnw.cmd spring-boot:run
pause
