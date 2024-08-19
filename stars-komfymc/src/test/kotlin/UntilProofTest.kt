import tools.aqua.*
import tools.aqua.parser.Trace
import kotlin.test.Test

class UntilProofTest {
    val trace = listOf(
        Trace(TS(1), setOf("a", "b")),
        Trace(TS(2), setOf("a", "b")),
        Trace(TS(3), setOf("a", "b", "c")),
        Trace(TS(3), setOf()),
        Trace(TS(3), setOf("a")),
        Trace(TS(4), setOf("a", "c")),
        Trace(TS(5), setOf("b", "c")),
        Trace(TS(6), setOf()),
    )

    @Test
    fun tt_test() {
        val result = eval(trace, "false Until[0,2] True")
        result.forEach { (_, proof) ->
            assert(proof is SatProof)
        }
    }
    @Test
    fun ft_test() {
        val result = eval(trace, "false Until[1,2] True")
        result.forEach { (_, proof) ->
            assert(proof is ViolationProof)
        }
    }
    @Test
    fun tf_test() {
        val result = eval(trace, "true Until[1,2] false")
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
        val result = eval(trace, "a U[1,2] (b and c)")
        assert(result[0] is SatProof)
        assert(result[1] is SatProof)
        assert(result[2] is ViolationProof)
        assert(result[3] is ViolationProof)
        assert(result[4] is SatProof)
    }
    @Test
    fun notPaperEx_test() {
        val result = eval(trace, "not (a U[1,2] (b and c))")
        assert(result[0] is ViolationProof)
        assert(result[1] is ViolationProof)
        assert(result[2] is SatProof)
        assert(result[3] is SatProof)
        assert(result[4] is ViolationProof)
    }

    @Test
    fun vInfTest() {
        val trace2 = listOf(
            Trace(TS(1), setOf("a")),
            Trace(TS(2), setOf("a")),
            Trace(TS(3), setOf("a")),
            Trace(TS(4), setOf("a")),
        )
        val result = eval(trace2, "a U[1,2] (b and c)")
        assert(result[0] is VUntilInf)
    }

    @Test
    fun noRighttest() {
        val trace2 = listOf(
            Trace(TS(1), setOf("a", "b", "c")),
            Trace(TS(2), setOf("a", "b")),
            Trace(TS(3), setOf("a")),
            Trace(TS(3), setOf()),
            Trace(TS(3), setOf("a", "b")),
            Trace(TS(4), setOf("a")),
            Trace(TS(5), setOf("a", "b", "c")),
            Trace(TS(6), setOf()),
            Trace(TS(7), setOf()),
        )
        val result = eval(trace2, "a U[1,2] (b and c)")
        assert(result[0] is ViolationProof)
        assert(result[1] is ViolationProof)
        assert(result[2] is ViolationProof)
        assert(result[3] is ViolationProof)
        assert(result[4] is SatProof)
        assert(result[5] is SatProof)
    }

}