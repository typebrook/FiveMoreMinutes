package io.typebrook.fivemoreminutes.utils

import io.typebrook.fmmcore.realm.projection.CoordPrinter

/**
 * Created by pham on 2017/11/5.
 */

// transform raw coordinates to integer string
val xy2IntString: CoordPrinter = { (x, y) ->
    val xString = x.toInt().toString().run {
        if (this.length <= 3) return@run this
        dropLast(3) + "-" + takeLast(3)
    }
    val yString = y.toInt().toString().run {
        if (this.length <= 3) return@run this
        dropLast(3) + "-" + takeLast(3)
    }
    xString to yString
}

// transform raw coordinates to Latitude/Longitude string with Degree with
val xy2DegreeString: CoordPrinter = { (lon, lat) ->

    val lonPrefix = if (lon >= 0) "東經 " else "西經 "
    val latPrefix = if (lat >= 0) "北緯 " else "南緯 "

    val lonString = lon
            .let(Math::abs)
            .with("%.6f")
            .run { dropLast(3).padStart(7, '0') + "-" + takeLast(3) }
    val latString = lat
            .let(Math::abs)
            .with("%.6f")
            .run { dropLast(3).padStart(7, '0') + "-" + takeLast(3) }

    "$lonPrefix $lonString 度" to "$latPrefix $latString 度"
}

// transform raw coordinates to Latitude/Longitude string with Degree/Minute with
typealias dmValue = Pair<Int, Double>
val degree2DM: (Double) -> dmValue = { degree ->
    val dValue = degree.toInt()
    val mValue = (degree - dValue) * 60
    dValue to mValue
}
val xy2DegMinString: CoordPrinter = { (lon, lat) ->

    val lonPrefix = if (lon >= 0) "東經 " else "西經 "
    val latPrefix = if (lat >= 0) "北緯 " else "南緯 "

    val dm2String = { (d, m): dmValue ->
        "${d}度 ${m.with("%.3f")}分"
    }

    val xString = "$lonPrefix ${lon.let(Math::abs).let(degree2DM).let(dm2String)}"
    val yString = "$latPrefix ${lat.let(Math::abs).let(degree2DM).let(dm2String)}"
    xString to yString
}

// transform raw coordinates to Latitude/Longitude string with Degree/Minute/Second with
typealias dmsValue = Triple<Int, Int, Double>
val degree2DMS: (Double) -> dmsValue = { degree ->
    val dValue = degree.toInt()
    val mValue = ((degree - dValue) * 60).toInt()
    val minute2Degree = mValue.toFloat() / 60
    val sValue = (degree - dValue - minute2Degree) * 3600
    Triple(dValue, mValue, sValue)
}
val xy2DMSString: CoordPrinter = { (lon, lat) ->

    val lonPrefix = if (lon >= 0) "東經 " else "西經 "
    val latPrefix = if (lat >= 0) "北緯 " else "南緯 "

    val dms2String = { (d, m, s): dmsValue ->
        "${d}度 ${m}分 ${s.with("%.1f")} 秒"
    }

    val xString = "$lonPrefix ${lon.let(Math::abs).let(degree2DMS).let(dms2String)}"
    val yString = "$latPrefix ${lat.let(Math::abs).let(degree2DMS).let(dms2String)}"
    xString to yString
}

fun Double.with(format: String): String = format.format(this)
fun Float.with(format: String): String = format.format(this)
