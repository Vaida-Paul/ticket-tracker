package com.example.skeleton.unit.service;

import com.example.skeleton.domain.enums.AuthProvider;
import com.example.skeleton.domain.enums.Role;
import com.example.skeleton.domain.model.RefreshToken;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.exception.EmailAlreadyExistsException;
import com.example.skeleton.exception.InvalidCredentialsException;
import com.example.skeleton.mapper.UserMapper;
import com.example.skeleton.repository.RefreshTokenRepository;
import com.example.skeleton.repository.UserRepository;
import com.example.skeleton.service.implementation.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.AuthResponse;
import org.openapitools.model.LoginRequest;
import org.openapitools.model.RegisterRequest;
import org.openapitools.model.UserDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpirationMs", 900000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", 604800000L);

        user = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .fullName("Test User")
                .role(Role.MEMBER)
                .authProvider(AuthProvider.LOCAL)
                .build();
        user.setId(1L);

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setEmail("test@example.com");
        userDto.setFullName("Test User");
    }

    @Test
    void whenRegisterWithValidData_thenSavesUserWithMemberRoleAndLocalProvider() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setFullName("New User");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$hashed");
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
        assertThat(saved.getFullName()).isEqualTo("New User");
        assertThat(saved.getRole()).isEqualTo(Role.MEMBER);
        assertThat(saved.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);

        verify(userRepository).existsByEmail("new@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userMapper).toDto(user);
        verifyNoInteractions(refreshTokenRepository, jwtEncoder);
    }

    @Test
    void  whenRegisterWithExistingEmail_thenThrowsEmailAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setFullName("Existing User");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("existing@example.com");

        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, userMapper, refreshTokenRepository, jwtEncoder);
    }


    @Test
    void whenLoginWithValidCredentials_thenReturnsTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(mockJwt("access-token-value"));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token-value");
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getExpiresIn()).isEqualTo(900L);

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "$2a$10$hashedpassword");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void  whenLoginWithNonExistentEmail_thenThrowsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

        verify(userRepository).findByEmail("ghost@example.com");
        verifyNoInteractions(passwordEncoder, jwtEncoder, refreshTokenRepository);
    }

    @Test
    void whenLoginWithWrongPassword_thenThrowsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedpassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("wrongpassword", "$2a$10$hashedpassword");
        verifyNoInteractions(jwtEncoder, refreshTokenRepository);
    }

    @Test
    void whenLoginWithNullPasswordHash_thenThrowsInvalidCredentialsException() {
        user.setPasswordHash(null);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository).findByEmail("test@example.com");
        verifyNoInteractions(passwordEncoder, jwtEncoder, refreshTokenRepository);
    }

    private Jwt mockJwt(String tokenValue) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "HS256")
                .subject("test@example.com")
                .claim("userId", 1L)
                .claim("role", "MEMBER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
    }

}
