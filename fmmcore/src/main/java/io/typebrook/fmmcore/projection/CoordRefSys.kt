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
fun XYPair.convert(from: CoordRefSys, to: CoordRefSys) = CoordRefSys.generateConverter(from, to)(this)
typealias XYString = Pair<String, String>
typealias CoordConverter = (XYPair) -> (XYPair)
typealias CoordPrinter = (XYPair) -> XYString

fun isValidInWGS84(xy: XYPair): Boolean {
    val (lon, lat) = xy
    return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180
}

enum class ParameterType {
    Code,
    Proj4
}

enum class Expression {
    Int,
    Degree,
    DegMin,
    DMS
}

@RealmClass
open class CoordRefSys(
        var displayName: String = "", // data stored in Realm
        @Ignore val type: ParameterType = ParameterType.Code,
        var parameter: String = "", // data stored in Realm
        isLonLatº: Boolean? = null
) : RealmObject() {

    var typeValue = type.ordinal // data stored in Realm

    @Ignore
    val isLonLat = isLonLatº ?: checkIsLonLat()

    val crs: CoordinateReferenceSystem
        get() = when (type) {
            ParameterType.Code -> CRSFactory().createFromName(parameter)
            ParameterType.Proj4 -> CRSFactory().createFromParameters(displayName, parameter)
        }

    private fun checkIsLonLat(): Boolean {
        val testCrs = CoordRefSys("", type, parameter, false)
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