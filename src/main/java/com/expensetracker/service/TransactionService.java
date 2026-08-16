package com.expensetracker.service;

import com.expensetracker.dto.*;
import com.expensetracker.model.Category;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for transaction management, filtering, pagination, and dashboard metrics.
 */
public interface TransactionService {

    TransactionResponse addTransaction(CreateTransactionRequest request, String username);

    List<TransactionResponse> getAllTransactions(String username);

    PagedResponse<TransactionResponse> searchAndFilterTransactionsPaginated(
            String username,
            String search,
            String type,
            Category category,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    DashboardSummaryResponse getDashboardSummary(String username);

    TransactionResponse updateTransaction(Long id, UpdateTransactionRequest request, String username);

    void deleteTransaction(Long id, String username);
}
