package com.expensetracker.dto;

import com.expensetracker.model.BudgetStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for budget status and metrics response payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {
    private String month;
    private BigDecimal budgetAmount;
    private BigDecimal totalExpenses;
    private BigDecimal remaining;
    private BigDecimal percentageUsed;
    private BudgetStatus status;
}
