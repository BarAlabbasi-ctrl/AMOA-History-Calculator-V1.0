package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.math.CalculatorEngine
import com.example.ui.theme.LocalAmoaColors
import com.example.units.UnitsData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitsScreen(
    onAddHistory: (mode: String, exprRaw: String, result: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAmoaColors.current

    var selectedCategoryId by remember { mutableStateOf("length") }
    val currentCategory = remember(selectedCategoryId) {
        UnitsData.categories.find { it.id == selectedCategoryId } ?: UnitsData.categories[0]
    }

    var fromUnitKey by remember(selectedCategoryId) {
        mutableStateOf(currentCategory.units.firstOrNull()?.key ?: "")
    }
    var toUnitKey by remember(selectedCategoryId) {
        mutableStateOf(
            if (currentCategory.units.size > 1) currentCategory.units[1].key
            else currentCategory.units.firstOrNull()?.key ?: ""
        )
    }

    var inputVal by remember { mutableStateOf("1") }
    var resultText by remember { mutableStateOf("—") }

    // Dropdown expanded states
    var categoryExpanded by remember { mutableStateOf(false) }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    fun doConvert() {
        val num = inputVal.toDoubleOrNull()
        if (num == null) {
            resultText = "Enter a valid value"
            return
        }

        val converted = UnitsData.convert(num, selectedCategoryId, fromUnitKey, toUnitKey)
        val formattedNum = CalculatorEngine.formatDisplay(CalculatorEngine.trimNum(converted))
        val finalResult = "$formattedNum $toUnitKey"
        resultText = finalResult

        val catName = currentCategory.label
        val fromLabel = currentCategory.units.find { it.key == fromUnitKey }?.label ?: fromUnitKey
        val toLabel = currentCategory.units.find { it.key == toUnitKey }?.label ?: toUnitKey
        onAddHistory("Length", "$inputVal $fromUnitKey → $toUnitKey ($catName)", finalResult)
    }

    // Auto-calculate on initial launch or change
    LaunchedEffect(inputVal, selectedCategoryId, fromUnitKey, toUnitKey) {
        val num = inputVal.toDoubleOrNull()
        if (num != null) {
            val converted = UnitsData.convert(num, selectedCategoryId, fromUnitKey, toUnitKey)
            val formattedNum = CalculatorEngine.formatDisplay(CalculatorEngine.trimNum(converted))
            resultText = "$formattedNum $toUnitKey"
        } else {
            resultText = "—"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.panelBg)
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Category Selector
            Column {
                Text(
                    text = "UNIT CATEGORY",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.textDim,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = currentCategory.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.brass,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text,
                            focusedContainerColor = colors.keyBg,
                            unfocusedContainerColor = colors.keyBg
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("category_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(colors.panelBg)
                    ) {
                        UnitsData.categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.label, color = colors.text) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Value Input
            Column {
                Text(
                    text = "VALUE TO CONVERT",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.textDim,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = inputVal,
                    onValueChange = { inputVal = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("0", color = colors.textDim) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.brass,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        focusedContainerColor = colors.keyBg,
                        unfocusedContainerColor = colors.keyBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unit_value_input")
                )
            }

            // From & To Units Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // FROM UNIT
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FROM",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.textDim,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val fromLabel = currentCategory.units.find { it.key == fromUnitKey }?.label ?: fromUnitKey

                    ExposedDropdownMenuBox(
                        expanded = fromExpanded,
                        onExpandedChange = { fromExpanded = !fromExpanded }
                    ) {
                        OutlinedTextField(
                            value = fromLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.brass,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.text,
                                unfocusedTextColor = colors.text,
                                focusedContainerColor = colors.keyBg,
                                unfocusedContainerColor = colors.keyBg
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = fromExpanded,
                            onDismissRequest = { fromExpanded = false },
                            modifier = Modifier.background(colors.panelBg)
                        ) {
                            currentCategory.units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u.label, color = colors.text, fontSize = 13.sp) },
                                    onClick = {
                                        fromUnitKey = u.key
                                        fromExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // TO UNIT
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TO",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.textDim,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val toLabel = currentCategory.units.find { it.key == toUnitKey }?.label ?: toUnitKey

                    ExposedDropdownMenuBox(
                        expanded = toExpanded,
                        onExpandedChange = { toExpanded = !toExpanded }
                    ) {
                        OutlinedTextField(
                            value = toLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.brass,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.text,
                                unfocusedTextColor = colors.text,
                                focusedContainerColor = colors.keyBg,
                                unfocusedContainerColor = colors.keyBg
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = toExpanded,
                            onDismissRequest = { toExpanded = false },
                            modifier = Modifier.background(colors.panelBg)
                        ) {
                            currentCategory.units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u.label, color = colors.text, fontSize = 13.sp) },
                                    onClick = {
                                        toUnitKey = u.key
                                        toExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Swap Units Button
            Button(
                onClick = {
                    val tmp = fromUnitKey
                    fromUnitKey = toUnitKey
                    toUnitKey = tmp
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.keyBg,
                    contentColor = colors.brass
                ),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("swap_units_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Swap",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "⇅ Swap Units",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Convert Button
            Button(
                onClick = { doConvert() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.brass,
                    contentColor = colors.equalsText
                ),
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("convert_units_button")
            ) {
                Text(
                    text = "Convert & Save to History",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Result Display Box
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
                        text = resultText,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.brass,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("unit_result_value")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currentCategory.label} Conversion Result",
                        fontSize = 11.sp,
                        color = colors.textDim,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
