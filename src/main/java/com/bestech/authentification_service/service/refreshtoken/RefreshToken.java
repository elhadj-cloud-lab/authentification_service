package com.bestech.authentification_service.service.refreshtoken;

import com.bestech.authentification_service.model.MyUser;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Entity
@NoArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = false)
    private Date expirationTime;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private MyUser user;

    public RefreshToken(String token, MyUser user, Date expirationTime) {
        this.token = token;
        this.user = user;
        this.expirationTime = expirationTime;
    }
}
