package com.example.chat.graph;

public record InvoiceResult(
        String invoiceNo,
        String policyNo,
        String status,
        String downloadUrl
) {
}
