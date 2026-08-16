package com.expensetracker.model;

/**
 * Enumeration representing budget tracking status based on percentage usage.
 */
public enum BudgetStatus {
    ON_TRACK,   // 0.00% - 79.99%
    NEAR_LIMIT, // 80.00% - 99.99%
    EXCEEDED    // 100.00%+
}
