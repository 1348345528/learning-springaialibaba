package com.example.auth.controller;

import com.example.auth.dto.RoleRequest;
import com.example.auth.dto.SysRoleVO;
import com.example.auth.service.SysRoleService;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sys/role")
public class SysRoleController {

    private final SysRoleService roleService;

    public SysRoleController(SysRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/list")
    public ResponseEntity<List<SysRoleVO>> listRoles() {
        return ResponseEntity.ok(roleService.listAll());
    }

    @GetMapping
    public ResponseEntity<List<SysRoleVO>> listRolesWithoutPath() {
        return listRoles();
    }

    @PostMapping
    public ResponseEntity<SysRoleVO> createRole(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.createRole(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SysRoleVO> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/menu/{roleId}")
    public ResponseEntity<List<Long>> getRoleMenuIds(@PathVariable Long roleId) {
        return ResponseEntity.ok(roleService.getRoleMenuIds(roleId));
    }

    @PostMapping("/{roleId}/menus")
    public ResponseEntity<Void> assignMenus(@PathVariable Long roleId, @RequestBody RoleMenuRequest request) {
        roleService.assignMenus(roleId, request.getMenuIds());
        return ResponseEntity.ok().build();
    }

    @Data
    static class RoleMenuRequest {
        private List<Long> menuIds;
    }
}
