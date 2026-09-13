package com.campusmarket.service;

import com.campusmarket.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusPolicyTest {

    @Test
    void sellerCanConfirmCreatedOrder() {
        assertDoesNotThrow(() ->
                OrderStatusPolicy.validateTransition("CREATED", "CONFIRMED", false, true));
    }

    @Test
    void buyerCannotConfirmOrder() {
        assertThrows(BusinessException.class, () ->
                OrderStatusPolicy.validateTransition("CREATED", "CONFIRMED", true, false));
    }

    @Test
    void completedOrderIsTerminal() {
        assertThrows(BusinessException.class, () ->
                OrderStatusPolicy.validateTransition("COMPLETED", "CREATED", true, true));
    }

    @Test
    void buyerCanCancelConfirmedOrder() {
        assertDoesNotThrow(() ->
                OrderStatusPolicy.validateTransition("CONFIRMED", "CANCELLED", true, false));
    }
}
