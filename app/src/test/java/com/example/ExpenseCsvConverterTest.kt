package com.example

import com.example.data.export.ExpenseReportExporter
import com.example.data.local.entity.ExpenseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseCsvConverterTest {

    @Test
    fun testConvertExpensesToCsv_basicExpense() {
        val expense = ExpenseEntity(
            id = 101L,
            title = "Team Lunch",
            amount = 45.50,
            currency = "$",
            dateMillis = 1726358400000L, // Fixed timestamp
            payerId = 1L,
            groupId = 2L,
            category = "Food & Dining",
            splitType = "EQUAL",
            splitDetailsJson = "{\"1\":22.75,\"2\":22.75}",
            notes = "Lunch meeting",
            isSettlement = false,
            settlementFromId = null,
            settlementToId = null,
            sourceMessage = "Paid 45.50 for lunch"
        )

        val userMap = mapOf(1L to "Alice", 2L to "Bob")
        val groupMap = mapOf(2L to "Work Trip")

        val csv = ExpenseReportExporter.convertExpensesToCsv(listOf(expense), userMap, groupMap)

        // Verify CSV header
        val lines = csv.trim().split("\n")
        assertEquals(2, lines.size)
        assertEquals("ID,Date,Time,Title,Category,Amount,Currency,Payer,Group,Split Type,Split Details,Notes,Is Settlement,Source Message", lines[0])

        // Verify row contents
        val dataRow = lines[1]
        assertTrue(dataRow.startsWith("101,"))
        assertTrue(dataRow.contains("Team Lunch"))
        assertTrue(dataRow.contains("Food & Dining"))
        assertTrue(dataRow.contains("45.50"))
        assertTrue(dataRow.contains("$"))
        assertTrue(dataRow.contains("Alice"))
        assertTrue(dataRow.contains("Work Trip"))
        assertTrue(dataRow.contains("EQUAL"))
        assertTrue(dataRow.contains("No")) // Is Settlement
    }

    @Test
    fun testConvertExpensesToCsv_escapingCommasAndQuotes() {
        val expenseWithSpecialChars = ExpenseEntity(
            id = 102L,
            title = "Groceries, Milk & \"Fancy\" Cheese",
            amount = 120.00,
            currency = "EUR",
            dateMillis = 1726358400000L,
            payerId = 2L,
            groupId = null,
            category = "Groceries",
            splitType = "EXACT",
            splitDetailsJson = "{}",
            notes = "Line 1\nLine 2 with, commas",
            isSettlement = false,
            sourceMessage = "Bank alert: \"Spent 120 EUR\""
        )

        val csv = ExpenseReportExporter.convertExpensesToCsv(listOf(expenseWithSpecialChars))

        // Ensure quotes are escaped with double quotes
        assertTrue(csv.contains("\"Groceries, Milk & \"\"Fancy\"\" Cheese\""))
        assertTrue(csv.contains("\"Bank alert: \"\"Spent 120 EUR\"\"\""))
    }

    @Test
    fun testEscapeCsv_handlesSpecialCases() {
        assertEquals("simple", ExpenseReportExporter.escapeCsv("simple"))
        assertEquals("\"value,with,commas\"", ExpenseReportExporter.escapeCsv("value,with,commas"))
        assertEquals("\"value with \"\"quotes\"\"\"", ExpenseReportExporter.escapeCsv("value with \"quotes\""))
        assertEquals("\"value\nwith\nnewlines\"", ExpenseReportExporter.escapeCsv("value\nwith\nnewlines"))
    }

    @Test
    fun testConvertExpensesToCsv_settlementRow() {
        val settlementExpense = ExpenseEntity(
            id = 103L,
            title = "Settlement payment",
            amount = 35.00,
            currency = "$",
            dateMillis = 1726358400000L,
            payerId = 2L,
            groupId = null,
            category = "Settlement",
            splitType = "SETTLEMENT",
            splitDetailsJson = "{}",
            notes = "Cleared debt",
            isSettlement = true,
            settlementFromId = 2L,
            settlementToId = 1L
        )

        val csv = ExpenseReportExporter.convertExpensesToCsv(listOf(settlementExpense))
        assertTrue(csv.contains("Yes")) // Is Settlement = Yes
        assertTrue(csv.contains("35.00"))
        assertTrue(csv.contains("Settlement"))
    }
}
