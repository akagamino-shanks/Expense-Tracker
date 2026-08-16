package com.expensetracker;

import com.expensetracker.dto.*;
import com.expensetracker.model.BudgetStatus;
import com.expensetracker.model.Category;
import com.expensetracker.model.User;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.service.BudgetService;
import com.expensetracker.service.TransactionService;
import com.expensetracker.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class SecurityAndTransactionTests {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testUserRegistrationAndBCryptHashing() {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "Password123!");
        UserResponse response = userService.registerUser(request);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());

        User dbUser = userRepository.findByUsername("testuser");
        assertNotNull(dbUser);
        assertNotEquals("Password123!", dbUser.getPassword());
        assertTrue(dbUser.getPassword().startsWith("$2a$") || dbUser.getPassword().startsWith("$2b$"));
        assertTrue(passwordEncoder.matches("Password123!", dbUser.getPassword()));
    }

    @Test
    void testUserLoginAndJwtGeneration() {
        RegisterRequest registerRequest = new RegisterRequest("alice", "alice@example.com", "SecretPass");
        userService.registerUser(registerRequest);

        AuthRequest loginRequest = new AuthRequest("alice", "SecretPass");
        AuthResponse response = userService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("alice", response.getUsername());
        assertEquals("alice@example.com", response.getEmail());
    }

    @Test
    void testLoginInvalidCredentialsFails() {
        RegisterRequest registerRequest = new RegisterRequest("bob", "bob@example.com", "SecretPass");
        userService.registerUser(registerRequest);

        AuthRequest invalidRequest = new AuthRequest("bob", "WrongPass");
        assertThrows(BadCredentialsException.class, () -> userService.login(invalidRequest));
    }

    @Test
    void testUserCannotAccessOrDeleteOtherUserTransactions() {
        RegisterRequest userARequest = new RegisterRequest("userA", "userA@example.com", "passA");
        userService.registerUser(userARequest);

        RegisterRequest userBRequest = new RegisterRequest("userB", "userB@example.com", "passB");
        userService.registerUser(userBRequest);

        CreateTransactionRequest tA = new CreateTransactionRequest("Salary", BigDecimal.valueOf(1000.00), LocalDate.now(), Category.OTHER);
        TransactionResponse savedTA = transactionService.addTransaction(tA, "userA");

        CreateTransactionRequest tB = new CreateTransactionRequest("Coffee", BigDecimal.valueOf(-5.00), LocalDate.now(), Category.FOOD);
        TransactionResponse savedTB = transactionService.addTransaction(tB, "userB");

        List<TransactionResponse> userATransactions = transactionService.getAllTransactions("userA");
        assertEquals(1, userATransactions.size());
        assertEquals("Salary", userATransactions.get(0).getText());

        List<TransactionResponse> userBTransactions = transactionService.getAllTransactions("userB");
        assertEquals(1, userBTransactions.size());
        assertEquals("Coffee", userBTransactions.get(0).getText());

        assertThrows(IllegalArgumentException.class, () -> {
            transactionService.deleteTransaction(savedTB.getId(), "userA");
        });
    }

    @Test
    void testEndToEndUserTransactionFlow() {
        // 1. Registration
        RegisterRequest regReq = new RegisterRequest("e2eUser", "e2e@example.com", "Pass123!");
        userService.registerUser(regReq);

        // 2. Login
        AuthResponse authRes = userService.login(new AuthRequest("e2eUser", "Pass123!"));
        assertNotNull(authRes.getToken());

        // 3. Create Transactions
        TransactionResponse t1 = transactionService.addTransaction(
                new CreateTransactionRequest("Salary", new BigDecimal("3000.00"), LocalDate.now(), Category.OTHER), "e2eUser");
        TransactionResponse t2 = transactionService.addTransaction(
                new CreateTransactionRequest("Rent", new BigDecimal("-1000.00"), LocalDate.now(), Category.BILLS), "e2eUser");

        // 4. Retrieve Transactions
        List<TransactionResponse> all = transactionService.getAllTransactions("e2eUser");
        assertEquals(2, all.size());

        // 5. Edit Transaction
        TransactionResponse edited = transactionService.updateTransaction(
                t2.getId(), new UpdateTransactionRequest("Updated Rent", new BigDecimal("-1200.00"), LocalDate.now(), Category.BILLS), "e2eUser");
        assertEquals("Updated Rent", edited.getText());
        assertEquals(0, new BigDecimal("-1200.00").compareTo(edited.getAmount()));

        // 6. Search & Filter Transactions
        PagedResponse<TransactionResponse> filtered = transactionService.searchAndFilterTransactionsPaginated(
                "e2eUser", "Rent", "EXPENSE", Category.BILLS, null, null, 0, 10, "date", "DESC");
        assertEquals(1, filtered.getContent().size());
        assertEquals("Updated Rent", filtered.getContent().get(0).getText());

        // 7. Delete Transaction
        transactionService.deleteTransaction(t1.getId(), "e2eUser");
        List<TransactionResponse> remaining = transactionService.getAllTransactions("e2eUser");
        assertEquals(1, remaining.size());
        assertEquals("Updated Rent", remaining.get(0).getText());
    }

    @Test
    void testFinancialCalculationsExactMatch() {
        RegisterRequest user = new RegisterRequest("finuser", "fin@example.com", "pass123");
        userService.registerUser(user);

        // Incomes: +100.00, +250.50
        transactionService.addTransaction(new CreateTransactionRequest("Income 1", new BigDecimal("100.00"), LocalDate.now(), Category.OTHER), "finuser");
        transactionService.addTransaction(new CreateTransactionRequest("Income 2", new BigDecimal("250.50"), LocalDate.now(), Category.OTHER), "finuser");

        // Expenses: -50.25, -100.25
        transactionService.addTransaction(new CreateTransactionRequest("Expense 1", new BigDecimal("-50.25"), LocalDate.now(), Category.FOOD), "finuser");
        transactionService.addTransaction(new CreateTransactionRequest("Expense 2", new BigDecimal("-100.25"), LocalDate.now(), Category.TRANSPORT), "finuser");

        DashboardSummaryResponse summary = transactionService.getDashboardSummary("finuser");

        assertEquals(0, new BigDecimal("350.50").compareTo(summary.getTotalIncome()));
        assertEquals(0, new BigDecimal("150.50").compareTo(summary.getTotalExpenses()));
        assertEquals(0, new BigDecimal("200.00").compareTo(summary.getBalance()));
        assertEquals(4, summary.getTransactionCount());
    }

    @Test
    void testDashboardUpdatesWhenTransactionsModifiedOrDeleted() {
        RegisterRequest user = new RegisterRequest("dashmod", "dmod@example.com", "pass123");
        userService.registerUser(user);

        // 1. Initial State: 0 expenses
        DashboardSummaryResponse s1 = transactionService.getDashboardSummary("dashmod");
        assertEquals(0, new BigDecimal("0.00").compareTo(s1.getTotalExpenses()));

        // 2. Add Expense (-200.00)
        TransactionResponse t = transactionService.addTransaction(
                new CreateTransactionRequest("Food", new BigDecimal("-200.00"), LocalDate.now(), Category.FOOD), "dashmod");
        DashboardSummaryResponse s2 = transactionService.getDashboardSummary("dashmod");
        assertEquals(0, new BigDecimal("200.00").compareTo(s2.getTotalExpenses()));
        assertEquals(0, new BigDecimal("200.00").compareTo(s2.getCategoryExpenses().get("FOOD")));

        // 3. Edit Expense to (-350.00)
        transactionService.updateTransaction(t.getId(),
                new UpdateTransactionRequest("More Food", new BigDecimal("-350.00"), LocalDate.now(), Category.FOOD), "dashmod");
        DashboardSummaryResponse s3 = transactionService.getDashboardSummary("dashmod");
        assertEquals(0, new BigDecimal("350.00").compareTo(s3.getTotalExpenses()));

        // 4. Delete Expense
        transactionService.deleteTransaction(t.getId(), "dashmod");
        DashboardSummaryResponse s4 = transactionService.getDashboardSummary("dashmod");
        assertEquals(0, new BigDecimal("0.00").compareTo(s4.getTotalExpenses()));
    }

    @Test
    void testBudgetUpdatesWhenTransactionsModifiedOrDeleted() {
        RegisterRequest user = new RegisterRequest("budmod", "bmod@example.com", "pass123");
        userService.registerUser(user);

        String currentMonth = LocalDate.now().toString().substring(0, 7);
        budgetService.setBudget(new CreateBudgetRequest(currentMonth, new BigDecimal("1000.00")), "budmod");

        // 1. Initial Budget Status
        BudgetResponse b1 = budgetService.getBudgetForMonth(currentMonth, "budmod");
        assertEquals(0, new BigDecimal("0.00").compareTo(b1.getTotalExpenses()));

        // 2. Add Expense (-500.00)
        TransactionResponse t = transactionService.addTransaction(
                new CreateTransactionRequest("Groceries", new BigDecimal("-500.00"), LocalDate.now(), Category.FOOD), "budmod");
        BudgetResponse b2 = budgetService.getBudgetForMonth(currentMonth, "budmod");
        assertEquals(0, new BigDecimal("500.00").compareTo(b2.getTotalExpenses()));
        assertEquals(0, new BigDecimal("500.00").compareTo(b2.getRemaining()));
        assertEquals(BudgetStatus.ON_TRACK, b2.getStatus());

        // 3. Edit Expense to (-850.00)
        transactionService.updateTransaction(t.getId(),
                new UpdateTransactionRequest("Big Groceries", new BigDecimal("-850.00"), LocalDate.now(), Category.FOOD), "budmod");
        BudgetResponse b3 = budgetService.getBudgetForMonth(currentMonth, "budmod");
        assertEquals(0, new BigDecimal("850.00").compareTo(b3.getTotalExpenses()));
        assertEquals(0, new BigDecimal("150.00").compareTo(b3.getRemaining()));
        assertEquals(BudgetStatus.NEAR_LIMIT, b3.getStatus());

        // 4. Delete Expense
        transactionService.deleteTransaction(t.getId(), "budmod");
        BudgetResponse b4 = budgetService.getBudgetForMonth(currentMonth, "budmod");
        assertEquals(0, new BigDecimal("0.00").compareTo(b4.getTotalExpenses()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(b4.getRemaining()));
        assertEquals(BudgetStatus.ON_TRACK, b4.getStatus());
    }

    @Test
    void testPaginationEdgeCases() {
        RegisterRequest user = new RegisterRequest("pageedge", "pedge@example.com", "pass123");
        userService.registerUser(user);

        // Query page 99 for user with 0 transactions
        PagedResponse<TransactionResponse> emptyPage = transactionService.searchAndFilterTransactionsPaginated(
                "pageedge", null, null, null, null, null, 99, 10, "date", "DESC");
        assertNotNull(emptyPage);
        assertTrue(emptyPage.getContent().isEmpty());
        assertEquals(0, emptyPage.getTotalElements());

        // Add 2 items
        transactionService.addTransaction(new CreateTransactionRequest("T1", new BigDecimal("10.00"), LocalDate.now(), Category.OTHER), "pageedge");
        transactionService.addTransaction(new CreateTransactionRequest("T2", new BigDecimal("20.00"), LocalDate.now(), Category.OTHER), "pageedge");

        // Query page 1 (out of range, only 1 page exists)
        PagedResponse<TransactionResponse> outOfRangePage = transactionService.searchAndFilterTransactionsPaginated(
                "pageedge", null, null, null, null, null, 1, 10, "date", "DESC");
        assertTrue(outOfRangePage.getContent().isEmpty());
        assertEquals(2, outOfRangePage.getTotalElements());
        assertEquals(1, outOfRangePage.getTotalPages());
    }

    @Test
    void testBudgetCrudOperations() {
        RegisterRequest user = new RegisterRequest("buser", "buser@example.com", "pass123");
        userService.registerUser(user);

        CreateBudgetRequest createReq = new CreateBudgetRequest("2026-08", new BigDecimal("1000.00"));
        BudgetResponse created = budgetService.setBudget(createReq, "buser");
        assertEquals("2026-08", created.getMonth());
        assertEquals(0, new BigDecimal("1000.00").compareTo(created.getBudgetAmount()));
        assertEquals(0, new BigDecimal("0.00").compareTo(created.getTotalExpenses()));
        assertEquals(BudgetStatus.ON_TRACK, created.getStatus());

        BudgetResponse retrieved = budgetService.getBudgetForMonth("2026-08", "buser");
        assertEquals(0, new BigDecimal("1000.00").compareTo(retrieved.getBudgetAmount()));

        UpdateBudgetRequest updateReq = new UpdateBudgetRequest(new BigDecimal("1500.00"));
        BudgetResponse updated = budgetService.updateBudget("2026-08", updateReq, "buser");
        assertEquals(0, new BigDecimal("1500.00").compareTo(updated.getBudgetAmount()));

        budgetService.deleteBudget("2026-08", "buser");
        assertThrows(IllegalArgumentException.class, () -> budgetService.getBudgetForMonth("2026-08", "buser"));
    }

    @Test
    void testBudgetUserIsolation() {
        RegisterRequest user1 = new RegisterRequest("biso1", "bi1@example.com", "p1");
        userService.registerUser(user1);

        RegisterRequest user2 = new RegisterRequest("biso2", "bi2@example.com", "p2");
        userService.registerUser(user2);

        budgetService.setBudget(new CreateBudgetRequest("2026-08", new BigDecimal("500.00")), "biso1");

        assertThrows(IllegalArgumentException.class, () -> budgetService.getBudgetForMonth("2026-08", "biso2"));
        assertThrows(IllegalArgumentException.class, () -> budgetService.updateBudget("2026-08", new UpdateBudgetRequest(new BigDecimal("999.00")), "biso2"));
        assertThrows(IllegalArgumentException.class, () -> budgetService.deleteBudget("2026-08", "biso2"));
    }
}
