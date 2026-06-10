package com.example.ui

import android.content.Context
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SimViewModel(
    application: Application,
    private val repository: SimRepository
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("sim_keeper_prefs", Context.MODE_PRIVATE)

    private val _appLanguage = MutableStateFlow(sharedPrefs.getString("app_language", "system") ?: "system")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun setAppLanguage(lang: String) {
        sharedPrefs.edit().putString("app_language", lang).apply()
        _appLanguage.value = lang
    }

    // Observe active lists from repository
    val simCards: StateFlow<List<SimCard>> = repository.allSimCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rules: StateFlow<List<KeepAliveRule>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryRecord>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Local state indicators
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Creates a new SIM card and automatically attaches predefined rules
     * if the operator matches templates (e.g., Giffgaff, CTExcel).
     */
    fun addSimCard(
        phoneNumber: String,
        operator: String,
        cardType: String,
        activationDate: Long,
        planDetails: String,
        balance: Double,
        currency: String,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                val newSim = SimCard(
                    phoneNumber = phoneNumber,
                    operator = operator,
                    cardType = cardType,
                    activationDate = activationDate,
                    planDetails = planDetails,
                    balance = balance,
                    currency = currency,
                    notes = notes
                )
                val simId = repository.insertSim(newSim).toInt()
                
                // Add first action log
                repository.insertHistoryRecord(
                    HistoryRecord(
                        simId = simId,
                        type = "Activation",
                        description = "SIM Card activated and enrolled in SIM Keeper."
                    )
                )

                // Attach predefined template rules based on selected operator
                repository.attachPredefinedRules(simId, operator, activationDate)
                _statusMessage.value = "SIM Card added successfully with keeping-alive rules!"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to add SIM Card: ${e.message}"
            }
        }
    }

    /**
     * Updates SIM card metadata.
     */
    fun updateSimCard(sim: SimCard) {
        viewModelScope.launch {
            try {
                repository.updateSim(sim)
                _statusMessage.value = "SIM card updated."
            } catch (e: Exception) {
                _statusMessage.value = "Failed to update SIM card: ${e.message}"
            }
        }
    }

    /**
     * Deletes a SIM card and all associated keep alive rules & history records.
     */
    fun deleteSimCard(sim: SimCard) {
        viewModelScope.launch {
            try {
                repository.deleteSim(sim)
                _statusMessage.value = "SIM Card and associated rules/records deleted."
            } catch (e: Exception) {
                _statusMessage.value = "Failed to delete SIM: ${e.message}"
            }
        }
    }

    /**
     * Inserts/Updates keep alive rules manually.
     */
    fun addOrUpdateRule(rule: KeepAliveRule) {
        viewModelScope.launch {
            try {
                if (rule.id == 0) {
                    repository.insertRule(rule)
                    _statusMessage.value = "Rule created."
                } else {
                    repository.updateRule(rule)
                    _statusMessage.value = "Rule updated."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error saving rule: ${e.message}"
            }
        }
    }

    fun deleteRule(rule: KeepAliveRule) {
        viewModelScope.launch {
            try {
                repository.deleteRule(rule)
                _statusMessage.value = "Keep alive rule deleted."
            } catch (e: Exception) {
                _statusMessage.value = "Error deleting rule: ${e.message}"
            }
        }
    }

    /**
     * Core functional keeper trigger: Executes manual/automatic keep-alive actions like Call, SMS, Topup.
     * This updates the rule's lastActiveDate to 'now' and logs a historical consumption/record event.
     */
    fun performKeepAliveAction(
        simId: Int,
        ruleId: Int,
        actionType: String,
        amountSpent: Double,
        description: String
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                
                // 1. Fetch and update the Rule last active timestamp
                val rule = repository.getRuleById(ruleId)
                if (rule != null) {
                    val updatedRule = rule.copy(lastActiveDate = now)
                    repository.updateRule(updatedRule)
                }

                // 2. Fetch and update SIM Card balance if change occurs
                val sim = repository.getSimById(simId)
                if (sim != null && amountSpent > 0.0) {
                    val updatedSim = sim.copy(balance = (sim.balance - amountSpent).coerceAtLeast(0.0))
                    repository.updateSim(updatedSim)
                }

                // 3. Insert History record for logs
                val actionDesc = if (amountSpent > 0.0) {
                    "$description - Cost: ${sim?.currency ?: "GBP"} ${String.format(Locale.getDefault(), "%.2f", amountSpent)}"
                } else {
                    description
                }

                repository.insertHistoryRecord(
                    HistoryRecord(
                        simId = simId,
                        type = actionType,
                        amount = amountSpent,
                        timestamp = now,
                        description = actionDesc
                    )
                )

                _statusMessage.value = "Keep-alive action recorded! Rule reset successfully."
            } catch (e: Exception) {
                _statusMessage.value = "Failed to record keeper action: ${e.message}"
            }
        }
    }

    /**
     * Exports entire database (SIM Cards, Rules, History) into a compact JSON string.
     */
    fun exportBackup(): String {
        return try {
            val root = JSONObject()
            
            // Convert SIM Cards
            val simsArray = JSONArray()
            simCards.value.forEach { sim ->
                val obj = JSONObject().apply {
                    put("id", sim.id)
                    put("phoneNumber", sim.phoneNumber)
                    put("operator", sim.operator)
                    put("cardType", sim.cardType)
                    put("activationDate", sim.activationDate)
                    put("planDetails", sim.planDetails)
                    put("balance", sim.balance)
                    put("currency", sim.currency)
                    put("notes", sim.notes)
                    put("isActive", sim.isActive)
                }
                simsArray.put(obj)
            }
            root.put("sims", simsArray)

            // Convert Rules
            val rulesArray = JSONArray()
            rules.value.forEach { r ->
                val obj = JSONObject().apply {
                    put("id", r.id)
                    put("simId", r.simId)
                    put("ruleName", r.ruleName)
                    put("isEnabled", r.isEnabled)
                    put("periodDays", r.periodDays)
                    put("warningDaysAdvancePrimary", r.warningDaysAdvancePrimary)
                    put("warningDaysAdvanceSecondary", r.warningDaysAdvanceSecondary)
                    put("minimumSpend", r.minimumSpend)
                    put("requiredAction", r.requiredAction)
                    put("lastActiveDate", r.lastActiveDate)
                    put("isPredefined", r.isPredefined)
                    put("notes", r.notes)
                }
                rulesArray.put(obj)
            }
            root.put("rules", rulesArray)

            // Convert Histories
            val historyArray = JSONArray()
            history.value.forEach { h ->
                val obj = JSONObject().apply {
                    put("id", h.id)
                    put("simId", h.simId)
                    put("type", h.type)
                    put("amount", h.amount)
                    put("timestamp", h.timestamp)
                    put("description", h.description)
                }
                historyArray.put(obj)
            }
            root.put("history", historyArray)

            root.toString(2)
        } catch (e: Exception) {
            Log.e("SimViewModel", "Error exporting database", e)
            ""
        }
    }

    /**
     * Imports from JSON string backup, wiping current records first or updating.
     */
    fun importBackup(jsonString: String) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonString)
                val sims = root.optJSONArray("sims")
                val rules = root.optJSONArray("rules")
                val history = root.optJSONArray("history")

                if (sims == null) {
                    _statusMessage.value = "Invalid backup format: Missing SIMs array"
                    return@launch
                }

                // Clean existing tables first
                simCards.value.forEach { repository.deleteSim(it) }

                // Import SIM Cards
                val oldToNewIdMap = mutableMapOf<Int, Int>()
                for (i in 0 until sims.length()) {
                    val simObj = sims.getJSONObject(i)
                    val oldId = simObj.getInt("id")
                    val sim = SimCard(
                        phoneNumber = simObj.getString("phoneNumber"),
                        operator = simObj.getString("operator"),
                        cardType = simObj.getString("cardType"),
                        activationDate = simObj.getLong("activationDate"),
                        planDetails = simObj.optString("planDetails", ""),
                        balance = simObj.optDouble("balance", 0.0),
                        currency = simObj.optString("currency", "GBP"),
                        notes = simObj.optString("notes", ""),
                        isActive = simObj.optBoolean("isActive", true)
                    )
                    val newId = repository.insertSim(sim).toInt()
                    oldToNewIdMap[oldId] = newId
                }

                // Import Rules
                if (rules != null) {
                    for (i in 0 until rules.length()) {
                        val ruleObj = rules.getJSONObject(i)
                        val oldSimId = ruleObj.getInt("simId")
                        val newSimId = oldToNewIdMap[oldSimId] ?: continue // skip if SIM no longer matches

                        val rule = KeepAliveRule(
                            simId = newSimId,
                            ruleName = ruleObj.getString("ruleName"),
                            isEnabled = ruleObj.optBoolean("isEnabled", true),
                            periodDays = ruleObj.optInt("periodDays", 180),
                            warningDaysAdvancePrimary = ruleObj.optInt("warningDaysAdvancePrimary", 30),
                            warningDaysAdvanceSecondary = ruleObj.optInt("warningDaysAdvanceSecondary", 5),
                            minimumSpend = ruleObj.optDouble("minimumSpend", 0.0),
                            requiredAction = ruleObj.optString("requiredAction", "Any Activity"),
                            lastActiveDate = ruleObj.optLong("lastActiveDate", System.currentTimeMillis()),
                            isPredefined = ruleObj.optBoolean("isPredefined", false),
                            notes = ruleObj.optString("notes", "")
                        )
                        repository.insertRule(rule)
                    }
                }

                // Import History records
                if (history != null) {
                    for (i in 0 until history.length()) {
                        val histObj = history.getJSONObject(i)
                        val oldSimId = histObj.getInt("simId")
                        val newSimId = oldToNewIdMap[oldSimId] ?: continue

                        val record = HistoryRecord(
                            simId = newSimId,
                            type = histObj.getString("type"),
                            amount = histObj.optDouble("amount", 0.0),
                            timestamp = histObj.getLong("timestamp"),
                            description = histObj.getString("description")
                        )
                        repository.insertHistoryRecord(record)
                    }
                }

                _statusMessage.value = "Backup data restored successfully!"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to restore backup: ${e.message}"
                Log.e("SimViewModel", "Error importing backup", e)
            }
        }
    }
}

/**
 * ViewModel Factory helper.
 */
class SimViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SimViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            val repository = SimRepository(
                database.simDao(),
                database.keepAliveRuleDao(),
                database.historyRecordDao()
            )
            return SimViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
