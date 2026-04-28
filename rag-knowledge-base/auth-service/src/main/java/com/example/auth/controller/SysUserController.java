package com.example.auth.controller;

import com.example.auth.dto.PageResult;
import com.example.auth.dto.SysUserVO;
import com.example.auth.dto.UserRequest;
import com.example.auth.service.SysUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/sys/user")
public class SysUserController {

    private final SysUserService sysUserService;

    public SysUserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @GetMapping("/list")
    public ResponseEntity<PageResult<SysUserVO>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(sysUserService.listUsers(keyword, page, size));
    }

    @GetMapping
    public ResponseEntity<PageResult<SysUserVO>> listUsersWithoutPath(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(sysUserService.listUsers(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SysUserVO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(sysUserService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<SysUserVO> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(sysUserService.createUser(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SysUserVO> updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        return ResponseEntity.ok(sysUserService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{userId}/roles")
    public ResponseEntity<?> assignRoles(@PathVariable Long userId, @RequestBody Map<String, java.util.List<Long>> body) {
        sysUserService.assignRoles(userId, body.get("roleIds"));
        return ResponseEntity.ok(Map.of("success", true));
    }
}
