package io.typebrook.fmmcore.projection

/**
 * Created by pham on 2017/11/5.
 */

val defaultPrinter: CoordPrinter = { (x, y) -> "$x" to "$y" }

val xy2DegreeString: CoordPrinter = { (lon, lat) ->

    val lonPrefix = if (lon >= 0) "東經" else "西經"
    val latPrefix = if (lat >= 0) "北緯" else "南緯"

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

    "$lonPrefix ${degree2Dms(lon)}" to "$latPrefix ${degree2Dms(lat)}"
}

val xy2TWDString: CoordPrinter = { (x, y) ->

    val xString = x.toInt().toString()
            .run { dropLast(3) + "-" + takeLast(3) }

    val yString = y.toInt().toString()
            .run { dropLast(3) + "-" + takeLast(3) }

    xString to yString
}

val WGS84_Degree = CRS.createFromCode("EPSG:4326", "WGS84(度)", xy2DegreeString)
val WGS84_DMS = CRS.createFromCode("EPSG:4326", "WGS84(度分秒)", xy2DMSString)
val TWD97 = CRS.createFromCode("EPSG:3826", "TWD97", xy2TWDString)
val TWD67 = CRS.createFromCode("EPSG:3828", "TWD67", xy2TWDString)