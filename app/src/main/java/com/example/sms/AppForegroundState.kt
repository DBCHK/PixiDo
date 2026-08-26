package com.example.sms

/**
 * Process-wide flag so [SmsReceiver] can skip the system tray when PixiDo is
 * already on screen (the in-app banner is shown instead).
 */
object AppForegroundState {
    @Volatile
    var isResumed: Boolean = false
}
