package com.clearing.netting.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradeObligationTest {

    private final LocalDate settleDate = LocalDate.of(2026, 9, 10);

    @Test
    void openObligationCanBeCancelledWithReason() {
        TradeObligation o = open();

        o.cancel("  duplicate entry  ");

        assertEquals(ObligationStatus.CANCELLED, o.getStatus());
        assertEquals("duplicate entry", o.getCancelReason());
    }

    @Test
    void cancelReasonIsRequired() {
        TradeObligation o = open();

        assertThrows(IllegalArgumentException.class, () -> o.cancel(null));
        assertThrows(IllegalArgumentException.class, () -> o.cancel("   "));
        assertEquals(ObligationStatus.OPEN, o.getStatus());
        assertNull(o.getCancelReason());
    }

    @Test
    void nettedObligationCannotBeCancelled() {
        TradeObligation o = open();
        o.markNetted("run-1");

        assertThrows(IllegalStateException.class, () -> o.cancel("too late"));
        assertEquals(ObligationStatus.NETTED, o.getStatus());
    }

    @Test
    void settledObligationCannotBeCancelled() {
        TradeObligation o = open();
        o.markNetted("run-1");
        o.markSettled();

        assertThrows(IllegalStateException.class, () -> o.cancel("too late"));
        assertEquals(ObligationStatus.SETTLED, o.getStatus());
    }

    @Test
    void cancelledObligationCannotBeCancelledAgain() {
        TradeObligation o = open();
        o.cancel("first");

        assertThrows(IllegalStateException.class, () -> o.cancel("second"));
        assertEquals("first", o.getCancelReason());
    }

    @Test
    void cancelledObligationCannotBeNetted() {
        TradeObligation o = open();
        o.cancel("withdrawn");

        assertThrows(IllegalStateException.class, () -> o.markNetted("run-1"));
        assertNull(o.getNettingRunId());
    }

    private TradeObligation open() {
        return TradeObligation.open(
                "A",
                "B",
                "USD",
                new BigDecimal("100"),
                settleDate.minusDays(1),
                settleDate);
    }
}
