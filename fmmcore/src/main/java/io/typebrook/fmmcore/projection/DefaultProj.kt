package io.typebrook.fmmcore.projection

/**
 * Created by pham on 2017/11/5.
 */


val xy2DegreeString: CoordPrinter = { (lon, lat) ->

    val lonPrefix = if (lon >= 0) "東經 " else "西經 "
    val latPrefix = if (lat >= 0) "北緯 " else "南緯 "

    val lonString = lon
            .let(Math::abs)
            .let { "%.6f".format(it) }
            .run { dropLast(3) + "-" + takeLast(3) }
    val latString = lat
            .let(Math::abs)
            .let { "%.6f".format(it) }
            .run { dropLast(3) + "-" + takeLast(3) }

    "$lonPrefix $lonString 度" to "$latPrefix $latString 度"
}

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
        "${d}度 ${"%.3f".format(m)}分"
    }

    val xString = "$lonPrefix ${lon.let(Math::abs).let(degree2DM).let(dm2String)}"
    val yString = "$latPrefix ${lat.let(Math::abs).let(degree2DM).let(dm2String)}"
    xString to yString
}

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
        "${d}度 ${m}分 ${"%.1f".format(s)} 秒"
    }

    val xString = "$lonPrefix ${lon.let(Math::abs).let(degree2DMS).let(dms2String)}"
    val yString = "$latPrefix ${lat.let(Math::abs).let(degree2DMS).let(dms2String)}"
    xString to yString
}

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

val WGS84 = CoordRefSys("WGS84", ParameterType.Code, "EPSG:4326", true)
val TWD97 = CoordRefSys("TWD97", ParameterType.Code, "EPSG:3826", false)
val TWD67 = CoordRefSys("TWD67", ParameterType.Proj4, "+proj=tmerc +lat_0=0 +lon_0=121 +k=0.9999 +x_0=250000 +y_0=0 +ellps=aust_SA +towgs84=-750.739,-359.515,-180.510,0.00003863,0.00001721,0.00000197,0.99998180 +units=m +no_defs", false)