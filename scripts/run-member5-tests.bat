@echo off
REM Chay cac test cua thanh vien 5 va luu ket qua lam minh chung vao logs\evidence (thu muc logs/ khong bi commit len Git).
REM Can SQL Server tren may nay cho 2 lop *ConcurrencyTests. Hay TAT website truoc khi chay.
cd /d "%~dp0.."
if not exist logs\evidence mkdir logs\evidence

call mvnw.cmd test -Dtest="ShopSocketServerTests,BoundedLineReaderTests,InventorySocketConcurrencyTests,OrderStockConcurrencyTests" > logs\evidence\member5-test-output.txt 2>&1

echo ===== Tom tat =====
findstr /C:"Tests run:" /C:"BUILD" /C:"[TEST]" logs\evidence\member5-test-output.txt
copy /Y target\surefire-reports\*.txt logs\evidence\ >nul 2>&1
echo.
echo Da luu day du ket qua vao logs\evidence (khong dua len Git). Cat bo mat khau, chuoi ket noi truoc khi chup anh.
