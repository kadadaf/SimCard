package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KeepAliveRule
import com.example.data.SimCard
import java.util.Locale
import com.example.R

/**
 * Calculates remaining days for a rule
 */
fun calculateRemainingDays(rule: KeepAliveRule): Int {
    val elapsedMillis = System.currentTimeMillis() - rule.lastActiveDate
    val elapsedDays = (elapsedMillis / (1000 * 60 * 60 * 24)).toInt()
    return rule.periodDays - elapsedDays
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    simCards: List<SimCard>,
    rules: List<KeepAliveRule>,
    onSelectSim: (SimCard) -> Unit,
    onNavigateToAddSim: () -> Unit,
    onNavigateToRules: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedOperatorFilter by remember { mutableStateOf<String?>(null) }
    var selectedCardTypeFilter by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf("Days Remaining") } // "Days Remaining", "Number", "Operator"

    val isZh = remember { Locale.getDefault().language == "zh" }

    val uniqueOperators = remember(simCards) {
        simCards.map { it.operator }.distinct()
    }
    val uniqueCardTypes = remember(simCards) {
        simCards.map { it.cardType }.distinct()
    }

    // Process list dynamically
    val processedSims = remember(simCards, rules, searchQuery, selectedOperatorFilter, selectedCardTypeFilter, sortBy) {
        var list = simCards.filter { sim ->
            val matchesQuery = sim.phoneNumber.contains(searchQuery, ignoreCase = true) || 
                               sim.operator.contains(searchQuery, ignoreCase = true) ||
                               sim.notes.contains(searchQuery, ignoreCase = true)
            
            val matchesOperator = selectedOperatorFilter == null || sim.operator == selectedOperatorFilter
            val matchesCardType = selectedCardTypeFilter == null || sim.cardType == selectedCardTypeFilter
            
            matchesQuery && matchesOperator && matchesCardType
        }

        // Helper to find the minimum remaining days for any rule of this sim card
        fun getMinDaysRemaining(simId: Int): Int {
            val simRules = rules.filter { it.simId == simId && it.isEnabled }
            if (simRules.isEmpty()) return 99999
            return simRules.minOf { calculateRemainingDays(it) }
        }

        list = when (sortBy) {
            "Number" -> list.sortedBy { it.phoneNumber }
            "Operator" -> list.sortedBy { it.operator }
            else -> list.sortedBy { getMinDaysRemaining(it.id) }
        }

        list
    }

    // Quick Stats Calculations
    val activeSimCount = simCards.size
    val attentionRequiredCount = remember(rules) {
        simCards.count { sim ->
            val simRules = rules.filter { it.simId == sim.id && it.isEnabled }
            simRules.any { calculateRemainingDays(it) <= it.warningDaysAdvancePrimary }
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp
                        )
                        val subtitle = if (isZh) "SIM卡防封与定时保活大师" else "Smart SIM Cards & Expiration Protect"
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.SimCard,
                        contentDescription = "App Icon",
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddSim,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .testTag("add_sim_fab")
                    .padding(bottom = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add SIM")
                    Spacer(modifier = Modifier.width(6.dp))
                    val addLabel = if (isZh) "登记卡片" else "Add SIM"
                    Text(addLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Stats Header Widget
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val dashboardTitleLabel = if (isZh) "卡片活跃监视盘" else "Standby Dashboard"
                            Text(
                                text = dashboardTitleLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                              )
                            Spacer(modifier = Modifier.height(6.dp))
                            val enrolledLabel = if (isZh) "受保护卡片: ${activeSimCount}张" else "Enrolled SIMs: $activeSimCount"
                            Text(
                                text = enrolledLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        // Danger Banner Alert Indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (attentionRequiredCount > 0)
                                        MaterialTheme.colorScheme.errorContainer
                                    else
                                        MaterialTheme.colorScheme.secondaryContainer
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (attentionRequiredCount > 0) Icons.Default.Warning else Icons.Default.VerifiedUser,
                                contentDescription = "Status icon",
                                tint = if (attentionRequiredCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            
                            val alertStatusText = if (attentionRequiredCount > 0) {
                                if (isZh) "${attentionRequiredCount}笔到期警告" else "$attentionRequiredCount Alerts"
                            } else {
                                if (isZh) "卡片全部安全" else "All Healthy"
                            }
                            Text(
                                text = alertStatusText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (attentionRequiredCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Search Bar and Filters
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.dashboard_search)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Filtering Row / Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val quickFiltersLabel = if (isZh) "快捷多维过滤器" else "Quick Filters"
                Text(
                    text = quickFiltersLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sort Dropdown button icon
                TextButton(
                    onClick = {
                        sortBy = when (sortBy) {
                            "Days Remaining" -> "Number"
                            "Number" -> "Operator"
                            else -> "Days Remaining"
                        }
                    },
                    modifier = Modifier.testTag("sort_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort Icon",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    val sortLabelText = if (isZh) {
                        when (sortBy) {
                            "Number" -> "电话号码"
                            "Operator" -> "运营商"
                            else -> "到期日前后"
                        }
                    } else {
                        sortBy
                    }
                    val sortByText = if (isZh) "排序：$sortLabelText" else "Sort: $sortLabelText"
                    Text(sortByText, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Quick Filters flow row
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Clear all filters chip if applicable
                if (selectedOperatorFilter != null || selectedCardTypeFilter != null) {
                    val resetText = if (isZh) "清除所有过滤器" else "Reset Filters"
                    InputChip(
                        selected = false,
                        onClick = {
                            selectedOperatorFilter = null
                            selectedCardTypeFilter = null
                        },
                        label = { Text(resetText) },
                        avatar = { Icon(Icons.Default.FilterListOff, contentDescription = "Reset") }
                    )
                }

                // Operator Filters Chips
                uniqueOperators.forEach { op ->
                    FilterChip(
                        selected = selectedOperatorFilter == op,
                        onClick = { selectedOperatorFilter = if (selectedOperatorFilter == op) null else op },
                        label = { Text(op) }
                    )
                }

                // Card Type Filters Chips
                uniqueCardTypes.forEach { type ->
                    val displayType = if (isZh) {
                        when (type) {
                            "Physical SIM" -> "实体卡"
                            "eSIM" -> "电子卡 eSIM"
                            else -> type
                        }
                    } else type
                    FilterChip(
                        selected = selectedCardTypeFilter == type,
                        onClick = { selectedCardTypeFilter = if (selectedCardTypeFilter == type) null else type },
                        label = { Text(displayType) }
                    )
                }
            }

            // SIM List View
            if (processedSims.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PermPhoneMsg,
                            contentDescription = "No SIM Cards",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val noSimsLabel = if (simCards.isEmpty()) {
                            if (isZh) "当前未登记任何SIM卡" else "No SIM cards enrolled yet"
                        } else {
                            if (isZh) "未找到匹配卡片条件" else "No matches found"
                        }
                        Text(
                            text = noSimsLabel,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val emptyDescriptionLabel = if (simCards.isEmpty()) {
                            if (isZh) "请点击右下角的“登记卡片”，录入您的首张预付费实体卡或eSIM，应用会为您自动铺设到期保活防封日程。" else "Tap 'Add SIM' below to enroll your first card and generate automatic expiration checks."
                        } else {
                            if (isZh) "请尝试调整您的搜索关键字或重置顶部的运营商/卡片材质过滤器。" else "Try adjusting your query or resetting operator filters."
                        }
                        Text(
                            text = emptyDescriptionLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
                ) {
                    items(processedSims, key = { it.id }) { sim ->
                        val simRules = rules.filter { it.simId == sim.id && it.isEnabled }
                        
                        SimCardItem(
                            sim = sim,
                            associatedRules = simRules,
                            onClick = { onSelectSim(sim) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimCardItem(
    sim: SimCard,
    associatedRules: List<KeepAliveRule>,
    onClick: () -> Unit
) {
    val isZh = remember { Locale.getDefault().language == "zh" }
    // Determine overall health & minimum days remaining
    val activeRulesOnly = associatedRules.filter { it.isEnabled }
    val minDaysRemaining = if (activeRulesOnly.isEmpty()) null else {
        activeRulesOnly.map { calculateRemainingDays(it) }.minOrNull()
    }

    // Health styling setup (dynamic Material 3 colors)
    val healthColor = when {
        minDaysRemaining == null -> MaterialTheme.colorScheme.outline
        minDaysRemaining <= 0 -> MaterialTheme.colorScheme.error // Expired / Alarm red
        minDaysRemaining <= 5 -> Color(0xFFD32F2F) // Critical danger
        minDaysRemaining <= 30 -> Color(0xFFFF9800) // Warning orange
        else -> Color(0xFF388E3C) // Healthy green
    }

    val riskText = when {
        minDaysRemaining == null -> if (isZh) "无需防过期检测" else "No Rules Active"
        minDaysRemaining <= 0 -> if (isZh) "卡片已超期失效！" else "Expired Risk"
        minDaysRemaining <= 5 -> if (isZh) "危险！极度急迫！" else "Urgent Alert"
        minDaysRemaining <= 30 -> if (isZh) "到期警告临界日" else "Warning Status"
        else -> if (isZh) "卡片状态十分安全" else "Standby Normal"
    }

    val iconBgColor = when {
        minDaysRemaining == null -> MaterialTheme.colorScheme.secondaryContainer
        minDaysRemaining <= 0 -> Color(0xFFFFDAD6)
        minDaysRemaining <= 5 -> Color(0xFFFFDAD6)
        minDaysRemaining <= 30 -> Color(0xFFFFE0B2)
        else -> Color(0xFFDCFEDD)
    }

    val iconColor = when {
        minDaysRemaining == null -> MaterialTheme.colorScheme.onSecondaryContainer
        minDaysRemaining <= 0 -> Color(0xFF410002)
        minDaysRemaining <= 5 -> Color(0xFF410002)
        minDaysRemaining <= 30 -> Color(0xFFE65100)
        else -> Color(0xFF123E15)
    }

    val statusIcon = when {
        minDaysRemaining == null -> Icons.Default.HelpOutline
        minDaysRemaining <= 5 -> Icons.Default.Warning
        minDaysRemaining <= 30 -> Icons.Default.NotificationsActive
        else -> Icons.Default.Shield
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sim_card_${sim.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, healthColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Leading Colored Block Icon (Matches HTML list item design)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = riskText,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Middle Text Block
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sim.phoneNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    val displayType = if (isZh) {
                        when (sim.cardType) {
                            "Physical SIM" -> "实体卡"
                            "eSIM" -> "电子卡 eSIM"
                            else -> sim.cardType
                        }
                    } else sim.cardType
                    Text(
                        text = "${sim.operator} • $displayType",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right Text Block
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val statusLabel = when {
                        minDaysRemaining == null -> if (isZh) "未设定保活" else "INACTIVE"
                        minDaysRemaining <= 5 -> if (isZh) "立即保活！" else "URGENT"
                        minDaysRemaining <= 30 -> if (isZh) "到期警告" else "ALERT"
                        else -> if (isZh) "安全保活中" else "ACTIVE"
                    }
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = healthColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    val secondaryLabel = if (minDaysRemaining != null) {
                        if (minDaysRemaining <= 0) {
                            if (isZh) "已于到期日超时" else "Expired"
                        } else {
                            if (isZh) "剩余 ${minDaysRemaining}天" else "${minDaysRemaining}d left"
                        }
                    } else {
                        if (isZh) "无闹钟守护" else "Standby"
                    }
                    Text(
                        text = secondaryLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Keep notes if present
            if (sim.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = "Notes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = sim.notes,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
