import tools.aqua.*
import tools.aqua.parser.Trace
import kotlin.test.Test

class SinceProofTest {
    val trace = listOf(
        Trace(TS(1), setOf("a", "b", "c")),
        Trace(TS(3), setOf("a", "b")),
        Trace(TS(3), setOf("a", "b")),
        Trace(TS(3), setOf()),
        Trace(TS(3), setOf("a")),
        Trace(TS(4), setOf("a")),
    )

    @Test
    fun tt_test() {
        val result = eval(trace, "True Since True")
        result.forEach { (_, proof) ->
            assert(proof is SatProof)
        }
    }

    @Test
    fun falseS12truetest() {
        val result = eval(trace, "False Since[1,2] True")
        result.forEach { (_, proof) ->
            assert(proof is ViolationProof)
        }
    }

    @Test
    fun ff_test() {
        val result = eval(trace, "false Since false")
        result.forEach { (_, proof) ->
            assert(proof is ViolationProof)
        }
    }

    @Test
    fun paperEx_test() {
        val result = eval(trace, "a S[1,2] (b and c)")
        assert(result[0] is ViolationProof)
        assert(result[1] is SatProof)
        assert(result[2] is SatProof)
        assert(result[3] is ViolationProof)
        assert(result[4] is ViolationProof)
        assert(result[5] is ViolationProof)
    }

    @Test
    fun vinf_test() {
        val trace2 = listOf(
            Trace(TS(2), setOf("a", "b")),
            Trace(TS(3), setOf("a", "b")),
            Trace(TS(4), setOf("a")),
        )
        val result = eval(trace2, "a S[1,2] (b and c)")
        assert(result[2] is VSinceInf)
    }

}