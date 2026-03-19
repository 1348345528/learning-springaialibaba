package com.example.doc.parser.impl;

import com.example.doc.parser.DocumentParser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.StringJoiner;

@Component
public class ExcelParser implements DocumentParser {

    @Override
    public String parse(byte[] content) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = new ByteArrayInputStream(content);
             Workbook workbook = new XSSFWorkbook(is)) {

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                sb.append("=== Sheet: ").append(sheet.getSheetName()).append(" ===\n");

                for (Row row : sheet) {
                    StringJoiner joiner = new StringJoiner("\t");
                    for (Cell cell : row) {
                        joiner.add(getCellValue(cell));
                    }
                    sb.append(joiner.toString()).append("\n");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel", e);
        }
        return sb.toString();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    @Override
    public boolean supports(String fileExtension) {
        return "xlsx".equalsIgnoreCase(fileExtension) || "xls".equalsIgnoreCase(fileExtension);
    }
}