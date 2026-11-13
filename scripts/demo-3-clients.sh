#!/bin/bash
# Chạy 3 client đồng thời (3 tiến trình JVM riêng) và in kết quả lẫn vào nhau như khi demo.
# Cách dùng: scripts/demo-3-clients.sh <SKU> <MA_DON>
DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$DIR/../socket-client/target/shop-socket-client-1.0.0.jar"
SKU="${1:-SKU-001-M-BLK}"
ORDER="${2:-ORD-0001}"

java -jar "$JAR" --name A --repeat 10 --interval 500 CHECK_STOCK "$SKU" &
java -jar "$JAR" --name B --repeat 10 --interval 700 CHECK_STOCK "$SKU" &
java -jar "$JAR" --name C --repeat 10 --interval 600 GET_ORDER_STATUS "$ORDER" &
wait
