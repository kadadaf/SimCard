package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import com.example.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSimScreen(
    onNavigateBack: () -> Unit,
    onSaveSim: (
        phoneNumber: String,
        operator: String,
        cardType: String,
        activationDate: Long,
        planDetails: String,
        balance: Double,
        currency: String,
        notes: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var phoneNumber by remember { mutableStateOf("") }
    var operatorInput by remember { mutableStateOf("") }
    var cardType by remember { mutableStateOf("Physical SIM") } // eSIM or Physical SIM
    var planDetails by remember { mutableStateOf("") }
    var balanceInput by remember { mutableStateOf("") }
    var currencyChosen by remember { mutableStateOf("GBP") }
    var notes by remember { mutableStateOf("") }

    // Date calculations
    val calendar = remember { Calendar.getInstance() }
    var activationTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                activationTimestamp = calendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    var phoneError by remember { mutableStateOf(false) }
    var operatorError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_sim_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // General Info Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    val infoBannerText = if (Locale.getDefault().language == "zh") {
                        "选择下方特定的运营商（例如：英国 Giffgaff 或 CTExcel）时，应用将自动为您关联生成其官方标准保活及充值防回收日程规则。"
                    } else {
                        "Special template rules (e.g., UK Giffgaff auto-check or CTExcel balance changes) are automatically linked if you select those carriers below."
                    }
                    Text(
                        text = infoBannerText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Phone Number Input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = {
                    phoneNumber = it
                    phoneError = false
                },
                label = { Text(stringResource(R.string.add_sim_phone) + " *") },
                placeholder = { Text(stringResource(R.string.add_sim_phone_placeholder)) },
                isError = phoneError,
                supportingText = {
                    if (phoneError) {
                        val errMsg = if (Locale.getDefault().language == "zh") "电话号码为必填项" else "Phone number is required"
                        Text(errMsg, color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_number_input"),
                singleLine = true
            )

            // Operator Choices
            Column {
                Text(
                    text = stringResource(R.string.add_sim_operator) + " *",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Predefined quick pills for operator
                val speedOperators = listOf("Giffgaff UK", "CTExcel UK", "China Mobile", "Vodafone UK", "EE")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    speedOperators.forEach { opName ->
                        FilterChip(
                            selected = operatorInput == opName,
                            onClick = {
                                operatorInput = opName
                                operatorError = false
                            },
                            label = { Text(opName) }
                        )
                    }
                }

                val typeOpLabel = if (Locale.getDefault().language == "zh") "或手动硬键入通信服务商名称" else "Or Type Operator Company Name"
                OutlinedTextField(
                    value = operatorInput,
                    onValueChange = {
                        operatorInput = it
                        operatorError = false
                    },
                    label = { Text(typeOpLabel) },
                    isError = operatorError,
                    supportingText = {
                        if (operatorError) {
                            val errMsg = if (Locale.getDefault().language == "zh") "运营商名称为必选项/必填项" else "Operator name is required"
                            Text(errMsg, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("operator_input"),
                    singleLine = true
                )
            }

            // Card Type (Physical SIM vs eSIM Row)
            Column {
                Text(
                    text = stringResource(R.string.add_sim_card_type),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val physicalLabel = if (Locale.getDefault().language == "zh") "实体 SIM卡 (Physical)" else "Physical SIM"
                    ElevatedButton(
                        onClick = { cardType = "Physical SIM" },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (cardType == "Physical SIM" || cardType == "实体 SIM卡 (Physical)") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(physicalLabel)
                    }

                    val esimLabel = if (Locale.getDefault().language == "zh") "电子卡 eSIM" else "eSIM"
                    ElevatedButton(
                        onClick = { cardType = "eSIM" },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (cardType == "eSIM" || cardType == "电子卡 eSIM") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(esimLabel)
                    }
                }
            }

            // Plan details
            OutlinedTextField(
                value = planDetails,
                onValueChange = { planDetails = it },
                label = { Text(stringResource(R.string.add_sim_plan)) },
                placeholder = { Text(stringResource(R.string.add_sim_plan_placeholder)) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Balance and Currency Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = balanceInput,
                    onValueChange = { balanceInput = it },
                    label = { Text(stringResource(R.string.add_sim_balance)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.5f),
                    placeholder = { Text("10.00") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = currencyChosen,
                    onValueChange = { currencyChosen = it.uppercase() },
                    label = { Text(stringResource(R.string.add_sim_currency)) },
                    placeholder = { Text("GBP") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Activation Date Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.add_sim_activation_date),
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = dateFormatter.format(Date(activationTimestamp)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                val selectDateText = if (Locale.getDefault().language == "zh") "选择日期" else "Select Date"
                Button(
                    onClick = { datePickerDialog.show() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(selectDateText)
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.add_sim_notes)) },
                placeholder = { Text(stringResource(R.string.add_sim_notes_placeholder)) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Action Button
            Button(
                onClick = {
                    if (phoneNumber.trim().isEmpty()) {
                        phoneError = true
                    }
                    if (operatorInput.trim().isEmpty()) {
                        operatorError = true
                    }

                    if (!phoneError && !operatorError) {
                        val numVal = balanceInput.toDoubleOrNull() ?: 0.0
                        onSaveSim(
                            phoneNumber.trim(),
                            operatorInput.trim(),
                            cardType,
                            activationTimestamp,
                            planDetails.trim(),
                            numVal,
                            currencyChosen.trim(),
                            notes.trim()
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_sim_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.add_sim_save_btn), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
