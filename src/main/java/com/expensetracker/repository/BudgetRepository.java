package com.expensetracker.repository;

import com.expensetracker.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Budget database operations.
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByUserUsernameAndYearMonth(String username, String yearMonth);

    boolean existsByUserUsernameAndYearMonth(String username, String yearMonth);
}
