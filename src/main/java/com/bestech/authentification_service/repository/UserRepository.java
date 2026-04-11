package com.bestech.authentification_service.repository;

import com.bestech.authentification_service.model.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<MyUser,Long> {
    MyUser findByUsername(String username);
    Optional<MyUser> findByEmail(String email);
}
