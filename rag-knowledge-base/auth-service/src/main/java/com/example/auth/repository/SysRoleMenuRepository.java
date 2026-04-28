package com.example.auth.repository;

import com.example.auth.entity.SysRoleMenu;
import com.example.auth.entity.SysRoleMenu.RoleMenuId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysRoleMenuRepository extends JpaRepository<SysRoleMenu, RoleMenuId> {
    List<SysRoleMenu> findByRoleId(Long roleId);
    void deleteByRoleId(Long roleId);
    void deleteByMenuId(Long menuId);
}
