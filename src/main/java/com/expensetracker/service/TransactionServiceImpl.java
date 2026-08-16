package com.expensetracker.service;

import com.expensetracker.dto.*;
import com.expensetracker.model.Category;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.User;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.specification.TransactionSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for managing transactions securely bound to authenticated users with DTO mapping, BigDecimal calculations, and dynamic filtering.
 */
@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("date", "amount", "text", "category", "id");
    private static final int MAX_PAGE_SIZE = 50;

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getText(),
                t.getAmount().setScale(2, RoundingMode.HALF_UP),
                t.getDate(),
                t.getCategory()
        );
    }

    @Override
    public TransactionResponse addTransaction(CreateTransactionRequest request, String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user not found");
        }

        Transaction transaction = new Transaction();
        transaction.setText(request.getText().trim());
        transaction.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        transaction.setDate(request.getDate() != null ? request.getDate() : LocalDate.now());
        transaction.setCategory(request.getCategory() != null ? request.getCategory() : Category.OTHER);
        transaction.setUser(user);

        Transaction saved = transactionRepository.save(transaction);
        return mapToResponse(saved);
    }

    @Override
    public List<TransactionResponse> getAllTransactions(String username) {
        return transactionRepository.findByUserUsername(username)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PagedResponse<TransactionResponse> searchAndFilterTransactionsPaginated(
            String username,
            String search,
            String type,
            Category category,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        String safeSortBy = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy.toLowerCase()))
                ? sortBy.toLowerCase()
                : "date";

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, safeSortBy);

        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);
        Specification<Transaction> spec = TransactionSpecification.getFilteredTransactions(
                username, search, type, category, startDate, endDate
        );

        Page<Transaction> pageResult = transactionRepository.findAll(spec, pageable);
        List<TransactionResponse> content = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isFirst(),
                pageResult.isLast()
        );
    }

    @Override
    public DashboardSummaryResponse getDashboardSummary(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user not found");
        }

        BigDecimal totalIncome = transactionRepository.sumIncomeByUsername(username);
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        totalIncome = totalIncome.setScale(2, RoundingMode.HALF_UP);

        BigDecimal rawExpenseSum = transactionRepository.sumExpenseByUsername(username);
        if (rawExpenseSum == null) rawExpenseSum = BigDecimal.ZERO;
        BigDecimal totalExpenses = rawExpenseSum.abs().setScale(2, RoundingMode.HALF_UP);

        BigDecimal balance = totalIncome.subtract(totalExpenses).setScale(2, RoundingMode.HALF_UP);
        long transactionCount = transactionRepository.countByUserUsername(username);

        // Category Breakdown
        Map<String, BigDecimal> categoryMap = new LinkedHashMap<>();
        for (Category c : Category.values()) {
            categoryMap.put(c.name(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        List<Object[]> catResults = transactionRepository.findCategoryExpensesByUsername(username);
        for (Object[] row : catResults) {
            Category cat = (Category) row[0];
            Number sum = (Number) row[1];
            if (cat != null && sum != null) {
                BigDecimal catExpense = new BigDecimal(sum.toString()).setScale(2, RoundingMode.HALF_UP);
                categoryMap.put(cat.name(), catExpense);
            }
        }

        // Monthly Breakdown
        Map<String, BigDecimal> monthlyMap = new TreeMap<>();
        List<Transaction> userTransactions = transactionRepository.findByUserUsername(username);
        for (Transaction t : userTransactions) {
            if (t.getAmount().compareTo(BigDecimal.ZERO) < 0 && t.getDate() != null) {
                String monthKey = t.getDate().getYear() + "-" + String.format("%02d", t.getDate().getMonthValue());
                BigDecimal posExpense = t.getAmount().abs();
                BigDecimal current = monthlyMap.getOrDefault(monthKey, BigDecimal.ZERO);
                monthlyMap.put(monthKey, current.add(posExpense).setScale(2, RoundingMode.HALF_UP));
            }
        }

        return new DashboardSummaryResponse(
                balance,
                totalIncome,
                totalExpenses,
                transactionCount,
                categoryMap,
                monthlyMap
        );
    }

    @Override
    public TransactionResponse updateTransaction(Long id, UpdateTransactionRequest request, String username) {
        Transaction existing = transactionRepository.findByIdAndUserUsername(id, username);
        if (existing == null) {
            throw new IllegalArgumentException("Transaction not found or access denied");
        }

        existing.setText(request.getText().trim());
        existing.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        existing.setCategory(request.getCategory() != null ? request.getCategory() : Category.OTHER);
        if (request.getDate() != null) {
            existing.setDate(request.getDate());
        }

        Transaction saved = transactionRepository.save(existing);
        return mapToResponse(saved);
    }

    @Override
    public void deleteTransaction(Long id, String username) {
        Transaction transaction = transactionRepository.findByIdAndUserUsername(id, username);
        if (transaction != null) {
            transactionRepository.delete(transaction);
        } else {
            throw new IllegalArgumentException("Transaction not found or access denied");
        }
    }
}
