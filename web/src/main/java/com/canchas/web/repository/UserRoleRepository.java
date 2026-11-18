package com.canchas.web.repository;

import com.canchas.web.model.UserRole;
import com.canchas.web.model.UserRoleId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.roleId = :roleId")
    long countByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT ur.userId FROM UserRole ur WHERE ur.roleId = :roleId ORDER BY ur.userId ASC")
    List<Long> findUserIdsByRoleId(@Param("roleId") Long roleId, Pageable pageable);
}

