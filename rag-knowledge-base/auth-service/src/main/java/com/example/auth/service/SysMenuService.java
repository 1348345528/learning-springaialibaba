package com.example.auth.service;

import com.example.auth.dto.MenuRequest;
import com.example.auth.dto.SysMenuVO;

import java.util.List;

public interface SysMenuService {
    List<SysMenuVO> getMenuTree();
    SysMenuVO createMenu(MenuRequest request);
    SysMenuVO updateMenu(Long id, MenuRequest request);
    void deleteMenu(Long id);
}
