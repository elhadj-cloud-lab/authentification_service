package com.bestech.authentification_service.repository;

import com.bestech.authentification_service.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role,Long> {

    Role findByRole(String role);
}
