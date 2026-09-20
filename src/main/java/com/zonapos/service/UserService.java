package com.zonapos.service;

import com.zonapos.dto.CreateUserRequest;
import com.zonapos.dto.ResetPasswordRequest;
import com.zonapos.dto.UpdateUserRequest;
import com.zonapos.dto.UserDto;
import com.zonapos.entity.Role;
import com.zonapos.entity.User;
import com.zonapos.exception.BadRequestException;
import com.zonapos.exception.ResourceNotFoundException;
import com.zonapos.repository.OutletRepository;
import com.zonapos.repository.RoleRepository;
import com.zonapos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OutletRepository outletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public List<UserDto> getUsersByTenant(Long tenantId, Long outletId) {
        List<User> users;
        if (outletId != null) {
            users = userRepository.findByTenantIdAndOutletId(tenantId, outletId);
        } else {
            users = userRepository.findByTenantId(tenantId);
        }
        return users.stream().map(authService::mapToUserDto).collect(Collectors.toList());
    }

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan dengan ID: " + id));
        return authService.mapToUserDto(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request, User currentUser) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new BadRequestException("Email sudah digunakan");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role tidak ditemukan"));

        User user = User.builder()
                .tenantId(currentUser.getTenantId())
                .outletId(request.getOutletId() != null ? request.getOutletId() : currentUser.getOutletId())
                .name(request.getName())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .isActive(true)
                .role(role)
                .createdBy(currentUser.getName())
                .build();

        user = userRepository.save(user);
        return authService.mapToUserDto(user);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request, User currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan dengan ID: " + id));

        if (!user.getTenantId().equals(currentUser.getTenantId())) {
            throw new BadRequestException("Akses ditolak: User bukan bagian dari tenant Anda");
        }

        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getIsActive() != null) user.setIsActive(request.getIsActive());
        if (request.getOutletId() != null) user.setOutletId(request.getOutletId());

        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role tidak ditemukan"));
            user.setRole(role);
        }

        user.setUpdatedBy(currentUser.getName());
        user = userRepository.save(user);
        return authService.mapToUserDto(user);
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request, User currentUser) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Konfirmasi password baru tidak cocok");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan dengan ID: " + id));

        if (!user.getTenantId().equals(currentUser.getTenantId())) {
            throw new BadRequestException("Akses ditolak");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedBy(currentUser.getName());
        userRepository.save(user);
    }

    @Transactional
    public UserDto toggleStatus(Long id, User currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan dengan ID: " + id));

        if (!user.getTenantId().equals(currentUser.getTenantId())) {
            throw new BadRequestException("Akses ditolak");
        }

        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        user.setUpdatedBy(currentUser.getName());
        user = userRepository.save(user);
        return authService.mapToUserDto(user);
    }
}
