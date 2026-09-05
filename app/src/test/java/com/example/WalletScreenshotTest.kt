package com.example

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
                            ),
                            AccountEntity(
                                id = 3,
                                name = "Visa",
                                type = AccountType.CREDIT_CARD.name,
                                balance = 320.50,
                                creditLimit = 5000.0,
                                colorHex = "#FF7A8A",
                                cardNetwork = "VISA",
                                lastFour = "5678",
                                expiryMonth = 5,
                                expiryYear = 29,
                                cardholderName = "Alex"
                            ),
                            AccountEntity(
                                id = 4,
                                name = "Platinum",
                                type = AccountType.CREDIT_CARD.name,
                                balance = 110.0,
                                creditLimit = 2500.0,
                                colorHex = "#F79E1B",
                                cardNetwork = "MASTERCARD",
                                lastFour = "4421",
                                expiryMonth = 11,
                                expiryYear = 28,
                                cardholderName = "Alex"
                            ),
                            AccountEntity(
                                id = 5,
                                name = "UPI Card",
                                type = AccountType.CREDIT_CARD.name,
                                balance = 45.0,
                                creditLimit = 1000.0,
                                colorHex = "#2E9E3E",
                                cardNetwork = "RUPAY",
                                lastFour = "8809",
                                expiryMonth = 8,
                                expiryYear = 30,
                                cardholderName = "Alex"
                            )
                        ),
                        currencyCode = "USD",
                        monthlyAllowance = 2500.0,
                        profile = UserProfile(displayName = "Alex"),
                        onDeleteBudgetItem = {},
                        onOpenAddBudget = {},
                        onAddAccount = { _ -> },
                        onEditAccount = {},
                        onDeleteAccount = {},
                        onTransfer = { _, _, _, _ -> },
                        onSetCurrency = {},
                        onSetMonthlyLimit = {}
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/wallet.png")
        composeTestRule.onNodeWithTag("spend_trend_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("spend_line_chart").assertExists()
        composeTestRule.onNodeWithTag("spend_range_day").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("spend_range_week").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("spend_range_month").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("spend_trend_card").assertIsDisplayed()
    }

    @Test
    fun tappingCardOpensDetailsNotEditor() {
        composeTestRule.setContent {
            PixiDoTheme {
                ProvideSoundEngine(enabled = false, hapticsEnabled = false) {
                    BudgetScreen(
                        budgetItems = emptyList(),
                        accounts = listOf(
                            AccountEntity(
                                id = 3,
                                name = "Visa",
                                type = AccountType.CREDIT_CARD.name,
                                balance = 320.50,
                                creditLimit = 5000.0,
                                cardNetwork = "VISA",
                                lastFour = "5678",
                                expiryMonth = 5,
                                expiryYear = 29,
                                cardholderName = "Alex"
                            )
                        ),
                        currencyCode = "USD",
                        monthlyAllowance = 2500.0,
                        profile = UserProfile(displayName = "Alex"),
                        onDeleteBudgetItem = {},
                        onOpenAddBudget = {},
                        onAddAccount = { _ -> },
                        onEditAccount = {},
                        onDeleteAccount = {},
                        onTransfer = { _, _, _, _ -> },
                        onSetCurrency = {},
                        onSetMonthlyLimit = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("wallet_stacked_cards").performClick()
        composeTestRule.onNodeWithTag("card_details_dialog").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("edit_account_dialog").assertCountEquals(0)
    }
}
