package com.example.skeleton.integration.controller;

import com.example.skeleton.config.SecurityProperties;
import com.example.skeleton.controller.AuthController;
import com.example.skeleton.exception.EmailAlreadyExistsException;
import com.example.skeleton.exception.InvalidCredentialsException;
import com.example.skeleton.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.openapitools.model.AuthResponse;
import org.openapitools.model.LoginRequest;
import org.openapitools.model.RegisterRequest;
import org.openapitools.model.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private SecurityProperties securityProperties;

    @Test
    void whenRegisterWithValidData_thenReturns201AndUser() throws Exception {
        String requestBody = """
                {
                    "email": "new@example.com",
                    "password": "password123",
                    "fullName": "New User"
                }
                """;

        UserDto response = new UserDto();
        response.setId(1L);
        response.setEmail("new@example.com");
        response.setFullName("New User");
        response.setRole(org.openapitools.model.Role.MEMBER);

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        when(authService.register(captor.capture())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.fullName").value("New User"))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        verify(authService).register(captor.capture());

        RegisterRequest captured = captor.getValue();
        assertThat(captured.getEmail()).isEqualTo("new@example.com");
        assertThat(captured.getPassword()).isEqualTo("password123");
        assertThat(captured.getFullName()).isEqualTo("New User");

        verifyNoMoreInteractions(authService);
    }

    @Test
    void whenRegisterWithExistingEmail_thenReturns409() throws Exception {
        String requestBody = """
                {
                    "email": "existing@example.com",
                    "password": "password123",
                    "fullName": "Existing User"
                }
                """;

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        when(authService.register(captor.capture()))
                .thenThrow(new EmailAlreadyExistsException("existing@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already registered: existing@example.com"));

        verify(authService).register(captor.capture());
        verifyNoMoreInteractions(authService);
    }

    @Test
    void whenRegisterWithInvalidEmail_thenReturns400() throws Exception {
        String requestBody = """
                {
                    "email": "not-an-email",
                    "password": "password123",
                    "fullName": "Test User"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("email")));

        verifyNoInteractions(authService);
    }

    @Test
    void whenRegisterWithShortPassword_thenReturns400() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "short",
                    "fullName": "Test User"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("password")));

        verifyNoInteractions(authService);
    }

    @Test
    void whenRegisterWithBlankFullName_thenReturns400() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "password123",
                    "fullName": ""
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("fullName")));

        verifyNoInteractions(authService);
    }

    @Test
    void whenLoginWithValidCredentials_thenReturns200AndTokens() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "password123"
                }
                """;

        AuthResponse response = new AuthResponse(
                "access-token-value",
                "refresh-token-value",
                900L
        );

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        when(authService.login(captor.capture())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-value"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-value"))
                .andExpect(jsonPath("$.expiresIn").value(900));

        verify(authService).login(captor.capture());

        LoginRequest captured = captor.getValue();
        assertThat(captured.getEmail()).isEqualTo("test@example.com");
        assertThat(captured.getPassword()).isEqualTo("password123");

        verifyNoMoreInteractions(authService);
    }

    @Test
    void whenLoginWithInvalidCredentials_thenReturns401() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "wrongpassword"
                }
                """;

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        when(authService.login(captor.capture()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(authService).login(captor.capture());
        verifyNoMoreInteractions(authService);
    }

    @Test
    void whenLoginWithMissingPassword_thenReturns400() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("password")));

        verifyNoInteractions(authService);
    }

    @Test
    void whenLoginWithMalformedJson_thenReturns400() throws Exception {
        String malformedJson = "{ \"email\": \"test@example.com\", \"password\": }";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Malformed Request"));

        verifyNoInteractions(authService);
    }
}