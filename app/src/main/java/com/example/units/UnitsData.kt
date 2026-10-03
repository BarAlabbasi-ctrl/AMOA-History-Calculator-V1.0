package com.example.units

data class UnitItem(
    val key: String,
    val label: String,
    val factor: Double = 1.0 // relative to base unit
)

data class UnitCategory(
    val id: String,
    val label: String,
    val baseUnit: String,
    val units: List<UnitItem>
)

object UnitsData {
    val categories: List<UnitCategory> = listOf(
        UnitCategory(
            id = "length",
            label = "Length",
            baseUnit = "m",
            units = listOf(
                UnitItem("mm", "Millimeter (mm)", 0.001),
                UnitItem("cm", "Centimeter (cm)", 0.01),
                UnitItem("m", "Meter (m)", 1.0),
                UnitItem("km", "Kilometer (km)", 1000.0),
                UnitItem("in", "Inch (in)", 0.0254),
                UnitItem("ft", "Foot (ft)", 0.3048),
                UnitItem("yd", "Yard (yd)", 0.9144),
                UnitItem("mi", "Mile (mi)", 1609.344),
                UnitItem("nmi", "Nautical Mile (nmi)", 1852.0)
            )
        ),
        UnitCategory(
            id = "weight",
            label = "Weight / Mass",
            baseUnit = "kg",
            units = listOf(
                UnitItem("mg", "Milligram (mg)", 0.000001),
                UnitItem("g", "Gram (g)", 0.001),
                UnitItem("kg", "Kilogram (kg)", 1.0),
                UnitItem("t", "Metric Ton (t)", 1000.0),
                UnitItem("oz", "Ounce (oz)", 0.028349523125),
                UnitItem("lb", "Pound (lb)", 0.45359237),
                UnitItem("st", "Stone (st)", 6.35029318)
            )
        ),
        UnitCategory(
            id = "temperature",
            label = "Temperature",
            baseUnit = "C",
            units = listOf(
                UnitItem("C", "Celsius (°C)", 1.0),
                UnitItem("F", "Fahrenheit (°F)", 1.0),
                UnitItem("K", "Kelvin (K)", 1.0)
            )
        ),
        UnitCategory(
            id = "area",
            label = "Area",
            baseUnit = "sqm",
            units = listOf(
                UnitItem("sqmm", "Sq Millimeter (mm²)", 0.000001),
                UnitItem("sqcm", "Sq Centimeter (cm²)", 0.0001),
                UnitItem("sqm", "Sq Meter (m²)", 1.0),
                UnitItem("sqkm", "Sq Kilometer (km²)", 1000000.0),
                UnitItem("sqin", "Sq Inch (in²)", 0.00064516),
                UnitItem("sqft", "Sq Foot (ft²)", 0.09290304),
                UnitItem("ac", "Acre (ac)", 4046.8564224),
                UnitItem("ha", "Hectare (ha)", 10000.0)
            )
        ),
        UnitCategory(
            id = "volume",
            label = "Volume",
            baseUnit = "l",
            units = listOf(
                UnitItem("ml", "Milliliter (ml)", 0.001),
                UnitItem("l", "Liter (l)", 1.0),
                UnitItem("cum", "Cubic Meter (m³)", 1000.0),
                UnitItem("tsp", "US Teaspoon (tsp)", 0.00492892),
                UnitItem("tbsp", "US Tablespoon (tbsp)", 0.0147868),
                UnitItem("floz", "US Fluid Ounce (fl oz)", 0.0295735),
                UnitItem("cup", "US Cup", 0.24),
                UnitItem("pt", "US Pint (pt)", 0.473176),
                UnitItem("qt", "US Quart (qt)", 0.946353),
                UnitItem("gal", "US Gallon (gal)", 3.78541)
            )
        ),
        UnitCategory(
            id = "speed",
            label = "Speed",
            baseUnit = "mps",
            units = listOf(
                UnitItem("mps", "Meter/sec (m/s)", 1.0),
                UnitItem("kmh", "Km/hour (km/h)", 0.277777778),
                UnitItem("mph", "Miles/hour (mph)", 0.44704),
                UnitItem("kn", "Knot (kn)", 0.514444),
                UnitItem("fps", "Feet/sec (ft/s)", 0.3048)
            )
        ),
        UnitCategory(
            id = "storage",
            label = "Data Storage",
            baseUnit = "B",
            units = listOf(
                UnitItem("B", "Byte (B)", 1.0),
                UnitItem("KB", "Kilobyte (KB)", 1024.0),
                UnitItem("MB", "Megabyte (MB)", 1048576.0),
                UnitItem("GB", "Gigabyte (GB)", 1073741824.0),
                UnitItem("TB", "Terabyte (TB)", 1099511627776.0),
                UnitItem("PB", "Petabyte (PB)", 1125899906842624.0)
            )
        ),
        UnitCategory(
            id = "energy",
            label = "Energy",
            baseUnit = "J",
            units = listOf(
                UnitItem("J", "Joule (J)", 1.0),
                UnitItem("kJ", "Kilojoule (kJ)", 1000.0),
                UnitItem("cal", "Calorie (cal)", 4.184),
                UnitItem("kcal", "Kilocalorie (kcal)", 4184.0),
                UnitItem("Wh", "Watt-hour (Wh)", 3600.0),
                UnitItem("kWh", "Kilowatt-hour (kWh)", 3600000.0)
            )
        ),
        UnitCategory(
            id = "pressure",
            label = "Pressure",
            baseUnit = "Pa",
            units = listOf(
                UnitItem("Pa", "Pascal (Pa)", 1.0),
                UnitItem("kPa", "Kilopascal (kPa)", 1000.0),
                UnitItem("bar", "Bar", 100000.0),
                UnitItem("psi", "PSI (psi)", 6894.75729),
                UnitItem("atm", "Atmosphere (atm)", 101325.0)
            )
        )
    )

    fun convert(value: Double, categoryId: String, fromKey: String, toKey: String): Double {
        if (categoryId == "temperature") {
            var celsius = value
            if (fromKey == "F") celsius = ((value - 32.0) * 5.0) / 9.0
            if (fromKey == "K") celsius = value - 273.15

            return when (toKey) {
                "C" -> celsius
                "F" -> (celsius * 9.0) / 5.0 + 32.0
                "K" -> celsius + 273.15
                else -> celsius
            }
        }

        val cat = categories.find { it.id == categoryId } ?: return value
        val fromUnit = cat.units.find { it.key == fromKey } ?: return value
        val toUnit = cat.units.find { it.key == toKey } ?: return value

        val baseValue = value * fromUnit.factor
        return baseValue / toUnit.factor
    }
}
