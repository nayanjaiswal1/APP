package com.example.domain.model

import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.UserEntity
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

data class DebtTransfer(
    val fromUserId: Long,
    val toUserId: Long,
    val amount: Double
)

data class UserBalanceSummary(
    val userId: Long,
    val userName: String,
    val userAvatarColor: String,
    val netBalance: Double, // positive = is owed, negative = owes
    val transfersToPay: List<DebtTransfer>,
    val transfersToReceive: List<DebtTransfer>
)

data class GroupSummary(
    val totalSpend: Double,
    val userNetBalance: Double,
    val simplifiedTransfers: List<DebtTransfer>,
    val directTransfers: List<DebtTransfer>,
    val memberBalances: Map<Long, Double>
)

enum class SplitMode(val displayName: String, val description: String) {
    EQUAL("=", "Split equally among selected members"),
    EXACT("$", "Specify exact amount per person"),
    PERCENTAGE("%", "Specify percentage share (total 100%)"),
    SHARES("1x", "Assign relative shares/weights")
}

object DebtEngine {

    /**
     * Parse splitDetailsJson into Map<Long, Double> (userId -> amount/share)
     */
    fun parseSplitDetails(json: String?): Map<Long, Double> {
        if (json.isNullOrBlank()) return emptyMap()
        val result = mutableMapOf<Long, Double>()
        try {
            val cleaned = json.trim().removeSurrounding("{", "}").trim()
            if (cleaned.isNotEmpty()) {
                val pairs = cleaned.split(",")
                for (pair in pairs) {
                    val parts = pair.split(":")
                    if (parts.size == 2) {
                        val key = parts[0].trim().replace("\"", "").toLongOrNull()
                        val value = parts[1].trim().toDoubleOrNull()
                        if (key != null && value != null) {
                            result[key] = value
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * Convert Map<Long, Double> into JSON string
     */
    fun formatSplitDetails(splits: Map<Long, Double>): String {
        val entries = splits.map { "\"${it.key}\":${it.value}" }.joinToString(",")
        return "{$entries}"
    }

    /**
     * Compute Net Balances for each member from a list of expenses.
     * Net balance > 0 means the user is owed money.
     * Net balance < 0 means the user owes money.
     */
    fun calculateNetBalances(
        allUserIds: List<Long>,
        expenses: List<ExpenseEntity>
    ): Map<Long, Double> {
        val balances = allUserIds.associateWith { 0.0 }.toMutableMap()

        for (expense in expenses) {
            if (expense.isSettlement) {
                val from = expense.settlementFromId
                val to = expense.settlementToId
                val amt = expense.amount
                if (from != null && to != null) {
                    balances[from] = (balances[from] ?: 0.0) + amt
                    balances[to] = (balances[to] ?: 0.0) - amt
                }
                continue
            }

            val payerId = expense.payerId
            val splits = parseSplitDetails(expense.splitDetailsJson)
            val totalAmount = expense.amount

            if (splits.isEmpty()) {
                // Fallback equal split if not saved
                val count = allUserIds.size.coerceAtLeast(1)
                val splitAmt = totalAmount / count
                balances[payerId] = (balances[payerId] ?: 0.0) + totalAmount
                for (uid in allUserIds) {
                    balances[uid] = (balances[uid] ?: 0.0) - splitAmt
                }
            } else {
                // Payer gets credited totalAmount
                balances[payerId] = (balances[payerId] ?: 0.0) + totalAmount

                when (expense.splitType) {
                    "EQUAL", "EXACT" -> {
                        for ((userId, share) in splits) {
                            balances[userId] = (balances[userId] ?: 0.0) - share
                        }
                    }
                    "PERCENTAGE" -> {
                        for ((userId, pct) in splits) {
                            val share = totalAmount * (pct / 100.0)
                            balances[userId] = (balances[userId] ?: 0.0) - share
                        }
                    }
                    "SHARES" -> {
                        val totalShares = splits.values.sum().coerceAtLeast(1.0)
                        for ((userId, shareWeight) in splits) {
                            val share = totalAmount * (shareWeight / totalShares)
                            balances[userId] = (balances[userId] ?: 0.0) - share
                        }
                    }
                    else -> {
                        for ((userId, share) in splits) {
                            balances[userId] = (balances[userId] ?: 0.0) - share
                        }
                    }
                }
            }
        }

        // Round to 2 decimal places
        return balances.mapValues { ((it.value * 100.0).roundToInt() / 100.0) }
    }

    /**
     * Greedy Min-Cash Flow algorithm to simplify debt graph into minimal number of transactions.
     */
    fun simplifyDebts(netBalances: Map<Long, Double>): List<DebtTransfer> {
        val debtors = mutableListOf<Pair<Long, Double>>() // balance < 0 (owes)
        val creditors = mutableListOf<Pair<Long, Double>>() // balance > 0 (is owed)

        for ((userId, balance) in netBalances) {
            if (balance < -0.01) {
                debtors.add(userId to abs(balance))
            } else if (balance > 0.01) {
                creditors.add(userId to balance)
            }
        }

        val simplifiedTransfers = mutableListOf<DebtTransfer>()

        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            debtors.sortByDescending { it.second }
            creditors.sortByDescending { it.second }

            val debtor = debtors.removeAt(0)
            val creditor = creditors.removeAt(0)

            val settledAmount = min(debtor.second, creditor.second)
            val roundedAmount = (settledAmount * 100.0).roundToInt() / 100.0

            if (roundedAmount > 0.01) {
                simplifiedTransfers.add(
                    DebtTransfer(
                        fromUserId = debtor.first,
                        toUserId = creditor.first,
                        amount = roundedAmount
                    )
                )
            }

            val remainingDebit = debtor.second - settledAmount
            val remainingCredit = creditor.second - settledAmount

            if (remainingDebit > 0.01) {
                debtors.add(debtor.first to remainingDebit)
            }
            if (remainingCredit > 0.01) {
                creditors.add(creditor.first to remainingCredit)
            }
        }

        return simplifiedTransfers
    }

    /**
     * Compute Pairwise Direct Transfers without graph simplification
     */
    fun calculateDirectTransfers(
        allUserIds: List<Long>,
        expenses: List<ExpenseEntity>
    ): List<DebtTransfer> {
        // matrix: matrix[A][B] = amount A owes B
        val matrix = mutableMapOf<Pair<Long, Long>, Double>()

        for (expense in expenses) {
            if (expense.isSettlement) {
                val from = expense.settlementFromId
                val to = expense.settlementToId
                val amt = expense.amount
                if (from != null && to != null) {
                    val key = from to to
                    matrix[key] = (matrix[key] ?: 0.0) - amt
                }
                continue
            }

            val payerId = expense.payerId
            val splits = parseSplitDetails(expense.splitDetailsJson)
            val totalAmount = expense.amount

            for ((userId, share) in splits) {
                if (userId != payerId) {
                    val actualShare = when (expense.splitType) {
                        "PERCENTAGE" -> totalAmount * (share / 100.0)
                        "SHARES" -> {
                            val totalShares = splits.values.sum().coerceAtLeast(1.0)
                            totalAmount * (share / totalShares)
                        }
                        else -> share
                    }
                    val key = userId to payerId
                    matrix[key] = (matrix[key] ?: 0.0) + actualShare
                }
            }
        }

        // Collapse opposite debts: if A owes B 30 and B owes A 10 -> A owes B 20
        val pairwise = mutableListOf<DebtTransfer>()
        val processed = mutableSetOf<Pair<Long, Long>>()

        for (u1 in allUserIds) {
            for (u2 in allUserIds) {
                if (u1 >= u2) continue
                val pair1 = u1 to u2
                val pair2 = u2 to u1
                val debt1 = matrix[pair1] ?: 0.0
                val debt2 = matrix[pair2] ?: 0.0

                val net = debt1 - debt2
                if (net > 0.01) {
                    pairwise.add(DebtTransfer(fromUserId = u1, toUserId = u2, amount = (net * 100.0).roundToInt() / 100.0))
                } else if (net < -0.01) {
                    pairwise.add(DebtTransfer(fromUserId = u2, toUserId = u1, amount = (abs(net) * 100.0).roundToInt() / 100.0))
                }
            }
        }

        return pairwise
    }
}
