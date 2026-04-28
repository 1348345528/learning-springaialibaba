package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UserRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    private String email;

    private Integer status = 1;

    private List<Long> roleIds;
}
