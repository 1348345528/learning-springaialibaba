package com.example.auth.service;

import com.example.auth.dto.MenuRequest;
import com.example.auth.dto.SysMenuVO;
import com.example.auth.entity.SysMenu;
import com.example.auth.repository.SysMenuRepository;
import com.example.auth.repository.SysRoleMenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuRepository menuRepository;
    private final SysRoleMenuRepository roleMenuRepository;

    public SysMenuServiceImpl(SysMenuRepository menuRepository, SysRoleMenuRepository roleMenuRepository) {
        this.menuRepository = menuRepository;
        this.roleMenuRepository = roleMenuRepository;
    }

    @Override
    public List<SysMenuVO> getMenuTree() {
        List<SysMenu> allMenus = menuRepository.findAll();
        return buildTree(allMenus);
    }

    @Override
    @Transactional
    public SysMenuVO createMenu(MenuRequest request) {
        SysMenu menu = new SysMenu();
        applyRequest(menu, request);
        menu.setCreateTime(LocalDateTime.now());
        menu = menuRepository.save(menu);
        return toVO(menu);
    }

    @Override
    @Transactional
    public SysMenuVO updateMenu(Long id, MenuRequest request) {
        SysMenu menu = menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu not found: " + id));
        applyRequest(menu, request);
        menu = menuRepository.save(menu);
        return toVO(menu);
    }

    @Override
    @Transactional
    public void deleteMenu(Long id) {
        // 删除角色-菜单关联
        roleMenuRepository.deleteByMenuId(id);

        // 如果有子菜单，将子菜单的 parentId 设为 0
        List<SysMenu> children = menuRepository.findByParentIdOrderBySortOrderAsc(id);
        for (SysMenu child : children) {
            child.setParentId(0L);
            menuRepository.save(child);
        }

        menuRepository.deleteById(id);
    }

    private void applyRequest(SysMenu menu, MenuRequest request) {
        menu.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        menu.setName(request.getName());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setIcon(request.getIcon());
        menu.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        menu.setMenuType(request.getMenuType());
        menu.setPermission(request.getPermission());
        menu.setVisible(request.getVisible() != null ? request.getVisible() : 1);
    }

    private SysMenuVO toVO(SysMenu menu) {
        SysMenuVO vo = new SysMenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setName(menu.getName());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setIcon(menu.getIcon());
        vo.setSortOrder(menu.getSortOrder());
        vo.setMenuType(menu.getMenuType());
        vo.setPermission(menu.getPermission());
        vo.setVisible(menu.getVisible());
        vo.setChildren(Collections.emptyList());
        return vo;
    }

    private List<SysMenuVO> buildTree(List<SysMenu> menus) {
        Map<Long, List<SysMenu>> parentToChildren = menus.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(SysMenu::getParentId));

        return menus.stream()
                .filter(m -> m.getParentId() == null || m.getParentId() == 0L)
                .sorted((a, b) -> {
                    int sortCompare = Integer.compare(
                            a.getSortOrder() != null ? a.getSortOrder() : 0,
                            b.getSortOrder() != null ? b.getSortOrder() : 0);
                    if (sortCompare != 0) return sortCompare;
                    return Long.compare(a.getId(), b.getId());
                })
                .map(m -> convertToNode(m, parentToChildren))
                .collect(Collectors.toList());
    }

    private SysMenuVO convertToNode(SysMenu menu, Map<Long, List<SysMenu>> parentToChildren) {
        SysMenuVO node = toVO(menu);

        List<SysMenu> children = parentToChildren.getOrDefault(menu.getId(), Collections.emptyList());
        if (!children.isEmpty()) {
            List<SysMenuVO> childNodes = children.stream()
                    .sorted((a, b) -> {
                        int sortCompare = Integer.compare(
                                a.getSortOrder() != null ? a.getSortOrder() : 0,
                                b.getSortOrder() != null ? b.getSortOrder() : 0);
                        if (sortCompare != 0) return sortCompare;
                        return Long.compare(a.getId(), b.getId());
                    })
                    .map(child -> convertToNode(child, parentToChildren))
                    .collect(Collectors.toList());
            node.setChildren(childNodes);
        }

        return node;
    }
}
