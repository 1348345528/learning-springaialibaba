package com.example.auth.service;

import com.example.auth.dto.RoleRequest;
import com.example.auth.dto.SysRoleVO;

import java.util.List;

public interface SysRoleService {
    List<SysRoleVO> listAll();
    SysRoleVO createRole(RoleRequest request);
    SysRoleVO updateRole(Long id, RoleRequest request);
    void deleteRole(Long id);
    List<Long> getRoleMenuIds(Long roleId);
    void assignMenus(Long roleId, List<Long> menuIds);
}
