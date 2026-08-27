package com.example.sms

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans the device SMS inbox for recent bank / UPI messages that were missed
 * (e.g. permission granted after the SMS arrived, or receiver not yet active).
 */
object SmsInboxScanner {

    private const val TAG = "PixiDoSmsScan"
    private const val LOOKBACK_MS = 48L * 60 * 60 * 1000
    private const val MAX_ROWS = 80

    fun hasReadPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasReceivePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun scanAndQueue(context: Context): Int = withContext(Dispatchers.IO) {
        if (!hasReadPermission(context)) return@withContext 0

        val appContext = context.applicationContext
        val since = System.currentTimeMillis() - LOOKBACK_MS
        var added = 0

        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(since.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        var cursor: Cursor? = null
        try {
            cursor = appContext.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            if (cursor == null) return@withContext 0

            val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
            var rows = 0

            while (cursor.moveToNext() && rows < MAX_ROWS) {
                rows++
                val sender = if (addressIdx >= 0) cursor.getString(addressIdx).orEmpty() else ""
                val body = if (bodyIdx >= 0) cursor.getString(bodyIdx).orEmpty() else ""
                val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                if (SmsImportStore.ingest(
                        context = appContext,
                        body = body,
                        sender = sender,
                        timestamp = date,
                        notifyIfBackground = false
                    )
                ) {
                    added++
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "READ_SMS denied", e)
        } catch (e: Exception) {
            Log.e(TAG, "Inbox scan failed", e)
        } finally {
            cursor?.close()
        }

        SmsImportStore.collapsePending(appContext)
        if (added > 0) Log.d(TAG, "Queued $added SMS transaction(s) from inbox")
        added
    }
}
