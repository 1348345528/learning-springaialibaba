package com.example.auth.repository;

import com.example.auth.entity.SysMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysMenuRepository extends JpaRepository<SysMenu, Long> {
    List<SysMenu> findByParentIdOrderBySortOrderAsc(Long parentId);

    /**
     * 根据用户ID查询其拥有的所有权限标识（permission字段，去重、非空）
     */
    @Query("SELECT DISTINCT m.permission FROM SysMenu m " +
           "JOIN SysRoleMenu rm ON m.id = rm.menuId " +
           "JOIN SysUserRole ur ON rm.roleId = ur.roleId " +
           "WHERE ur.userId = :userId AND m.permission IS NOT NULL AND m.permission != ''")
    List<String> findPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询其可见的菜单（menu_type=2 菜单，且visible=1），按父级和排序字段组装树形结构
     */
    @Query("SELECT DISTINCT m FROM SysMenu m " +
           "JOIN SysRoleMenu rm ON m.id = rm.menuId " +
           "JOIN SysUserRole ur ON rm.roleId = ur.roleId " +
           "WHERE ur.userId = :userId AND m.visible = 1 AND m.menuType IN (1, 2) " +
           "ORDER BY m.parentId ASC, m.sortOrder ASC")
    List<SysMenu> findMenusByUserId(@Param("userId") Long userId);
}
