package com.example.chat.graph;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class InvoiceMockService {

    public InvoiceEligibility checkInvoiceEligibility(String policyNo) {
        if (policyNo == null || policyNo.isBlank()) {
            return new InvoiceEligibility(false, "Policy number is required before invoicing.");
        }
        if (policyNo.toUpperCase().contains("NO")) {
            return new InvoiceEligibility(false, "The mocked policy is not eligible for invoicing.");
        }
        return new InvoiceEligibility(true, "The mocked policy is eligible for invoicing.");
    }

    public InvoicePreview assembleInvoicePreview(String policyNo) {
        return new InvoicePreview(
                policyNo,
                "Zhang San",
                "Zhang San",
                "91310000MOCK12345X",
                new BigDecimal("1288.00"),
                "Insurance premium"
        );
    }

    public InvoiceResult generateInvoice(InvoicePreview preview) {
        String suffix = preview.policyNo().replaceAll("[^A-Za-z0-9]", "");
        return new InvoiceResult(
                "INV-" + suffix,
                preview.policyNo(),
                "GENERATED",
                "/api/mock-invoices/INV-" + suffix + ".pdf"
        );
    }
}
