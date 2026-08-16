package com.expensetracker.controller;

import com.expensetracker.dto.BudgetResponse;
import com.expensetracker.dto.CreateBudgetRequest;
import com.expensetracker.dto.UpdateBudgetRequest;
import com.expensetracker.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * REST controller for authenticated user monthly budget management.
 */
@RestController
@RequestMapping("/ExpTrack/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> setBudget(@Valid @RequestBody CreateBudgetRequest request, Principal principal) {
        BudgetResponse response = budgetService.setBudget(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{month}")
    public ResponseEntity<BudgetResponse> getBudgetForMonth(@PathVariable String month, Principal principal) {
        BudgetResponse response = budgetService.getBudgetForMonth(month, principal.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{month}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable String month,
            @Valid @RequestBody UpdateBudgetRequest request,
            Principal principal) {
        BudgetResponse response = budgetService.updateBudget(month, request, principal.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{month}")
    public ResponseEntity<Void> deleteBudget(@PathVariable String month, Principal principal) {
        budgetService.deleteBudget(month, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
