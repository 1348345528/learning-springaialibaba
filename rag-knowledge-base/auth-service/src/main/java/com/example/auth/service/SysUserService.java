package com.example.auth.service;

import com.example.auth.dto.PageResult;
import com.example.auth.dto.SysUserVO;
import com.example.auth.dto.UserRequest;

public interface SysUserService {
    PageResult<SysUserVO> listUsers(String keyword, int page, int size);

    SysUserVO getUserById(Long id);

    SysUserVO createUser(UserRequest request);

    SysUserVO updateUser(Long id, UserRequest request);

    void deleteUser(Long id);

    void assignRoles(Long userId, java.util.List<Long> roleIds);
}
