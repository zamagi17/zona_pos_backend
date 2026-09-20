package com.zonapos.service;

import com.zonapos.dto.OutletDto;
import com.zonapos.entity.Outlet;
import com.zonapos.entity.User;
import com.zonapos.exception.BadRequestException;
import com.zonapos.exception.ResourceNotFoundException;
import com.zonapos.repository.OutletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OutletService {

    private final OutletRepository outletRepository;

    public List<OutletDto> getOutletsByTenant(Long tenantId) {
        return outletRepository.findByTenantId(tenantId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public OutletDto getOutletById(Long id) {
        Outlet outlet = outletRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Outlet tidak ditemukan dengan ID: " + id));
        return mapToDto(outlet);
    }

    @Transactional
    public OutletDto createOutlet(OutletDto dto, User currentUser) {
        Outlet outlet = Outlet.builder()
                .tenantId(currentUser.getTenantId())
                .name(dto.getName())
                .address(dto.getAddress())
                .managerName(dto.getManagerName())
                .managerPhone(dto.getManagerPhone())
                .managerEmail(dto.getManagerEmail())
                .createdBy(currentUser.getName())
                .build();
        outlet = outletRepository.save(outlet);
        return mapToDto(outlet);
    }

    @Transactional
    public OutletDto updateOutlet(Long id, OutletDto dto, User currentUser) {
        Outlet outlet = outletRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Outlet tidak ditemukan dengan ID: " + id));

        if (!outlet.getTenantId().equals(currentUser.getTenantId())) {
            throw new BadRequestException("Akses ditolak");
        }

        outlet.setName(dto.getName());
        outlet.setAddress(dto.getAddress());
        outlet.setManagerName(dto.getManagerName());
        outlet.setManagerPhone(dto.getManagerPhone());
        outlet.setManagerEmail(dto.getManagerEmail());
        outlet.setUpdatedBy(currentUser.getName());

        outlet = outletRepository.save(outlet);
        return mapToDto(outlet);
    }

    private OutletDto mapToDto(Outlet o) {
        return OutletDto.builder()
                .id(o.getId())
                .tenantId(o.getTenantId())
                .name(o.getName())
                .address(o.getAddress())
                .managerName(o.getManagerName())
                .managerPhone(o.getManagerPhone())
                .managerEmail(o.getManagerEmail())
                .build();
    }
}
