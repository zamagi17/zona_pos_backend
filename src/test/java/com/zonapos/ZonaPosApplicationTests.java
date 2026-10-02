package com.zonapos;

import com.zonapos.dto.*;
import com.zonapos.entity.User;
import com.zonapos.repository.StockRepository;
import com.zonapos.repository.UserRepository;
import com.zonapos.service.AuthService;
import com.zonapos.service.ShiftService;
import com.zonapos.service.StockService;
import com.zonapos.service.TransactionService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ZonaPosApplicationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private StockService stockService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private com.zonapos.service.PromotionService promotionService;

    private static Long activeShiftId;
    private static Long completedTrxId;

    @Test
    @Order(1)
    void contextLoads() {
        assertNotNull(authService);
    }

    @Test
    @Order(2)
    void testDemoOwnerLogin() {
        LoginRequest request = new LoginRequest();
        request.setEmail("owner@zonapos.com");
        request.setPassword("password123");

        AuthResponse response = authService.login(request);
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("owner@zonapos.com", response.getUser().getEmail());
        assertEquals("ROLE_TENANT_OWNER", response.getUser().getRole());
    }

    @Test
    @Order(3)
    void testDemoCashierLogin() {
        LoginRequest request = new LoginRequest();
        request.setEmail("kasir@zonapos.com");
        request.setPassword("password123");

        AuthResponse response = authService.login(request);
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("kasir@zonapos.com", response.getUser().getEmail());
        assertEquals("ROLE_CASHIER", response.getUser().getRole());
        assertEquals(86400000L, response.getExpiresIn()); // 24 hours for cashier
    }

    @Test
    @Order(4)
    void testOpenShift() {
        User cashier = userRepository.findByEmail("kasir@zonapos.com").orElseThrow();
        OpenShiftRequest request = new OpenShiftRequest();
        request.setOutletId(1L);
        request.setStartCash(100000.0);

        ShiftResponse shift = shiftService.openShift(request, cashier);
        assertNotNull(shift);
        assertEquals("OPEN", shift.getStatus());
        assertEquals(100000.0, shift.getStartCash());
        activeShiftId = shift.getId();
    }

    @Test
    @Order(5)
    void testCheckoutWithStockDeduction() {
        User cashier = userRepository.findByEmail("kasir@zonapos.com").orElseThrow();

        // Check initial stock for Product 1 (Kopi Susu) at Outlet 1
        var stockBefore = stockRepository.findByProductIdAndVariantIdIsNullAndOutletId(1L, 1L).orElseThrow();
        long initialQty = stockBefore.getQuantity();

        CheckoutRequest request = new CheckoutRequest();
        request.setOutletId(1L);
        request.setShiftId(activeShiftId);
        request.setPaymentMethod("CASH");
        request.setAmountPaid(50000.0);

        CartItemDto item = CartItemDto.builder()
                .productId(1L)
                .productName("Kopi Susu Gula Aren")
                .quantity(2)
                .unitPrice(18000.0)
                .basePrice(8000.0)
                .discount(0.0)
                .tax(0.0)
                .subtotal(36000.0)
                .build();
        request.setItems(List.of(item));

        TransactionResponse trx = transactionService.checkout(request, cashier);
        assertNotNull(trx);
        assertEquals("COMPLETED", trx.getStatus());
        assertEquals(36000.0, trx.getGrandTotal());
        assertEquals(14000.0, trx.getChangeAmount()); // 50000 - 36000 = 14000
        completedTrxId = trx.getId();

        // Verify stock is deducted by 2
        var stockAfter = stockRepository.findByProductIdAndVariantIdIsNullAndOutletId(1L, 1L).orElseThrow();
        assertEquals(initialQty - 2, stockAfter.getQuantity());
    }

    @Test
    @Order(6)
    void testRefundWithStockRestoration() {
        User manager = userRepository.findByEmail("manager@zonapos.com").orElseThrow();

        var stockBefore = stockRepository.findByProductIdAndVariantIdIsNullAndOutletId(1L, 1L).orElseThrow();
        long qtyBeforeRefund = stockBefore.getQuantity();

        RefundRequest request = new RefundRequest();
        request.setReason("Salah input pesanan pelanggan");

        TransactionResponse refunded = transactionService.refund(completedTrxId, request, manager);
        assertNotNull(refunded);
        assertEquals("REFUNDED", refunded.getStatus());

        // Verify stock is restored by 2
        var stockAfter = stockRepository.findByProductIdAndVariantIdIsNullAndOutletId(1L, 1L).orElseThrow();
        assertEquals(qtyBeforeRefund + 2, stockAfter.getQuantity());
    }

    @Test
    @Order(7)
    void testCloseShift() {
        User cashier = userRepository.findByEmail("kasir@zonapos.com").orElseThrow();

        CloseShiftRequest request = new CloseShiftRequest();
        request.setActualCash(100000.0); // expected is 100000 since the sale was refunded

        ShiftResponse shift = shiftService.closeShift(activeShiftId, request, cashier);
        assertNotNull(shift);
        assertEquals("CLOSED", shift.getStatus());
        assertNotNull(shift.getClosedAt());
    }

    @Test
    @Order(8)
    void testVoucherValidationAndPromotionService() {
        // Test HEMAT10 with subtotal 100,000 -> 10% = 10,000
        var validRes = promotionService.validateVoucher(1L, 1L, "HEMAT10", 100000.0);
        assertTrue(validRes.isValid());
        assertEquals("HEMAT10", validRes.getCode());
        assertEquals(10000.0, validRes.getDiscountAmount());

        // Test GRANDOPENING (min 100,000) with subtotal 40,000 -> should fail validation
        var invalidRes = promotionService.validateVoucher(1L, 1L, "GRANDOPENING", 40000.0);
        assertFalse(invalidRes.isValid());
        assertTrue(invalidRes.getMessage().contains("Minimal belanja"));
    }

    @Test
    @Order(9)
    void testOrderDiscountAndVoucherCheckout() {
        User cashier = userRepository.findByEmail("kasir@zonapos.com").orElseThrow();

        // Open a new shift for testing checkout with discounts
        OpenShiftRequest openReq = new OpenShiftRequest();
        openReq.setOutletId(1L);
        openReq.setStartCash(50000.0);
        ShiftResponse shift = shiftService.openShift(openReq, cashier);
        assertNotNull(shift);

        CheckoutRequest request = new CheckoutRequest();
        request.setOutletId(1L);
        request.setShiftId(shift.getId());
        request.setPaymentMethod("CASH");

        CartItemDto item = CartItemDto.builder()
                .productId(1L)
                .productName("Kopi Susu Gula Aren")
                .quantity(5)
                .unitPrice(20000.0) // subtotal = 100,000
                .basePrice(8000.0)
                .discount(0.0)
                .tax(0.0)
                .subtotal(100000.0)
                .build();
        request.setItems(List.of(item));

        // Apply Order Discount: 10% (= 10,000)
        request.setOrderDiscountType("PERCENT");
        request.setOrderDiscountRate(10.0);

        // Apply Voucher: HEMAT10 (10% of remaining 90,000 = 9,000)
        request.setVoucherCode("HEMAT10");

        // Grand Total = 100,000 - 10,000 (order disc) - 9,000 (voucher disc) = 81,000
        request.setAmountPaid(100000.0);

        TransactionResponse trx = transactionService.checkout(request, cashier);
        assertNotNull(trx);
        assertEquals("COMPLETED", trx.getStatus());
        assertEquals(100000.0, trx.getSubtotal());
        assertEquals(10000.0, trx.getOrderDiscount());
        assertEquals(9000.0, trx.getVoucherDiscount());
        assertEquals(19000.0, trx.getDiscount()); // Total discount = 10,000 + 9,000
        assertEquals(81000.0, trx.getGrandTotal());
        assertEquals(19000.0, trx.getChangeAmount()); // 100,000 - 81,000 = 19,000
    }
}
