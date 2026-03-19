package com.example.doc.parser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentParserFactory {

    @Autowired
    private List<DocumentParser> parsers;

    public DocumentParser getParser(String fileExtension) {
        return parsers.stream()
                .filter(p -> p.supports(fileExtension))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file type: " + fileExtension));
    }
}
