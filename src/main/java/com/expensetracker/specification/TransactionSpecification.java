package com.expensetracker.specification;

import com.expensetracker.model.Category;
import com.expensetracker.model.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to build dynamic JPA Specifications for filtering transactions securely per user.
 */
public class TransactionSpecification {

    public static Specification<Transaction> getFilteredTransactions(
            String username,
            String search,
            String type,
            Category category,
            LocalDate startDate,
            LocalDate endDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // MANDATORY SECURITY CLAUSE: Always restrict to authenticated user
            predicates.add(cb.equal(root.get("user").get("username"), username));

            // 1. Case-insensitive partial description search
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("text")), pattern));
            }

            // 2. Filter by type (INCOME: amount > 0, EXPENSE: amount < 0)
            if (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type)) {
                if ("INCOME".equalsIgnoreCase(type)) {
                    predicates.add(cb.gt(root.get("amount"), BigDecimal.ZERO));
                } else if ("EXPENSE".equalsIgnoreCase(type)) {
                    predicates.add(cb.lt(root.get("amount"), BigDecimal.ZERO));
                }
            }

            // 3. Filter by category
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            // 4. Filter by date range
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
