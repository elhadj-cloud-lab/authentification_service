package com.bestech.authentification_service.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@Tag("integration")
class TokenBlacklistIntegrationTest {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/users";
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void login_tracksJtiInRedis_logout_blacklistsToken() {
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                baseUrl + "/login",
                Map.of("username", "admin", "password", "admin1234"),
                String.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String accessToken = loginResponse.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String refreshToken = loginResponse.getHeaders().getFirst("Refresh-Token");
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        DecodedJWT decoded = JWT.decode(accessToken);
        String jti = decoded.getId();
        String username = decoded.getSubject();
        assertThat(jti).isNotBlank();
        assertThat(username).isEqualTo("admin");

        Set<String> trackedJtis = redisTemplate.opsForSet().members("user:jti:" + username);
        assertThat(trackedJtis).contains(jti);

        HttpHeaders logoutHeaders = new HttpHeaders();
        logoutHeaders.setBearerAuth(accessToken);
        logoutHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map<String, String>> logoutResponse = restTemplate.exchange(
                baseUrl + "/logout",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("refreshToken", refreshToken), logoutHeaders),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(logoutResponse.getBody())
                .containsEntry("message", "Logout successful")
                .containsEntry("status", "SUCCESS");

        assertThat(redisTemplate.opsForValue().get("blacklist:jti:" + jti)).isEqualTo("revoked");
        assertThat(redisTemplate.opsForSet().members("user:jti:" + username)).doesNotContain(jti);

        HttpHeaders protectedHeaders = new HttpHeaders();
        protectedHeaders.setBearerAuth(accessToken);

        ResponseEntity<String> protectedResponse = restTemplate.exchange(
                baseUrl + "/all",
                HttpMethod.GET,
                new HttpEntity<>(protectedHeaders),
                String.class
        );

        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(protectedResponse.getBody()).contains("token_revoked");
    }
}
