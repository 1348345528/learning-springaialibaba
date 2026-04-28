package com.example.auth.service;

import com.example.auth.dto.RoleRequest;
import com.example.auth.dto.SysRoleVO;
import com.example.auth.entity.SysRole;
import com.example.auth.entity.SysRoleMenu;
import com.example.auth.repository.SysRoleMenuRepository;
import com.example.auth.repository.SysRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleRepository roleRepository;
    private final SysRoleMenuRepository roleMenuRepository;

    public SysRoleServiceImpl(SysRoleRepository roleRepository, SysRoleMenuRepository roleMenuRepository) {
        this.roleRepository = roleRepository;
        this.roleMenuRepository = roleMenuRepository;
    }

    @Override
    public List<SysRoleVO> listAll() {
        return roleRepository.findAll().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SysRoleVO createRole(RoleRequest request) {
        SysRole role = new SysRole();
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        role.setCreateTime(LocalDateTime.now());
        role = roleRepository.save(role);

        if (request.getMenuIds() != null) {
            assignMenusToRole(role.getId(), request.getMenuIds());
        }

        return toVO(role);
    }

    @Override
    @Transactional
    public SysRoleVO updateRole(Long id, RoleRequest request) {
        SysRole role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));

        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        role = roleRepository.save(role);

        // 重新分配菜单
        roleMenuRepository.deleteByRoleId(id);
        if (request.getMenuIds() != null) {
            assignMenusToRole(id, request.getMenuIds());
        }

        return toVO(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        roleMenuRepository.deleteByRoleId(id);
        roleRepository.deleteById(id);
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return roleMenuRepository.findByRoleId(roleId).stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuRepository.deleteByRoleId(roleId);
        if (menuIds != null) {
            assignMenusToRole(roleId, menuIds);
        }
    }

    private void assignMenusToRole(Long roleId, List<Long> menuIds) {
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuRepository.save(rm);
        }
    }

    private SysRoleVO toVO(SysRole role) {
        SysRoleVO vo = new SysRoleVO();
        vo.setId(role.getId());
        vo.setName(role.getName());
        vo.setCode(role.getCode());
        vo.setDescription(role.getDescription());
        return vo;
    }
}
