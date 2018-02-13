package io.typebrook.fmmcore.projection

import io.realm.annotations.RealmClass

/**
 * Created by pham on 2018/2/11.
 */

enum class Section(val xy: XYPair) {
    A(170000.0 to 2750000.0),
    B(250000.0 to 2750000.0),
    C(330000.0 to 2750000.0),
    D(170000.0 to 2700000.0),
    E(250000.0 to 2700000.0),
    F(330000.0 to 2700000.0),
    G(170000.0 to 2650000.0),
    H(250000.0 to 2650000.0),
    J(170000.0 to 2600000.0),
    K(170000.0 to 2600000.0),
    L(250000.0 to 2600000.0),
    M(170000.0 to 2550000.0),
    N(170000.0 to 2550000.0),
    O(250000.0 to 2550000.0),
    P(170000.0 to 2500000.0),
    Q(170000.0 to 2500000.0),
    R(250000.0 to 2500000.0),
    T(170000.0 to 2450000.0),
    U(250000.0 to 2450000.0),
    V(170000.0 to 2400000.0),
    W(250000.0 to 2400000.0)
}

enum class Square {
    A, B, C, D, E, F, G, H
}

@RealmClass
object TaipowerCrs : CoordRefSys(
        "台灣電力座標",
        ParameterType.Proj4,
        "+proj=tmerc +lat_0=0 +lon_0=121 +k=0.9999 +x_0=250000 +y_0=0 +ellps=aust_SA  +towgs84=-750.739,-359.515,-180.510,0.00003863,0.00001721,0.00000197,0.99998180 +units=m +no_defs"
        ) {

    @Throws
    fun reverseMask(coord: String): XYPair {
        val section = coord[0].toString()
        val sectionXY = Section.values().firstOrNull { it.name == section }?.xy
                ?: throw UnknownError()
        val numberXY = coord.substring(1, 3).toInt() * 800 to coord.substring(3, 5).toInt() * 500
        val squareXY = coord.substring(5, 7).run {
            val xIndex = Square.values().first { it.name == this[0].toString() }.ordinal
            val yIndex = Square.values().first { it.name == this[1].toString() }.ordinal
            xIndex * 100 to yIndex * 100
        }
        val visionXY = coord.substring(7, 9).run {
            this[0].toInt() * 100 to this[1].toInt() * 100
        }

        return sectionXY.first + numberXY.first + squareXY.first + visionXY.first to
                sectionXY.second + numberXY.second + squareXY.second + visionXY.second
    }
}