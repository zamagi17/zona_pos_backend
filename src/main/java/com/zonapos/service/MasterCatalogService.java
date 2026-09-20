package com.zonapos.service;

import com.zonapos.dto.CategoryDto;
import com.zonapos.dto.StorageDto;
import com.zonapos.dto.UnitDto;
import com.zonapos.entity.Category;
import com.zonapos.entity.Storage;
import com.zonapos.entity.Unit;
import com.zonapos.entity.User;
import com.zonapos.exception.BadRequestException;
import com.zonapos.exception.ResourceNotFoundException;
import com.zonapos.repository.CategoryRepository;
import com.zonapos.repository.StorageRepository;
import com.zonapos.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MasterCatalogService {

    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final StorageRepository storageRepository;

    // --- CATEGORY ---
    public List<CategoryDto> getCategories(Long tenantId) {
        return categoryRepository.findByTenantId(tenantId)
                .stream().map(c -> CategoryDto.builder()
                        .id(c.getId())
                        .tenantId(c.getTenantId())
                        .code(c.getCode())
                        .name(c.getName())
                        .parentId(c.getParentId())
                        .isParent(c.getIsParent())
                        .desc(c.getDesc())
                        .build()).collect(Collectors.toList());
    }

    @Transactional
    public CategoryDto createCategory(CategoryDto dto, User currentUser) {
        if (categoryRepository.findByTenantIdAndCode(currentUser.getTenantId(), dto.getCode()).isPresent()) {
            throw new BadRequestException("Kode kategori sudah digunakan");
        }

        Category category = Category.builder()
                .tenantId(currentUser.getTenantId())
                .code(dto.getCode())
                .name(dto.getName())
                .parentId(dto.getParentId())
                .isParent(Boolean.TRUE.equals(dto.getIsParent()))
                .desc(dto.getDesc())
                .createdBy(currentUser.getName())
                .build();
        category = categoryRepository.save(category);
        dto.setId(category.getId());
        dto.setTenantId(category.getTenantId());
        return dto;
    }

    // --- UNIT ---
    public List<UnitDto> getUnits(Long tenantId) {
        return unitRepository.findByTenantId(tenantId)
                .stream().map(u -> UnitDto.builder()
                        .id(u.getId())
                        .tenantId(u.getTenantId())
                        .code(u.getCode())
                        .name(u.getName())
                        .size(u.getSize())
                        .build()).collect(Collectors.toList());
    }

    @Transactional
    public UnitDto createUnit(UnitDto dto, User currentUser) {
        if (unitRepository.findByTenantIdAndCode(currentUser.getTenantId(), dto.getCode()).isPresent()) {
            throw new BadRequestException("Kode unit sudah digunakan");
        }

        Unit unit = Unit.builder()
                .tenantId(currentUser.getTenantId())
                .code(dto.getCode())
                .name(dto.getName())
                .size(dto.getSize() != null ? dto.getSize() : 1)
                .createdBy(currentUser.getName())
                .build();
        unit = unitRepository.save(unit);
        dto.setId(unit.getId());
        dto.setTenantId(unit.getTenantId());
        return dto;
    }

    // --- STORAGE ---
    public List<StorageDto> getStorages(Long tenantId) {
        return storageRepository.findByTenantId(tenantId)
                .stream().map(s -> StorageDto.builder()
                        .id(s.getId())
                        .tenantId(s.getTenantId())
                        .code(s.getCode())
                        .name(s.getName())
                        .build()).collect(Collectors.toList());
    }

    @Transactional
    public StorageDto createStorage(StorageDto dto, User currentUser) {
        if (storageRepository.findByTenantIdAndCode(currentUser.getTenantId(), dto.getCode()).isPresent()) {
            throw new BadRequestException("Kode gudang sudah digunakan");
        }

        Storage storage = Storage.builder()
                .tenantId(currentUser.getTenantId())
                .code(dto.getCode())
                .name(dto.getName())
                .createdBy(currentUser.getName())
                .build();
        storage = storageRepository.save(storage);
        dto.setId(storage.getId());
        dto.setTenantId(storage.getTenantId());
        return dto;
    }
}
