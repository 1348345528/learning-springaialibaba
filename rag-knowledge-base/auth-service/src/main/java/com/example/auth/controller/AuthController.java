package com.example.auth.controller;

import com.example.auth.dto.LoginRequest;
import com.example.auth.security.JwtUtil;
import com.example.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", userDetails.getUsername());
        response.put("roles", userDetails.getAuthorities());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/info")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authorization) {
        // 从 token 中提取用户名
        String token = authorization.substring(7);
        String username = jwtUtil.extractUsername(token);
        Map<String, Object> userInfo = authService.getUserInfo(username);
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/menu/tree")
    public ResponseEntity<?> getMenuTree(@RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        String username = jwtUtil.extractUsername(token);
        Object menuTree = authService.getMenuTree(username);
        return ResponseEntity.ok(menuTree);
    }
}
