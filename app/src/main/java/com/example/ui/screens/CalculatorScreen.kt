package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.math.CalculatorEngine
import com.example.ui.theme.LocalAmoaColors
import kotlinx.coroutines.launch

@Composable
fun CalculatorScreen(
    initialExpr: String? = null,
    isFunctionsOpen: Boolean = false,
    onToggleFunctions: (Boolean) -> Unit = {},
    onCommitHistory: (exprRaw: String, result: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAmoaColors.current
    val coroutineScope = rememberCoroutineScope()

    var exprString by remember { mutableStateOf("") }
    var cursorPos by remember { mutableIntStateOf(0) }
    var showingResult by remember { mutableStateOf(false) }
    var finishedExprDisplay by remember { mutableStateOf("") }
    var degMode by remember { mutableStateOf(true) }

    // Reload from history
    LaunchedEffect(initialExpr) {
        if (!initialExpr.isNullOrEmpty()) {
            exprString = initialExpr
            cursorPos = initialExpr.length
            showingResult = false
            finishedExprDisplay = ""
        }
    }

    val insertString: (String) -> Unit = { strToInsert ->
        var currentExpr = exprString
        var currentPos = cursorPos

        if (showingResult || currentExpr == "Error") {
            currentExpr = ""
            currentPos = 0
            showingResult = false
            finishedExprDisplay = ""
        }

        val clampedPos = currentPos.coerceIn(0, currentExpr.length)
        val nextExpr = currentExpr.substring(0, clampedPos) + strToInsert + currentExpr.substring(clampedPos)
        exprString = nextExpr
        cursorPos = clampedPos + strToInsert.length
    }

    val inputDigit: (String) -> Unit = { d -> insertString(d) }

    val toggleSign: () -> Unit = {
        if (exprString != "Error" && exprString.isNotEmpty()) {
            val (start, end) = CalculatorEngine.findNumberBounds(exprString, cursorPos)
            if (start != end) {
                val numStr = exprString.substring(start, end)
                val isMinus = numStr.startsWith("-") || numStr.startsWith("−")
                val nextExpr = if (isMinus) {
                    exprString.substring(0, start) + numStr.substring(1) + exprString.substring(end)
                } else {
                    exprString.substring(0, start) + "−" + numStr + exprString.substring(end)
                }
                val delta = if (isMinus) -1 else 1
                exprString = nextExpr
                cursorPos = (cursorPos + delta).coerceIn(0, nextExpr.length)
                showingResult = false
            }
        }
    }

    val commitCurrentToHistory: () -> Unit = {
        if (exprString.isNotEmpty() && exprString != "Error") {
            val res = CalculatorEngine.evaluateMathExpression(exprString, degMode)
            if (!res.isNaN() && res.isFinite()) {
                val formatted = CalculatorEngine.formatDisplay(CalculatorEngine.trimNum(res))
                onCommitHistory(exprString, formatted)
            }
        }
    }

    val clearAll: () -> Unit = {
        if (!showingResult) {
            commitCurrentToHistory()
        }
        exprString = ""
        cursorPos = 0
        showingResult = false
        finishedExprDisplay = ""
    }

    val deleteLast: () -> Unit = {
        if (showingResult || exprString == "Error") {
            clearAll()
        } else if (cursorPos > 0 && exprString.isNotEmpty()) {
            val clamped = cursorPos.coerceIn(1, exprString.length)
            val nextExpr = exprString.substring(0, clamped - 1) + exprString.substring(clamped)
            exprString = nextExpr
            cursorPos = clamped - 1
        }
    }

    val chooseOp: (String) -> Unit = { op ->
        if (showingResult && exprString != "Error") {
            val base = exprString
            val nextExpr = base + op
            showingResult = false
            finishedExprDisplay = ""
            exprString = nextExpr
            cursorPos = nextExpr.length
        } else {
            insertString(op)
        }
    }

    val calculate: () -> Unit = {
        if (exprString.isNotEmpty() && exprString != "Error") {
            val res = CalculatorEngine.evaluateMathExpression(exprString, degMode)
            if (res.isNaN() || !res.isFinite()) {
                finishedExprDisplay = exprString
                exprString = "Error"
                showingResult = true
            } else {
                val resultStr = CalculatorEngine.trimNum(res)
                val formatted = CalculatorEngine.formatDisplay(resultStr)
                onCommitHistory(exprString, formatted)
                finishedExprDisplay = exprString
                exprString = resultStr
                cursorPos = resultStr.length
                showingResult = true
            }
        }
    }

    val livePreview = remember(exprString, degMode) {
        CalculatorEngine.computeLivePreview(exprString, degMode)
    }

    val bigDisplayValue = when {
        showingResult -> if (exprString == "Error") "Error" else CalculatorEngine.formatDisplay(exprString)
        exprString.isEmpty() -> "0"
        livePreview != null -> CalculatorEngine.formatDisplay(livePreview)
        else -> CalculatorEngine.formatDisplay(exprString)
    }

    // Auto-calculate single line result font size so it NEVER wraps onto 2 lines and NEVER jumps!
    val resultFontSize = when {
        bigDisplayValue.length <= 8 -> 44.sp
        bigDisplayValue.length <= 11 -> 36.sp
        bigDisplayValue.length <= 14 -> 30.sp
        bigDisplayValue.length <= 18 -> 24.sp
        else -> 20.sp
    }

    val verticalScrollState = rememberScrollState()

    // Smooth auto-scroll downward as expression lines grow and push upwards
    LaunchedEffect(exprString, cursorPos, finishedExprDisplay) {
        verticalScrollState.animateScrollTo(verticalScrollState.maxValue)
    }

    // 50/50 SCREEN LAYOUT
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 6.dp)
    ) {
        // ==========================================
        // UPPER 50%: CALCULATION DISPLAY AREA
        // 1) Upward-flowing multi-line calculation
        // 2) Single-line strictly fitted result
        // 3) Standard tight natural typography (no wide gaps)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Exactly 50%
                .padding(horizontal = 24.dp, vertical = 6.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Vertical scrolling container for previous and active calculation lines
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(verticalScrollState),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Previous finished calculation (pushed up)
                    if (showingResult && finishedExprDisplay.isNotEmpty()) {
                        Text(
                            text = CalculatorEngine.formatDisplay(finishedExprDisplay) + " =",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Default,
                            color = colors.textDim,
                            textAlign = TextAlign.End,
                            letterSpacing = 0.sp,
                            lineHeight = 24.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .testTag("finished_expr_display")
                        )
                    }

                    // Active Calculation Input (wrapping vertically and going upwards!)
                    if (exprString.isEmpty() && !showingResult) {
                        Text(
                            text = "Enter calculation",
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Default,
                            color = colors.textDim.copy(alpha = 0.5f),
                            textAlign = TextAlign.End,
                            letterSpacing = 0.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    } else if (!showingResult) {
                        // Natural AnnotatedString with tight standard kerning and clean cursor
                        val clamped = cursorPos.coerceIn(0, exprString.length)
                        val before = exprString.substring(0, clamped)
                        val after = exprString.substring(clamped)

                        val annotatedExpr = buildAnnotatedString {
                            append(before)
                            withStyle(SpanStyle(color = colors.brass, fontWeight = FontWeight.Bold)) {
                                append("│")
                            }
                            append(after)
                        }

                        Text(
                            text = annotatedExpr,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Default,
                            color = colors.text,
                            textAlign = TextAlign.End,
                            letterSpacing = 0.sp,
                            lineHeight = 30.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // SINGLE-LINE RESULT DISPLAY
                // Fits neatly on one line, auto-scales down for large numbers, standard spacing
                Text(
                    text = bigDisplayValue,
                    fontSize = resultFontSize,
                    fontFamily = FontFamily.Default, // Standard natural digit spacing, no wide monospace gaps!
                    fontWeight = FontWeight.Bold,
                    color = colors.brass,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.sp, // Crisp, natural standard spacing
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calculator_result_value")
                )

                // Cursor navigation helper pills
                if (exprString.isNotEmpty() && !showingResult) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap arrows to move cursor",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Default,
                            color = colors.textDim.copy(alpha = 0.7f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Step Left
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.keyBg)
                                    .border(1.dp, colors.keyBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (cursorPos > 0) cursorPos--
                                    }
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                                    .testTag("cursor_left_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Cursor Left",
                                    tint = colors.textDim,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Step Right
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.keyBg)
                                    .border(1.dp, colors.keyBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (cursorPos < exprString.length) cursorPos++
                                    }
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                                    .testTag("cursor_right_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Cursor Right",
                                    tint = colors.textDim,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Functions Drawer if open
        AnimatedVisibility(
            visible = isFunctionsOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.panelBg)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCIENTIFIC FUNCTIONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.brass
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.keyBg)
                            .border(1.dp, colors.keyBorder, RoundedCornerShape(6.dp))
                            .clickable { degMode = !degMode }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .testTag("deg_rad_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (degMode) "DEG" else "RAD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.brass
                        )
                    }
                }

                val sciKeys = listOf(
                    listOf("sin", "cos", "tan", "asin", "acos", "atan"),
                    listOf("sinh", "cosh", "tanh", "sqrt", "cbrt", "x²"),
                    listOf("xʸ", "10ˣ", "eˣ", "log", "ln", "x!"),
                    listOf("nCr", "nPr", "mod", "|x|", "π", "e"),
                    listOf("(", ")", "[", "]", "^", "%")
                )

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    sciKeys.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            rowKeys.forEach { keyLabel ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(26.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(colors.funcBg)
                                        .border(1.dp, colors.funcBorder, RoundedCornerShape(5.dp))
                                        .clickable {
                                            when (keyLabel) {
                                                "sin" -> insertString("sin(")
                                                "cos" -> insertString("cos(")
                                                "tan" -> insertString("tan(")
                                                "asin" -> insertString("asin(")
                                                "acos" -> insertString("acos(")
                                                "atan" -> insertString("atan(")
                                                "sinh" -> insertString("sinh(")
                                                "cosh" -> insertString("cosh(")
                                                "tanh" -> insertString("tanh(")
                                                "sqrt" -> insertString("sqrt(")
                                                "cbrt" -> insertString("cbrt(")
                                                "x²" -> insertString("^2")
                                                "xʸ" -> insertString("^")
                                                "10ˣ" -> insertString("10^(")
                                                "eˣ" -> insertString("e^(")
                                                "log" -> insertString("log(")
                                                "ln" -> insertString("ln(")
                                                "x!" -> insertString("!")
                                                "nCr" -> insertString(" nCr ")
                                                "nPr" -> insertString(" nPr ")
                                                "mod" -> insertString(" mod ")
                                                "|x|" -> insertString("abs(")
                                                "π" -> insertString("π")
                                                "e" -> insertString("e")
                                                "(" -> insertString("(")
                                                ")" -> insertString(")")
                                                "[" -> insertString("[")
                                                "]" -> insertString("]")
                                                "^" -> insertString("^")
                                                "%" -> chooseOp("%")
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = keyLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.funcText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // LOWER 50%: NUMBER PAD
        // Generous, comfortable keys each filling 1f weight
        // Standard typography
        // ==========================================
        val keyCornerRadius = 12.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Exactly 50%
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ROW 1: C, DEL, ±, ÷
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcKey50(text = "C", textColor = colors.teal, bgColor = colors.funcBg, borderColor = colors.funcBorder, cornerRadius = keyCornerRadius, modifier = Modifier.weight(1f), tag = "key_clear", onClick = clearAll)
                CalcKey50(text = "DEL", textColor = colors.teal, bgColor = colors.funcBg, borderColor = colors.funcBorder, cornerRadius = keyCornerRadius, modifier = Modifier.weight(1f), tag = "key_delete", onClick = deleteLast)
                CalcKey50(text = "±", textColor = colors.teal, bgColor = colors.funcBg, borderColor = colors.funcBorder, cornerRadius = keyCornerRadius, modifier = Modifier.weight(1f), tag = "key_sign", onClick = toggleSign)
                CalcKey50(text = "÷", textColor = colors.brass, bgColor = colors.opBg, borderColor = colors.opBorder, cornerRadius = keyCornerRadius, fontSize = 24.sp, modifier = Modifier.weight(1f), tag = "key_divide", onClick = { chooseOp("÷") })
            }

            // ROW 2: 7, 8, 9, ×
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcKey50(text = "7", cornerRadius = keyCornerRadius, modifier = Modifier.weight(1f), tag = "key_7", onClick = { inputDigit("7") })
                CalcKey50(text = "8", cornerRadius = keyCornerRadius, modifier = Modifier.weight(1f), tag = "key_8", onClick = { inputDigit("8") })
                CalcKey50(text = "9", cornerRadius = keyCornerRadius, modifier = Modifier.weight(1f), tag = "key_9", onClick = { inputDigit("9") })
                CalcKey50(text = "×", textColor = colors.brass, bgColor = colors.opBg, borderColor = colors.opBorder, cornerRadius = keyCornerRadius, fontSize = 24.sp, modifier = Modifier.weight(1f), tag = "key_multiply", onClick = { chooseOp("×") })
            }

            // ROW 3: 4, 5, 6, −
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcKey50(text = "4", cornerRadius = keyCornerRadius, modifier = Modifier.weight(1f), tag = "key_4", onClick = { inputDigit("4") })
                CalcKey50(text = "5", cornerRadius = keyCornerRadius, modifier = Modifier.weight(1f), tag = "key_5", onClick = { inputDigit("5") })
                CalcKey50(text = "6", cornerRadius = keyCornerRadius, modifier = Modifier.weight(1f), tag = "key_6", onClick = { inputDigit("6") })
                CalcKey50(text = "−", textColor = colors.brass, bgColor = colors.opBg, borderColor = colors.opBorder, cornerRadius = keyCornerRadius, fontSize = 24.sp, modifier = Modifier.weight(1f), tag = "key_subtract", onClick = { chooseOp("−") })
            }

            // ROW 4: 1, 2, 3, +
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcKey50(text = "1", cornerRadius = keyCornerRadius, modifier = Modifier.weight(1f), tag = "key_1", onClick = { inputDigit("1") })
                CalcKey50(text = "2", cornerRadius = keyCornerRadius, modifier = Modifier.weight(1f), tag = "key_2", onClick = { inputDigit("2") })
                CalcKey50(text = "3", cornerRadius = keyCornerRadius, modifier = Modifier.weight(1f), tag = "key_3", onClick = { inputDigit("3") })
                CalcKey50(text = "+", textColor = colors.brass, bgColor = colors.opBg, borderColor = colors.opBorder, cornerRadius = keyCornerRadius, fontSize = 24.sp, modifier = Modifier.weight(1f), tag = "key_add", onClick = { chooseOp("+") })
            }

            // ROW 5: 0, 00, 000, .
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcKey50(text = "0", cornerRadius = keyCornerRadius, modifier = Modifier.weight(1f), tag = "key_0", onClick = { inputDigit("0") })
                CalcKey50(text = "00", cornerRadius = keyCornerRadius, fontSize = 18.sp, modifier = Modifier.weight(1f), tag = "key_00", onClick = { inputDigit("00") })
                CalcKey50(text = "000", cornerRadius = keyCornerRadius, fontSize = 16.sp, modifier = Modifier.weight(1f), tag = "key_000", onClick = { inputDigit("000") })
                CalcKey50(text = ".", cornerRadius = keyCornerRadius, fontSize = 24.sp, modifier = Modifier.weight(1f), tag = "key_dot", onClick = { inputDigit(".") })
            }

            // ROW 6: %, = (Equals spans 3 columns, high-contrast gold/brass!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcKey50(
                    text = "%",
                    textColor = colors.brass,
                    bgColor = colors.opBg,
                    borderColor = colors.opBorder,
                    cornerRadius = keyCornerRadius,
                    modifier = Modifier.weight(1f),
                    tag = "key_percent",
                    onClick = { chooseOp("%") }
                )

                // Equals Button
                CalcKey50(
                    text = "=",
                    textColor = colors.equalsText,
                    bgColor = colors.equalsBg,
                    borderColor = colors.brass,
                    cornerRadius = keyCornerRadius,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(3f),
                    tag = "key_equals",
                    onClick = calculate
                )
            }
        }
    }
}

@Composable
fun CalcKey50(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = LocalAmoaColors.current.keyText,
    bgColor: Color = LocalAmoaColors.current.keyBg,
    borderColor: Color = LocalAmoaColors.current.keyBorder,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 21.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .clickable { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = FontFamily.Default,
            color = textColor,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center
        )
    }
}
