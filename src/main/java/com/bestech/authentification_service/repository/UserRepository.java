package com.bestech.authentification_service.repository;

import com.bestech.authentification_service.model.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<MyUser,Long> {
    MyUser findByUsername(String username);
}
