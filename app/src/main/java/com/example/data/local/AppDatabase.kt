package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.GroupDao
import com.example.data.local.dao.InvoiceDao
import com.example.data.local.dao.ParsedMessageDao
import com.example.data.local.dao.StatementDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.ParsedMessageEntity
import com.example.data.local.entity.StatementEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        GroupEntity::class,
        ExpenseEntity::class,
        ParsedMessageEntity::class,
        AccountEntity::class,
        StatementEntity::class,
        InvoiceEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun parsedMessageDao(): ParsedMessageDao
    abstract fun accountDao(): AccountDao
    abstract fun statementDao(): StatementDao
    abstract fun invoiceDao(): InvoiceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "split_expense_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            val userDao = db.userDao()
            val groupDao = db.groupDao()
            val expenseDao = db.expenseDao()
            val parsedMsgDao = db.parsedMessageDao()

            // 1. Create Default Users (You + Friends)
            val u1 = UserEntity(id = 1, name = "You", phone = "+1 (555) 019-2834", avatarColorHex = "#00B77D", isCurrentUser = true)
            val u2 = UserEntity(id = 2, name = "Alex Rivers", phone = "+1 (555) 014-9921", avatarColorHex = "#42A5F5", isCurrentUser = false)
            val u3 = UserEntity(id = 3, name = "Emma Watson", phone = "+1 (555) 017-3829", avatarColorHex = "#AB47BC", isCurrentUser = false)
            val u4 = UserEntity(id = 4, name = "Liam Chen", phone = "+1 (555) 018-4720", avatarColorHex = "#FF7043", isCurrentUser = false)
            val u5 = UserEntity(id = 5, name = "Sophia Martinez", phone = "+1 (555) 012-8843", avatarColorHex = "#26A69A", isCurrentUser = false)

            userDao.insertUsers(listOf(u1, u2, u3, u4, u5))

            // 2. Create Default Groups
            val g1 = GroupEntity(
                id = 1,
                name = "Tahoe Cabin Trip 🌲",
                category = "Trip",
                iconName = "cabin",
                coverColorHex = "#00B77D",
                memberIds = "1,2,3,4"
            )
            val g2 = GroupEntity(
                id = 2,
                name = "Apt 4B Roommates 🏠",
                category = "Home",
                iconName = "home",
                coverColorHex = "#6366F1",
                memberIds = "1,2,5"
            )
            val g3 = GroupEntity(
                id = 3,
                name = "Weekend Foodies 🍕",
                category = "Dining",
                iconName = "restaurant",
                coverColorHex = "#FF7043",
                memberIds = "1,3,4,5"
            )

            groupDao.insertGroup(g1)
            groupDao.insertGroup(g2)
            groupDao.insertGroup(g3)

            val now = System.currentTimeMillis()
            val day = 86400000L

            // 3. Create Sample Expenses
            val exp1 = ExpenseEntity(
                id = 1,
                groupId = 1,
                title = "Airbnb Cabin Booking",
                amount = 480.0,
                currency = "$",
                category = "RENT",
                payerId = 1, // You paid
                splitType = "EQUAL",
                splitDetailsJson = """{"1":120.0,"2":120.0,"3":120.0,"4":120.0}""",
                dateMillis = now - 3 * day,
                notes = "3 nights stay at South Lake Tahoe"
            )

            val exp2 = ExpenseEntity(
                id = 2,
                groupId = 1,
                title = "Groceries & BBQ supplies",
                amount = 140.0,
                currency = "$",
                category = "FOOD",
                payerId = 2, // Alex paid
                splitType = "EQUAL",
                splitDetailsJson = """{"1":35.0,"2":35.0,"3":35.0,"4":35.0}""",
                dateMillis = now - 2 * day,
                notes = "Safeway run"
            )

            val exp3 = ExpenseEntity(
                id = 3,
                groupId = 2,
                title = "High Speed Fiber Internet",
                amount = 75.0,
                currency = "$",
                category = "UTILITIES",
                payerId = 1, // You paid
                splitType = "EQUAL",
                splitDetailsJson = """{"1":25.0,"2":25.0,"5":25.0}""",
                dateMillis = now - 1 * day,
                notes = "Monthly bill"
            )

            val exp4 = ExpenseEntity(
                id = 4,
                groupId = null, // Direct 1-on-1 with Emma
                title = "Uber to Downtown Concert",
                amount = 32.0,
                currency = "$",
                category = "TRAVEL",
                payerId = 3, // Emma paid
                splitType = "EQUAL",
                splitDetailsJson = """{"1":16.0,"3":16.0}""",
                dateMillis = now - 12 * 3600000L,
                notes = "Split 2 ways"
            )

            val exp5 = ExpenseEntity(
                id = 5,
                groupId = 3,
                title = "Sushi Omakase Dinner",
                amount = 220.0,
                currency = "$",
                category = "FOOD",
                payerId = 4, // Liam paid
                splitType = "EQUAL",
                splitDetailsJson = """{"1":55.0,"3":55.0,"4":55.0,"5":55.0}""",
                dateMillis = now - 5 * 3600000L,
                notes = "Friday team dinner"
            )

            expenseDao.insertExpense(exp1)
            expenseDao.insertExpense(exp2)
            expenseDao.insertExpense(exp3)
            expenseDao.insertExpense(exp4)
            expenseDao.insertExpense(exp5)

            // 4. Sample Parsed Messages for Quick Demo
            val pm1 = ParsedMessageEntity(
                id = 1,
                rawMessage = "Bank Alert: Debited $64.80 at TRADER JOES #142 on 01-Sep. Avail Bal $3,420.12.",
                parsedTitle = "Trader Joe's Groceries",
                parsedAmount = 64.80,
                parsedCurrency = "$",
                parsedCategory = "SHOPPING",
                parsedMerchant = "Trader Joe's",
                suggestedSplitCount = 3,
                status = "NEW",
                createdAt = now - 2 * 3600000L
            )

            val pm2 = ParsedMessageEntity(
                id = 2,
                rawMessage = "Hey guys! Total for Italian dinner was 150 including tip. 3 of us so 50 each please!",
                parsedTitle = "Italian Dinner & Tip",
                parsedAmount = 150.0,
                parsedCurrency = "$",
                parsedCategory = "FOOD",
                parsedMerchant = "Italian Restaurant",
                suggestedSplitCount = 3,
                status = "NEW",
                createdAt = now - 4 * 3600000L
            )

            parsedMsgDao.insertParsedMessage(pm1)
            parsedMsgDao.insertParsedMessage(pm2)

            // 5. Create Default Accounts (Bank, Credit Card, Digital Wallet)
            val accDao = db.accountDao()
            val acc1 = AccountEntity(
                id = 1,
                name = "Chase Sapphire Preferred",
                type = "CREDIT_CARD",
                institution = "JPMorgan Chase",
                accountNumberLast4 = "4819",
                balance = 2450.80,
                currency = "$",
                colorHex = "#21005D",
                iconName = "credit_card"
            )
            val acc2 = AccountEntity(
                id = 2,
                name = "Bank of America Checking",
                type = "CHECKING",
                institution = "Bank of America",
                accountNumberLast4 = "9102",
                balance = 4820.50,
                currency = "$",
                colorHex = "#6750A4",
                iconName = "account_balance"
            )
            val acc3 = AccountEntity(
                id = 3,
                name = "Apple Card (Mastercard)",
                type = "CREDIT_CARD",
                institution = "Goldman Sachs",
                accountNumberLast4 = "3341",
                balance = 890.25,
                currency = "$",
                colorHex = "#7D5260",
                iconName = "credit_card"
            )
            val acc4 = AccountEntity(
                id = 4,
                name = "Apple Cash / Digital Wallet",
                type = "WALLET",
                institution = "Apple Inc.",
                accountNumberLast4 = "0011",
                balance = 345.00,
                currency = "$",
                colorHex = "#4F378B",
                iconName = "wallet"
            )
            accDao.insertAccounts(listOf(acc1, acc2, acc3, acc4))

            // 6. Create Sample Stored Translated Invoice
            val invDao = db.invoiceDao()
            val sampleInvoice = InvoiceEntity(
                id = 1,
                invoiceNumber = "INV-JP-88219",
                vendorName = "Tokyo Izakaya Torikizoku",
                totalAmount = 68.50,
                subtotalAmount = 62.00,
                taxAmount = 6.50,
                tipAmount = 0.0,
                currency = "$",
                dateMillis = now - 86400000L,
                originalLanguage = "Japanese",
                translatedLanguage = "English",
                rawOcrText = """
                    鳥貴族 渋谷店 (Torikizoku Shibuya)
                    領収書 (Receipt) No. 88219
                    ----------------------------
                    焼き鳥盛り合わせ (Yakitori Platter x2) : ¥1,800 ($12.00)
                    生ビール (Draft Beer x4) : ¥2,400 ($16.00)
                    特製ラーメン (Special Ramen x2) : ¥2,200 ($14.50)
                    枝豆 & 餃子 (Edamame & Gyoza) : ¥1,500 ($10.00)
                    刺身盛り合わせ (Sashimi Set) : ¥2,400 ($16.00)
                    ----------------------------
                    小計 (Subtotal): ¥10,300 ($68.50)
                    消費税 10% (Tax): ¥1,030
                    合計 (Total): ¥11,330 ($68.50 USD)
                """.trimIndent(),
                translatedSummary = "Dinner at Tokyo Izakaya Torikizoku in Shibuya with Yakitori, Draft Beers, Ramen, and Sashimi.",
                lineItemsJson = """[
                    {"originalName":"焼き鳥盛り合わせ","translatedName":"Yakitori Platter (Chicken Skewers)","quantity":2,"unitPrice":6.00,"totalPrice":12.00},
                    {"originalName":"生ビール","translatedName":"Japanese Draft Beers","quantity":4,"unitPrice":4.00,"totalPrice":16.00},
                    {"originalName":"特製ラーメン","translatedName":"Special Tonkotsu Ramen Bowls","quantity":2,"unitPrice":7.25,"totalPrice":14.50},
                    {"originalName":"枝豆 & 餃子","translatedName":"Steamed Edamame & Crispy Gyoza","quantity":1,"unitPrice":10.00,"totalPrice":10.00},
                    {"originalName":"刺身盛り合わせ","translatedName":"Fresh Chef Sashimi Platter","quantity":1,"unitPrice":16.00,"totalPrice":16.00}
                ]""",
                category = "FOOD"
            )
            invDao.insertInvoice(sampleInvoice)
        }
    }
}
