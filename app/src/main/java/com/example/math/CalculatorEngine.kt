package com.example.math

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.*

object CalculatorEngine {

    fun formatDisplay(numStr: String?): String {
        if (numStr == null || numStr == "Error") return "Error"
        val isNegative = numStr.startsWith("-") || numStr.startsWith("−")
        val clean = if (isNegative) numStr.substring(1) else numStr

        if (clean.matches(Regex("""^\d+(\.\d+)?(e[+-]?\d+)?$""", RegexOption.IGNORE_CASE))) {
            val parts = clean.split(".")
            val intPart = parts[0]
            val decPart = if (parts.size > 1) parts[1] else null
            val formattedInt = formatWithCommas(intPart)
            return (if (isNegative) "−" else "") + formattedInt + (if (decPart != null) ".$decPart" else "")
        }

        // Format embedded numbers inside expressions
        return (if (isNegative) "−" else "") + clean.replace(Regex("""\b\d+(\.\d+)?\b""")) { matchResult ->
            val num = matchResult.value
            val parts = num.split(".")
            val intPart = parts[0]
            val decPart = if (parts.size > 1) parts[1] else null
            formatWithCommas(intPart) + (if (decPart != null) ".$decPart" else "")
        }
    }

    private fun formatWithCommas(intStr: String): String {
        if (intStr.isEmpty()) return "0"
        val sb = StringBuilder()
        val len = intStr.length
        for (i in 0 until len) {
            sb.append(intStr[i])
            val distFromEnd = len - 1 - i
            if (distFromEnd > 0 && distFromEnd % 3 == 0) {
                sb.append(",")
            }
        }
        return sb.toString()
    }

    fun trimNum(value: Double): String {
        if (!value.isFinite() || value.isNaN()) return "Error"
        return try {
            val bd = BigDecimal(value.toString())
            val rounded = bd.setScale(12, RoundingMode.HALF_UP).stripTrailingZeros()
            val plain = rounded.toPlainString()
            if (plain == "-0") "0" else plain
        } catch (_: Exception) {
            val s = String.format(Locale.US, "%.10f", value).trimEnd('0').trimEnd('.')
            if (s == "-0" || s.isEmpty()) "0" else s
        }
    }

    private fun factorial(n: Double): Double {
        if (n < 0 || n != floor(n)) return Double.NaN
        if (n == 0.0 || n == 1.0) return 1.0
        if (n > 170.0) return Double.POSITIVE_INFINITY
        var res = 1.0
        val count = n.toLong()
        for (i in 2..count) {
            res *= i
        }
        return res
    }

    private fun nPr(n: Double, r: Double): Double {
        if (n < 0 || r < 0 || r > n) return Double.NaN
        return factorial(n) / factorial(n - r)
    }

    private fun nCr(n: Double, r: Double): Double {
        if (n < 0 || r < 0 || r > n) return Double.NaN
        return factorial(n) / (factorial(r) * factorial(n - r))
    }

    // Resolves chained percentages: A + B% + C% applies to running total
    private fun resolveChainedPercent(s: String): String {
        val numRegex = Regex("""^-?[0-9]*\.?[0-9]+$""")
        var depth = 0
        data class Part(val text: String, val sign: Char)
        val parts = mutableListOf<Part>()
        var current = StringBuilder()
        var currentSign = '+'

        for (i in s.indices) {
            val c = s[i]
            if (c == '(') depth++
            if (c == ')') depth--
            if (depth == 0 && (c == '+' || c == '-') && current.toString().trim().isNotEmpty()) {
                parts.add(Part(current.toString(), currentSign))
                current = StringBuilder()
                currentSign = c
                continue
            }
            current.append(c)
        }
        parts.add(Part(current.toString(), currentSign))

        if (parts.size <= 1) return s

        val rebuilt = StringBuilder()
        var running = 0.0
        var runningKnown = false

        parts.forEachIndexed { idx, part ->
            val trimmed = part.text.trim()
            val bareMatch = Regex("""^([0-9]*\.?[0-9]+)%$""").matchEntire(trimmed)

            if (idx == 0) {
                rebuilt.append(part.text)
                if (trimmed.matches(numRegex)) {
                    running = trimmed.toDoubleOrNull() ?: 0.0
                    runningKnown = true
                }
                return@forEachIndexed
            }

            if (bareMatch != null && runningKnown) {
                val pct = bareMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                val amt = (running * pct) / 100.0
                rebuilt.append(part.sign).append("($amt)")
                running = if (part.sign == '+') running + amt else running - amt
                return@forEachIndexed
            }

            rebuilt.append(part.sign).append(part.text)
            if (runningKnown && trimmed.matches(numRegex)) {
                val plain = trimmed.toDoubleOrNull() ?: 0.0
                running = if (part.sign == '+') running + plain else running - plain
            } else {
                runningKnown = false
            }
        }

        return rebuilt.toString()
    }

