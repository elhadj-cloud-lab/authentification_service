package com.bestech.authentification_service.service.refreshtoken;

import com.bestech.authentification_service.model.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    RefreshToken findByToken(String token);

    void deleteByUser(MyUser user);
}
