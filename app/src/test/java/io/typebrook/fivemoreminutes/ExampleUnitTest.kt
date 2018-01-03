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
        class blabla {
            val name: String

            constructor(name: String) {
                this.name = name
                println("in constructor")
            }

            init {
                println("in init")
            }
        }

        blabla("yes")
    }
}