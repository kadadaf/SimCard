package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BackupTable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SimViewModel
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    viewModel: SimViewModel,
    onTriggerCheck: () -> Unit,
    onExportBackup: () -> String,
    onImportBackup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var showImportDialog by remember { mutableStateOf(false) }
    var showExplanationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // App Description Intro Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_protection_engine),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_duration_explanation),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Language Selection Section
            Text(stringResource(R.string.settings_language), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var expanded by remember { mutableStateOf(false) }
                    val appLanguage by viewModel.appLanguage.collectAsState()
                    val languageLabel = when (appLanguage) {
                        "en" -> "English"
                        "zh" -> "简体中文 (Chinese)"
                        else -> "System Default (跟随系统)"
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_language), fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.settings_language_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(languageLabel)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Language")
                            }
                            
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("System Default (跟随系统)") },
                                    onClick = {
                                        viewModel.setAppLanguage("system")
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("English") },
                                    onClick = {
                                        viewModel.setAppLanguage("en")
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("简体中文 (Chinese)") },
                                    onClick = {
                                        viewModel.setAppLanguage("zh")
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Quick Status alarm checks
            Text(stringResource(R.string.settings_engine_status), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.settings_background_checks), fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.settings_auto_inspect), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }

                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(stringResource(R.string.settings_active), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onTriggerCheck()
                            Toast.makeText(context, context.getString(R.string.settings_toast_engine_triggered), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Simulate")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_trigger_audit_btn))
                    }
                }
            }

            // Explaining Operator Keeping-Alive policies
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.settings_rules_guide), fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.settings_carrier_policies), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }

                        IconButton(onClick = { showExplanationDialog = true }) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Explain Guide")
                        }
                    }
                }
            }

            // Backup Controls Area
            Text(stringResource(R.string.settings_erase_backup_header), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_backup_title), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.settings_backup_intro), fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // EXPORT BUTTON
                        ElevatedButton(
                            onClick = {
                                val jsonStr = onExportBackup()
                                if (jsonStr.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(jsonStr))
                                    Toast.makeText(context, context.getString(R.string.settings_toast_copied), Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, context.getString(R.string.settings_toast_no_data), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy JSON", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_copy_backup_btn))
                        }

                        // IMPORT BUTTON
                        Button(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = "Import DB", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_paste_restore_btn))
                        }
                    }
                }
            }

            // Credits Footer
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Verified, contentDescription = "Secured", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.app_name) + " v1.0 • Offline-First Sandbox Mode", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }

    // Explaining dialog code
    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            title = { Text(stringResource(R.string.settings_rules_guide), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    val giffgaffHintTitle = if (viewModel.appLanguage.value == "zh") "💡 英国 Giffgaff（180天检查）：" else "💡 Giffgaff UK (180 days Check):"
                    val giffgaffHintText = if (viewModel.appLanguage.value == "zh") {
                        "每 180 天，您必须执行以下至少一项操作以保活：\n• 给另一个号码拨打电话或发送短信\n• 连接移动网络使用蜂窝数据上网\n• 充值话费余额（Giffgaff支持最低10英镑充值）\n否则，SIM卡将因无任何消费活动而过期，您的电话号码也会被回收注销。"
                    } else {
                        "Every 180 days, you must do at least one of the following:\n• Make a call or send a text (SMS) to another number\n• Connect to cellular data on mobile network\n• Top-up your credit balance (Giffgaff supports min 10 GBP recharge)\nOtherwise, the SIM expires and your phone number is recycled."
                    }

                    val ctexcelHintTitle = if (viewModel.appLanguage.value == "zh") "💡 英国 CTExcel（180天余额变动）：" else "💡 CTExcel UK (180 days Balance Change):"
                    val ctexcelHintText = if (viewModel.appLanguage.value == "zh") {
                        "• 必须在 180 天内产生使用量或余额扣除（例如：发送一条短信或保单自动扣除每月1英镑的服务费）来保持余额产生变动。\n• 新激活卡需在收到卡片起 10 天内完成注册并完成首次话费扣除变动以确认有效性。"
                    } else {
                        "• Must generate usage or a balance deduction (e.g. sending a text message or auto-renewing £1/month standby service fee) within 180 days.\n• New SIM activation card must receive cellular activity/first usage within 10 days of physical registration to confirm status."
                    }

                    Text(giffgaffHintTitle, fontWeight = FontWeight.Bold)
                    Text(giffgaffHintText, fontSize = 13.sp)

                    HorizontalDivider()

                    Text(ctexcelHintTitle, fontWeight = FontWeight.Bold)
                    Text(ctexcelHintText, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showExplanationDialog = false }) {
                    Text(stringResource(R.string.settings_cancel_btn))
                }
            }
        )
    }

    // Restore Paste Dialog
    if (showImportDialog) {
        var inputBackupJson by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(R.string.settings_restore_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_restore_msg), fontSize = 13.sp)
                    Text(stringResource(R.string.settings_restore_warning), fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    
                    OutlinedTextField(
                        value = inputBackupJson,
                        onValueChange = { inputBackupJson = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text(stringResource(R.string.settings_paste_placeholder)) },
                        maxLines = 10,
                        minLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputBackupJson.trim().isNotEmpty()) {
                            try {
                                onImportBackup(inputBackupJson.trim())
                                showImportDialog = false
                                Toast.makeText(context, context.getString(R.string.settings_toast_restore_success), Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.settings_toast_restore_failed) + e.message, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.settings_toast_empty_input), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_confirm_restore_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(stringResource(R.string.settings_cancel_btn))
                }
            }
        )
    }
}
