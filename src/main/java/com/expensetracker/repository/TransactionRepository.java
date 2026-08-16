package com.expensetracker.repository;

import com.expensetracker.model.Category;
import com.expensetracker.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository interface for Transaction entity database operations with JPA Specification and aggregation support.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByUserUsername(String username);

    List<Transaction> findByUserUsernameAndCategory(String username, Category category);

    Transaction findByIdAndUserUsername(Long id, String username);

    long countByUserUsername(String username);

    @Query("SELECT COALESCE(SUM(t.amount), 0.0) FROM Transaction t WHERE t.user.username = :username AND t.amount > 0")
    BigDecimal sumIncomeByUsername(@Param("username") String username);

    @Query("SELECT COALESCE(SUM(t.amount), 0.0) FROM Transaction t WHERE t.user.username = :username AND t.amount < 0")
    BigDecimal sumExpenseByUsername(@Param("username") String username);

    @Query("SELECT t.category, COALESCE(SUM(ABS(t.amount)), 0.0) FROM Transaction t WHERE t.user.username = :username AND t.amount < 0 GROUP BY t.category")
    List<Object[]> findCategoryExpensesByUsername(@Param("username") String username);
}
