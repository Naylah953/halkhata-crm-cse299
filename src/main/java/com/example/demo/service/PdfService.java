package com.example.demo.service;

import com.example.demo.domain.Order;
import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class PdfService {

    public byte[] generateOrderReceipt(Order order) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);

        document.open();
        // Add Title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        document.add(new Paragraph("Order Receipt - " + order.getTenant().getName(), titleFont));
        document.add(new Paragraph("Order ID: " + order.getId()));
        document.add(new Paragraph("Date: " + LocalDateTime.now().toString()));
        document.add(new Paragraph(" ")); // Spacer

        AtomicReference<BigDecimal> orderSum = new AtomicReference<>(BigDecimal.valueOf(0));

        // Add Table for Items
        Table table = new Table(3); // Product, Qty, Price
        table.addCell("Product");
        table.addCell("Qty");
        table.addCell("Price");

        order.getItems().forEach(item -> {
            table.addCell(item.getProduct().getBaseName());
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell(item.getUnitPrice() + " BDT");

            // BigDecimal requires .add() and you must return the result
            orderSum.updateAndGet(v -> v.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
        });

        document.add(table);
// Use .get() to extract the value from AtomicReference
        document.add(new Paragraph("\nTotal Amount: " + orderSum.get() + " BDT"));

        document.close();
        return out.toByteArray();
    }
}