    private class Parser(
        private val tokens: List<String>,
        private val degMode: Boolean
    ) {
        var pos = 0
        val funcs = listOf("sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh", "sqrt", "cbrt", "log", "ln", "abs")

        fun parse(): Double {
            return try {
                parseExpression()
            } catch (_: Exception) {
                Double.NaN
            }
        }

        fun parseExpression(): Double {
            var left = parseTerm()
            while (pos < tokens.size) {
                val op = tokens[pos]
                if (op == "+" || op == "-") {
                    pos++
                    val right = parseTerm()
                    left = if (op == "+") left + right else left - right
                } else break
            }
            return left
        }

        fun parseTerm(): Double {
            var left = parsePower()
            while (pos < tokens.size) {
                val op = tokens[pos]
                if (op in listOf("*", "/", "%", "mod", "nCr", "nPr")) {
                    pos++
                    val right = parsePower()
                    when (op) {
                        "*" -> left *= right
                        "/" -> left = if (right == 0.0) Double.NaN else left / right
                        "%" -> left = (left * right) / 100.0
                        "mod" -> left %= right
                        "nCr" -> left = nCr(left, right)
                        "nPr" -> left = nPr(left, right)
                    }
                } else break
            }
            return left
        }

        fun parsePower(): Double {
            var left = parseFactor()
            if (pos < tokens.size && tokens[pos] == "^") {
                pos++
                val right = parsePower()
                left = left.pow(right)
            }
            return left
        }

        fun parseFactor(): Double {
            if (pos >= tokens.size) return Double.NaN
            val op = tokens[pos]
            if (op == "-") {
                pos++
                return -parseFactor()
            }
            if (op == "+") {
                pos++
                return parseFactor()
            }
            var value = parsePrimary()
            while (pos < tokens.size && tokens[pos] == "!") {
                pos++
                value = factorial(value)
            }
            return value
        }

        fun parsePrimary(): Double {
            if (pos >= tokens.size) return Double.NaN
            val token = tokens[pos]

            if (token == "(") {
                pos++
                val value = parseExpression()
                if (pos < tokens.size && tokens[pos] == ")") pos++
                return value
            }

            if (token == "|") {
                pos++
                val value = parseExpression()
                if (pos < tokens.size && tokens[pos] == "|") pos++
                return abs(value)
            }

            if (token.matches(Regex("""^[0-9.]+$"""))) {
                pos++
                return token.toDoubleOrNull() ?: Double.NaN
            }

            if (token in funcs) {
                val fn = token
                pos++
                var arg = 0.0
                if (pos < tokens.size && tokens[pos] == "(") {
                    pos++
                    arg = parseExpression()
                    if (pos < tokens.size && tokens[pos] == ")") pos++
                } else {
                    arg = parsePrimary()
                }

                fun toRad(deg: Double) = deg * Math.PI / 180.0
                fun toDeg(rad: Double) = rad * 180.0 / Math.PI

                return when (fn) {
                    "sin" -> sin(if (degMode) toRad(arg) else arg)
                    "cos" -> cos(if (degMode) toRad(arg) else arg)
                    "tan" -> tan(if (degMode) toRad(arg) else arg)
                    "asin" -> if (degMode) toDeg(asin(arg)) else asin(arg)
                    "acos" -> if (degMode) toDeg(acos(arg)) else acos(arg)
                    "atan" -> if (degMode) toDeg(atan(arg)) else atan(arg)
                    "sinh" -> sinh(arg)
                    "cosh" -> cosh(arg)
                    "tanh" -> tanh(arg)
                    "sqrt" -> sqrt(arg)
                    "cbrt" -> cbrt(arg)
                    "log" -> log10(arg)
                    "ln" -> ln(arg)
                    "abs" -> abs(arg)
                    else -> Double.NaN
                }
            }

            pos++
            return Double.NaN
        }
    }

