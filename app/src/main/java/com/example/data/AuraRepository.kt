package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class AuraRepository(private val dao: AuraDao) {

    val allTasks: Flow<List<TaskEntity>> = dao.getAllTasks()
    val allBudgetItems: Flow<List<BudgetItemEntity>> = dao.getAllBudgetItems()
    val allCalendarEvents: Flow<List<CalendarEventEntity>> = dao.getAllCalendarEvents()
    val allGoals: Flow<List<GoalEntity>> = dao.getAllGoals()

    suspend fun addTask(task: TaskEntity) = dao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = dao.updateTask(task)
    suspend fun deleteTask(taskId: Int) = dao.deleteTaskById(taskId)

    suspend fun addBudgetItem(item: BudgetItemEntity) = dao.insertBudgetItem(item)
    suspend fun deleteBudgetItem(itemId: Int) = dao.deleteBudgetItemById(itemId)

    suspend fun addCalendarEvent(event: CalendarEventEntity) = dao.insertCalendarEvent(event)
    suspend fun updateCalendarEvent(event: CalendarEventEntity) = dao.updateCalendarEvent(event)
    suspend fun deleteCalendarEvent(eventId: Int) = dao.deleteCalendarEventById(eventId)

    suspend fun addGoal(goal: GoalEntity) = dao.insertGoal(goal)
    suspend fun updateGoal(goal: GoalEntity) = dao.updateGoal(goal)
    suspend fun deleteGoal(goalId: Int) = dao.deleteGoalById(goalId)

    /**
     * Pre-populates sample data if database is fresh on first launch.
     */
    suspend fun prepopulateIfEmpty() {
        val currentTasks = dao.getAllTasks().first()
        if (currentTasks.isEmpty()) {
            val now = System.currentTimeMillis()

            // Pre-populate Goals first so tasks/budget can link
            val goalTokyoId = dao.insertGoal(
                GoalEntity(
                    title = "Tokyo Summer Trip ✈️",
                    category = "Travel ✈️",
                    targetAmount = 2500.0,
                    currentAmount = 1450.0,
                    unit = "$",
                    deadlineStr = "Aug 2027",
                    colorHex = "#38BDF8"
                )
            ).toInt()

            val goalEmergencyId = dao.insertGoal(
                GoalEntity(
                    title = "Emergency Vibe Savings 💰",
                    category = "Savings 💰",
                    targetAmount = 1000.0,
                    currentAmount = 650.0,
                    unit = "$",
                    deadlineStr = "Dec 2026",
                    colorHex = "#10B981"
                )
            ).toInt()

            dao.insertGoal(
                GoalEntity(
                    title = "Read 12 Mindset Books 📚",
                    category = "Mindset 🧠",
                    targetAmount = 12.0,
                    currentAmount = 5.0,
                    unit = "books",
                    deadlineStr = "End of Year",
                    colorHex = "#A78BFA"
                )
            )

            // Pre-populate Tasks
            dao.insertTask(
                TaskEntity(
                    title = "Finish Figma App Design System 🎨",
                    category = "Side Hustle",
                    priority = "HIGH_FIRE",
                    dueTimeStr = "5:00 PM",
                    subtasks = "Create color tokens; Design card components; Export icons",
                    completedSubtasks = "Create color tokens",
                    linkedGoalId = goalTokyoId,
                    xpReward = 50
                )
            )

            dao.insertTask(
                TaskEntity(
                    title = "Pilates & Core Strength Workout 🧘",
                    category = "Health",
                    priority = "QUICK_WIN",
                    dueTimeStr = "7:00 PM",
                    streakCount = 4,
                    xpReward = 25
                )
            )

            dao.insertTask(
                TaskEntity(
                    title = "Weekly Grocery Prep & Smoothies 🫐",
                    category = "Life",
                    priority = "CORE_GOAL",
                    dueTimeStr = "Tomorrow",
                    xpReward = 20
                )
            )

            dao.insertTask(
                TaskEntity(
                    title = "Brainstorm TikTok Content Calendar 🚀",
                    category = "Side Hustle",
                    priority = "BRAINSTORM",
                    dueTimeStr = "This Week",
                    xpReward = 30
                )
            )

            // Pre-populate Budget Items
            dao.insertBudgetItem(
                BudgetItemEntity(
                    title = "UI Design Freelance Client Payment",
                    amount = 650.0,
                    isExpense = false,
                    category = "Hustle Income 💸",
                    note = "Milestone 1 payout"
                )
            )

            dao.insertBudgetItem(
                BudgetItemEntity(
                    title = "Matcha Oat Latte & Croissant 🍵",
                    amount = 8.50,
                    isExpense = true,
                    category = "Food & Coffee ☕"
                )
            )

            dao.insertBudgetItem(
                BudgetItemEntity(
                    title = "Spotify Student Premium 🎵",
                    amount = 5.99,
                    isExpense = true,
                    category = "Subscriptions 🎵"
                )
            )

            dao.insertBudgetItem(
                BudgetItemEntity(
                    title = "Tokyo Flight Savings Transfer ✈️",
                    amount = 200.0,
                    isExpense = true,
                    category = "Savings & Wealth 💰",
                    note = "Pushed to Tokyo Goal"
                )
            )

            // Pre-populate Calendar Events
            val cal = Calendar.getInstance()
            
            dao.insertCalendarEvent(
                CalendarEventEntity(
                    title = "Deep Focus: Figma Prototyping",
                    category = "Deep Work",
                    dateMillis = cal.timeInMillis,
                    timeSlot = "10:00 AM - 12:00 PM"
                )
            )

            dao.insertCalendarEvent(
                CalendarEventEntity(
                    title = "Coffee Catch-up with Maya ☕",
                    category = "Social & Hangouts",
                    dateMillis = cal.timeInMillis,
                    timeSlot = "3:30 PM - 4:30 PM"
                )
            )

            cal.add(Calendar.DAY_OF_YEAR, 1)
            dao.insertCalendarEvent(
                CalendarEventEntity(
                    title = "Pilates & Stretch Class",
                    category = "Fitness & Wellness",
                    dateMillis = cal.timeInMillis,
                    timeSlot = "8:00 AM - 9:00 AM"
                )
            )
        }
    }
}
