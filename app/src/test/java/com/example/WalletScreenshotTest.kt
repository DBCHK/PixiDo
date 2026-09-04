package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.example.audio.ProvideSoundEngine
import com.example.data.AccountEntity
import com.example.data.AccountType
import com.example.data.BudgetItemEntity
import com.example.data.TransactionType
import com.example.data.UserProfile
import com.example.ui.screens.BudgetScreen
import com.example.ui.theme.PixiDoTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class WalletScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun wallet_screenshot() {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000
        composeTestRule.setContent {
            PixiDoTheme {
                ProvideSoundEngine(enabled = false, hapticsEnabled = false) {
                    BudgetScreen(
                        budgetItems = listOf(
                            BudgetItemEntity(
                                id = 1,
                                title = "Apple",
                                amount = 60.0,
                                isExpense = true,
                                category = "Shopping",
                                timestamp = now - 2 * day,
                                transactionType = TransactionType.EXPENSE.name
                            ),
                            BudgetItemEntity(
                                id = 2,
                                title = "Salary",
                                amount = 2000.0,
                                isExpense = false,
                                category = "Salary",
                                timestamp = now - 10 * day,
                                transactionType = TransactionType.INCOME.name
                            ),
                            BudgetItemEntity(
                                id = 3,
                                title = "Threads",
                                amount = 300.0,
                                isExpense = true,
                                category = "Subscriptions",
                                timestamp = now - 5 * day,
                                transactionType = TransactionType.EXPENSE.name
                            ),
                            BudgetItemEntity(
                                id = 4,
                                title = "Coffee",
                                amount = 18.0,
                                isExpense = true,
                                category = "Food",
                                timestamp = now - 4L * 60 * 60 * 1000,
                                transactionType = TransactionType.EXPENSE.name
                            ),
                            BudgetItemEntity(
                                id = 5,
                                title = "Rideshare",
                                amount = 24.0,
                                isExpense = true,
                                category = "Transport",
                                timestamp = now - day,
                                transactionType = TransactionType.EXPENSE.name
                            ),
                            BudgetItemEntity(
                                id = 6,
                                title = "Groceries",
                                amount = 86.0,
                                isExpense = true,
                                category = "Food",
                                timestamp = now - 6 * day,
                                transactionType = TransactionType.EXPENSE.name
                            )
                        ),
                        accounts = listOf(
                            AccountEntity(
                                id = 1,
                                name = "Everyday",
                                type = AccountType.BANK.name,
                                balance = 7854.43,
                                colorHex = "#4DA3FF",
                                isPrimary = true
                            ),
                            AccountEntity(
                                id = 2,
                                name = "Cash",
                                type = AccountType.CASH.name,
                                balance = 120.0,
                                colorHex = "#F4E24C"
                            )
                        ),
                        currencyCode = "USD",
                        monthlyAllowance = 2500.0,
                        profile = UserProfile(displayName = "Alex"),
                        onDeleteBudgetItem = {},
                        onOpenAddBudget = {},
                        onAddAccount = { _, _, _, _, _ -> },
                        onEditAccount = {},
                        onDeleteAccount = {},
                        onTransfer = { _, _, _, _ -> },
                        onSetCurrency = {},
                        onSetMonthlyLimit = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("spend_trend_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("spend_line_chart").assertExists()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/wallet.png")
        composeTestRule.onNodeWithTag("spend_range_day").performClick()
        composeTestRule.onNodeWithTag("spend_range_week").performClick()
        composeTestRule.onNodeWithTag("spend_range_month").performClick()
        composeTestRule.onNodeWithTag("spend_trend_card").assertIsDisplayed()
    }
}
