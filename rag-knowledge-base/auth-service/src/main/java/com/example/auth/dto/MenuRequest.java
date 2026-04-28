package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MenuRequest {
    private Long parentId;
    @NotBlank
    private String name;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    @NotNull
    private Integer menuType;
    private String permission;
    private Integer visible;
}
