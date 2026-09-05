package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.example.audio.ProvideSoundEngine
import com.example.data.CalendarEventEntity
import com.example.data.GoalActivityEntity
import com.example.data.GoalEntity
import com.example.data.HabitStats
import com.example.data.TaskEntity
import com.example.data.UserProfile
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.PixiDoTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class TabChromeScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val now = System.currentTimeMillis()
    private val profile = UserProfile(displayName = "Alex")
    private val tasks = listOf(
        TaskEntity(
            id = 1,
            title = "Delivery App UI Kit",
            category = "Work",
            priority = "HIGH_FIRE",
            dueDateMillis = now,
            dueTimeStr = "Today · 09:00",
            notes = "We got a project to make a delivery ui kit called Foodnow...",
            subtasks = "Interview;Ideate;Wireframe",
            completedSubtasks = "Interview;Ideate"
        ),
        TaskEntity(
            id = 2,
            title = "Buy groceries",
            category = "Personal",
            priority = "QUICK_WIN",
            dueDateMillis = now,
            dueTimeStr = "Today · 18:00",
            notes = "Weekly restock — fruit, milk, oats",
            subtasks = "List;Shop"
        )
    )
    private val goals = listOf(
        GoalEntity(
            id = 1,
            title = "Drink 2L of water",
            category = "Health",
            targetAmount = 1.0,
            unit = "done",
            isSimple = true
        ),
        GoalEntity(
            id = 2,
            title = "Emergency fund",
            category = "Savings",
            targetAmount = 2000.0,
            currentAmount = 450.0,
            unit = "$"
        )
    )

    @Test
    fun tasks_screenshot() {
        composeTestRule.setContent {
            PixiDoTheme {
                ProvideSoundEngine(enabled = false, hapticsEnabled = false) {
                    TasksScreen(
                        tasks = tasks,
                        goals = goals,
                        notes = emptyList(),
                        profile = profile,
                        onToggleTask = {},
                        onToggleSubtask = { _, _ -> },
                        onDeleteTask = {},
                        onEditTask = {},
                        onOpenAddTask = {},
                        onOpenFocusMode = {},
                        onOpenProfile = {},
                        onAddNote = { _, _ -> },
                        onToggleNotePin = {},
                        onDeleteNote = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tasks.png")
    }

    @Test
    fun tasks_detail_screenshot() {
        composeTestRule.setContent {
            PixiDoTheme {
                ProvideSoundEngine(enabled = false, hapticsEnabled = false) {
                    TasksScreen(
                        tasks = tasks,
                        goals = goals,
                        notes = emptyList(),
                        profile = profile,
                        onToggleTask = {},
                        onToggleSubtask = { _, _ -> },
                        onDeleteTask = {},
                        onEditTask = {},
                        onOpenAddTask = {},
                        onOpenFocusMode = {},
                        onOpenProfile = {},
                        onAddNote = { _, _ -> },
                        onToggleNotePin = {},
                        onDeleteNote = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("task_item_1").performClick()
        composeTestRule.onNodeWithTag("task_progress_card").assertExists()
        composeTestRule.onNodeWithTag("task_phase_list").assertExists()
        composeTestRule.onNodeWithTag("phase_row_Interview").assertExists()
        composeTestRule.onNodeWithTag("task_timeline_card").assertExists()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tasks_detail.png")
    }

    @Test
    fun calendar_screenshot() {
        composeTestRule.setContent {
            PixiDoTheme {
                CalendarScreen(
                    events = listOf(2, 5, 6, 7, 8, 11, 14, 15, 16, 20, 23, 24, 27, 30).mapIndexed { i, day ->
                        CalendarEventEntity(
                            id = i + 1,
                            title = "Block $day",
                            category = if (i % 3 == 0) "Fitness" else "Work",
                            dateMillis = dayOfMonth(day),
                            timeSlot = "10:00"
                        )
                    } + listOf(
                        CalendarEventEntity(
                            id = 40,
                            title = "Standup",
                            category = "Work",
                            dateMillis = now,
                            timeSlot = "09:00",
                            startMillis = now
                        ),
                        CalendarEventEntity(
                            id = 41,
                            title = "Invoice",
                            category = "Bills",
                            dateMillis = dayOfMonth(10),
                            timeSlot = "10:00",
                            isCompleted = true
                        ),
                        CalendarEventEntity(
                            id = 42,
                            title = "Wrap up",
                            category = "Work",
                            dateMillis = dayOfMonth(25),
                            timeSlot = "16:00",
                            isCompleted = true
                        )
                    ),
                    tasks = tasks,
                    selectedDateMillis = now,
                    profile = profile,
                    onSelectDate = {},
                    onToggleEvent = {},
                    onDeleteEvent = {},
                    onOpenAddEvent = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/calendar.png")
    }

    @Test
    fun goals_screenshot() {
        val today = HabitStats.dayKey()
        val yesterday = HabitStats.shiftDay(today, -1)
        composeTestRule.setContent {
            PixiDoTheme {
                ProvideSoundEngine(enabled = false, hapticsEnabled = false) {
                    GoalsScreen(
                        goals = goals,
                        currencyCode = "USD",
                        goalActivity = listOf(
                            GoalActivityEntity(
                                goalId = 1,
                                dateKey = today,
                                completedCount = 1
                            ),
                            GoalActivityEntity(
                                goalId = 1,
                                dateKey = yesterday,
                                completedCount = 1
                            )
                        ),
                        profile = profile,
                        onUpdateGoalProgress = { _, _ -> },
                        onToggleHabit = { _, _ -> },
                        onDeleteGoal = {},
                        onOpenAddGoal = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/goals.png")
    }

    private fun dayOfMonth(day: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
