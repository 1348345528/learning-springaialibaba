package com.example.auth.service;

import com.example.auth.dto.PageResult;
import com.example.auth.dto.SysRoleVO;
import com.example.auth.dto.SysUserVO;
import com.example.auth.dto.UserRequest;
import com.example.auth.entity.SysRole;
import com.example.auth.entity.SysUser;
import com.example.auth.entity.SysUserRole;
import com.example.auth.repository.SysRoleRepository;
import com.example.auth.repository.SysUserRepository;
import com.example.auth.repository.SysUserRoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysUserServiceImpl implements SysUserService {

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public SysUserServiceImpl(SysUserRepository userRepository,
                               SysRoleRepository roleRepository,
                               SysUserRoleRepository userRoleRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<SysUserVO> listUsers(String keyword, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<SysUser> userPage;

        if (keyword != null && !keyword.isBlank()) {
            // JPA 从 findAll 加 Specification 需要额外接口，简单用 contains 查询
            userPage = userRepository.findByUsernameContaining(keyword, pageRequest);
        } else {
            userPage = userRepository.findAll(pageRequest);
        }

        return PageResult.from(userPage.map(this::toUserVO));
    }

    @Override
    public SysUserVO getUserById(Long id) {
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        return toUserVO(user);
    }

    @Override
    @Transactional
    public SysUserVO createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在: " + request.getUsername());
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user = userRepository.save(user);

        // 分配角色
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            assignUserRoles(user.getId(), request.getRoleIds());
        }

        return toUserVO(user);
    }

    @Override
    @Transactional
    public SysUserVO updateUser(Long id, UserRequest request) {
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        user.setUpdateTime(LocalDateTime.now());
        user = userRepository.save(user);

        // 更新角色
        if (request.getRoleIds() != null) {
            assignUserRoles(user.getId(), request.getRoleIds());
        }

        return toUserVO(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("用户不存在: " + id);
        }
        userRoleRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("用户不存在: " + userId);
        }
        assignUserRoles(userId, roleIds);
    }

    private void assignUserRoles(Long userId, List<Long> roleIds) {
        userRoleRepository.deleteByUserId(userId);
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleRepository.save(ur);
        }
    }

    private SysUserVO toUserVO(SysUser user) {
        SysUserVO vo = new SysUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());

        // 获取用户角色
        List<SysRole> roles = roleRepository.findRolesByUserId(user.getId());
        if (roles != null) {
            vo.setRoles(roles.stream().map(r -> {
                SysRoleVO roleVO = new SysRoleVO();
                roleVO.setId(r.getId());
                roleVO.setName(r.getName());
                roleVO.setCode(r.getCode());
                return roleVO;
            }).collect(Collectors.toList()));
        } else {
            vo.setRoles(Collections.emptyList());
        }

        return vo;
    }
}
