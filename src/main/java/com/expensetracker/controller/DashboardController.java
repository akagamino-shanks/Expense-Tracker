package com.expensetracker.controller;

import com.expensetracker.dto.DashboardSummaryResponse;
import com.expensetracker.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * REST controller for authenticated user dashboard statistics and metrics endpoints.
 */
@RestController
@RequestMapping("/ExpTrack/dashboard")
public class DashboardController {

    private final TransactionService transactionService;

    public DashboardController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(Principal principal) {
        DashboardSummaryResponse summary = transactionService.getDashboardSummary(principal.getName());
        return ResponseEntity.ok(summary);
    }
}
