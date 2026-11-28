package com.example.clothingshop.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {

    private final JdbcTemplate jdbcTemplate;

    public AdminReportController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public String reportPage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model) {

        if (fromDate == null) fromDate = LocalDate.now().minusDays(30);
        if (toDate == null) toDate = LocalDate.now();

        String startStr = fromDate + " 00:00:00";
        String endStr = toDate + " 23:59:59";

        BigDecimal totalRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(TotalAmount), 0) FROM Orders WHERE OrderStatus = 'COMPLETED' AND OrderDate BETWEEN ? AND ?",
                BigDecimal.class, startStr, endStr);

        Integer completedOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Orders WHERE OrderStatus = 'COMPLETED' AND OrderDate BETWEEN ? AND ?",
                Integer.class, startStr, endStr);

        Integer totalStock = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(StockQuantity), 0) FROM ProductVariants WHERE IsActive = 1",
                Integer.class);

        var orders = jdbcTemplate.queryForList("""
            SELECT o.OrderId, o.OrderCode, u.FullName, o.OrderDate, o.PaymentMethod, o.TotalAmount
            FROM Orders o
            JOIN Users u ON u.UserId = o.UserId
            WHERE o.OrderStatus = 'COMPLETED' AND o.OrderDate BETWEEN ? AND ?
            ORDER BY o.OrderDate DESC
        """, startStr, endStr);

        var topProducts = jdbcTemplate.queryForList("""
            SELECT TOP 5 p.ProductName, SUM(oi.Quantity) AS TotalSold, SUM(oi.LineTotal) AS Revenue
            FROM OrderItems oi
            JOIN Orders o ON o.OrderId = oi.OrderId
            JOIN ProductVariants pv ON pv.VariantId = oi.VariantId
            JOIN Products p ON p.ProductId = pv.ProductId
            WHERE o.OrderStatus = 'COMPLETED' AND o.OrderDate BETWEEN ? AND ?
            GROUP BY p.ProductName
            ORDER BY TotalSold DESC
        """, startStr, endStr);

        var dailyChart = jdbcTemplate.queryForList("""
            SELECT CAST(OrderDate AS DATE) AS Day, SUM(TotalAmount) AS Revenue
            FROM Orders
            WHERE OrderStatus = 'COMPLETED' AND o.OrderDate BETWEEN ? AND ?
            GROUP BY CAST(OrderDate AS DATE)
            ORDER BY Day ASC
        """.replace("o.OrderDate", "OrderDate"), startStr, endStr);

        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        model.addAttribute("completedOrders", completedOrders != null ? completedOrders : 0);
        model.addAttribute("totalStock", totalStock != null ? totalStock : 0);
        model.addAttribute("orders", orders);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("chartLabels", dailyChart.stream().map(r -> String.valueOf(r.get("Day"))).toList());
        model.addAttribute("chartValues", dailyChart.stream().map(r -> r.get("Revenue")).toList());

        return "admin/reports";
    }

    @GetMapping("/export-excel")
    public void exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            HttpServletResponse response) throws IOException {

        if (fromDate == null) fromDate = LocalDate.now().minusDays(30);
        if (toDate == null) toDate = LocalDate.now();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=BaoCao_DoanhThu_" + fromDate + "_den_" + toDate + ".xlsx");

        var orders = jdbcTemplate.queryForList("""
            SELECT o.OrderCode, u.FullName, o.OrderDate, o.PaymentMethod, o.OrderStatus, o.TotalAmount
            FROM Orders o
            JOIN Users u ON u.UserId = o.UserId
            WHERE o.OrderStatus = 'COMPLETED' AND o.OrderDate BETWEEN ? AND ?
            ORDER BY o.OrderDate DESC
        """, fromDate + " 00:00:00", toDate + " 23:59:59");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("BaoCaoDoanhThu");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            String[] columns = {"Mã đơn hàng", "Khách hàng", "Ngày đặt", "Phương thức TT", "Trạng thái", "Tổng tiền (VNĐ)"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            double sum = 0;
            for (var o : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(String.valueOf(o.get("OrderCode")));
                row.createCell(1).setCellValue(String.valueOf(o.get("FullName")));
                row.createCell(2).setCellValue(String.valueOf(o.get("OrderDate")));
                row.createCell(3).setCellValue(String.valueOf(o.get("PaymentMethod")));
                row.createCell(4).setCellValue(String.valueOf(o.get("OrderStatus")));
                double amount = o.get("TotalAmount") != null ? ((Number) o.get("TotalAmount")).doubleValue() : 0;
                row.createCell(5).setCellValue(amount);
                sum += amount;
            }

            Row totalRow = sheet.createRow(rowIdx);
            Cell lblCell = totalRow.createCell(4);
            lblCell.setCellValue("TỔNG DOANH THU:");
            lblCell.setCellStyle(headerStyle);

            Cell valCell = totalRow.createCell(5);
            valCell.setCellValue(sum);
            valCell.setCellStyle(headerStyle);

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(response.getOutputStream());
        }
    }
}