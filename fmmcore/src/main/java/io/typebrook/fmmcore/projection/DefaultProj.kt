package io.typebrook.fmmcore.projection

/**
 * Created by pham on 2017/11/5.
 */


val xy2DegreeString: CoordPrinter = { (lon, lat) ->

    val lonPrefix = if (lon >= 0) "東經" else "西經"
    val latPrefix = if (lat >= 0) "北緯  " else "南緯  "

    val lonString = lon
            .let { Math.abs(it) }
            .let { "%.6f".format(it) }
            .run { dropLast(3) + "-" + takeLast(3) }
    val latString = lat
            .let { Math.abs(it) }
            .let { "%.6f".format(it) }
            .run { dropLast(3) + "-" + takeLast(3) }

    "$lonPrefix $lonString 度" to "$latPrefix $latString 度"
}

val xy2DMSString: CoordPrinter = { (lon, lat) ->

    val lonPrefix = if (lon >= 0) "東經" else "西經"
    val latPrefix = if (lat >= 0) "北緯" else "南緯"

    val degree2Dms = { degree: Double ->
        val dValue = degree.toInt()
        val mValue = ((degree - dValue) * 60).toInt()
        val minute2Degree = mValue.toFloat() / 60
        val sValue = (degree - dValue - minute2Degree) * 3600

        "${dValue}度${mValue}分${"%.1f".format(sValue)} 秒"
    }

    "$lonPrefix ${degree2Dms(Math.abs(lon))}" to "$latPrefix ${degree2Dms(Math.abs(lat))}"
}

val xy2MeterString: CoordPrinter = { (x, y) ->
    val xString = x.toInt().toString().run { dropLast(3) + "-" + takeLast(3) }
    val yString = y.toInt().toString().run { dropLast(3) + "-" + takeLast(3) }
    xString to yString
}

val WGS84 = Datum(ParameterType.Code.ordinal, "EPSG:4326", "WGS84", true)
val TWD97 = Datum(ParameterType.Code.ordinal, "EPSG:3826", "TWD97")
val TWD67 = Datum(ParameterType.BursaWolf.ordinal, "+proj=tmerc +lat_0=0 +lon_0=121 +k=0.9999 +x_0=250000 +y_0=0 +ellps=aust_SA +towgs84=-752,-358,-179,-0.0000011698,0.0000018398,0.0000009822,0.00002329 +units=m +no_defs", "TWD67")