package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.math.CalculatorEngine
import com.example.units.UnitsData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AMOA Calculator", appName)
    }

    @Test
    fun `test basic arithmetic and formatting`() {
        val res = CalculatorEngine.evaluateMathExpression("2500+7500")
        assertEquals(10000.0, res, 0.0001)
        val formatted = CalculatorEngine.formatDisplay(CalculatorEngine.trimNum(res))
        assertEquals("10,000", formatted)
    }

    @Test
    fun `test chained percentage calculation`() {
        // 26000 + 10% = 28600, + 10% = 31460 (physical calculator behavior)
        val res = CalculatorEngine.evaluateMathExpression("26000+10%+10%")
        assertEquals(31460.0, res, 0.0001)
    }

    @Test
    fun `test scientific functions`() {
        // sin(30) in DEG mode = 0.5
        val sin30 = CalculatorEngine.evaluateMathExpression("sin(30)", degMode = true)
        assertEquals(0.5, sin30, 0.0001)

        // sqrt(144) = 12
        val sqrtVal = CalculatorEngine.evaluateMathExpression("sqrt(144)")
        assertEquals(12.0, sqrtVal, 0.0001)

        // 5! = 120
        val factVal = CalculatorEngine.evaluateMathExpression("5!")
        assertEquals(120.0, factVal, 0.0001)
    }

    @Test
    fun `test unit conversion`() {
        // 1 km = 1000 m
        val meters = UnitsData.convert(1.0, "length", "km", "m")
        assertEquals(1000.0, meters, 0.001)

        // 100 C = 212 F
        val fahrenheit = UnitsData.convert(100.0, "temperature", "C", "F")
        assertEquals(212.0, fahrenheit, 0.001)
    }
}
