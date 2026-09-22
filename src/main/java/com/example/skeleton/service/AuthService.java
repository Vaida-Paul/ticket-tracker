package com.example.skeleton.service;

import org.openapitools.model.AuthResponse;
import org.openapitools.model.LoginRequest;
import org.openapitools.model.RegisterRequest;
import org.openapitools.model.UserDto;

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

}
