# Expense Tracker 💰

[![Java](https://img.shields.io/badge/Language-Java_17+-orange?style=flat-square&logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Framework-Spring_Boot_3-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Security-JWT_%26_BCrypt-blue?style=flat-square&logo=springsecurity)](https://spring.io/projects/spring-security)
[![MySQL](https://img.shields.io/badge/Database-MySQL_8-blue?style=flat-square&logo=mysql)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](#)

A full-stack, enterprise-grade **Expense Tracker** application built using **Spring Boot 3**, **Spring Security**, **Stateless JWT Authentication**, **Spring Data JPA**, **MySQL**, and a responsive **Vanilla JavaScript** frontend. 

Engineered with monetary precision (`BigDecimal`), strict security data isolation, dynamic JPA specification queries, server-side pagination, real-time dashboard analytics, and monthly budgeting.

---

## Key Features

### 🔐 Security & User Management
* **User Registration & Login**: Public endpoints for user registration and authentication.
* **BCrypt Password Hashing**: Passwords stored as salted BCrypt hashes (`BCryptPasswordEncoder`).
* **Stateless JWT Authentication**: Access tokens signed via JJWT 0.12.6, passed via HTTP Bearer headers.
* **User Data Isolation (BOLA Protection)**: Users can access and mutate **only** their own transactions and budgets. Identifiers are extracted directly from the Spring Security `Principal`.

### 💳 Transaction Management
* **Income & Expense Tracking**: Create, view, edit, and delete transactions.
* **Transaction Categories**: Categorize spending into `FOOD`, `TRANSPORT`, `SHOPPING`, `BILLS`, `EDUCATION`, `ENTERTAINMENT`, `HEALTH`, or `OTHER`.
* **Monetary Precision**: All currency calculations use `BigDecimal` with scale 2 (`RoundingMode.HALF_UP`) to eliminate binary floating-point rounding errors.

### 🔍 Dynamic Search, Filtering & Server-Side Pagination
* **Search & Filters**: Case-insensitive partial description search, transaction type (`INCOME`/`EXPENSE`), category filtering, and date range filtering built with Spring Data JPA `Specification`.
* **Server-Side Pagination**: Efficient paginated querying via `Pageable` and `PagedResponse<T>` DTOs.
* **Whitelisted Sorting**: Safe sorting by `"date"`, `"amount"`, `"text"`, `"category"`, or `"id"`.

### 📊 Dashboard Analytics
* **Real-Time Summary Cards**: Displays Current Balance, Total Income, Total Expenses, and Transaction Count.
* **Spending Breakdowns**: Aggregated category expense breakdown and monthly spending trends calculated directly in MySQL via JPQL queries.

### 🎯 Monthly Budgets
* **Monthly Spending Limits**: Set, retrieve, update, and delete spending budgets per month (`YYYY-MM`).
* **Status Tracking**: Evaluates spending status automatically:
  * **`ON_TRACK`**: `0.00%` – `79.99%` usage (Green)
  * **`NEAR_LIMIT`**: `80.00%` – `99.99%` usage (Orange)
  * **`EXCEEDED`**: `100.00%+` usage (Red)

### ⚙️ Robust API Boundaries & Validation
* **DTO-Based Architecture**: All endpoints consume and produce DTOs. Persistence entities are never exposed across HTTP boundaries.
* **Declarative Bean Validation**: Enforces Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Size`, `@Email`, `@DecimalMin`).
* **Centralized Exception Handling**: Structured JSON error responses (`ErrorResponse`) managed by `@RestControllerAdvice`.

---

## Tech Stack

* **Backend Framework**: Java 17+, Spring Boot 3.x, Spring Data JPA, Spring Security
* **Authentication**: JJWT (Java JWT Library 0.12.6), BCrypt
* **Database**: MySQL 8.x (H2 supported for automated integration testing)
* **Validation**: Jakarta Validation (`jakarta.validation-api`)
* **Frontend**: Vanilla HTML5, Vanilla CSS3, Vanilla ES6 JavaScript (Fetch API, `sessionStorage`)
* **Build Tool**: Maven

---

## System Architecture

```
+-------------------------------------------------------------+
|                      Client Browser                         |
|      Static HTML5 / CSS3 / ES6 Vanilla JavaScript           |
+------------------------------+------------------------------+
                               |
                               | HTTP REST Requests (JSON)
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

## Database Schema & Indexes

* **`users`**: `id` (PK), `username` (UK, Indexed), `email` (UK, Indexed), `password` (BCrypt hash).
* **`transaction`**: `id` (PK), `user_id` (FK, Indexed), `text`, `amount` (BigDecimal 12,2), `date` (Indexed with `user_id`), `category` (Enum String).
* **`budget`**: `id` (PK), `user_id` (FK), `budget_month` (Indexed with `user_id`), `amount` (BigDecimal 12,2), Unique Constraint on `(user_id, budget_month)`.

---

## Quick Setup & Installation Guide

### Prerequisites
* Java JDK 17 or higher
* Maven 3.8+
* MySQL Server 8.0+

### 1. Database Setup
Log into your MySQL client and create the database:
```sql
CREATE DATABASE expense_tracker;
```

### 2. Backend Configuration
Copy `src/main/resources/application-example.properties` to `src/main/resources/application.properties` and update credentials:
```properties
server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/expense_tracker?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_mysql_password

jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
jwt.expiration=86400000
```

### 3. Build & Run Backend
In the project root directory:
```bash
# Compile and test
mvn clean test

# Run application
mvn spring-boot:run
```
The backend server will start on `http://localhost:8080`.

### 4. Run Frontend
Open `Expense-Tracker-Frontend/login.html` directly in any standard web browser or serve it using VS Code Live Server / static file server.

---

## Running Automated Integration Tests

Execute the complete Maven test suite:
```bash
mvn test
```
The automated test suite runs in-memory using H2 database and verifies:
* Registration & BCrypt password hashing
* JWT authentication & 401 error handling
* Multi-user authorization & security isolation
* `BigDecimal` financial math precision
* Dashboard calculations & zero-data handling
* Monthly Budget CRUD, spending calculations, and status transitions

---

## Project Structure

```
Expense-Tracker-main/
├── API.md                              # Detailed REST API Documentation
├── ARCHITECTURE.md                     # System Architecture & Data Flow Guide
├── pom.xml                             # Maven Dependencies
├── src/
│   ├── main/
│   │   ├── java/com/expensetracker/
│   │   │   ├── controller/             # REST Controllers (User, Transaction, Dashboard, Budget)
│   │   │   ├── dto/                    # Request & Response DTOs
│   │   │   ├── exception/              # Global Exception Handler (@RestControllerAdvice)
│   │   │   ├── model/                  # JPA Entities (User, Transaction, Budget, Category, BudgetStatus)
│   │   │   ├── repository/             # Spring Data JPA Repositories
│   │   │   ├── security/               # SecurityConfig, JWT Filter & Token Provider
│   │   │   ├── service/                # Business Logic Interfaces & Implementations
│   │   │   └── specification/          # Dynamic JPA Criteria Specification Builders
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-example.properties
│   └── test/
│       └── java/com/expensetracker/    # Integration & Security Test Suite
└── Expense-Tracker-Frontend/
    ├── index.html                      # Landing Redirect View
    ├── login.html                      # Authentication View
    ├── register.html                   # Registration View
    ├── expense_tracker.html            # Main Dashboard & Budget View
    ├── script.js                       # Frontend ES6 Logic & API Handlers
    └── styles.css                      # Unified Stylesheet
```

---

## Future Improvements

* **Category-Specific Budgets**: Allow setting distinct monthly spending caps for individual categories (e.g. Food vs. Entertainment).
* **CSV / PDF Export**: Server-side export of filtered transaction history to CSV and PDF formats.
* **Recurring Transactions**: Scheduled automatic generation of monthly recurring income and expense items.

---

## License

This project is open source and available under the [MIT License](LICENSE).
