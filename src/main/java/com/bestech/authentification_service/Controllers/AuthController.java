package com.bestech.authentification_service.Controllers;

import com.bestech.authentification_service.security.LogoutService;
import com.bestech.authentification_service.service.refreshtoken.LogoutRequest;
import com.bestech.authentification_service.service.refreshtoken.RefreshTokenRequest;
import com.bestech.authentification_service.service.refreshtoken.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final RefreshTokenService refreshTokenService;
    private final LogoutService logoutService;

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@RequestBody RefreshTokenRequest request,
                                        HttpServletResponse response) {
        RefreshTokenService.TokenPair tokens = refreshTokenService.refresh(request.getRefreshToken());
        response.addHeader("Authorization", tokens.accessToken());
        response.addHeader("Refresh-Token", tokens.refreshToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody(required = false) LogoutRequest request,
                                                      @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String accessToken = extractBearerToken(authHeader);
        String refreshToken = request != null ? request.getRefreshToken() : null;
        logoutService.logout(accessToken, refreshToken);
        return ResponseEntity.ok(Map.of("message", "Logout successful", "status", "SUCCESS"));
    }

    @PostMapping("/admin/revoke/{username}")
    public ResponseEntity<Void> revokeUserSessions(@PathVariable String username) {
        logoutService.revokeAllSessions(username);
        return ResponseEntity.ok().build();
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
