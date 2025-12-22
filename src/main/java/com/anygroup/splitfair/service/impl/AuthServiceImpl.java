package com.anygroup.splitfair.service.impl;

import com.anygroup.splitfair.dto.Auth.AuthResponse;
import com.anygroup.splitfair.dto.Auth.ChangePasswordRequest;
import com.anygroup.splitfair.dto.Auth.LoginRequest;
import com.anygroup.splitfair.dto.Auth.RegisterRequest;
import com.anygroup.splitfair.enums.RoleType;
import com.anygroup.splitfair.model.Role;
import com.anygroup.splitfair.model.User;
import com.anygroup.splitfair.repository.RoleRepository;
import com.anygroup.splitfair.repository.UserRepository;
import com.anygroup.splitfair.service.AuthService;
import com.anygroup.splitfair.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.anygroup.splitfair.dto.Auth.FirebaseTokenRequest;
import com.anygroup.splitfair.util.FirebaseTokenUtil;
import com.anygroup.splitfair.enums.UserStatus;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        Role role = roleRepository.findByName(RoleType.USER)
                .orElseThrow(() -> new RuntimeException("Role USER not found"));

        User user = User.builder()
                .userName(request.getUserName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());

        AuthResponse res = new AuthResponse();
        res.setToken(token);
        res.setUserName(user.getUserName()); // 👈 getUserName()
        res.setRole(user.getRole().getName().name());
        res.setUserId(user.getId()); // 👈 set userId in AuthResponse
        res.setEmail(user.getEmail()); // 👈 set email in AuthResponse
        res.setAvatar(user.getAvatar());
        return res;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Tìm user theo email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Kiểm tra password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Sinh token JWT
        String token = jwtUtil.generateToken(user.getEmail());

        // Trả response
        AuthResponse res = new AuthResponse();
        res.setToken(token);
        res.setUserName(user.getUserName());
        res.setRole(user.getRole().getName().name());
        res.setUserId(user.getId()); // 👈 set userId in AuthResponse
        res.setEmail(user.getEmail()); // 👈 set email in AuthResponse
        res.setAvatar(user.getAvatar());
        return res;
    }

    @Override
    public AuthResponse getAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        AuthResponse res = new AuthResponse();
        res.setUserName(user.getUserName());
        res.setRole(user.getRole().getName().name());
        res.setUserId(user.getId());
        res.setEmail(user.getEmail());
        res.setAvatar(user.getAvatar());
        // Không set token, vì đây là hàm lấy thông tin, không phải tạo token mới
        return res;
    }

    @Override
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
    @Override
    public AuthResponse loginWithGoogle(FirebaseTokenRequest request) {
        try {
            var decoded = FirebaseTokenUtil.verify(request.getToken());
            String email = decoded.getEmail();

            // Tìm hoặc tạo mới User
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                Role role = roleRepository.findByName(RoleType.USER)
                        .orElseThrow(() -> new RuntimeException("Default role USER not found"));

                // Sử dụng mật khẩu ngẫu nhiên để thỏa mãn cấu trúc DB và Security
                String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString() + "_GOOGLE");

                User newUser = User.builder()
                        .userName(decoded.getName() != null ? decoded.getName() : email.split("@")[0])
                        .email(email)
                        .password(randomPassword)
                        .avatar(decoded.getPicture())
                        .role(role)
                        .status(UserStatus.ACTIVE)
                        .build();

                newUser = userRepository.save(newUser);



                return newUser;
            });

            // Cập nhật lại Avatar từ Google nếu có thay đổi
            if (decoded.getPicture() != null && !decoded.getPicture().equals(user.getAvatar())) {
                user.setAvatar(decoded.getPicture());
                userRepository.save(user);
            }

            // Tạo Token hệ thống
            String systemToken = jwtUtil.generateToken(user.getEmail());

            // Trả về AuthResponse (Đảm bảo đầy đủ các trường FE cần)
            AuthResponse res = new AuthResponse();
            res.setToken(systemToken);
            res.setUserId(user.getId());
            res.setUserName(user.getUserName());
            res.setEmail(user.getEmail());
            res.setRole(user.getRole().getName().name());
            res.setAvatar(user.getAvatar());

            return res;
        } catch (Exception e) {
            throw new RuntimeException("Xác thực Google thất bại: " + e.getMessage());
        }
    }
}
