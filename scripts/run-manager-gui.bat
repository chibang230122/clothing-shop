@echo off
setlocal
cd /d "%~dp0.."

if /i "%~1"=="--rebuild" (
    del /q "socket-client\target\shop-socket-client-1.0.0.jar" 2>nul
)

if not exist "socket-client\target\shop-socket-client-1.0.0.jar" (
    echo Dang dong goi shop-socket-client-1.0.0.jar...
    call mvnw.cmd -f socket-client\pom.xml clean package -DskipTests -Dproject.build.sourceEncoding=UTF-8 -Dmaven.compiler.encoding=UTF-8
    if errorlevel 1 (
        echo [LOI] Dong goi socket-client that bai!
        pause
        exit /b 1
    )
)

cd /d "%~dp0..\socket-client"
start "" javaw -Dfile.encoding=UTF-8 -jar target\shop-socket-client-1.0.0.jar