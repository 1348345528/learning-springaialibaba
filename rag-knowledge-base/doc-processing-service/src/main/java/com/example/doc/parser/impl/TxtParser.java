package com.example.doc.parser.impl;

import com.example.doc.parser.DocumentParser;
import org.springframework.stereotype.Component;

@Component
public class TxtParser implements DocumentParser {

    @Override
    public String parse(byte[] content) {
        return new String(content);
    }

    @Override
    public boolean supports(String fileExtension) {
        return "txt".equalsIgnoreCase(fileExtension);
    }
}
