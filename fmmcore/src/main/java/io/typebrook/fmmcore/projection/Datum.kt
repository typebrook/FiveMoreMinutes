package io.typebrook.fmmcore.projection

import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.osgeo.proj4j.CRSFactory
import org.osgeo.proj4j.CoordinateReferenceSystem
import org.osgeo.proj4j.CoordinateTransformFactory
import org.osgeo.proj4j.ProjCoordinate
import java.util.*

/**
 * Created by pham on 2017/11/13.
 * func to provide a coordinate converter for different system
 */

typealias XYPair = Pair<Double, Double>
typealias XYString = Pair<String, String>
typealias CoordConverter = (XYPair) -> (XYPair)
typealias CoordPrinter = (XYPair) -> XYString

enum class ParameterType {
    Code,
    BursaWolf
}

@RealmClass
open class Datum() : RealmObject() {

    @PrimaryKey
    private var id: String = UUID.randomUUID().toString()
    var typeValue: Int = ParameterType.Code.ordinal
    var parameter: String = ""
    var displayName: String = ""
    @Ignore
    var printerº: CoordPrinter? = null

    @Throws
    constructor(parameterType: ParameterType, parameter: String, displayName: String, printer: CoordPrinter? = null
    ) : this() {
        this.typeValue = parameterType.ordinal
        this.parameter = parameter
        this.displayName = displayName
        this.printerº = printer
        this.crs
    }

    val crs: CoordinateReferenceSystem
        get() = when (ParameterType.values()[typeValue]) {
            ParameterType.Code -> CRSFactory().createFromName(parameter)
            ParameterType.BursaWolf -> CRSFactory().createFromParameters(displayName, parameter)
        }

    override fun hashCode(): Int = parameter.hashCode() shl typeValue
    override fun equals(other: Any?): Boolean = other is Datum
            && typeValue == other.typeValue
            && parameter == other.parameter
            && printerº == other.printerº

    companion object {

        fun buildByCode(parameter: String, displayName: String, printer: CoordPrinter? = null)
                = Datum(ParameterType.Code, parameter, displayName, printer)

        fun buildByBursaWolf(parameter: String, displayName: String, printer: CoordPrinter? = null)
                = Datum(ParameterType.BursaWolf, parameter, displayName, printer)

        fun generateConverter(crs1: Datum, crs2: Datum): CoordConverter {
            if (crs1 == crs2) return { xyPair -> xyPair }

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