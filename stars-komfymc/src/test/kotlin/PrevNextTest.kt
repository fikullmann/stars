import org.junit.jupiter.api.Test
import tools.aqua.*
import tools.aqua.parser.Trace


class PrevNextTest {
    @Test
    fun tt_test() {
        val trace = listOf(
            Trace(TS(1), setOf("a", "b", "c")),
            Trace(TS(3), setOf("a", "b"))
        )
        val result = eval(trace, "Next True")

        assert(SatNext(SatTT(TP(1))) == result[0])
    }

    @Test
    fun ff_test() {
        val trace = listOf(
            Trace(TS(1), setOf("a", "b", "c")),
            Trace(TS(3), setOf("a", "b"))
        )
        val result = eval(trace, "Next False")

        assert(VNext(VFF(TP(1))) == result[0])
    }

    /**
     * @1 a b c -> True
     * @3 a b -> False VNextOutL
     * @3 a
     */
    @Test
    fun intervalBeforeTest() {
        val trace = listOf(
            Trace(TS(1), setOf("a", "b", "c")),
            Trace(TS(3), setOf("a", "b")),
            Trace(TS(3), setOf("a")),
        )
        val result = eval(trace, "Next[1,3] a")

        assert(SatNext(SatVar(TP(1), "a")) == result[0])
        assert(VNextOutL(TP(1)) == result[1])
    }

    /**
     * @1 a b c FALSE VNextOutR
     * @5 a b
     */
    @Test
    fun intervalAfterTest() {
        val trace = listOf(
            Trace(TS(1), setOf("a", "b", "c")),
            Trace(TS(5), setOf("a", "b"))
        )
        val result = eval(trace, "Next[1,3] a")

        assert(VNextOutR(TP(0)) == result[0])
    }

}