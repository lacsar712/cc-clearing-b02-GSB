package com.clearing.netting.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradeObligationCancelTest {

    private TradeObligation open() {
        return TradeObligation.open(
                "A", "B", "USD", new BigDecimal("100"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10));
    }

    @Test
    void openObligationCanBeCancelledWithReason() {
        TradeObligation o = open();
        o.cancel("双方协商终止");
        assertEquals(ObligationStatus.CANCELLED, o.getStatus());
        assertEquals("双方协商终止", o.getCancelReason());
    }

    @Test
    void cancelRequiresReason() {
        TradeObligation o = open();
        assertThrows(IllegalArgumentException.class, () -> o.cancel("  "));
    }

    @Test
    void nettedObligationCannotBeCancelled() {
        TradeObligation o = open();
        o.markNetted("run-1");
        assertThrows(IllegalStateException.class, () -> o.cancel("late cancel"));
        assertEquals(ObligationStatus.NETTED, o.getStatus());
    }

    @Test
    void settledObligationCannotBeCancelled() {
        TradeObligation o = open();
        o.markNetted("run-1");
        o.markSettled();
        assertThrows(IllegalStateException.class, () -> o.cancel("late cancel"));
        assertEquals(ObligationStatus.SETTLED, o.getStatus());
    }

    @Test
    void cancelledObligationCannotBeCancelledAgain() {
        TradeObligation o = open();
        o.cancel("first");
        assertThrows(IllegalStateException.class, () -> o.cancel("second"));
        assertEquals("first", o.getCancelReason());
    }
}
