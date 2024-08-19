import org.junit.jupiter.api.Test
import tools.aqua.*
import tools.aqua.domain.FormulaBuilder.Companion.formula
import tools.aqua.domain.*
import tools.aqua.parser.Trace
import why.positionOnLaneTrace

class AlwaysProofTest {
    val trace = listOf(
        Trace(TS(1), setOf("a")),
        Trace(TS(3), setOf("a")),
        Trace(TS(4), setOf("a")),
        Trace(TS(5), setOf("")),
        Trace(TS(6), setOf("a")),
    )

    @Test
    fun ttTest() {
        val result = eval(trace, "Always[1,2] True")
        assert(result.size == 2)
        result.forEach { assert(it.value is SatProof) }
    }

    @Test
    fun ffTest() {
        val result = eval(trace, "Always[1,2] False")
        assert(result.size == 2)
        result.forEach { assert(it.value is ViolationProof) }
    }

    @Test
    fun exTest() {
        val result = eval(trace, "Always[1,2] a")
        assert(result.size == 2)
        assert(result[0] is SatProof)
        assert(result[1] is ViolationProof)
    }

    @Test
    fun exTestDSL() {
        val formula = formula { v: Ref<Vehicle> ->
            always(1 to 2) {
                pred(v) { v.now().positionOnLane < 100.0 }
            }
        }
        val ref = Ref(Vehicle::class, 1, true)
        val result = eval(positionOnLaneTrace(), formula.holds(ref))
        //assert(result.size == 2)
        assert(result[0] is SatProof)
        assert(result[1] is ViolationProof)
        assert(result[2] is ViolationProof)
        assert(result[3] is SatProof)
        assert(result[4] is SatProof)
    }
}