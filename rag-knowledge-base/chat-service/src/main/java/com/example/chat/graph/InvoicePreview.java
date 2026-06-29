package com.example.chat.graph;

import java.math.BigDecimal;

public record InvoicePreview(
        String policyNo,
        String policyHolder,
        String buyerName,
        String taxNo,
        BigDecimal amount,
        String itemName
) {
}
