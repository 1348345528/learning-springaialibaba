package com.example.doc.parser.impl;

import com.example.doc.parser.DocumentParser;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.List;

@Component
public class DocxParser implements DocumentParser {

    @Override
    public String parse(byte[] content) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content));
             StringWriter writer = new StringWriter()) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                writer.write(paragraph.getText());
                writer.write("\n");
            }
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse DOCX", e);
        }
    }

    @Override
    public boolean supports(String fileExtension) {
        return "docx".equalsIgnoreCase(fileExtension);
    }
}
