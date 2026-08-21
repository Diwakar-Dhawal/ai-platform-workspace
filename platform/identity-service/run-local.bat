@echo off
set TZ=UTC
set JAVA_TOOL_OPTIONS=-Duser.timezone=UTC
cd /d C:\Users\Diwak\Downloads\ai-platform-workspace\platform\identity-service
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--server.tomcat.uri-encoding=UTF-8"
