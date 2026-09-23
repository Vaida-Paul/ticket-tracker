package com.example.skeleton.service.implementation;

import com.example.skeleton.domain.enums.AuthProvider;
import com.example.skeleton.domain.enums.Role;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.exception.EmailAlreadyExistsException;
import com.example.skeleton.exception.InvalidCredentialsException;
import com.example.skeleton.exception.InvalidRequestException;
import com.example.skeleton.mapper.UserMapper;
import com.example.skeleton.repository.UserRepository;
import com.example.skeleton.security.JwtService;
import com.example.skeleton.security.RefreshTokenService;
import com.example.skeleton.security.SecurityUtils;
import com.example.skeleton.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto register(RegisterRequest request) {
        log.info("Registering user: email={}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.MEMBER)
                .authProvider(AuthProvider.LOCAL)
                .build();

        User saved = userRepository.save(user);
        log.info("Registered user id={}", saved.getId());
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt: email={}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);

        log.info("Login successful for user id={}", user.getId());
        return new AuthResponse(accessToken, refreshToken, jwtService.getAccessTokenExpirationSeconds());
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        log.info("Refresh token exchange requested");

        var result = refreshTokenService.rotate(request.getRefreshToken());
        User user = result.user();

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = result.newRawToken();

        log.info("Refresh successful for user id={}", user.getId());
        return new AuthResponse(newAccessToken, newRefreshToken, jwtService.getAccessTokenExpirationSeconds());
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        log.info("Logout requested");
        refreshTokenService.revoke(request.getRefreshToken());
    }

    @Override
    @Transactional
    public void logoutAll() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new InvalidRequestException("Not authenticated");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidRequestException("User not found"));

        refreshTokenService.revokeAllForUser(user);
        log.info("Logout-all for user id={}", userId);
    }
}