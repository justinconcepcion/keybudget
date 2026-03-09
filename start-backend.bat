@echo off

set JAVA_HOME=T:\Java\jdk21.0.10_7

REM Load secrets from backend/.env
REM File format: export KEY=VALUE (one per line, comments start with #)
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("T:\workspace\keybudget\backend\.env") do (
    REM Strip 'export ' prefix from the key
    set "key=%%A"
    call set "key=%%key:export =%%"
    call set "%%key%%=%%B"
)

if not defined GOOGLE_CLIENT_ID (
    echo ERROR: Could not load secrets from backend\.env
    echo Create backend\.env -- see application-dev.properties for required vars.
    pause
    exit /b 1
)

echo Secrets loaded from backend\.env
echo Starting KeyBudget backend (dev profile)...
echo   Backend:  http://localhost:8080
echo   Frontend: http://localhost:5173 (run start-frontend.bat)
echo   Health:   http://localhost:8080/actuator/health
echo.

cd /d T:\workspace\keybudget\backend
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
pause
