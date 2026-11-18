@echo off
REM Build Socket Client bang Maven Wrapper cua project (khong can cai Maven rieng).
REM Ket qua: socket-client\target\shop-socket-client-1.0.0.jar
cd /d "%~dp0.."
call mvnw.cmd -q -f socket-client\pom.xml package
echo Xong: socket-client\target\shop-socket-client-1.0.0.jar
