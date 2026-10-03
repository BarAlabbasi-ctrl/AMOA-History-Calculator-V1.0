package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.time.TimeCalculator
import com.example.ui.theme.LocalAmoaColors
import java.util.Calendar

enum class TimeMode {
    ELAPSED,
    INOUT,
    ADDSUB
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeScreen(
    onAddHistory: (mode: String, exprRaw: String, result: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAmoaColors.current
    val context = LocalContext.current

    var selectedMode by remember { mutableStateOf(TimeMode.ELAPSED) }

    // Elapsed State
    val now = remember { System.currentTimeMillis() }
    var startMillis by remember { mutableLongStateOf(now - 3600000 * 8) }
    var endMillis by remember { mutableLongStateOf(now) }
    var elapsedResultText by remember { mutableStateOf("—") }

    // Shift In/Out State
    var inHour by remember { mutableIntStateOf(9) }
    var inMinute by remember { mutableIntStateOf(0) }
    var outHour by remember { mutableIntStateOf(17) }
    var outMinute by remember { mutableIntStateOf(30) }
    var breakMinutesText by remember { mutableStateOf("30") }

    // Add / Subtract State
    var baseMillis by remember { mutableLongStateOf(now) }
    var amountText by remember { mutableStateOf("2") }
    var timeUnit by remember { mutableStateOf("hours") }
    var isAdd by remember { mutableStateOf(true) }
    var addSubResultText by remember { mutableStateOf("—") }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    // Helper to open Date + Time picker
    fun pickDateTime(initialMillis: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
        val dateDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val timeDialog = TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        cal.set(Calendar.MINUTE, minute)
                        onPicked(cal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false
                )
                timeDialog.show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dateDialog.show()
    }

    // Helper to pick Time only
    fun pickTime(initialHour: Int, initialMinute: Int, onPicked: (Int, Int) -> Unit) {
        val dialog = TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(hour, minute) },
            initialHour,
            initialMinute,
            false
        )
        dialog.show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Selector Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeMode.values().forEach { mode ->
                val isSelected = selectedMode == mode
                val title = when (mode) {
                    TimeMode.ELAPSED -> "Elapsed"
                    TimeMode.INOUT -> "Time In/Out"
                    TimeMode.ADDSUB -> "Add/Sub"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.teal else colors.keyBg)
                        .border(1.dp, if (isSelected) colors.teal else colors.border, RoundedCornerShape(8.dp))
                        .clickable { selectedMode = mode }
                        .testTag("time_mode_${mode.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF0D1A18) else colors.textDim
                    )
                }
            }
        }

        // Panel Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.panelBg)
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedMode) {
                TimeMode.ELAPSED -> {
                    // Start Time Picker
                    Column {
                        Text(
                            text = "START DATE & TIME",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = colors.textDim,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.keyBg)
                                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                .clickable { pickDateTime(startMillis) { startMillis = it } }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = TimeCalculator.formatDateTime(startMillis),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.text
                            )
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = colors.brass, modifier = Modifier.size(18.dp))
                        }
                    }

                    // End Time Picker
                    Column {
                        Text(
                            text = "END DATE & TIME",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = colors.textDim,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.keyBg)
                                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                .clickable { pickDateTime(endMillis) { endMillis = it } }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = TimeCalculator.formatDateTime(endMillis),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.text
                            )
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = colors.brass, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Calculate Elapsed Button
                    Button(
                        onClick = {
                            val res = TimeCalculator.calculateElapsed(startMillis, endMillis)
                            elapsedResultText = res.formatted
                            val sStr = TimeCalculator.formatDateTime(startMillis)
                            val eStr = TimeCalculator.formatDateTime(endMillis)
                            onAddHistory("Time", "$sStr → $eStr", res.formatted)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.brass,
                            contentColor = colors.equalsText
                        ),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("calc_elapsed_button")
                    ) {
                        Text("Calculate Elapsed Time", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    // Result Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.keyBg)
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = elapsedResultText,
                                fontSize = 26.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = colors.brass,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Total Elapsed Time", fontSize = 11.sp, color = colors.textDim)
                        }
                    }
                }

                TimeMode.INOUT -> {
                    // Shift In and Out Pickers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TIME IN", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.textDim)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.keyBg)
                                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                    .clickable { pickTime(inHour, inMinute) { h, m -> inHour = h; inMinute = m } }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(TimeCalculator.formatTimeOnly(inHour, inMinute), fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = colors.text)
                                Icon(Icons.Default.Schedule, contentDescription = "Time In", tint = colors.brass, modifier = Modifier.size(16.dp))
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("TIME OUT", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.textDim)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.keyBg)
                                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                    .clickable { pickTime(outHour, outMinute) { h, m -> outHour = h; outMinute = m } }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(TimeCalculator.formatTimeOnly(outHour, outMinute), fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = colors.text)
                                Icon(Icons.Default.Schedule, contentDescription = "Time Out", tint = colors.brass, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Break minutes
                    Column {
                        Text("BREAK TIME (MINUTES, OPTIONAL)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.textDim)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = breakMinutesText,
                            onValueChange = { breakMinutesText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("0", color = colors.textDim) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.brass,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.text,
                                unfocusedTextColor = colors.text,
                                focusedContainerColor = colors.keyBg,
                                unfocusedContainerColor = colors.keyBg
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Live Shift Calculation
                    val bMin = breakMinutesText.toIntOrNull() ?: 0
                    val shiftResult = remember(inHour, inMinute, outHour, outMinute, bMin) {
                        TimeCalculator.calculateShift(inHour, inMinute, outHour, outMinute, bMin)
                    }

                    // Shift Result Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.keyBg)
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${shiftResult.netHours}h ${shiftResult.netRemainMin}m",
                                fontSize = 30.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = colors.brass,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${shiftResult.netMinutes} minutes • ${String.format(java.util.Locale.US, "%.2f", shiftResult.netDecimalHours)} hours net",
                                fontSize = 12.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textDim
                            )
                            if (bMin > 0) {
                                Text(
                                    text = "Gross: ${shiftResult.grossHours}h ${shiftResult.grossRemainMin}m − ${bMin}m break",
                                    fontSize = 11.sp,
                                    color = colors.teal,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            if (shiftResult.crossesMidnight) {
                                Text(
                                    text = "🌙 Crosses midnight (night shift into next day)",
                                    fontSize = 11.sp,
                                    color = colors.brass,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // Log Shift Button
                    Button(
                        onClick = {
                            val inStr = TimeCalculator.formatTimeOnly(inHour, inMinute)
                            val outStr = TimeCalculator.formatTimeOnly(outHour, outMinute)
                            val breakNote = if (bMin > 0) " minus ${bMin}m break" else ""
                            onAddHistory("Time", "Time In $inStr → Time Out $outStr$breakNote", shiftResult.formattedSummary)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.brass,
                            contentColor = colors.equalsText
                        ),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("log_shift_button")
                    ) {
                        Text("Log This Shift to History", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                TimeMode.ADDSUB -> {
                    // Base Date Time
                    Column {
                        Text("BASE DATE & TIME", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.textDim)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.keyBg)
                                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                .clickable { pickDateTime(baseMillis) { baseMillis = it } }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(TimeCalculator.formatDateTime(baseMillis), fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = colors.text)
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = colors.brass, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Amount & Unit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AMOUNT", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.textDim)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.brass,
                                    unfocusedBorderColor = colors.border,
                                    focusedTextColor = colors.text,
                                    unfocusedTextColor = colors.text,
                                    focusedContainerColor = colors.keyBg,
                                    unfocusedContainerColor = colors.keyBg
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("UNIT", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.textDim)
                            Spacer(modifier = Modifier.height(6.dp))

                            ExposedDropdownMenuBox(
                                expanded = unitDropdownExpanded,
                                onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = timeUnit.replaceFirstChar { it.uppercase() },
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.brass,
                                        unfocusedBorderColor = colors.border,
                                        focusedTextColor = colors.text,
                                        unfocusedTextColor = colors.text,
                                        focusedContainerColor = colors.keyBg,
                                        unfocusedContainerColor = colors.keyBg
                                    ),
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = unitDropdownExpanded,
                                    onDismissRequest = { unitDropdownExpanded = false },
                                    modifier = Modifier.background(colors.panelBg)
                                ) {
                                    listOf("minutes", "hours", "days").forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text(u.replaceFirstChar { it.uppercase() }, color = colors.text) },
                                            onClick = {
                                                timeUnit = u
                                                unitDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Operation Toggle (Add / Subtract)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isAdd = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAdd) colors.teal else colors.keyBg,
                                contentColor = if (isAdd) Color(0xFF0D1A18) else colors.textDim
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Add (+)", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { isAdd = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isAdd) colors.teal else colors.keyBg,
                                contentColor = if (!isAdd) Color(0xFF0D1A18) else colors.textDim
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Subtract (−)", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Calculate Button
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            val res = TimeCalculator.calculateAddSub(baseMillis, amt, timeUnit, isAdd)
                            addSubResultText = res
                            val op = if (isAdd) "+" else "−"
                            val baseStr = TimeCalculator.formatDateTime(baseMillis)
                            onAddHistory("Time", "$baseStr $op $amt $timeUnit", res)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.brass,
                            contentColor = colors.equalsText
                        ),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("calc_addsub_button")
                    ) {
                        Text("Calculate New Date & Time", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    // Result Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.keyBg)
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = addSubResultText,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = colors.brass,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Calculated Date & Time", fontSize = 11.sp, color = colors.textDim)
                        }
                    }
                }
            }
        }
    }
}
