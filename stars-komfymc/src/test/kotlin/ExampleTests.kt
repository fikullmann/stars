import org.junit.jupiter.api.Test
import tools.aqua.TS
import tools.aqua.parser.Trace
import java.io.File

class ExampleTests {
    @Test
    fun testTest() {
        val splittedLine = "@38 q".trim().drop(1).split(" ")

        val xyz = splittedLine.drop(1).toSet()
        println()
    }
}