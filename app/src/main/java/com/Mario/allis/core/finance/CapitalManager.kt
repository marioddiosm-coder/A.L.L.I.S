package com.Mario.allis.core.finance

data class BudgetEntry(
    val category: String,
    val amount: Double,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class BudgetSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double
)

class CapitalManager {

    private val incomes = mutableListOf<BudgetEntry>()
    private val expenses = mutableListOf<BudgetEntry>()

    fun addIncome(category: String, amount: Double, description: String) {
        incomes.add(BudgetEntry(category, amount, description))
    }

    fun addExpense(category: String, amount: Double, description: String) {
        expenses.add(BudgetEntry(category, amount, description))
    }

    fun summarize(): BudgetSummary {
        val income = incomes.sumOf { it.amount }
        val expense = expenses.sumOf { it.amount }
        return BudgetSummary(
            totalIncome = income,
            totalExpense = expense,
            balance = income - expense
        )
    }

    fun recentActivity(limit: Int = 5): List<BudgetEntry> {
        return (incomes + expenses).sortedByDescending { it.timestamp }.take(limit)
    }
}
