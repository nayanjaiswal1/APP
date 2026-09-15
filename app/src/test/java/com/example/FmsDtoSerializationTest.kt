package com.example

import com.example.data.remote.dto.AccountResponseDto
import com.example.data.remote.dto.AutoCategorizeDto
import com.example.data.remote.dto.CreateAccountDto
import com.example.data.remote.dto.CreateGroupDto
import com.example.data.remote.dto.CreateTransactionDto
import com.example.data.remote.dto.HealthCheckDto
import com.example.data.remote.dto.LoginDto
import com.example.data.remote.dto.LoginResponse
import com.example.data.remote.dto.NaturalLanguageQueryDto
import com.example.data.remote.dto.RegisterDto
import com.example.data.remote.dto.TransactionResponseDto
import com.example.data.remote.dto.UserResponseDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FmsDtoSerializationTest {

    private lateinit var moshi: Moshi

    @Before
    fun setup() {
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun testLoginResponseSerialization() {
        val json = """
            {
                "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                "user": {
                    "id": "usr-123",
                    "email": "user@example.com",
                    "firstName": "Nayan",
                    "lastName": "Jaiswal",
                    "role": "user"
                }
            }
        """.trimIndent()

        val adapter = moshi.adapter(LoginResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", response?.effectiveToken)
        assertEquals("Nayan Jaiswal", response?.user?.displayName)
        assertEquals("user@example.com", response?.user?.email)
    }

    @Test
    fun testCreateTransactionDtoSerialization() {
        val dto = CreateTransactionDto(
            type = "expense",
            amount = 45.99,
            description = "Supermarket Groceries",
            date = "2026-09-15T14:30:00Z",
            accountId = "acc-001",
            categoryId = "cat-groceries",
            notes = "Weekly grocery run",
            paymentMode = "credit_card"
        )

        val adapter = moshi.adapter(CreateTransactionDto::class.java)
        val json = adapter.toJson(dto)

        assertTrue(json.contains("\"amount\":45.99"))
        assertTrue(json.contains("\"type\":\"expense\""))
        assertTrue(json.contains("\"accountId\":\"acc-001\""))
    }

    @Test
    fun testAccountResponseDtoDeserialization() {
        val json = """
            {
                "id": "acc-999",
                "name": "HDFC Checking",
                "type": "bank",
                "balance": 1540.50,
                "currency": "USD",
                "institution": "HDFC Bank",
                "accountNumber": "4321"
            }
        """.trimIndent()

        val adapter = moshi.adapter(AccountResponseDto::class.java)
        val account = adapter.fromJson(json)

        assertNotNull(account)
        assertEquals("acc-999", account?.id)
        assertEquals("HDFC Checking", account?.name)
        assertEquals(1540.50, account?.balance ?: 0.0, 0.001)
        assertEquals("4321", account?.accountNumber)
    }

    @Test
    fun testHealthCheckDeserialization() {
        val json = """
            {
                "status": "ok",
                "version": "1.0.0",
                "timestamp": "2026-09-15T12:00:00Z"
            }
        """.trimIndent()

        val adapter = moshi.adapter(HealthCheckDto::class.java)
        val health = adapter.fromJson(json)

        assertNotNull(health)
        assertEquals("ok", health?.status)
        assertEquals("1.0.0", health?.version)
    }
}
