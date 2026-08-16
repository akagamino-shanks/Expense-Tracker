package com.expensetracker.service;

import com.expensetracker.dto.BudgetResponse;
import com.expensetracker.dto.CreateBudgetRequest;
import com.expensetracker.dto.UpdateBudgetRequest;

/**
 * Service interface for monthly budget operations and status metrics calculation.
 */
public interface BudgetService {

    BudgetResponse setBudget(CreateBudgetRequest request, String username);

    BudgetResponse getBudgetForMonth(String month, String username);

    BudgetResponse updateBudget(String month, UpdateBudgetRequest request, String username);

    void deleteBudget(String month, String username);
}
