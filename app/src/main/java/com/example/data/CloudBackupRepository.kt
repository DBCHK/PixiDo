package com.example.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Cloud backup / restore of all app data under the signed-in Google account.
 *
 * Firestore path: users/{uid}/backup/latest
 */
class CloudBackupRepository(
    private val context: Context,
    private val auraRepository: AuraRepository,
    private val preferences: UserPreferencesRepository
) {

    private fun firestore(): FirebaseFirestore? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        FirebaseFirestore.getInstance()
    }.getOrNull()

    private fun uid(): String? = runCatching {
        FirebaseAuth.getInstance().currentUser?.uid
    }.getOrNull()

    /**
     * Upload local Room + preferences snapshot for the signed-in user.
     * @return epoch millis of successful backup, or null on failure
     */
    suspend fun backupNow(): Result<Long> {
        val db = firestore()
            ?: return Result.failure(IllegalStateException("Firebase is not configured."))
        val userId = uid()
            ?: return Result.failure(IllegalStateException("Sign in with Google to back up."))

        return try {
            val snapshot = auraRepository.exportSnapshot()
            val prefsMap = preferences.exportPreferencesMap()
            val now = System.currentTimeMillis()

            val payload = hashMapOf(
                "version" to BACKUP_VERSION,
                "updatedAt" to now,
                "preferences" to prefsMap,
                "tasks" to snapshot.tasks.map { it.toMap() },
                "budgetItems" to snapshot.budgetItems.map { it.toMap() },
                "calendarEvents" to snapshot.calendarEvents.map { it.toMap() },
                "goals" to snapshot.goals.map { it.toMap() },
                "accounts" to snapshot.accounts.map { it.toMap() },
                "dailyActivity" to snapshot.dailyActivity.map { it.toMap() },
                "notes" to snapshot.notes.map { it.toMap() }
            )

            db.collection("users")
                .document(userId)
                .collection("backup")
                .document("latest")
                .set(payload, SetOptions.merge())
                .await()

            // Lightweight meta for account page
            db.collection("users")
                .document(userId)
                .set(
                    mapOf(
                        "lastBackupAt" to now,
                        "email" to (FirebaseAuth.getInstance().currentUser?.email.orEmpty()),
                        "displayName" to (FirebaseAuth.getInstance().currentUser?.displayName.orEmpty())
                    ),
                    SetOptions.merge()
                )
                .await()

            preferences.setLastBackupAt(now)
            Result.success(now)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download cloud snapshot if present and restore into local storage.
     * @return true if a backup was restored, false if none existed
     */
    suspend fun restoreIfAvailable(): Result<Boolean> {
        val db = firestore()
            ?: return Result.failure(IllegalStateException("Firebase is not configured."))
        val userId = uid()
            ?: return Result.failure(IllegalStateException("Sign in with Google to restore."))

        return try {
            val doc = db.collection("users")
                .document(userId)
                .collection("backup")
                .document("latest")
                .get()
                .await()

            if (!doc.exists()) {
                return Result.success(false)
            }

            @Suppress("UNCHECKED_CAST")
            val data = doc.data ?: return Result.success(false)

            val snapshot = AppDataSnapshot(
                tasks = (data["tasks"] as? List<*>)?.mapNotNull { mapToTask(it) }.orEmpty(),
                budgetItems = (data["budgetItems"] as? List<*>)?.mapNotNull { mapToBudget(it) }.orEmpty(),
                calendarEvents = (data["calendarEvents"] as? List<*>)?.mapNotNull { mapToEvent(it) }.orEmpty(),
                goals = (data["goals"] as? List<*>)?.mapNotNull { mapToGoal(it) }.orEmpty(),
                accounts = (data["accounts"] as? List<*>)?.mapNotNull { mapToAccount(it) }.orEmpty(),
                dailyActivity = (data["dailyActivity"] as? List<*>)?.mapNotNull { mapToActivity(it) }.orEmpty(),
                notes = (data["notes"] as? List<*>)?.mapNotNull { mapToNote(it) }.orEmpty()
            )

            auraRepository.importSnapshot(snapshot)

            @Suppress("UNCHECKED_CAST")
            val prefs = data["preferences"] as? Map<String, Any?>
            if (prefs != null) {
                preferences.importPreferencesMap(prefs)
            }

            val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            preferences.setLastBackupAt(updatedAt)

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * After first sign-in: restore cloud data if any; otherwise upload local as the first backup.
     */
    suspend fun syncOnSignIn(): Result<String> {
        return try {
            val restored = restoreIfAvailable().getOrThrow()
            if (restored) {
                Result.success("Restored your data from Google account.")
            } else {
                backupNow().getOrThrow()
                Result.success("Signed in · first cloud backup saved.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val BACKUP_VERSION = 1
    }
}

// ── Entity ↔ Map helpers ────────────────────────────────────────────────────

private fun TaskEntity.toMap() = mapOf(
    "id" to id,
    "title" to title,
    "category" to category,
    "priority" to priority,
    "dueDateMillis" to dueDateMillis,
    "dueTimeStr" to dueTimeStr,
    "isCompleted" to isCompleted,
    "completedAtMillis" to completedAtMillis,
    "streakCount" to streakCount,
    "subtasks" to subtasks,
    "completedSubtasks" to completedSubtasks,
    "linkedGoalId" to linkedGoalId,
    "xpReward" to xpReward
)

private fun BudgetItemEntity.toMap() = mapOf(
    "id" to id,
    "title" to title,
    "amount" to amount,
    "isExpense" to isExpense,
    "category" to category,
    "timestamp" to timestamp,
    "note" to note,
    "accountId" to accountId,
    "transactionType" to transactionType,
    "relatedAccountId" to relatedAccountId
)

private fun CalendarEventEntity.toMap() = mapOf(
    "id" to id,
    "title" to title,
    "description" to description,
    "category" to category,
    "dateMillis" to dateMillis,
    "timeSlot" to timeSlot,
    "startMillis" to startMillis,
    "isCompleted" to isCompleted
)

private fun GoalEntity.toMap() = mapOf(
    "id" to id,
    "title" to title,
    "category" to category,
    "targetAmount" to targetAmount,
    "currentAmount" to currentAmount,
    "unit" to unit,
    "deadlineStr" to deadlineStr,
    "colorHex" to colorHex,
    "isCompleted" to isCompleted
)

private fun AccountEntity.toMap() = mapOf(
    "id" to id,
    "name" to name,
    "type" to type,
    "balance" to balance,
    "creditLimit" to creditLimit,
    "monthlyUsage" to monthlyUsage,
    "currencyCode" to currencyCode,
    "colorHex" to colorHex,
    "isPrimary" to isPrimary,
    "notes" to notes,
    "createdAt" to createdAt
)

private fun DailyActivityEntity.toMap() = mapOf(
    "dateKey" to dateKey,
    "completedCount" to completedCount,
    "xpEarned" to xpEarned
)

private fun NoteEntity.toMap() = mapOf(
    "id" to id,
    "content" to content,
    "colorHex" to colorHex,
    "isPinned" to isPinned,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

@Suppress("UNCHECKED_CAST")
private fun mapToTask(raw: Any?): TaskEntity? {
    val m = raw as? Map<String, Any?> ?: return null
    return TaskEntity(
        id = (m["id"] as? Number)?.toInt() ?: 0,
        title = m["title"] as? String ?: return null,
        category = m["category"] as? String ?: "Other",
        priority = m["priority"] as? String ?: "CORE_GOAL",
        dueDateMillis = (m["dueDateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        dueTimeStr = m["dueTimeStr"] as? String ?: "",
        isCompleted = m["isCompleted"] as? Boolean ?: false,
        completedAtMillis = (m["completedAtMillis"] as? Number)?.toLong(),
        streakCount = (m["streakCount"] as? Number)?.toInt() ?: 1,
        subtasks = m["subtasks"] as? String ?: "",
        completedSubtasks = m["completedSubtasks"] as? String ?: "",
        linkedGoalId = (m["linkedGoalId"] as? Number)?.toInt(),
        xpReward = (m["xpReward"] as? Number)?.toInt() ?: 20
    )
}

@Suppress("UNCHECKED_CAST")
private fun mapToBudget(raw: Any?): BudgetItemEntity? {
    val m = raw as? Map<String, Any?> ?: return null
    val isExpense = m["isExpense"] as? Boolean ?: true
    return BudgetItemEntity(
        id = (m["id"] as? Number)?.toInt() ?: 0,
        title = m["title"] as? String ?: return null,
        amount = (m["amount"] as? Number)?.toDouble() ?: 0.0,
        isExpense = isExpense,
        category = m["category"] as? String ?: "Other",
        timestamp = (m["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        note = m["note"] as? String ?: "",
        accountId = (m["accountId"] as? Number)?.toInt(),
        transactionType = m["transactionType"] as? String
            ?: if (isExpense) TransactionType.EXPENSE.name else TransactionType.INCOME.name,
        relatedAccountId = (m["relatedAccountId"] as? Number)?.toInt()
    )
}

@Suppress("UNCHECKED_CAST")
private fun mapToEvent(raw: Any?): CalendarEventEntity? {
    val m = raw as? Map<String, Any?> ?: return null
    return CalendarEventEntity(
        id = (m["id"] as? Number)?.toInt() ?: 0,
        title = m["title"] as? String ?: return null,
        description = m["description"] as? String ?: "",
        category = m["category"] as? String ?: "Other",
        dateMillis = (m["dateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        timeSlot = m["timeSlot"] as? String ?: "",
        startMillis = (m["startMillis"] as? Number)?.toLong() ?: 0L,
        isCompleted = m["isCompleted"] as? Boolean ?: false
    )
}

@Suppress("UNCHECKED_CAST")
private fun mapToGoal(raw: Any?): GoalEntity? {
    val m = raw as? Map<String, Any?> ?: return null
    return GoalEntity(
        id = (m["id"] as? Number)?.toInt() ?: 0,
        title = m["title"] as? String ?: return null,
        category = m["category"] as? String ?: "Other",
        targetAmount = (m["targetAmount"] as? Number)?.toDouble() ?: 0.0,
        currentAmount = (m["currentAmount"] as? Number)?.toDouble() ?: 0.0,
        unit = m["unit"] as? String ?: "$",
        deadlineStr = m["deadlineStr"] as? String ?: "",
        colorHex = m["colorHex"] as? String ?: "#A78BFA",
        isCompleted = m["isCompleted"] as? Boolean ?: false
    )
}

@Suppress("UNCHECKED_CAST")
private fun mapToAccount(raw: Any?): AccountEntity? {
    val m = raw as? Map<String, Any?> ?: return null
    return AccountEntity(
        id = (m["id"] as? Number)?.toInt() ?: 0,
        name = m["name"] as? String ?: return null,
        type = m["type"] as? String ?: AccountType.BANK.name,
        balance = (m["balance"] as? Number)?.toDouble() ?: 0.0,
        creditLimit = (m["creditLimit"] as? Number)?.toDouble() ?: 0.0,
        monthlyUsage = (m["monthlyUsage"] as? Number)?.toDouble() ?: 0.0,
        currencyCode = m["currencyCode"] as? String ?: "USD",
        colorHex = m["colorHex"] as? String ?: "#7C3AED",
        isPrimary = m["isPrimary"] as? Boolean ?: false,
        notes = m["notes"] as? String ?: "",
        createdAt = (m["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
}

@Suppress("UNCHECKED_CAST")
private fun mapToActivity(raw: Any?): DailyActivityEntity? {
    val m = raw as? Map<String, Any?> ?: return null
    val key = m["dateKey"] as? String ?: return null
    return DailyActivityEntity(
        dateKey = key,
        completedCount = (m["completedCount"] as? Number)?.toInt() ?: 0,
        xpEarned = (m["xpEarned"] as? Number)?.toInt() ?: 0
    )
}

@Suppress("UNCHECKED_CAST")
private fun mapToNote(raw: Any?): NoteEntity? {
    val m = raw as? Map<String, Any?> ?: return null
    return NoteEntity(
        id = (m["id"] as? Number)?.toInt() ?: 0,
        content = m["content"] as? String ?: return null,
        colorHex = m["colorHex"] as? String ?: "#7C3AED",
        isPinned = m["isPinned"] as? Boolean ?: false,
        createdAt = (m["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
}
