package io.typebrook.fivemoreminutes

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    @Test
    @Throws(Exception::class)
    fun wrongSubList() {
        println(listOf<String>("at").subList(0, 1))
    }
}