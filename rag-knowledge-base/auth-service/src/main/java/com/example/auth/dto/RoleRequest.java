package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class RoleRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String code;
    private String description;
    private List<Long> menuIds;
}
