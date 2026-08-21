@echo off
REM Start all services for local development
REM Usage: scripts\start-all.bat
REM
REM Prerequisites: PostgreSQL must be running on port 5432

echo ============================================
echo  AI Platform - Local Development Startup
echo ============================================
echo.

set JAVA_TOOL_OPTIONS=-Duser.timezone=UTC
set PROJECT_ROOT=%~dp0..

REM --- Start Identity Service ---
echo [1/2] Starting Identity Service on port 8081...
cd /d %PROJECT_ROOT%\platform\identity-service
start "Identity Service" cmd /c "mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local > C:\tmp\identity-service.log 2>&1"
echo       Started. Log: C:\tmp\identity-service.log
echo.

REM --- Wait for Identity Service ---
echo Waiting 45 seconds for Identity Service to start...
timeout /t 45 /nobreak >nul

REM --- Start Gateway ---
echo [2/2] Starting Gateway on port 8080...
cd /d %PROJECT_ROOT%\applications\insighttube\gateway
start "Gateway" cmd /c "mvnw.cmd spring-boot:run > C:\tmp\gateway.log 2>&1"
echo       Started. Log: C:\tmp\gateway.log
echo.

echo ============================================
echo  Services starting:
echo    Identity Service: http://localhost:8081
echo    Gateway:          http://localhost:8080
echo    PostgreSQL:       localhost:5432
echo ============================================
echo.
echo Logs:
echo   Identity: C:\tmp\identity-service.log
echo   Gateway:  C:\tmp\gateway.log
echo.
echo Press any key to check service health...
pause >nul

echo.
echo --- Health Checks ---
curl -s http://localhost:8081/identity-service/health 2>nul || echo Identity Service: NOT READY
echo.
curl -s http://localhost:8080/actuator/health 2>nul || echo Gateway: NOT READY
echo.
echo.
pause
