# Expense Tracker REST API Documentation

All protected endpoints require HTTP Bearer Token authentication via the `Authorization` header:
`Authorization: Bearer <your_jwt_token>`

---

## Authentication Endpoints

### 1. User Registration
* **HTTP Method**: `POST`
* **Path**: `/ExpTrack/register`
* **Authentication**: Public
* **Request Body**:
  ```json
  {
    "username": "alice",
    "email": "alice@example.com",
    "password": "Password123!"
  }
  ```
* **Response** (`201 Created`):
  ```json
  {
    "id": 1,
    "username": "alice",
    "email": "alice@example.com"
  }
  ```

### 2. User Login
* **HTTP Method**: `POST`
* **Path**: `/ExpTrack/login`
* **Authentication**: Public
* **Request Body**:
  ```json
  {
    "username": "alice",
    "password": "Password123!"
  }
  ```
* **Response** (`200 OK`):
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "username": "alice",
    "email": "alice@example.com"
  }
  ```

---

## Transaction Endpoints

### 3. Add Transaction
* **HTTP Method**: `POST`
* **Path**: `/ExpTrack/transactions`
* **Authentication**: Required
* **Request Body**:
  ```json
  {
    "text": "Grocery Shopping",
    "amount": -150.50,
    "category": "FOOD",
    "date": "2026-08-16"
  }
  ```
* **Response** (`201 Created`):
  ```json
  {
    "id": 101,
    "text": "Grocery Shopping",
    "amount": -150.50,
    "date": "2026-08-16",
    "category": "FOOD"
  }
  ```

### 4. Search, Filter, and Paginate Transactions
* **HTTP Method**: `GET`
* **Path**: `/ExpTrack/transactions`
* **Authentication**: Required
* **Query Parameters**:
  * `search` *(optional)*: Case-insensitive text filter.
  * `type` *(optional)*: `ALL`, `INCOME` (`amount > 0`), `EXPENSE` (`amount < 0`).
  * `category` *(optional)*: `FOOD`, `TRANSPORT`, `SHOPPING`, `BILLS`, `EDUCATION`, `ENTERTAINMENT`, `HEALTH`, `OTHER`.
  * `startDate` *(optional)*: `YYYY-MM-DD`.
  * `endDate` *(optional)*: `YYYY-MM-DD`.
  * `page` *(optional, default `0`)*: Page index.
  * `size` *(optional, default `10`, max `50`)*: Page size.
  * `sortBy` *(optional, default `"date"`)*: Allowed: `"date"`, `"amount"`, `"text"`, `"category"`, `"id"`.
  * `sortDir` *(optional, default `"DESC"`)*: `ASC` or `DESC`.
* **Response** (`200 OK`):
  ```json
  {
    "content": [
      {
        "id": 101,
        "text": "Grocery Shopping",
        "amount": -150.50,
        "date": "2026-08-16",
        "category": "FOOD"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
  ```

### 5. Update Transaction
* **HTTP Method**: `PUT`
* **Path**: `/ExpTrack/transactions/{id}`
* **Authentication**: Required
* **Request Body**:
  ```json
  {
    "text": "Supermarket Groceries",
    "amount": -175.00,
    "category": "FOOD",
    "date": "2026-08-16"
  }
  ```
* **Response** (`200 OK`):
  ```json
  {
    "id": 101,
    "text": "Supermarket Groceries",
    "amount": -175.00,
    "date": "2026-08-16",
    "category": "FOOD"
  }
  ```

### 6. Delete Transaction
* **HTTP Method**: `DELETE`
* **Path**: `/ExpTrack/transactions/{id}`
* **Authentication**: Required
* **Response** (`204 No Content`)

---

## Dashboard Analytics Endpoint

### 7. Get Dashboard Metrics
* **HTTP Method**: `GET`
* **Path**: `/ExpTrack/dashboard`
* **Authentication**: Required
* **Response** (`200 OK`):
  ```json
  {
    "balance": 1825.00,
    "totalIncome": 2500.00,
    "totalExpenses": 675.00,
    "transactionCount": 4,
    "categoryExpenses": {
      "FOOD": 175.00,
      "TRANSPORT": 50.00,
      "BILLS": 450.00,
      "SHOPPING": 0.00,
      "EDUCATION": 0.00,
      "ENTERTAINMENT": 0.00,
      "HEALTH": 0.00,
      "OTHER": 0.00
    },
    "monthlyExpenses": {
      "2026-08": 675.00
    }
  }
  ```

---

## Monthly Budget Endpoints

### 8. Set Monthly Budget
* **HTTP Method**: `POST`
* **Path**: `/ExpTrack/budgets`
* **Authentication**: Required
* **Request Body**:
  ```json
  {
    "month": "2026-08",
    "amount": 1000.00
  }
  ```
* **Response** (`201 Created`):
  ```json
  {
    "month": "2026-08",
    "budgetAmount": 1000.00,
    "totalExpenses": 675.00,
    "remaining": 325.00,
    "percentageUsed": 67.50,
    "status": "ON_TRACK"
  }
  ```

### 9. Get Monthly Budget Status
* **HTTP Method**: `GET`
* **Path**: `/ExpTrack/budgets/{month}` (e.g. `/ExpTrack/budgets/2026-08`)
* **Authentication**: Required
* **Response** (`200 OK`):
  ```json
  {
    "month": "2026-08",
    "budgetAmount": 1000.00,
    "totalExpenses": 675.00,
    "remaining": 325.00,
    "percentageUsed": 67.50,
    "status": "ON_TRACK"
  }
  ```

### 10. Update Monthly Budget
* **HTTP Method**: `PUT`
* **Path**: `/ExpTrack/budgets/{month}`
* **Authentication**: Required
* **Request Body**:
  ```json
  {
    "amount": 1200.00
  }
  ```
* **Response** (`200 OK`):
  ```json
  {
    "month": "2026-08",
    "budgetAmount": 1200.00,
    "totalExpenses": 675.00,
    "remaining": 525.00,
    "percentageUsed": 56.25,
    "status": "ON_TRACK"
  }
  ```

### 11. Delete Monthly Budget
* **HTTP Method**: `DELETE`
* **Path**: `/ExpTrack/budgets/{month}`
* **Authentication**: Required
* **Response** (`204 No Content`)

---

## Standard Error Response Format

Validation failures or bad requests return standardized error payloads:

```json
{
  "timestamp": "2026-08-16T18:47:24",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed for one or more fields",
  "path": "/ExpTrack/transactions",
  "validationErrors": {
    "text": "Description cannot be blank",
    "amount": "Amount is required"
  }
}
```
