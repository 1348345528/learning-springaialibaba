package com.example.doc.parser.impl;

import com.example.doc.parser.DocumentParser;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
public class PdfParser implements DocumentParser {
    private final Tika tika = new Tika();

    @Override
    public String parse(byte[] content) {
        try {
            return tika.parseToString(new java.io.ByteArrayInputStream(content));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse PDF", e);
        }
    }

    @Override
    public boolean supports(String fileExtension) {
        return "pdf".equalsIgnoreCase(fileExtension);
    }
}
