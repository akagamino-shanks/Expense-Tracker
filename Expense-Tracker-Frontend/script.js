const API_BASE_URL = "http://localhost:8080/ExpTrack";

const balance = document.getElementById("balance");
const income = document.getElementById("income");
const expense = document.getElementById("expense");
const transactionCount = document.getElementById("transaction-count");

const budgetMonthInput = document.getElementById("budget-month-input");
const budgetContent = document.getElementById("budget-content");

const categoryBars = document.getElementById("category-bars");
const monthlyBars = document.getElementById("monthly-bars");

const list = document.getElementById("list");
const text = document.getElementById("text");
const amount = document.getElementById("amount");
const categorySelect = document.getElementById("category");
const formTitle = document.getElementById("form-title");
const logoutBtn = document.getElementById("logoutBtn");
const addTransaction = document.getElementById("add-transaction");
const cancelEditBtn = document.getElementById("cancel-edit");

// Filter Controls
const filterSearch = document.getElementById("filter-search");
const filterType = document.getElementById("filter-type");
const filterCategory = document.getElementById("filter-category");
const filterStartDate = document.getElementById("filter-start-date");
const filterEndDate = document.getElementById("filter-end-date");
const applyFiltersBtn = document.getElementById("apply-filters");
const clearFiltersBtn = document.getElementById("clear-filters");

// Pagination Controls
const prevPageBtn = document.getElementById("prev-page");
const nextPageBtn = document.getElementById("next-page");
const pageInfo = document.getElementById("page-info");

const token = sessionStorage.getItem("token");
const username = sessionStorage.getItem("user");

if (!token || !username) {
  window.location.href = "login.html";
}

logoutBtn.addEventListener("click", () => {
  sessionStorage.clear();
  window.location.href = "login.html";
});

let transactions = [];
let editingTransactionId = null;
let editingTransactionDate = null;

// Pagination State
let currentPage = 0;
let pageSize = 10;
let totalPages = 1;
let isFirstPage = true;
let isLastPage = true;

