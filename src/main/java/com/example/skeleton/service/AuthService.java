package com.example.skeleton.service;

import org.openapitools.model.*;

public interface AuthService {

    /**
     * Registers a new local user account.
     *
     * @param request the registration data containing email, password, and full name
     * @return the created user DTO
     * @throws com.example.skeleton.exception.EmailAlreadyExistsException
     * if a user with the given email already exists
     */
    UserDto register(RegisterRequest request);

    /**
    * Authenticates a user using email and password.
    * Generates and returns a new access token and refresh token.
    *
    * @param request the login credentials
    * @return authentication response containing JWT tokens and expiration information
    * @throws com.example.skeleton.exception.InvalidCredentialsException
    * if the email or password is invalid
     */
    AuthResponse login(LoginRequest request);

    /**
     * Exchanges a valid refresh token for a new access token and refresh token.
     * The previous refresh token is rotated and can no longer be used.
     *
     * @param request the refresh token request containing the current refresh token
     * @return authentication response containing the new JWT tokens and expiration information
     * @throws com.example.skeleton.exception.InvalidRequestException
     * if the provided refresh token is invalid, expired, or revoked
     */
    AuthResponse refresh(RefreshRequest request);

    /**
     * Logs out the current session by revoking the provided refresh token.
     * After logout, the refresh token can no longer be used to obtain new access tokens.
     *
     * @param request the logout request containing the refresh token to revoke
     * @throws com.example.skeleton.exception.InvalidRequestException
     * if the provided refresh token is invalid or cannot be revoked
     */
    void logout(LogoutRequest request);

    /**
     * Logs out the currently authenticated user from all active sessions.
     * Revokes all refresh tokens associated with the user account.
     *
     * @throws com.example.skeleton.exception.InvalidRequestException
     * if no authenticated user is found or the user does not exist
     */
    void logoutAll();
}
