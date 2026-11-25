package com.example.clothingshop.controller;

import com.example.clothingshop.service.InvoiceService;
import com.example.clothingshop.service.InvoiceService.Invoice;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final InvoiceService invoices;
    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    private final String host;

    public InvoiceController(InvoiceService invoices, ObjectProvider<JavaMailSender> sender,
                             @Value("${app.password-reset.from:}") String from,
                             @Value("${spring.mail.host:}") String host) {
        this.invoices = invoices;
        this.sender = sender;
        this.from = from;
        this.host = host;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(@PathVariable long id) {
        Invoice invoice = invoices.export(id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(invoice.filename()).build().toString())
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8")).body(invoice.content());
    }

    @PostMapping("/{id}/email")
    public ResponseEntity<Map<String, String>> email(@PathVariable long id) {
        Invoice invoice = invoices.export(id);
        JavaMailSender mail = sender.getIfAvailable();
        if (mail == null || from.isBlank() || host.isBlank()) {
            return ResponseEntity.status(503).body(Map.of("message", "Cửa hàng chưa cấu hình SMTP."));
        }
        try {
            var message = mail.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(invoice.email());
            helper.setSubject("Clothing Shop – Hóa đơn bán hàng #" + id);
            helper.setText("Cảm ơn bạn đã mua hàng tại Clothing Shop. Hóa đơn của bạn có trong tệp đính kèm.");
            helper.addAttachment(invoice.filename(), new ByteArrayResource(invoice.content()), "text/html;charset=UTF-8");
            mail.send(message);
            return ResponseEntity.ok(Map.of("message", "Máy chủ thư đã tiếp nhận hóa đơn."));
        } catch (MessagingException | MailException ex) {
            return ResponseEntity.status(502).body(Map.of("message", "Gửi hóa đơn chưa thành công. Vui lòng kiểm tra SMTP."));
        }
    }
}
