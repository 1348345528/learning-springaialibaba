package com.example.auth.controller;

import com.example.auth.dto.MenuRequest;
import com.example.auth.dto.SysMenuVO;
import com.example.auth.service.SysMenuService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sys/menu")
public class SysMenuController {

    private final SysMenuService menuService;

    public SysMenuController(SysMenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/tree")
    public ResponseEntity<List<SysMenuVO>> getMenuTree() {
        return ResponseEntity.ok(menuService.getMenuTree());
    }

    @PostMapping
    public ResponseEntity<SysMenuVO> createMenu(@Valid @RequestBody MenuRequest request) {
        return ResponseEntity.ok(menuService.createMenu(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SysMenuVO> updateMenu(@PathVariable Long id, @Valid @RequestBody MenuRequest request) {
        return ResponseEntity.ok(menuService.updateMenu(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return ResponseEntity.ok().build();
    }
}
