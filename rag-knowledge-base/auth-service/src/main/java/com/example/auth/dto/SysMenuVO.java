package com.example.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class SysMenuVO {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer menuType;
    private String permission;
    private Integer visible;
    private List<SysMenuVO> children;
}
