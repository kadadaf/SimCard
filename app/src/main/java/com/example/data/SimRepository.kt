package com.example.data

import kotlinx.coroutines.flow.Flow

class SimRepository(
    private val simDao: SimDao,
    private val keepAliveRuleDao: KeepAliveRuleDao,
    private val historyRecordDao: HistoryRecordDao
) {
    val allSimCards: Flow<List<SimCard>> = simDao.getAllSimCards()
    val allRules: Flow<List<KeepAliveRule>> = keepAliveRuleDao.getAllRules()
    val allHistory: Flow<List<HistoryRecord>> = historyRecordDao.getAllHistory()

    suspend fun getSimById(id: Int): SimCard? = simDao.getSimCardById(id)
    fun getSimByIdFlow(id: Int): Flow<SimCard?> = simDao.getSimCardByIdFlow(id)

    suspend fun insertSim(simCard: SimCard): Long {
        return simDao.insertSimCard(simCard)
    }

    suspend fun updateSim(simCard: SimCard) {
        simDao.updateSimCard(simCard)
    }

    suspend fun deleteSim(simCard: SimCard) {
        // Cascade delete child rules & histories
        keepAliveRuleDao.deleteRulesBySimId(simCard.id)
        historyRecordDao.deleteHistoryBySimId(simCard.id)
        simDao.deleteSimCard(simCard)
    }

    fun getRulesForSim(simId: Int): Flow<List<KeepAliveRule>> = keepAliveRuleDao.getRulesForSim(simId)
    suspend fun getRulesForSimList(simId: Int): List<KeepAliveRule> = keepAliveRuleDao.getRulesForSimList(simId)
    suspend fun getActiveRules(): List<KeepAliveRule> = keepAliveRuleDao.getActiveRules()
    suspend fun getRuleById(id: Int): KeepAliveRule? = keepAliveRuleDao.getRuleById(id)

    suspend fun insertRule(rule: KeepAliveRule): Long {
        return keepAliveRuleDao.insertRule(rule)
    }

    suspend fun updateRule(rule: KeepAliveRule) {
        keepAliveRuleDao.updateRule(rule)
    }

    suspend fun deleteRule(rule: KeepAliveRule) {
        keepAliveRuleDao.deleteRule(rule)
    }

    fun getHistoryForSim(simId: Int): Flow<List<HistoryRecord>> = historyRecordDao.getHistoryForSim(simId)
    suspend fun getHistoryForSimList(simId: Int): List<HistoryRecord> = historyRecordDao.getHistoryForSimList(simId)

    suspend fun insertHistoryRecord(record: HistoryRecord): Long {
        return historyRecordDao.insertRecord(record)
    }

    suspend fun deleteHistoryRecord(record: HistoryRecord) {
        historyRecordDao.deleteRecord(record)
    }

    /**
     * Set up default keep-alive templates based on the operator selected.
     */
    suspend fun attachPredefinedRules(simId: Int, operator: String, activationDate: Long) {
        val normalizedOperator = operator.trim().lowercase()
        if (normalizedOperator.contains("giffgaff")) {
            // Predefined 180-day rule for Giffgaff (Phone call, SMS, Mobile data, or top-up required)
            val rule = KeepAliveRule(
                simId = simId,
                ruleName = "Giffgaff 180-Day Keep Alive Plan",
                isEnabled = true,
                periodDays = 180,
                warningDaysAdvancePrimary = 30,
                warningDaysAdvanceSecondary = 5,
                minimumSpend = 0.1, // SMS/call costs some money if no active monthly goodybag
                requiredAction = "SMS / Call / Data / Top-up",
                lastActiveDate = activationDate,
                isPredefined = true,
                notes = "To keep the SIM active, make a call, send an SMS, use cellular data, or top up the balance at least once every 180 days."
            )
            insertRule(rule)
            insertHistoryRecord(
                HistoryRecord(
                    simId = simId,
                    type = "Rule Activated",
                    description = "Giffgaff UK 180-day keep-alive rule attached."
                )
            )
        } else if (normalizedOperator.contains("ctexcel")) {
            // CTExcel UK predefined active requirements
            val rule1 = KeepAliveRule(
                simId = simId,
                ruleName = "CTExcel 180-Day Balance Change",
                isEnabled = true,
                periodDays = 180,
                warningDaysAdvancePrimary = 30,
                warningDaysAdvanceSecondary = 5,
                minimumSpend = 0.01,
                requiredAction = "Balance Change (Charge/Top-up)",
                lastActiveDate = activationDate,
                isPredefined = true,
                notes = "Requires a balance change (usage of balance or top up) within 180 days to keep the SIM card from expiring."
            )
            insertRule(rule1)

            // CTExcel UK new card 10-day usage warning
            val rule2 = KeepAliveRule(
                simId = simId,
                ruleName = "CTExcel New Card 10-Day Activation Use",
                isEnabled = true,
                periodDays = 10,
                warningDaysAdvancePrimary = 3,
                warningDaysAdvanceSecondary = 1,
                requiredAction = "First Top-up/Activation Call",
                lastActiveDate = activationDate,
                isPredefined = true,
                notes = "New SIM status requires active use/top-up within 10 days of activation."
            )
            insertRule(rule2)

            insertHistoryRecord(
                HistoryRecord(
                    simId = simId,
                    type = "Rule Activated",
                    description = "CTExcel UK active balance rule and 10-day safety rules attached."
                )
            )
        } else {
            // Default 180-day keeper helper rule for other operators
            val defaultRule = KeepAliveRule(
                simId = simId,
                ruleName = "$operator Standby Keep-Alive Rule",
                isEnabled = true,
                periodDays = 180,
                warningDaysAdvancePrimary = 30,
                warningDaysAdvanceSecondary = 7,
                requiredAction = "Any Keep-Alive Activity",
                lastActiveDate = activationDate,
                isPredefined = false,
                notes = "Generates reminders for basic SIM preservation every 180 days."
            )
            insertRule(defaultRule)
        }
    }
}
