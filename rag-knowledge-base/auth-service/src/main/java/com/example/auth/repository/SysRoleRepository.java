package com.example.auth.repository;

import com.example.auth.entity.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysRoleRepository extends JpaRepository<SysRole, Long> {
    SysRole findByCode(String code);

    @Query("SELECT r FROM SysRole r JOIN SysUserRole ur ON r.id = ur.roleId WHERE ur.userId = :userId")
    List<SysRole> findRolesByUserId(@Param("userId") Long userId);
}
