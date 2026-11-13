@echo off
REM Mo 3 cua so Client chay dong thoi (moi client la mot tien trinh JVM rieng).
REM Cach dung: scripts\demo-3-clients.bat <SKU> <MA_DON>
REM Vi du    : scripts\demo-3-clients.bat SKU-001-M-BLK ORD-0001
chcp 65001 >nul
set JAR=%~dp0..\socket-client\target\shop-socket-client-1.0.0.jar
set SKU=%1
if "%SKU%"=="" set SKU=SKU-001-M-BLK
set ORDER=%2
if "%ORDER%"=="" set ORDER=ORD-0001

start "Client A" cmd /k java -jar "%JAR%" --name A --repeat 10 --interval 500 CHECK_STOCK %SKU%
start "Client B" cmd /k java -jar "%JAR%" --name B --repeat 10 --interval 700 CHECK_STOCK %SKU%
start "Client C" cmd /k java -jar "%JAR%" --name C --repeat 10 --interval 600 GET_ORDER_STATUS %ORDER%
