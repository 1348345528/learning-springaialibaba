package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResult<T> {
    private List<T> records;
    private long total;
    private int size;
    private int current;

    public static <T> PageResult<T> from(Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber() + 1
        );
    }
}
