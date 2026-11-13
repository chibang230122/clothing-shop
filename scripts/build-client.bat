@echo off
REM Build Socket Client thanh socket-client\target\shop-socket-client-1.0.0.jar
cd /d "%~dp0..\socket-client"
mvn -q package
echo Xong: socket-client\target\shop-socket-client-1.0.0.jar
