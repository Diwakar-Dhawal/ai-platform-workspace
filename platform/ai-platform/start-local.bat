@echo off
cd /d C:\Users\Diwak\Downloads\ai-platform-workspace\platform\ai-platform

:: Load .env file
for /f "usebackq tokens=1,* delims==" %%a in ("..\..\.env") do (
    set "%%a=%%b"
)

set TZ=UTC
set JAVA_TOOL_OPTIONS=-Duser.timezone=UTC

mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
