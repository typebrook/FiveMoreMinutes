package io.typebrook.fmmcore.projection

import org.osgeo.proj4j.CRSFactory
import org.osgeo.proj4j.CoordinateReferenceSystem
import org.osgeo.proj4j.CoordinateTransformFactory
import org.osgeo.proj4j.ProjCoordinate

/**
 * Created by pham on 2017/11/5.
 * func to provide a coordinate converter for different system
 */

typealias XYPair = Pair<Double, Double>
typealias XYString = Pair<String, String>
typealias CoordConverter = (XYPair) -> (XYPair)
typealias CoordPrinter = (XYPair) -> XYString

data class CRS(private val crs: CoordinateReferenceSystem,
               val displayName: String, val printer: CoordPrinter) :
        CoordinateReferenceSystem(crs.name, crs.parameters, crs.datum, crs.projection) {

    companion object {
        fun createFromCode(code: String,
                           displayName: String? = null,
                           printer: CoordPrinter = defaultPrinter
        ): CRS = CRSFactory().createFromName(code).run { CRS(this, displayName ?: name, printer) }

        fun createFromParameters(parameters: String,
                                 displayName: String,
                                 printer: CoordPrinter = defaultPrinter
        ): CRS = CRSFactory().createFromParameters(displayName, parameters).run { CRS(this, displayName, printer) }
    }
}

fun generateConverter(crs1: CRS, crs2: CRS): CoordConverter {
    if (crs1 == crs2) return { xyPair -> xyPair }

    val trans = CoordinateTransformFactory().createTransform(crs1, crs2)

    return { (x, y): XYPair ->
        val p1 = ProjCoordinate(x, y)
        val p2 = ProjCoordinate()
        trans.transform(p1, p2)

        p2.x to p2.y
    }
}
