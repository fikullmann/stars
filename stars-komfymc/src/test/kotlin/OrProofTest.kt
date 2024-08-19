import org.junit.jupiter.api.Test
import tools.aqua.*
import tools.aqua.parser.Trace

class OrProofTest {
    @Test
    fun ttTest() {
        val trace = listOf(
            Trace(TS(1), setOf("a", "b", "c")),
        )
        val result = eval(trace, "True or True")
        assert(SatOrL(SatTT(TP(0))) == result[0])
    }

    @Test
    fun ttMinTest() {
        val trace = listOf(
            Trace(TS(1), setOf("a", "b", "c")),
        )
        val result = eval(trace, "(a and False) or True")
        assert(SatOrR(SatTT(TP(0))) == result[0])
    }

    @Test
    fun ffTest() {
        val trace = listOf(
            Trace(TS(1), setOf("a", "b", "c")),
        )
        val result = eval(trace, "False or False")
        assert(VOr(VFF(TP(0)), VFF(TP(0))) == result[0])
    }

    @Test
    fun aOrBTestFalse() {
        val trace = listOf(
            Trace(TS(1), setOf()),
        )
        val result = eval(trace, "a or b")
        assert(VOr(VVar(TP(0), "a"), VVar(TP(0), "b")) == result[0])
    }

    @Test
    fun aOrBTestTrue() {
        val trace = listOf(
            Trace(TS(1), setOf("a", "b", "c")),
        )
        val result = eval(trace, "a or b")
        assert(SatOrL(SatVar(TP(0), "a")) == result[0])
    }
}