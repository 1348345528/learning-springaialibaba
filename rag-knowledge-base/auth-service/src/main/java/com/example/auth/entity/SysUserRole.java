package com.example.auth.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "sys_user_role")
@IdClass(SysUserRole.UserRoleId.class)
public class SysUserRole {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "role_id")
    private Long roleId;

    @Data
    public static class UserRoleId implements Serializable {
        private Long userId;
        private Long roleId;
    }
}
