package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Data Transfer Object containing aggregated financial metrics for the user dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private BigDecimal balance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private long transactionCount;
    private Map<String, BigDecimal> categoryExpenses;
    private Map<String, BigDecimal> monthlyExpenses;
}
