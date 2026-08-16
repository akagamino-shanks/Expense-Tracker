package com.expensetracker.service;

import com.expensetracker.dto.BudgetResponse;
import com.expensetracker.dto.CreateBudgetRequest;
import com.expensetracker.dto.UpdateBudgetRequest;
import com.expensetracker.model.Budget;
import com.expensetracker.model.BudgetStatus;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.User;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.specification.TransactionSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Service implementation for managing monthly user budgets, calculating actual expense spending, and evaluating budget status.
 */
@Service
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public BudgetServiceImpl(BudgetRepository budgetRepository, TransactionRepository transactionRepository, UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    private void validateMonthFormat(String month) {
        if (month == null || !month.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw new IllegalArgumentException("Month must be in valid YYYY-MM format");
        }
    }

    private BigDecimal calculateTotalExpensesForMonth(String username, String yearMonth) {
        LocalDate startDate;
        try {
            startDate = LocalDate.parse(yearMonth + "-01");
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid month format");
        }
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        Specification<Transaction> spec = TransactionSpecification.getFilteredTransactions(
                username, null, "EXPENSE", null, startDate, endDate
        );

        List<Transaction> expenseTransactions = transactionRepository.findAll(spec);
        BigDecimal totalExpenses = BigDecimal.ZERO;
        for (Transaction t : expenseTransactions) {
            if (t.getAmount() != null && t.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                totalExpenses = totalExpenses.add(t.getAmount().abs());
            }
        }
        return totalExpenses.setScale(2, RoundingMode.HALF_UP);
    }

    private BudgetResponse buildBudgetResponse(String yearMonth, BigDecimal budgetAmount, BigDecimal totalExpenses) {
        BigDecimal remaining = budgetAmount.subtract(totalExpenses).setScale(2, RoundingMode.HALF_UP);

        BigDecimal percentageUsed;
        if (budgetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            percentageUsed = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            percentageUsed = totalExpenses.divide(budgetAmount, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BudgetStatus status;
        if (percentageUsed.compareTo(new BigDecimal("80.00")) < 0) {
            status = BudgetStatus.ON_TRACK;
        } else if (percentageUsed.compareTo(new BigDecimal("100.00")) < 0) {
            status = BudgetStatus.NEAR_LIMIT;
        } else {
            status = BudgetStatus.EXCEEDED;
        }

        return new BudgetResponse(
                yearMonth,
                budgetAmount.setScale(2, RoundingMode.HALF_UP),
                totalExpenses,
                remaining,
                percentageUsed,
                status
        );
    }

    @Override
    public BudgetResponse setBudget(CreateBudgetRequest request, String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user not found");
        }

        validateMonthFormat(request.getMonth());

        if (budgetRepository.existsByUserUsernameAndYearMonth(username, request.getMonth())) {
            throw new IllegalArgumentException("Budget already exists for month " + request.getMonth() + ". Use update instead.");
        }

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setYearMonth(request.getMonth());
        budget.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));

        Budget saved = budgetRepository.save(budget);
        BigDecimal totalExpenses = calculateTotalExpensesForMonth(username, saved.getYearMonth());

        return buildBudgetResponse(saved.getYearMonth(), saved.getAmount(), totalExpenses);
    }

    @Override
    public BudgetResponse getBudgetForMonth(String month, String username) {
        validateMonthFormat(month);

        Budget budget = budgetRepository.findByUserUsernameAndYearMonth(username, month)
                .orElseThrow(() -> new IllegalArgumentException("No budget found for month: " + month));

        BigDecimal totalExpenses = calculateTotalExpensesForMonth(username, month);
        return buildBudgetResponse(budget.getYearMonth(), budget.getAmount(), totalExpenses);
    }

    @Override
    public BudgetResponse updateBudget(String month, UpdateBudgetRequest request, String username) {
        validateMonthFormat(month);

        Budget budget = budgetRepository.findByUserUsernameAndYearMonth(username, month)
                .orElseThrow(() -> new IllegalArgumentException("No budget found for month: " + month));

        budget.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        Budget updated = budgetRepository.save(budget);

        BigDecimal totalExpenses = calculateTotalExpensesForMonth(username, month);
        return buildBudgetResponse(updated.getYearMonth(), updated.getAmount(), totalExpenses);
    }

    @Override
    public void deleteBudget(String month, String username) {
        validateMonthFormat(month);

        Budget budget = budgetRepository.findByUserUsernameAndYearMonth(username, month)
                .orElseThrow(() -> new IllegalArgumentException("No budget found for month: " + month));

        budgetRepository.delete(budget);
    }
}
