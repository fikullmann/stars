import org.junit.jupiter.api.Test
import tools.aqua.*
import tools.aqua.parser.Trace

class EventuallyProofTest {
    val trace = listOf(
        Trace(TS(1), setOf("a")),
        Trace(TS(2), setOf("a")),
        Trace(TS(3), setOf("")),
        Trace(TS(4), setOf("")),
        Trace(TS(5), setOf("a")),
        Trace(TS(6), setOf("")),
    )

    @Test
    fun ttTest() {
        val result = eval(trace, "Eventually[1,2] True")
        assert(result.size == 3)
        result.forEach { assert(it.value is SatProof) }
    }

    @Test
    fun ffTest() {
        val result = eval(trace, "Eventually[1,2] False")
        assert(result.size == 3)
        result.forEach { assert(it.value is ViolationProof) }
    }

    @Test
    fun exTest() {
        val result = eval(trace, "Eventually[1,2] a")
        assert(result.size == 3)
        assert(result[0] is SatProof)
        assert(result[1] is ViolationProof)
        assert(result[2] is SatProof)
    }
}