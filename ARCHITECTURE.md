# Expense Tracker Architecture & System Design

This document details the architectural design, security boundaries, database schema, data flow, and component interactions within the **Expense Tracker** application.

---

## High-Level System Architecture

```
+-------------------------------------------------------------+
|                      Client Browser                         |
|      Static HTML5 / CSS3 / ES6 Vanilla JavaScript           |
+------------------------------+------------------------------+
                               |
                               | HTTP REST requests (JSON)
                               | Header: Authorization: Bearer <JWT>
                               v
+-------------------------------------------------------------+
|                     Spring Boot Backend                     |
|                                                             |
|   +-----------------------------------------------------+   |
|   |         JwtAuthenticationFilter & Security          |   |
|   +--------------------------+--------------------------+   |
|                              |                              |
|                              v                              |
|   +-----------------------------------------------------+   |
|   |       REST Controllers (DTO & @Valid Bound)         |   |
|   +--------------------------+--------------------------+   |
|                              |                              |
|                              v                              |
|   +-----------------------------------------------------+   |
|   |       Service Layer (Business Logic & Math)         |   |
|   +--------------------------+--------------------------+   |
|                              |                              |
|                              v                              |
|   +-----------------------------------------------------+   |
|   |   Repository Layer (Spring Data JPA & Specifications)|  |
|   +--------------------------+--------------------------+   |
+------------------------------|------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                      MySQL Database                         |
|             (users, transaction, budget tables)             |
+-------------------------------------------------------------+
```

---

## Data Flow Diagrams

### 1. Authentication Flow

#### User Registration
```
User (Form Input)
  └──> POST /ExpTrack/register (RegisterRequest DTO)
         └──> UserController (Jakarta @Valid Validation)
                └──> UserServiceImpl
                       ├──> BCryptPasswordEncoder.encode(rawPassword)
                       └──> UserRepository.save(User entity)
                              └──> Returns UserResponse DTO (Password Excluded)
```

#### User Login & Token Issuance
```
User (Login Form)
  └──> POST /ExpTrack/login (AuthRequest DTO)
         └──> UserController
                └──> UserServiceImpl
                       ├──> AuthenticationManager.authenticate()
                       ├──> BCrypt Password Verification
                       └──> JwtTokenProvider.generateToken(username)
                              └──> Returns AuthResponse DTO (JWT Token)
```

#### Authenticated Request Processing
```
Incoming HTTP Request
  └──> JwtAuthenticationFilter
         ├──> Extracts "Authorization: Bearer <token>"
         ├──> JwtTokenProvider.validateToken(token)
         └──> Sets UserDetails in SecurityContextHolder
                └──> Controller receives Principal (Security Context Identity)
```

---

### 2. Transaction Flow

```
User Action (Add / Edit / Delete / Filter)
  └──> HTTP Request with Bearer Token
         └──> TransactionController (Receives DTO & Principal)
                └──> TransactionServiceImpl
                       ├──> Verifies Ownership (findByIdAndUserUsername)
                       ├──> Converts amount to BigDecimal (RoundingMode.HALF_UP)
                       └──> TransactionRepository (JPA / Specification Criteria)
                              └──> MySQL Database
```

---

### 3. Dashboard Analytics Flow

```
GET /ExpTrack/dashboard
  └──> DashboardController
         └──> TransactionServiceImpl
                ├──> JPQL Query: SUM(amount) WHERE amount > 0 (totalIncome)
                ├──> JPQL Query: SUM(amount) WHERE amount < 0 (rawExpenses)
                ├──> Computes balance = totalIncome - Math.abs(rawExpenses)
                ├──> Groups Category & Monthly spending maps
                └──> Returns DashboardSummaryResponse DTO
                       └──> JavaScript renders Summary Cards & Visual Progress Bars
```

---

### 4. Monthly Budget Status Flow

```
GET /ExpTrack/budgets/{month}
  └──> BudgetController
         └──> BudgetServiceImpl
                ├──> Queries Budget entity for (user, month)
                ├──> Queries TransactionSpecification for user's expenses in month
                ├──> Computes remaining = budgetAmount - totalExpenses
                ├──> Computes percentageUsed = (totalExpenses / budgetAmount) * 100
                ├──> Evaluates Status:
                │      ├── < 80.00%  ➔ ON_TRACK
                │      ├── < 100.00% ➔ NEAR_LIMIT
                │      └── >= 100.00% ➔ EXCEEDED
                └──> Returns BudgetResponse DTO
                       └──> JavaScript renders Status Badge, Meter Bar & Overflow Warnings
```

---

## Security Architecture & Design Principles

1. **Stateless JWT Security**: The server maintains no HTTP session state. State is verified per-request via cryptographically signed JWT tokens.
2. **Strict Identity Isolation**: Clients cannot query or mutate data by passing user IDs. Identities are derived strictly from `SecurityContextHolder.getContext().getAuthentication().getName()`.
3. **Monetary Precision**: All currency metrics use `BigDecimal` with scale 2 (`RoundingMode.HALF_UP`) to eliminate binary floating-point rounding errors.
4. **Encapsulated DTO Boundaries**: HTTP Controllers accept only `@Valid` Request DTOs and return Response DTOs. JPA entities are never exposed across API boundaries.
