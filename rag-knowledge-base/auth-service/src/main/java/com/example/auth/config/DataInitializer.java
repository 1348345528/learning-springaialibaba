package com.example.auth.config;

import com.example.auth.entity.SysMenu;
import com.example.auth.entity.SysRole;
import com.example.auth.entity.SysRoleMenu;
import com.example.auth.entity.SysUser;
import com.example.auth.entity.SysUserRole;
import com.example.auth.repository.SysMenuRepository;
import com.example.auth.repository.SysRoleMenuRepository;
import com.example.auth.repository.SysRoleRepository;
import com.example.auth.repository.SysUserRepository;
import com.example.auth.repository.SysUserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final SysMenuRepository menuRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleMenuRepository roleMenuRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername("admin")) {
            log.info("Initial data already exists, skipping initialization.");
            return;
        }

        log.info("Initializing default data...");

        // Create roles
        SysRole adminRole = new SysRole();
        adminRole.setName("管理员");
        adminRole.setCode("ROLE_ADMIN");
        adminRole.setCreateTime(LocalDateTime.now());
        adminRole = roleRepository.save(adminRole);

        SysRole userRole = new SysRole();
        userRole.setName("普通用户");
        userRole.setCode("ROLE_USER");
        userRole.setCreateTime(LocalDateTime.now());
        userRole = roleRepository.save(userRole);

        // Create admin user
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEmail("admin@example.com");
        admin.setStatus(1);
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        admin = userRepository.save(admin);

        // Create regular user
        SysUser user = new SysUser();
        user.setUsername("user");
        user.setPassword(passwordEncoder.encode("user123"));
        user.setEmail("user@example.com");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user = userRepository.save(user);

        // Assign roles
        SysUserRole adminUserRole = new SysUserRole();
        adminUserRole.setUserId(admin.getId());
        adminUserRole.setRoleId(adminRole.getId());
        userRoleRepository.save(adminUserRole);

        SysUserRole normalUserRole = new SysUserRole();
        normalUserRole.setUserId(user.getId());
        normalUserRole.setRoleId(userRole.getId());
        userRoleRepository.save(normalUserRole);

        // Create menus
        SysMenu ragDir = createMenu(null, "RAG知识库维护", null, null, "DatabaseOutlined", 1, 1, null);
        SysMenu docMenu = createMenu(ragDir.getId(), "文档管理", "/rag/document", "DocumentUpload", null, 2, 2, null);
        SysMenu chunkMenu = createMenu(ragDir.getId(), "知识块管理", "/rag/chunk", "ChunkManagement", null, 2, 2, null);
        SysMenu chatMenu = createMenu(null, "智能问答", "/chat", "ChatQA", "RobotOutlined", 1, 2, null);

        SysMenu sysDir = createMenu(null, "系统管理", null, null, "SettingOutlined", 1, 1, "ROLE_ADMIN");
        SysMenu userMgmt = createMenu(sysDir.getId(), "用户管理", "/sys/user", "UserManagement", null, 2, 2, "ROLE_ADMIN");
        SysMenu roleMgmt = createMenu(sysDir.getId(), "角色管理", "/sys/role", "RoleManagement", null, 2, 2, "ROLE_ADMIN");
        SysMenu menuMgmt = createMenu(sysDir.getId(), "菜单管理", "/sys/menu", "MenuManagement", null, 2, 2, "ROLE_ADMIN");

        List<SysMenu> allMenus = List.of(ragDir, docMenu, chunkMenu, chatMenu, sysDir, userMgmt, roleMgmt, menuMgmt);

        // Assign all menus to admin role
        for (SysMenu menu : allMenus) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(adminRole.getId());
            rm.setMenuId(menu.getId());
            roleMenuRepository.save(rm);
        }

        // Assign only RAG menus + chat to user role
        for (SysMenu menu : List.of(ragDir, docMenu, chunkMenu, chatMenu)) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(userRole.getId());
            rm.setMenuId(menu.getId());
            roleMenuRepository.save(rm);
        }

        log.info("Default data initialized successfully.");
    }

    private SysMenu createMenu(Long parentId, String name, String path, String component, String icon,
                               Integer sortOrder, Integer menuType, String permission) {
        SysMenu menu = new SysMenu();
        menu.setParentId(parentId != null ? parentId : 0L);
        menu.setName(name);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setIcon(icon);
        menu.setSortOrder(sortOrder);
        menu.setMenuType(menuType);
        menu.setPermission(permission);
        menu.setVisible(1);
        menu.setCreateTime(LocalDateTime.now());
        return menuRepository.save(menu);
    }
}
