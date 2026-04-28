package com.example.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SysUserVO {
    private Long id;
    private String username;
    private String email;
    private Integer status;
    private List<SysRoleVO> roles;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
