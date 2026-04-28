package com.example.auth.repository;

import com.example.auth.entity.SysUserRole;
import com.example.auth.entity.SysUserRole.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysUserRoleRepository extends JpaRepository<SysUserRole, UserRoleId> {
    List<SysUserRole> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
