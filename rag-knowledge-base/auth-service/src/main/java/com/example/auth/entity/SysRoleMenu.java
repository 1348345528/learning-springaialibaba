package com.example.auth.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "sys_role_menu")
@IdClass(SysRoleMenu.RoleMenuId.class)
public class SysRoleMenu {
    @Id
    @Column(name = "role_id")
    private Long roleId;

    @Id
    @Column(name = "menu_id")
    private Long menuId;

    @Data
    public static class RoleMenuId implements Serializable {
        private Long roleId;
        private Long menuId;
    }
}
