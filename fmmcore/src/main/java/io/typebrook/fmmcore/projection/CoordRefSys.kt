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
    return lat > -90 && lat < 90 && lon > -180 && lon < 180
}

fun XYPair.convert(from: CoordRefSys, to: CoordRefSys) = CoordRefSys.generateConverter(from, to)(this)
fun XYPair.isValid(crs: CoordRefSys) = isValidInWGS84(this.convert(crs, CoordRefSys.WGS84))

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
        @Ignore val isLonLatº: Boolean? = null
) : RealmObject() {

    var typeValue: Int = type.ordinal // data stored in Realm

    val crs: CoordinateReferenceSystem
        get() = when (type) {
            ParameterType.Code -> CRSFactory().createFromName(parameter)
            ParameterType.Proj4 -> CRSFactory().createFromParameters(displayName, parameter)
        }

    fun checkIsLonLat(): Boolean {
        val converter = generateConverter(WGS84, this)
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

        val WGS84 = CoordRefSys("WGS84", ParameterType.Code, "EPSG:4326", true)
        val TWD97 = CoordRefSys("TWD97", ParameterType.Code, "EPSG:3826", false)
        val TWD67 = CoordRefSys("TWD67", ParameterType.Proj4, "+proj=tmerc +lat_0=0 +lon_0=121 +k=0.9999 +x_0=250000 +y_0=0 +ellps=aust_SA +towgs84=-750.739,-359.515,-180.510,0.00003863,0.00001721,0.00000197,0.99998180 +units=m +no_defs", false)
    }
}