package com.anygroup.splitfair.service.impl;

import com.anygroup.splitfair.dto.UserBankInfoRequest;
import com.anygroup.splitfair.dto.UserDTO;
import com.anygroup.splitfair.enums.RoleType;
import com.anygroup.splitfair.enums.UserStatus;
import com.anygroup.splitfair.mapper.UserMapper;
import com.anygroup.splitfair.model.Role;
import com.anygroup.splitfair.model.User;
import com.anygroup.splitfair.repository.RoleRepository;
import com.anygroup.splitfair.repository.UserRepository;
import com.anygroup.splitfair.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    @Value("${app.file.avatar-storage:data/avatars}")
    private String avatarStorageLocation;

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO getUserById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toDTO)
                .orElse(null);
    }

    @Override
    public UserDTO createUser(UserDTO dto) {
        User user = userMapper.toEntity(dto);

        // Nếu không chỉ định role → mặc định là USER
        Role role = roleRepository.findByName(RoleType.USER)
                .orElseThrow(() -> new RuntimeException("Default role USER not found"));
        user.setRole(role);

        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        return userMapper.toDTO(user);
    }
    @Override
    public UserDTO updateUser(UserDTO dto) {
        User existingUser = userRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Cập nhật thông tin cơ bản
        existingUser.setUserName(dto.getName());
        existingUser.setEmail(dto.getEmail());
        existingUser.setStatus(dto.getStatus());
        
        // Cập nhật avatar nếu có
        if (dto.getAvatar() != null) {
            existingUser.setAvatar(dto.getAvatar());
        }

        // Cập nhật role nếu có
        if (dto.getRoleId() != null) {
            Role role = roleRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            existingUser.setRole(role);
        }

        // Lưu lại
        existingUser = userRepository.save(existingUser);

        return userMapper.toDTO(existingUser);
    }

    @Override
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public List<UserDTO> searchUsers(String query) {
        return userRepository.findByUserNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query)
                .stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO uploadAvatar(UUID userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Tạo thư mục nếu chưa có
        Path dirPath = Paths.get(System.getProperty("user.dir"), avatarStorageLocation);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        // 2. Tạo tên file mới (tránh trùng lặp)
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = dirPath.resolve(fileName);

        // 3. Lưu file vào server
        file.transferTo(filePath.toFile());

        // 4. Tạo URL để FE gọi
        // (Chúng ta sẽ tạo endpoint GET /api/users/avatar/{fileName} ở Controller)
        String fileUrl = "/api/users/avatar/" + fileName;

        // 5. Cập nhật trường 'avatar' của User
        user.setAvatar(fileUrl);
        user = userRepository.save(user);

        return userMapper.toDTO(user);
    }

    @Override
    public void updateMyBankInfo(String email, UserBankInfoRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setBankCode(request.getBankCode());
        user.setBankAccountNo(request.getBankAccountNo());
        user.setBankAccountName(request.getBankAccountName());

        userRepository.save(user);
    }

}
