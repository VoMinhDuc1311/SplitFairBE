package com.anygroup.splitfair.service;

import com.anygroup.splitfair.dto.UserBankInfoRequest;
import com.anygroup.splitfair.dto.UserDTO;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    List<UserDTO> getAllUsers();
    UserDTO getUserById(UUID id);
    UserDTO createUser(UserDTO dto);
    UserDTO updateUser(UserDTO dto);
    void deleteUser(UUID id);

    // Tìm kiếm user theo tên hoặc email (không phân biệt hoa thường)
    List<UserDTO> searchUsers(String query);

    UserDTO uploadAvatar(UUID userId, MultipartFile file) throws IOException;
    void updateMyBankInfo(String email, UserBankInfoRequest request);
}