    fun evaluateMathExpression(rawExpr: String?, degMode: Boolean = true): Double {
        if (rawExpr.isNullOrBlank() || rawExpr == "Error") return Double.NaN

        var s = rawExpr
            .replace("−", "-")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("[", "(")
            .replace("]", ")")
            .replace("{", "(")
            .replace("}", ")")
            .replace("π", "(${Math.PI})")
            .replace(Regex("""e(?![a-z0-9_])""", RegexOption.IGNORE_CASE), "(${Math.E})")

        s = resolveChainedPercent(s)
        s = s.replace(Regex("""([*/])\s*([0-9]*\.?[0-9]+)%""")) { m ->
            "${m.groupValues[1]}(${m.groupValues[2]}/100)"
        }
        s = s.replace(Regex("""([0-9]*\.?[0-9]+)%""")) { m ->
            "(${m.groupValues[1]}/100)"
        }

        // Auto-balance open parentheses
        var openCount = 0
        for (c in s) {
            if (c == '(') openCount++
            if (c == ')') openCount--
        }
        while (openCount > 0) {
            s += ")"
            openCount--
        }

        // Tokenize
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c.isWhitespace()) {
                i++
                continue
            }
            if (c.isDigit() || c == '.') {
                val num = StringBuilder()
                while (i < s.length && (s[i].isDigit() || s[i] == '.')) {
                    num.append(s[i])
                    i++
                }
                tokens.add(num.toString())
            } else if (c.isLetter()) {
                val word = StringBuilder()
                while (i < s.length && s[i].isLetter()) {
                    word.append(s[i])
                    i++
                }
                tokens.add(word.toString())
            } else if (c in listOf('+', '-', '*', '/', '^', '%', '!', '(', ')', '|')) {
                tokens.add(c.toString())
                i++
            } else {
                i++
            }
        }

        // Handle implicit multiplication (e.g., 2(3) -> 2*(3), 5sin(30) -> 5*sin(30))
        val processedTokens = mutableListOf<String>()
        val funcs = listOf("sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh", "sqrt", "cbrt", "log", "ln", "abs")

        for (idx in tokens.indices) {
            val curr = tokens[idx]
            val prev = if (processedTokens.isNotEmpty()) processedTokens.last() else null
            if (prev != null && (prev[0].isDigit() || prev == ")" || prev == "!") &&
                (curr == "(" || curr in funcs || curr[0].isDigit())
            ) {
                processedTokens.add("*")
            }
            processedTokens.add(curr)
        }

        val parser = Parser(processedTokens, degMode)
        return parser.parse()
    }

    fun computeLivePreview(str: String?, degMode: Boolean = true): String? {
        if (str.isNullOrBlank() || str == "Error") return null
        if (str.matches(Regex("""^[0-9.]+$"""))) return null // plain number typed so far

        val trimmed = str.replace(Regex("""[+\-−×÷*/^]+$"""), "")
        if (trimmed.isEmpty()) return null

        return try {
            val res = evaluateMathExpression(trimmed, degMode)
            if (res.isNaN() || !res.isFinite()) null else trimNum(res)
        } catch (_: Exception) {
            null
        }
    }

    fun findNumberBounds(exprString: String, anchor: Int): Pair<Int, Int> {
        var left = anchor.coerceIn(0, exprString.length)
        var right = anchor.coerceIn(0, exprString.length)

        while (left > 0 && (exprString[left - 1].isDigit() || exprString[left - 1] == '.')) {
            left--
        }
        while (right < exprString.length && (exprString[right].isDigit() || exprString[right] == '.')) {
            right++
        }
        if (left == right && anchor > 0 && (exprString[anchor - 1].isDigit() || exprString[anchor - 1] == '.')) {
            right = anchor
            left = anchor
            while (left > 0 && (exprString[left - 1].isDigit() || exprString[left - 1] == '.')) {
                left--
            }
        }
        if (left > 0 && (exprString[left - 1] == '-' || exprString[left - 1] == '−')) {
            left--
        }
        return Pair(left, right)
    }
}
