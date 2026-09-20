package com.zonapos.service;

import com.zonapos.config.JwtTokenProvider;
import com.zonapos.dto.AuthResponse;
import com.zonapos.dto.LoginRequest;
import com.zonapos.dto.RegisterTenantRequest;
import com.zonapos.dto.UserDto;
import com.zonapos.entity.Outlet;
import com.zonapos.entity.Role;
import com.zonapos.entity.Tenant;
import com.zonapos.entity.User;
import com.zonapos.exception.BadRequestException;
import com.zonapos.exception.ResourceNotFoundException;
import com.zonapos.repository.OutletRepository;
import com.zonapos.repository.RoleRepository;
import com.zonapos.repository.TenantRepository;
import com.zonapos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final OutletRepository outletRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Email atau password tidak sesuai"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Email atau password tidak sesuai");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new BadRequestException("Akun Anda sedang dinonaktifkan. Silakan hubungi admin.");
        }

        String token = tokenProvider.generateToken(user);
        long expiresIn = tokenProvider.getExpirationDuration(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(mapToUserDto(user))
                .build();
    }

    @Transactional
    public AuthResponse registerTenant(RegisterTenantRequest request) {
        if (userRepository.existsByEmail(request.getOwnerEmail().trim().toLowerCase())) {
            throw new BadRequestException("Email owner sudah terdaftar di sistem");
        }

        // 1. Create Tenant
        Tenant tenant = Tenant.builder()
                .name(request.getTenantName())
                .address(request.getAddress())
                .ownerName(request.getOwnerName())
                .ownerPhone(request.getOwnerPhone())
                .ownerEmail(request.getOwnerEmail().trim().toLowerCase())
                .status("ACTIVE")
                .createdBy("SELF_REGISTER")
                .build();
        tenant = tenantRepository.save(tenant);

        // 2. Fetch or create OWNER role
        Role ownerRole = roleRepository.findByName("ROLE_TENANT_OWNER")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_TENANT_OWNER")));

        // 3. Create Default Outlet for Tenant
        Outlet defaultOutlet = Outlet.builder()
                .tenantId(tenant.getId())
                .name("Outlet Utama - " + tenant.getName())
                .address(tenant.getAddress())
                .managerName(tenant.getOwnerName())
                .managerPhone(tenant.getOwnerPhone())
                .managerEmail(tenant.getOwnerEmail())
                .createdBy("SYSTEM")
                .build();
        defaultOutlet = outletRepository.save(defaultOutlet);

        // 4. Create Owner User
        User ownerUser = User.builder()
                .tenantId(tenant.getId())
                .outletId(defaultOutlet.getId())
                .name(request.getOwnerName())
                .email(request.getOwnerEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getOwnerPhone())
                .isActive(true)
                .role(ownerRole)
                .createdBy("SELF_REGISTER")
                .build();
        ownerUser = userRepository.save(ownerUser);

        String token = tokenProvider.generateToken(ownerUser);
        long expiresIn = tokenProvider.getExpirationDuration(ownerUser);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(mapToUserDto(ownerUser))
                .build();
    }

    public UserDto mapToUserDto(User user) {
        String outletName = null;
        if (user.getOutletId() != null) {
            outletName = outletRepository.findById(user.getOutletId())
                    .map(Outlet::getName).orElse(null);
        }

        return UserDto.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .outletId(user.getOutletId())
                .outletName(outletName)
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .isActive(user.getIsActive())
                .role(user.getRole() != null ? user.getRole().getName() : "ROLE_USER")
                .roleId(user.getRole() != null ? user.getRole().getId() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
