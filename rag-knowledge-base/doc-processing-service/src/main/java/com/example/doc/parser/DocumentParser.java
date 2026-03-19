package com.example.doc.parser;

public interface DocumentParser {
    String parse(byte[] content);
    boolean supports(String fileExtension);
}