function getAuthHeaders() {
  return {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${token}`
  };
}

function handleUnauthorizedResponse(res) {
  if (res.status === 401) {
    sessionStorage.clear();
    window.location.href = "login.html";
    return true;
  }
  return false;
}

function resetForm() {
  editingTransactionId = null;
  editingTransactionDate = null;
  text.value = "";
  amount.value = "";
  categorySelect.value = "OTHER";
  addTransaction.textContent = "Add Transaction";
  formTitle.textContent = "Add Transaction";
  cancelEditBtn.style.display = "none";
}

function startEdit(transaction) {
  editingTransactionId = transaction.id;
  editingTransactionDate = transaction.date;
  text.value = transaction.text;
  amount.value = transaction.amount;
  categorySelect.value = transaction.category || "OTHER";
  addTransaction.textContent = "Save Changes";
  formTitle.textContent = "Edit Transaction";
  cancelEditBtn.style.display = "inline-block";
}

cancelEditBtn.addEventListener("click", resetForm);

// Set default budget month to current YYYY-MM
budgetMonthInput.value = new Date().toISOString().substring(0, 7);
budgetMonthInput.addEventListener("change", fetchBudgetForSelectedMonth);

// Fetch Monthly Budget Logic
function fetchBudgetForSelectedMonth() {
  const selectedMonth = budgetMonthInput.value;
  if (!selectedMonth) return;

  fetch(`${API_BASE_URL}/budgets/${selectedMonth}`, {
    headers: getAuthHeaders()
  })
    .then((res) => {
      if (handleUnauthorizedResponse(res)) return null;
      if (res.status === 400 || res.status === 404) {
        renderEmptyBudgetState(selectedMonth);
        return null;
      }
      return res.json();
    })
    .then((data) => {
      if (data && data.budgetAmount !== undefined) {
        renderBudgetDetails(data);
      }
    })
    .catch((err) => {
      console.error("Budget fetch error", err);
      renderEmptyBudgetState(selectedMonth);
    });
}

function renderEmptyBudgetState(month) {
  budgetContent.innerHTML = `
    <div style="text-align: center; padding: 10px 0;">
      <p class="empty-msg">No budget set for ${month}.</p>
      <div style="display: flex; gap: 8px; justify-content: center; margin-top: 10px;">
        <input type="number" id="new-budget-amt" placeholder="Enter budget amount" style="padding: 6px 10px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px;">
        <button class="btn btn-sm" id="save-new-budget-btn">Set Budget</button>
      </div>
    </div>
  `;

  document.getElementById("save-new-budget-btn").addEventListener("click", () => {
    const amtInput = document.getElementById("new-budget-amt");
    const val = parseFloat(amtInput.value);
    if (isNaN(val) || val <= 0) {
      alert("Please enter a valid budget amount greater than zero.");
      return;
    }

    fetch(`${API_BASE_URL}/budgets`, {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify({ month: month, amount: val })
    })
      .then((res) => {
        if (handleUnauthorizedResponse(res)) return null;
        return res.json();
      })
      .then((data) => {
        if (data && data.budgetAmount !== undefined) {
          fetchBudgetForSelectedMonth();
        } else if (data && data.message) {
          alert("Error: " + data.message);
        }
      })
      .catch((err) => console.error("Create budget failed", err));
  });
}

function renderBudgetDetails(budgetData) {
  let badgeClass = "badge-on-track";
  let fillClass = "fill-on-track";
  let statusText = "ON TRACK";

  if (budgetData.status === "NEAR_LIMIT") {
    badgeClass = "badge-near-limit";
    fillClass = "fill-near-limit";
    statusText = "NEAR LIMIT";
  } else if (budgetData.status === "EXCEEDED") {
    badgeClass = "badge-exceeded";
    fillClass = "fill-exceeded";
    statusText = "EXCEEDED";
  }

  const fillWidth = Math.min(budgetData.percentageUsed, 100);
  const isExceeded = budgetData.status === "EXCEEDED";

  budgetContent.innerHTML = `
    <div style="display: flex; justify-content: space-between; align-items: center;">
      <span style="font-weight: 600; font-size: 14px;">Month: ${budgetData.month}</span>
      <span class="status-badge ${badgeClass}">${statusText}</span>
    </div>

    <div class="budget-metrics-grid">
      <div class="budget-metric-card">
        <h4>Budget</h4>
        <p>$${budgetData.budgetAmount.toFixed(2)}</p>
      </div>
      <div class="budget-metric-card">
        <h4>Spent</h4>
        <p>$${budgetData.totalExpenses.toFixed(2)}</p>
      </div>
      <div class="budget-metric-card">
        <h4>Remaining</h4>
        <p style="color: ${budgetData.remaining < 0 ? '#e74c3c' : '#2ecc71'};">$${budgetData.remaining.toFixed(2)}</p>
      </div>
    </div>

    <div class="budget-progress-container">
      <div class="budget-progress-info">
        <span>Usage</span>
        <span>${budgetData.percentageUsed.toFixed(2)}%</span>
      </div>
      <div class="budget-progress-track">
        <div class="budget-progress-fill ${fillClass}" style="width: ${fillWidth}%;"></div>
      </div>
      ${isExceeded ? `<div class="budget-overflow-msg">Exceeded by $${Math.abs(budgetData.remaining).toFixed(2)}</div>` : ''}
    </div>

    <div class="budget-actions">
      <button class="btn btn-sm" id="edit-budget-btn">Edit Budget</button>
      <button class="btn btn-sm btn-danger" id="delete-budget-btn">Delete Budget</button>
    </div>
  `;

  document.getElementById("edit-budget-btn").addEventListener("click", () => {
    const newAmtStr = prompt("Enter new monthly budget amount:", budgetData.budgetAmount);
    if (!newAmtStr) return;
    const newAmt = parseFloat(newAmtStr);
    if (isNaN(newAmt) || newAmt <= 0) {
      alert("Please enter a valid amount greater than zero.");
      return;
    }

    fetch(`${API_BASE_URL}/budgets/${budgetData.month}`, {
      method: "PUT",
      headers: getAuthHeaders(),
      body: JSON.stringify({ amount: newAmt })
    })
      .then((res) => {
        if (handleUnauthorizedResponse(res)) return null;
        return res.json();
      })
      .then((data) => {
        if (data && data.budgetAmount !== undefined) {
          fetchBudgetForSelectedMonth();
        } else if (data && data.message) {
          alert("Error: " + data.message);
        }
      })
      .catch((err) => console.error("Edit budget failed", err));
  });

  document.getElementById("delete-budget-btn").addEventListener("click", () => {
    if (!confirm(`Are you sure you want to delete the budget for ${budgetData.month}?`)) return;

    fetch(`${API_BASE_URL}/budgets/${budgetData.month}`, {
      method: "DELETE",
      headers: getAuthHeaders()
    })
      .then((res) => {
        if (handleUnauthorizedResponse(res)) return;
        if (res.ok) {
          fetchBudgetForSelectedMonth();
        }
      })
      .catch((err) => console.error("Delete budget failed", err));
  });
}

// Fetch Dashboard Aggregated Analytics from Backend
function fetchDashboardSummary() {
  fetch(`${API_BASE_URL}/dashboard`, {
    headers: getAuthHeaders()
  })
    .then((res) => {
      if (handleUnauthorizedResponse(res)) return null;
      return res.json();
    })
    .then((data) => {
      if (!data) return;

      // Update Summary Cards
      balance.textContent = `$${data.balance.toFixed(2)}`;
      income.textContent = `$${data.totalIncome.toFixed(2)}`;
      expense.textContent = `$${data.totalExpenses.toFixed(2)}`;
      transactionCount.textContent = data.transactionCount;

      // Render Category Spending Bars (Empty state handling)
      categoryBars.innerHTML = "";
      const catEntries = Object.entries(data.categoryExpenses).filter(([_, val]) => val > 0);
      if (catEntries.length === 0) {
        categoryBars.innerHTML = `<p class="empty-msg">No expense transactions recorded.</p>`;
      } else {
        const maxExpense = Math.max(...catEntries.map(([_, val]) => val));
        catEntries.forEach(([cat, val]) => {
          const pct = maxExpense > 0 ? (val / maxExpense) * 100 : 0;
          const barItem = document.createElement("div");
          barItem.className = "bar-item";

          const label = document.createElement("div");
          label.className = "bar-label";
          label.innerHTML = `<span>${cat}</span><span>$${val.toFixed(2)}</span>`;

          const track = document.createElement("div");
          track.className = "bar-track";
          const fill = document.createElement("div");
          fill.className = "bar-fill";
          fill.style.width = `${pct}%`;
          track.appendChild(fill);

          barItem.appendChild(label);
          barItem.appendChild(track);
          categoryBars.appendChild(barItem);
        });
      }

      // Render Monthly Trend Bars (Empty state handling)
      monthlyBars.innerHTML = "";
      const monthEntries = Object.entries(data.monthlyExpenses);
      if (monthEntries.length === 0) {
        monthlyBars.innerHTML = `<p class="empty-msg">No historical trend data available.</p>`;
      } else {
        const maxMonthly = Math.max(...monthEntries.map(([_, val]) => val));
        monthEntries.forEach(([month, val]) => {
          const pct = maxMonthly > 0 ? (val / maxMonthly) * 100 : 0;
          const barItem = document.createElement("div");
          barItem.className = "bar-item";

          const label = document.createElement("div");
          label.className = "bar-label";
          label.innerHTML = `<span>${month}</span><span>$${val.toFixed(2)}</span>`;

          const track = document.createElement("div");
          track.className = "bar-track";
          const fill = document.createElement("div");
          fill.className = "bar-fill";
          fill.style.backgroundColor = "#e74c3c";
          fill.style.width = `${pct}%`;
          track.appendChild(fill);

          barItem.appendChild(label);
          barItem.appendChild(track);
          monthlyBars.appendChild(barItem);
        });
      }
    })
    .catch((err) => console.error("Dashboard fetch failed", err));
}

// XSS-safe DOM node creation
function addTransactionDOM(transaction) {
  const tr = document.createElement("tr");

  const tdDate = document.createElement("td");
  tdDate.textContent = transaction.date;

  const tdCategory = document.createElement("td");
  tdCategory.textContent = transaction.category || "OTHER";

  const tdText = document.createElement("td");
  tdText.textContent = transaction.text;

  const tdAmount = document.createElement("td");
  tdAmount.className = `amount ${transaction.amount < 0 ? "expense" : ""}`;
  tdAmount.textContent = `$${transaction.amount.toFixed(2)}`;

  const tdAction = document.createElement("td");
  
  const editBtn = document.createElement("button");
  editBtn.textContent = "Edit";
  editBtn.className = "edit-btn";
  editBtn.addEventListener("click", () => startEdit(transaction));

  const deleteBtn = document.createElement("button");
  deleteBtn.textContent = "X";
  deleteBtn.addEventListener("click", () => removeTransaction(transaction.id));

  tdAction.appendChild(editBtn);
  tdAction.appendChild(deleteBtn);

  tr.appendChild(tdDate);
  tr.appendChild(tdCategory);
  tr.appendChild(tdText);
  tr.appendChild(tdAmount);
  tr.appendChild(tdAction);

  list.appendChild(tr);
}

function removeTransaction(id) {
  fetch(`${API_BASE_URL}/transactions/${id}`, {
    method: "DELETE",
    headers: getAuthHeaders()
  })
    .then((res) => {
      if (handleUnauthorizedResponse(res)) return;
      if (res.ok) {
        if (editingTransactionId === id) {
          resetForm();
        }
        fetchFilteredTransactions();
        fetchDashboardSummary();
        fetchBudgetForSelectedMonth();
      }
    })
    .catch((err) => console.error("Delete failed", err));
}

function updateUI() {
  list.innerHTML = "";
  transactions.forEach(addTransactionDOM);
}

addTransaction.addEventListener("click", () => {
  const textValue = text.value.trim();
  const amountValue = parseFloat(amount.value);
  const categoryValue = categorySelect.value;

  if (!textValue || isNaN(amountValue)) {
    alert("Please enter valid description and amount.");
    return;
  }

  const transactionPayload = {
    text: textValue,
    amount: amountValue,
    category: categoryValue,
    date: editingTransactionDate || new Date().toISOString().split("T")[0],
  };

  if (editingTransactionId !== null) {
    fetch(`${API_BASE_URL}/transactions/${editingTransactionId}`, {
      method: "PUT",
      headers: getAuthHeaders(),
      body: JSON.stringify(transactionPayload),
    })
      .then((res) => {
        if (handleUnauthorizedResponse(res)) return null;
        return res.json();
      })
      .then((data) => {
        if (data && !data.message) {
          fetchFilteredTransactions();
          fetchDashboardSummary();
          fetchBudgetForSelectedMonth();
          resetForm();
        } else if (data && data.message) {
          alert("Error: " + data.message);
        }
      })
      .catch((err) => console.error("Update failed", err));
  } else {
    fetch(`${API_BASE_URL}/transactions`, {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify(transactionPayload),
    })
      .then((res) => {
        if (handleUnauthorizedResponse(res)) return null;
        return res.json();
      })
      .then((data) => {
        if (data && !data.message) {
          currentPage = 0;
          fetchFilteredTransactions();
          fetchDashboardSummary();
          fetchBudgetForSelectedMonth();
          resetForm();
        } else if (data && data.message) {
          alert("Error: " + data.message);
        }
      })
      .catch((err) => console.error("Add failed", err));
  }
});

// Dynamic Filter & Paginated API Fetch Function
function fetchFilteredTransactions() {
  const searchVal = filterSearch.value.trim();
  const typeVal = filterType.value;
  const categoryVal = filterCategory.value;
  const startDateVal = filterStartDate.value;
  const endDateVal = filterEndDate.value;

  const params = new URLSearchParams();
  if (searchVal) params.append("search", searchVal);
  if (typeVal && typeVal !== "ALL") params.append("type", typeVal);
  if (categoryVal && categoryVal !== "ALL") params.append("category", categoryVal);
  if (startDateVal) params.append("startDate", startDateVal);
  if (endDateVal) params.append("endDate", endDateVal);

  params.append("page", currentPage);
  params.append("size", pageSize);
  params.append("sortBy", "date");
  params.append("sortDir", "DESC");

  fetch(`${API_BASE_URL}/transactions?${params.toString()}`, {
    headers: getAuthHeaders()
  })
    .then((res) => {
      if (handleUnauthorizedResponse(res)) return null;
      return res.json();
    })
    .then((data) => {
      if (data && data.content !== undefined) {
        transactions = data.content;
        currentPage = data.page;
        totalPages = Math.max(1, data.totalPages);
        isFirstPage = data.first;
        isLastPage = data.last;

        updateUI();

        // Update Pagination Controls
        prevPageBtn.disabled = isFirstPage;
        nextPageBtn.disabled = isLastPage;
        pageInfo.textContent = `Page ${currentPage + 1} of ${totalPages} (Total: ${data.totalElements})`;
      } else if (data && data.message) {
        alert("Filter Error: " + data.message);
      }
    })
    .catch((err) => console.error("Filter fetch failed", err));
}

applyFiltersBtn.addEventListener("click", () => {
  currentPage = 0;
  fetchFilteredTransactions();
});

clearFiltersBtn.addEventListener("click", () => {
  filterSearch.value = "";
  filterType.value = "ALL";
  filterCategory.value = "ALL";
  filterStartDate.value = "";
  filterEndDate.value = "";
  currentPage = 0;
  fetchFilteredTransactions();
});

prevPageBtn.addEventListener("click", () => {
  if (!isFirstPage) {
    currentPage--;
    fetchFilteredTransactions();
  }
});

nextPageBtn.addEventListener("click", () => {
  if (!isLastPage) {
    currentPage++;
    fetchFilteredTransactions();
  }
});

function init() {
  fetchDashboardSummary();
  fetchBudgetForSelectedMonth();
  fetchFilteredTransactions();
}

init();
