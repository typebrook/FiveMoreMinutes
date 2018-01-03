package io.typebrook.fmmcore.projection

import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.RealmClass
import org.osgeo.proj4j.CRSFactory
import org.osgeo.proj4j.CoordinateReferenceSystem
import org.osgeo.proj4j.CoordinateTransformFactory
import org.osgeo.proj4j.ProjCoordinate

/**
 * Created by pham on 2017/11/13.
 * func to provide a coordinate converter for different system
 */

typealias XYPair = Pair<Double, Double>
typealias XYString = Pair<String, String>
typealias CoordConverter = (XYPair) -> (XYPair)
typealias CoordPrinter = (XYPair) -> XYString

fun isValidInWGS84(xy: XYPair): Boolean {
    val (lon, lat) = xy
    return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180
}

enum class ParameterType {
    Code,
    BursaWolf
}

enum class Expression {
    Int,
    Degree,
    DegMin,
    DMS
}

@RealmClass
open class CoordRefSys() : RealmObject() {

    var typeValue: Int = ParameterType.Code.ordinal
    var parameter: String = ""
    var displayName: String = ""
    var isLonLat = true

    constructor(
            type: ParameterType,
            parameter: String,
            displayName: String,
            isLonLatº: Boolean? = null
    ) : this() {
        this.typeValue = type.ordinal
        this.parameter = parameter
        this.displayName = displayName
        this.isLonLat = isLonLatº ?: checkIsLonLat()
    }

    val crs: CoordinateReferenceSystem
        get() = when (ParameterType.values()[typeValue]) {
            ParameterType.Code -> CRSFactory().createFromName(parameter)
            ParameterType.BursaWolf -> CRSFactory().createFromParameters(displayName, parameter)
        }

    private fun checkIsLonLat(): Boolean {
        val testCrs = CoordRefSys(ParameterType.values()[typeValue], parameter, "", false)
        val converter = generateConverter(WGS84, testCrs)
        return converter(179.0 to 89.0).let { (x, y) -> x < 180 && y < 90 }
    }

    companion object {

        fun generateConverter(crs1: CoordRefSys, crs2: CoordRefSys): CoordConverter {
            if (crs1.parameter == crs2.parameter) return { xyPair -> xyPair }

            val trans = CoordinateTransformFactory().createTransform(crs1.crs, crs2.crs)
            return { (x, y): XYPair ->
                val p1 = ProjCoordinate(x, y)
                val p2 = ProjCoordinate()
                trans.transform(p1, p2)

                p2.x to p2.y
            }
        }
    }
}