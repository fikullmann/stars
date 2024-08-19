import org.junit.jupiter.api.Test
import tools.aqua.*
import tools.aqua.domain.FormulaBuilder.Companion.formula
import tools.aqua.domain.Entity
import tools.aqua.domain.Ref
import tools.aqua.parser.Trace
import tools.aqua.domain.Vehicle

class AndProofTest {
    @Test
    fun ttTest() {
        val trace = listOf(
            Trace(TS(1), setOf("a", "b", "c")),
        )
        val result = eval(trace, "True and True")
        assert(SatAnd(SatTT(TP(0)), SatTT(TP(0))) == result[0])
    }


    @Test
    fun ffTest() {
        val trace = listOf(
            Trace(TS(1), setOf("a", "b", "c")),
        )
        val result = eval(trace, "False and False")
        assert(VAndL(VFF(TP(0))) == result[0])
    }

    @Test
    fun falseMinTest() {
        val trace = listOf(
            Trace(TS(1), setOf("a", "b", "c")),
        )
        val result = eval(trace, "(a and false) and False")
        assert(VAndR(VFF(TP(0))) == result[0])
    }

    @Test
    fun a_and_b_test_false() {
        val trace = listOf(
            Trace(TS(1), setOf("a")),
        )
        val result = eval(trace, "a and b")
        assert(VAndR(VVar(TP(0), "b")) == result[0])
    }

    @Test
    fun aAndBTestTrue() {
        val trace = listOf(
            Trace(TS(1), setOf("a", "b")),
        )
        val result = eval(trace, "a and b")
        assert(SatAnd(SatVar(TP(0), "a"), SatVar(TP(0), "b")) == result[0])
    }

}