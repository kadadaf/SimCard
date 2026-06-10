package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistoryRecord
import com.example.data.KeepAliveRule
import com.example.data.SimCard
import java.text.SimpleDateFormat
import java.util.*
import com.example.R

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    sim: SimCard,
    rules: List<KeepAliveRule>,
    history: List<HistoryRecord>,
    onNavigateBack: () -> Unit,
    onDeleteSim: () -> Unit,
    onRecordAction: (
        simId: Int,
        ruleId: Int,
        actionType: String,
        amount: Double,
        description: String
    ) -> Unit,
    onAddCustomRule: (KeepAliveRule) -> Unit,
    onDeleteCustomRule: (KeepAliveRule) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    var activeTab by remember { mutableStateOf("Rules") } // "Rules" or "History"

    val isZh = remember { Locale.getDefault().language == "zh" }

    // Dialog state for recording keeping-alive action
    var showActionDialog by remember { mutableStateOf(false) }
    var activeRuleForAction by remember { mutableStateOf<KeepAliveRule?>(null) }

    // Dialog state for custom rule creation
    var showCustomRuleDialog by remember { mutableStateOf(false) }

    // Dialog state for confirming deletion
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isZh) "SIM卡 存储详情" else "SIM Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete SIM", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            
            // SIM Quick Metadata Overview CARD
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sim.phoneNumber,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        val displayType = if (isZh) {
                            when (sim.cardType) {
                                "Physical SIM" -> "实体卡 SIM"
                                "eSIM" -> "电子卡 eSIM"
                                else -> sim.cardType
                            }
                        } else sim.cardType
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = displayType,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Operator
                        Icon(Icons.Default.CellTower, contentDescription = "Carrier", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = sim.operator,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Current Reference Balance
                        val balanceLabel = if (isZh) "预计余额:" else "Balance:"
                        Text(
                            text = "$balanceLabel ${sim.currency} ${String.format(Locale.getDefault(), "%.2f", sim.balance)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (sim.planDetails.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val planLabel = if (isZh) "预设套餐: " else "Plan: "
                        Text(
                            text = "$planLabel${sim.planDetails}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    if (sim.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        val notesLabel = if (isZh) "物理/PIN码备注: " else "Notes: "
                        Text(
                            text = "$notesLabel${sim.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation selector tab bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val rulesTabTitle = if (isZh) "到期保活闹钟 (${rules.size})" else "Keeping-Alive Rules (${rules.size})"
                ElevatedFilterChip(
                    selected = activeTab == "Rules",
                    onClick = { activeTab = "Rules" },
                    label = { Text(rulesTabTitle) },
                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = "Rules") }
                )

                val historyTabTitle = if (isZh) "历史履约日志 (${history.size})" else "History logs (${history.size})"
                ElevatedFilterChip(
                    selected = activeTab == "History",
                    onClick = { activeTab = "History" },
                    label = { Text(historyTabTitle) },
                    leadingIcon = { Icon(Icons.Default.History, contentDescription = "History") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body content according to Tab selected
            if (activeTab == "Rules") {
                // RULES TAB
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rulesTableHeading = if (isZh) "生命到期保活日程针" else "Active Expiration Rules"
                    Text(
                        text = rulesTableHeading,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val customRuleLabel = if (isZh) "创建自定保活报警" else "Add Custom Rule"
                    TextButton(onClick = { showCustomRuleDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add custom rule", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(customRuleLabel)
                    }
                }

                if (rules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        val noRulesText = if (isZh) "当前未配置任何自动或到期保活规则。" else "No active keeping-alive rules configured."
                        Text(
                            text = noRulesText,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(rules, key = { rule -> rule.id }) { rule ->
                            RuleListItem(
                                rule = rule,
                                simCurrency = sim.currency,
                                onActionClick = {
                                    activeRuleForAction = rule
                                    showActionDialog = true
                                },
                                onDeleteClick = { onDeleteCustomRule(rule) }
                            )
                        }
                    }
                }

            } else {
                // HISTORY TAB
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        val noHistoryText = if (isZh) "当前无任何履约及停用警报发出。" else "No historical logs or alarms recorded."
                        Text(
                            text = noHistoryText,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(history, key = { record -> record.id }) { record ->
                            HistoryItem(record = record, currencyStr = sim.currency)
                        }
                    }
                }
            }
        }
    }

    // Action dialog: trigger local keeper renewal
    if (showActionDialog && activeRuleForAction != null) {
        val rule = activeRuleForAction!!
        
        var actionTypeSelected by remember { mutableStateOf(rule.requiredAction.split("/").firstOrNull()?.trim() ?: "SMS") }
        var amountSpentInput by remember { mutableStateOf("") }
        var logDetailsInput by remember { mutableStateOf(if (isZh) "通过应用清单手动触发并重置了物理卡保活周期。" else "Preemptively triggered keeping-alive activity via checklist.") }

        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = { Text(if (isZh) "登记保活履约履踪" else "Record Keeper Activity", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    val promptText = if (isZh) {
                        "请在此登记您对该联络号物理卡做过的一笔动作。这会将其重置倒数至下一满期周期。"
                    } else {
                        "This registers a keep-alive action, resetting your rule cycle timer to today."
                    }
                    Text(promptText)

                    Text(
                        text = (if (isZh) "定位保活闹钟: " else "Selected Rule: ") + rule.ruleName,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Action type chooser
                    Column {
                        val behaviorLabel = if (isZh) "进行的操作类型" else "Action Action Type"
                        Text(behaviorLabel, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        val availableActions = listOf("SMS", "Call", "Cellular Data", "Top-up", "Balance Change")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            availableActions.forEach { item ->
                                val chipDisplay = if (isZh) {
                                    when (item) {
                                        "SMS" -> "发送短信"
                                        "Call" -> "拨打电话"
                                        "Cellular Data" -> "消耗上网数据"
                                        "Top-up" -> "话费充值"
                                        "Balance Change" -> "余额发生扣减"
                                        else -> item
                                    }
                                } else item
                                FilterChip(
                                    selected = actionTypeSelected == item,
                                    onClick = { actionTypeSelected = item },
                                    label = { Text(chipDisplay) }
                                )
                            }
                        }
                    }

                    // Charge incurred
                    val financialIncurred = if (isZh) "产生的扣款费用 (${sim.currency})" else "Financial Cost Incurred (${sim.currency})"
                    OutlinedTextField(
                        value = amountSpentInput,
                        onValueChange = { amountSpentInput = it },
                        label = { Text(financialIncurred) },
                        placeholder = { Text("0.10") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    // Description details
                    val logDetailsLabel = if (isZh) "此操作文字履约备注" else "Activity Description Logs"
                    OutlinedTextField(
                        value = logDetailsInput,
                        onValueChange = { logDetailsInput = it },
                        label = { Text(logDetailsLabel) },
                        placeholder = { Text(if (isZh) "例如：已给运营商发送保活短信。" else "e.g. Sent hello sms to support number.") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cost = amountSpentInput.toDoubleOrNull() ?: 0.0
                        onRecordAction(
                            sim.id,
                            rule.id,
                            actionTypeSelected,
                            cost,
                            logDetailsInput.trim()
                        )
                        showActionDialog = false
                    }
                ) {
                    Text(if (isZh) "确认此笔履约" else "Confirm Active Activity")
                }
            },
            dismissButton = {
                TextButton(onClick = { showActionDialog = false }) {
                    Text(if (isZh) "取消" else "Cancel")
                }
            }
        )
    }

    // Modal custom keeping-alive rule setup
    if (showCustomRuleDialog) {
        var ruleNameInput by remember { mutableStateOf(if (isZh) "自定卡片保活预警" else "My Custom Safety Checking") }
        var cycleDaysInput by remember { mutableStateOf("180") }
        var warn1Input by remember { mutableStateOf("30") }
        var warn2Input by remember { mutableStateOf("5") }
        var requiredActionInput by remember { mutableStateOf(if (isZh) "发送短信或拨挂电话" else "Send SMS / Dial number") }
        var notesInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCustomRuleDialog = false },
            title = { Text(if (isZh) "新建自定保活预警闹钟" else "Add Custom Keeping-Alive Rule", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = ruleNameInput,
                        onValueChange = { ruleNameInput = it },
                        label = { Text(if (isZh) "保活闹钟显示名称" else "Rule Display Name") },
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cycleDaysInput,
                            onValueChange = { cycleDaysInput = it },
                            label = { Text(if (isZh) "规定周期天数" else "Cycle Days") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = requiredActionInput,
                            onValueChange = { requiredActionInput = it },
                            label = { Text(if (isZh) "所需履约行为" else "Behavior Code Needed") },
                            placeholder = { Text("e.g. Call") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = warn1Input,
                            onValueChange = { warn1Input = it },
                            label = { Text(if (isZh) "一级报警天数" else "Warn 1 Advance") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = warn2Input,
                            onValueChange = { warn2Input = it },
                            label = { Text(if (isZh) "二级极度危险预警" else "Warn 2 Advance") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text(if (isZh) "如何操作或保活要领技巧" else "Rule Details / Operator guidelines") },
                        placeholder = { Text(if (isZh) "写明如何安全低成本进行，例如: 向任何普通手机代号发一短信变动..." else "Explain how to solve or perform active behavior...") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cycle = cycleDaysInput.toIntOrNull() ?: 180
                        val pr = warn1Input.toIntOrNull() ?: 30
                        val sec = warn2Input.toIntOrNull() ?: 5
                        val newRule = KeepAliveRule(
                            simId = sim.id,
                            ruleName = ruleNameInput.trim(),
                            isEnabled = true,
                            periodDays = cycle,
                            warningDaysAdvancePrimary = pr,
                            warningDaysAdvanceSecondary = sec,
                            requiredAction = requiredActionInput.trim(),
                            lastActiveDate = System.currentTimeMillis(),
                            isPredefined = false,
                            notes = notesInput.trim()
                        )
                        onAddCustomRule(newRule)
                        showCustomRuleDialog = false
                    }
                ) {
                    Text(if (isZh) "增加至我的闹钟" else "Add Rule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRuleDialog = false }) {
                    Text(if (isZh) "取消" else "Cancel")
                }
            }
        )
    }

    // SIM card deletion confirming Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(if (isZh) "抹除卡片及所有数据？" else "Delete SIM Card & Erase Data?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text(if (isZh) "这将永久在该受控沙盒中抹除号码“${sim.phoneNumber}”，连同其名下的历史消费流水、自动闹钟与安全规则，删除后将再也无法恢复。" else "This will permanently delete phone number '${sim.phoneNumber}' from SIM Keeper, together with its historical check alerts, records, and active tracking rules. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteSim()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isZh) "永久抹除一切" else "Erase Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(if (isZh) "取消" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun RuleListItem(
    rule: KeepAliveRule,
    simCurrency: String,
    onActionClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val remainingDays = calculateRemainingDays(rule)
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val isZh = remember { Locale.getDefault().language == "zh" }

    val statusColor = when {
        remainingDays <= 0 -> MaterialTheme.colorScheme.error
        remainingDays <= rule.warningDaysAdvanceSecondary -> Color(0xFFD32F2F)
        remainingDays <= rule.warningDaysAdvancePrimary -> Color(0xFFFF9800)
        else -> Color(0xFF388E3C)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.ruleName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (rule.isPredefined) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = if (isZh) "运营商推荐规则" else "System Template",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                if (!rule.isPredefined) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Delete Rule", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Days details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(if (isZh) "距离到期日剩余" else "Days Remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    val countdownStr = if (remainingDays <= 0) {
                        if (isZh) "已超期！极危！" else "EXPIRED RISK"
                    } else {
                        if (isZh) "${remainingDays} 天" else "$remainingDays Days Left"
                    }
                    Text(
                        text = countdownStr,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = statusColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(if (isZh) "活跃周期保障" else "Active Cycle", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(text = if (isZh) "每 ${rule.periodDays} 天保活" else "${rule.periodDays} Days Interval", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Last Check date info
            Text(
                text = (if (isZh) "卡片最近一次触发活跃日期： " else "Last Keep-Alive Action recorded at: ") + formatter.format(Date(rule.lastActiveDate)),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (rule.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = rule.notes,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Task,
                        contentDescription = "Required Behavior",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    val reqDisplay = if (isZh) {
                        when (rule.requiredAction) {
                            "SMS" -> "发送一条活体短信"
                            "Call" -> "拨打一个正常电话"
                            "Cellular Data" -> "触发任意网络数据"
                            "Top-up" -> "话费额值充值"
                            "Balance Change" -> "任何使余额产生减少"
                            else -> rule.requiredAction
                        }
                    } else rule.requiredAction
                    Text(
                        text = (if (isZh) "卡主要求： " else "Behavior Required: ") + reqDisplay,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = statusColor, contentColor = Color.White),
                    modifier = Modifier.testTag("verify_action_button_${rule.id}")
                ) {
                    Icon(Icons.Default.Done, contentDescription = "Perform action icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isZh) "我已完成该操作" else "Perform Action")
                }
            }
        }
    }
}

@Composable
fun HistoryItem(record: HistoryRecord, currencyStr: String) {
    val dateStr = remember(record.timestamp) {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        format.format(Date(record.timestamp))
    }
    val isZh = remember { Locale.getDefault().language == "zh" }

    val iconValue = when (record.type) {
        "Activation" -> Icons.Default.AddCard
        "Alert Sent" -> Icons.Default.WarningAmber
        "Rule Activated" -> Icons.Default.AutoMode
        "SMS" -> Icons.Outlined.Sms
        "Call" -> Icons.Outlined.PhoneEnabled
        "Top-up" -> Icons.Default.Payments
        "Cellular Data" -> Icons.Default.LeakAdd
        else -> Icons.Default.ReceiptLong
    }

    val iconColor = when (record.type) {
        "Alert Sent" -> MaterialTheme.colorScheme.error
        "Top-up" -> MaterialTheme.colorScheme.primary
        "Activation" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = iconValue, contentDescription = "Log type info", tint = iconColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val recordTypeLabel = if (isZh) {
                        when (record.type) {
                            "Activation" -> "档案创建/激活卡片"
                            "Alert Sent" -> "停机警报发出"
                            "Rule Activated" -> "保活规则生效"
                            "SMS" -> "执行发送短信保活"
                            "Call" -> "执行呼叫电话保活"
                            "Top-up" -> "执行账户充值"
                            "Cellular Data" -> "执行数据上网保活"
                            else -> record.type
                        }
                    } else record.type

                    Text(
                        text = recordTypeLabel,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = record.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
