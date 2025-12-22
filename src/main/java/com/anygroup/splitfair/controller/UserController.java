package com.anygroup.splitfair.controller;

import com.anygroup.splitfair.dto.Auth.FirebaseTokenRequest;
import com.anygroup.splitfair.dto.UserDTO;
import com.anygroup.splitfair.enums.UserStatus;
import com.anygroup.splitfair.mapper.UserMapper;
import com.anygroup.splitfair.repository.UserRepository;
import com.anygroup.splitfair.service.UserService;
import com.anygroup.splitfair.util.FirebaseTokenUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @Value("${app.file.avatar-storage:data/avatars}")
    private String avatarStorageLocation;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }


    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
        UserDTO user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }


    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO dto) {
        UserDTO created = userService.createUser(dto);
        return ResponseEntity.ok(created);
    }


    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable UUID id, @RequestBody UserDTO dto) {
        dto.setId(id);
        UserDTO updated = userService.updateUser(dto);
        return ResponseEntity.ok(updated);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }


    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDTO> updateUserStatus(
            @PathVariable UUID id,
            @RequestParam("status") String status
    ) {
        UserDTO user = userService.getUserById(id);
        if (user == null) return ResponseEntity.notFound().build();

        user.setStatus(UserStatus.valueOf(status.toUpperCase()));
        UserDTO updated = userService.updateUser(user);
        return ResponseEntity.ok(updated);
    }

    // Tìm kiếm user theo tên hoặc email (không phân biệt hoa thường)
    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam("query") String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }


    @PostMapping("/{id}/avatar")
    public ResponseEntity<UserDTO> uploadAvatar(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) throws IOException {
        UserDTO updatedUser = userService.uploadAvatar(id, file);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/avatar/{fileName:.+}")
    public ResponseEntity<Resource> serveAvatar(@PathVariable String fileName) throws IOException {
        Path dirPath = Paths.get(System.getProperty("user.dir"), avatarStorageLocation);
        Path filePath = dirPath.resolve(fileName);
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() || resource.isReadable()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG) // Hoặc PNG tùy bạn
                    .body(resource);
        } else {
            throw new RuntimeException("Could not read the file!");
        }
    }
}
