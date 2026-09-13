package com.campusmarket.ai.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallBudgetTest {

    @Test
    void shouldStopAfterConfiguredNumberOfToolCalls() {
        ToolCallBudget budget = new ToolCallBudget(2);

        assertTrue(budget.tryAcquire());
        assertTrue(budget.tryAcquire());
        assertFalse(budget.tryAcquire());
    }
}
