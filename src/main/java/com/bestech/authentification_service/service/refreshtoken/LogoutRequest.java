package com.bestech.authentification_service.service.refreshtoken;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
}
