package com.expensetracker.dto;

import com.expensetracker.model.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object representing transaction data returned to clients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String text;
    private BigDecimal amount;
    private LocalDate date;
    private Category category;
}
