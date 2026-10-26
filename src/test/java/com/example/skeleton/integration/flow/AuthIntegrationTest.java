package com.example.skeleton.integration.flow;

import com.example.skeleton.repository.RefreshTokenRepository;
import com.example.skeleton.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "security.dev-mode=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void fullRefreshFlow_rotateOnUseDetectReuseAndLogout() throws Exception {
        registerUser("flow@example.com", "password123", "Flow User");

        LoginTokens tokens1 = login("flow@example.com", "password123");
        assertThat(tokens1.accessToken()).isNotBlank();
        assertThat(tokens1.refreshToken()).isNotBlank();

        LoginTokens tokens2 = refresh(tokens1.refreshToken());
        assertThat(tokens2.refreshToken()).isNotEqualTo(tokens1.refreshToken());
        assertThat(tokens2.accessToken()).isNotBlank();

        expectRefreshUnauthorized(tokens1.refreshToken());

        LoginTokens tokens3 = refresh(tokens2.refreshToken());
        assertThat(tokens3.refreshToken()).isNotEqualTo(tokens2.refreshToken());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens3.refreshToken() + "\"}"))
                .andExpect(status().isNoContent());

        expectRefreshUnauthorized(tokens3.refreshToken());
    }

    @Test
    void whenLogoutAll_thenAllRefreshTokensRevoked() throws Exception {
        registerUser("logoutall@example.com", "password123", "Logout All");

        LoginTokens session1 = login("logoutall@example.com", "password123");
        LoginTokens session2 = login("logoutall@example.com", "password123");
        LoginTokens session3 = login("logoutall@example.com", "password123");

        assertThat(refreshTokenRepository.findAll())
                .filteredOn(t -> !t.isRevoked())
                .hasSize(3);

        mockMvc.perform(post("/api/auth/logout-all")
                        .header("Authorization", "Bearer " + session1.accessToken()))
                .andExpect(status().isNoContent());

        assertThat(refreshTokenRepository.findAll())
                .allMatch(t -> t.isRevoked());

        expectRefreshUnauthorized(session1.refreshToken());
        expectRefreshUnauthorized(session2.refreshToken());
        expectRefreshUnauthorized(session3.refreshToken());
    }

    @Test
    void whenLogoutAllWithoutAuth_thenReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/logout-all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenNoToken_thenProtectedEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenInvalidToken_thenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenTamperedToken_thenReturns401() throws Exception {
        registerUser("tamper@example.com", "password123", "Tamper User");
        LoginTokens tokens = login("tamper@example.com", "password123");

        String tampered = tokens.accessToken().substring(0, tokens.accessToken().length() - 1) + "X";

        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenMemberRole_thenCanAccessTicketsButNotProjects() throws Exception {
        registerUser("member@example.com", "password123", "Member User");
        LoginTokens tokens = login("member@example.com", "password123");

        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenLoginWithWrongPassword_thenReturns401() throws Exception {
        registerUser("wrongpass@example.com", "password123", "Wrong Pass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"wrongpass@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenLoginWithNonExistentEmail_thenReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenRegisterWithExistingEmail_thenReturns409() throws Exception {
        registerUser("dup@example.com", "password123", "Dup User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@example.com\",\"password\":\"password123\",\"fullName\":\"Dup\"}"))
                .andExpect(status().isConflict());
    }

    private void registerUser(String email, String password, String fullName) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"%s\"}",
                                email, password, fullName)))
                .andExpect(status().isCreated());
    }

    private LoginTokens login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s\",\"password\":\"%s\"}",
                                email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new LoginTokens(
                JsonPath.read(response, "$.accessToken"),
                JsonPath.read(response, "$.refreshToken")
        );
    }

    private LoginTokens refresh(String refreshToken) throws Exception {
        String response = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new LoginTokens(
                JsonPath.read(response, "$.accessToken"),
                JsonPath.read(response, "$.refreshToken")
        );
    }

    private void expectRefreshUnauthorized(String refreshToken) throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    private record LoginTokens(String accessToken, String refreshToken) {}
}