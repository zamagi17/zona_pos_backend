package com.zonapos;

import com.zonapos.dto.PurchaseOrderDto;
import com.zonapos.dto.SupplierDto;
import com.zonapos.entity.*;
import com.zonapos.repository.*;
import com.zonapos.service.SupplierService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private StorageRepository storageRepository;

    @Mock
    private OutletRepository outletRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SupplierService supplierService;

    private Long tenantId = 1L;

    @Test
    void testGetAllPurchaseOrders_Success() {
        PurchaseOrder po = PurchaseOrder.builder()
                .id(1L)
                .poNo("PO-20261002-0001")
                .tenantId(tenantId)
                .supplierId(10L)
                .storageId(20L)
                .productId(30L)
                .quantity(100L)
                .purchasePrice(15000.0)
                .totalCost(1500000.0)
                .invoiceNo("INV-VENDOR-99")
                .remarks("Bahan baku kopi")
                .userId(5L)
                .createdAt(LocalDateTime.now())
                .build();

        when(purchaseOrderRepository.findPurchaseOrdersFiltered(
                eq(tenantId), any(), any(), any(), any(), any()
        )).thenReturn(List.of(po));

        when(supplierRepository.findAllById(any())).thenReturn(List.of(
                Supplier.builder().id(10L).name("PT Sumber Pangan Nusantara").build()
        ));
        when(productRepository.findAllById(any())).thenReturn(List.of(
                Product.builder().id(30L).name("Biji Kopi Arabika 1kg").build()
        ));
        when(productVariantRepository.findAllById(any())).thenReturn(List.of());
        when(storageRepository.findAllById(any())).thenReturn(List.of(
                Storage.builder().id(20L).name("Gudang Utama").build()
        ));
        when(outletRepository.findAllById(any())).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of(
                User.builder().id(5L).name("Kasir Budi").build()
        ));

        List<PurchaseOrderDto> results = supplierService.getAllPurchaseOrders(
                tenantId, null, null, null, null, null, null
        );

        assertNotNull(results);
        assertEquals(1, results.size());
        PurchaseOrderDto dto = results.get(0);
        assertEquals("PO-20261002-0001", dto.getPoNo());
        assertEquals("PT Sumber Pangan Nusantara", dto.getSupplierName());
        assertEquals("Biji Kopi Arabika 1kg", dto.getProductName());
        assertEquals("Gudang Utama", dto.getStorageName());
        assertEquals("Kasir Budi", dto.getUserName());
        assertEquals(100L, dto.getQuantity());
        assertEquals(1500000.0, dto.getTotalCost());
    }

    @Test
    void testGetAllPurchaseOrders_WithSearchFilter() {
        PurchaseOrder po1 = PurchaseOrder.builder()
                .id(1L)
                .poNo("PO-20261002-0001")
                .tenantId(tenantId)
                .supplierId(10L)
                .productId(30L)
                .quantity(50L)
                .purchasePrice(10000.0)
                .totalCost(500000.0)
                .invoiceNo("INV-ABC")
                .remarks("Pesanan Tepung")
                .build();

        PurchaseOrder po2 = PurchaseOrder.builder()
                .id(2L)
                .poNo("PO-20261002-0002")
                .tenantId(tenantId)
                .supplierId(10L)
                .productId(31L)
                .quantity(20L)
                .purchasePrice(25000.0)
                .totalCost(500000.0)
                .invoiceNo("INV-XYZ")
                .remarks("Pesanan Minyak Goreng")
                .build();

        when(purchaseOrderRepository.findPurchaseOrdersFiltered(
                eq(tenantId), any(), any(), any(), any(), any()
        )).thenReturn(List.of(po1, po2));

        when(supplierRepository.findAllById(any())).thenReturn(List.of(
                Supplier.builder().id(10L).name("PT Sinar Jaya").build()
        ));
        when(productRepository.findAllById(any())).thenReturn(List.of(
                Product.builder().id(30L).name("Tepung Terigu Segitiga").build(),
                Product.builder().id(31L).name("Minyak Goreng 2L").build()
        ));
        when(productVariantRepository.findAllById(any())).thenReturn(List.of());
        when(storageRepository.findAllById(any())).thenReturn(List.of());
        when(outletRepository.findAllById(any())).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of());

        // Search "Tepung"
        List<PurchaseOrderDto> results = supplierService.getAllPurchaseOrders(
                tenantId, null, null, null, null, null, "Tepung"
        );

        assertEquals(1, results.size());
        assertEquals("PO-20261002-0001", results.get(0).getPoNo());
        assertEquals("Tepung Terigu Segitiga", results.get(0).getProductName());
    }

    @Test
    void testToggleSupplierStatus() {
        Supplier sup = Supplier.builder()
                .id(1L)
                .tenantId(tenantId)
                .name("PT Mitra Sejati")
                .isActive(true)
                .build();

        when(supplierRepository.findByIdAndTenantId(1L, tenantId)).thenReturn(Optional.of(sup));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierDto toggled = supplierService.toggleStatus(1L, tenantId);
        assertFalse(toggled.getIsActive());
    }
}
