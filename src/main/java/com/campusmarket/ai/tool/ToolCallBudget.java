package com.campusmarket.ai.tool;

import java.util.concurrent.atomic.AtomicInteger;

public class ToolCallBudget {

    private static final int DEFAULT_MAX_CALLS = 6;

    private final int maxCalls;
    private final AtomicInteger usedCalls = new AtomicInteger();

    public ToolCallBudget() {
        this(DEFAULT_MAX_CALLS);
    }

    public ToolCallBudget(int maxCalls) {
        if (maxCalls < 1) {
            throw new IllegalArgumentException("maxCalls must be positive");
        }
        this.maxCalls = maxCalls;
    }

    public boolean tryAcquire() {
        return usedCalls.incrementAndGet() <= maxCalls;
    }
}
