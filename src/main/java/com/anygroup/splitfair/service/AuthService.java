package com.anygroup.splitfair.service;

import com.anygroup.splitfair.dto.Auth.*;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse getAccount(String email);
    void changePassword(String email, ChangePasswordRequest request);
    AuthResponse loginWithGoogle(FirebaseTokenRequest request);
}
