package com.example.auth.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_menu")
public class SysMenu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId = 0L;

    @Column(nullable = false)
    private String name;

    private String path;

    private String component;

    private String icon;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "menu_type")
    private Integer menuType; // 1目录 2菜单 3按钮

    private String permission;

    private Integer visible = 1;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}
