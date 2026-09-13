package com.swu.smartgrade.user;

import com.swu.smartgrade.common.auth.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByClassIdAndRole(Long classId, Role role);

    List<User> findByClassId(Long classId);
}
