@echo off
set JAVA_HOME=T:\Java\jdk21.0.10_7

REM Load secrets from backend/.env
REM File format: export KEY=VALUE (one per line, comments start with #)
for /f "usebackq tokens=*" %%L in ("T:\workspace\keybudget\backend\.env") do (
    set "line=%%L"
    if not "%%L"=="" (
        REM Strip 'export ' prefix and set the variable
        call set "var=%%line:export =%%"
        call set "%%var%%"
    )
)

if not defined GOOGLE_CLIENT_ID (
    echo ERROR: Could not load secrets from backend\.env
    echo Create backend\.env — see application-dev.properties for required vars.
    pause
    exit /b 1
)

cd /d T:\workspace\keybudget\backend
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
pause
