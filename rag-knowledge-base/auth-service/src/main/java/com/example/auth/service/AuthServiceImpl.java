package com.example.auth.service;

import com.example.auth.entity.SysUser;
import com.example.auth.entity.SysRole;
import com.example.auth.entity.SysMenu;
import com.example.auth.repository.SysUserRepository;
import com.example.auth.repository.SysRoleRepository;
import com.example.auth.repository.SysMenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private SysRoleRepository roleRepository;

    @Autowired
    private SysMenuRepository menuRepository;

    @Override
    public Map<String, Object> getUserInfo(String username) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // 获取用户角色
        List<SysRole> roles = roleRepository.findRolesByUserId(user.getId());
        List<String> roleCodes = roles.stream().map(SysRole::getCode).collect(Collectors.toList());

        // 获取用户权限（基于角色的菜单权限标识）
        List<String> permissions = menuRepository.findPermissionsByUserId(user.getId());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("status", user.getStatus());
        userInfo.put("roles", roleCodes);
        userInfo.put("permissions", permissions);
        return userInfo;
    }

    @Override
    public Object getMenuTree(String username) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // 获取用户可见的菜单（根据角色授权的菜单，且 visible=1）
        List<SysMenu> menus = menuRepository.findMenusByUserId(user.getId());
        // 构建树形结构
        return buildMenuTree(menus);
    }

    private List<Map<String, Object>> buildMenuTree(List<SysMenu> menus) {
        Map<Long, List<SysMenu>> parentToChildren = menus.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(SysMenu::getParentId));

        return menus.stream()
                .filter(m -> m.getParentId() == null || m.getParentId() == 0L)
                .map(m -> convertToNode(m, parentToChildren))
                .collect(Collectors.toList());
    }

    private Map<String, Object> convertToNode(SysMenu menu, Map<Long, List<SysMenu>> parentToChildren) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", menu.getId());
        node.put("name", menu.getName());
        node.put("path", menu.getPath());
        node.put("component", menu.getComponent());
        node.put("icon", menu.getIcon());
        node.put("sortOrder", menu.getSortOrder());
        node.put("menuType", menu.getMenuType());
        node.put("permission", menu.getPermission());
        node.put("visible", menu.getVisible());

        List<SysMenu> children = parentToChildren.getOrDefault(menu.getId(), Collections.emptyList());
        if (!children.isEmpty()) {
            List<Map<String, Object>> childNodes = children.stream()
                    .map(child -> convertToNode(child, parentToChildren))
                    .collect(Collectors.toList());
            node.put("children", childNodes);
        } else {
            node.put("children", Collections.emptyList());
        }
        return node;
    }
}
