package com.example.auth.service;

import java.util.Map;

public interface AuthService {
    Map<String, Object> getUserInfo(String username);
    Object getMenuTree(String username);
}
