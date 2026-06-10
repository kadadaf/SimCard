package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.HistoryRecord
import com.example.data.SimRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SimKeepAliveReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SimKeepAliveReceiver", "Checking SIM card keep-alive statuses...")

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val repository = SimRepository(
                    database.simDao(),
                    database.keepAliveRuleDao(),
                    database.historyRecordDao()
                )

                val activeRules = repository.getActiveRules()
                val currentTime = System.currentTimeMillis()

                for (rule in activeRules) {
                    val sim = repository.getSimById(rule.simId) ?: continue
                    
                    val timeElapsed = currentTime - rule.lastActiveDate
                    val daysElapsed = TimeUnit.MILLISECONDS.toDays(timeElapsed).toInt()
                    val daysRemaining = rule.periodDays - daysElapsed

                    val titleText = "SIM Card Reminder: ${sim.phoneNumber} (${sim.operator})"
                    var alertMessage = ""
                    var isWarning = false

                    if (daysRemaining <= 0) {
                        alertMessage = "CRITICAL: Keeping-alive rule '${rule.ruleName}' is EXPIRED by ${-daysRemaining} days! Perform active action to preserve your SIM card."
                        isWarning = true
                    } else if (daysRemaining <= rule.warningDaysAdvanceSecondary) {
                        alertMessage = "URGENT Check: Only $daysRemaining days left to perform keeper activities for '${rule.ruleName}'."
                        isWarning = true
                    } else if (daysRemaining <= rule.warningDaysAdvancePrimary) {
                        alertMessage = "Reminder: $daysRemaining days left to check keep-alive status of '${rule.ruleName}'."
                        isWarning = true
                    }

                    if (isWarning) {
                        // Check if we've already logged an alert for this rule within the last 24 hours
                        val histories = repository.getHistoryForSimList(rule.simId)
                        val lastAlert24h = histories.any {
                            it.type == "Alert Sent" && 
                            it.description.contains(rule.ruleName) && 
                            (currentTime - it.timestamp) < TimeUnit.DAYS.toMillis(1)
                        }

                        if (!lastAlert24h) {
                            // Show Notification
                            val notificationId = rule.id + 1000
                            NotificationHelper.showNotification(
                                context,
                                notificationId,
                                titleText,
                                alertMessage,
                                simId = sim.id
                            )

                            // Save to database archive
                            repository.insertHistoryRecord(
                                HistoryRecord(
                                    simId = sim.id,
                                    type = "Alert Sent",
                                    description = alertMessage,
                                    timestamp = currentTime
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SimKeepAliveReceiver", "Error checking active rules", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun schedulePeriodicCheck(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SimKeepAliveReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Trigger every 12 hours to verify statuses
            val interval = AlarmManager.INTERVAL_HALF_DAY
            val triggerAt = SystemClock.elapsedRealtime() + 10000 // In 10 seconds initially

            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                interval,
                pendingIntent
            )
            Log.d("SimKeepAliveReceiver", "Periodic active checks scheduled.")
        }
    }
}
