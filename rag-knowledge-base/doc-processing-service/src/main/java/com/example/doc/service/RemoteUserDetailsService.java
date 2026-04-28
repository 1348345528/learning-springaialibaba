package com.example.doc.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class RemoteUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 简单返回一个 UserDetails 对象，权限为空
        // 实际可以从 token 中解析出权限，但为了简化，直接返回包含用户名的 User 对象
        return User.withUsername(username)
                .password("")
                .authorities(Collections.emptyList())
                .build();
    }
}
