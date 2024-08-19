import tools.aqua.domain.*
import tools.aqua.parser.parse
import kotlin.test.Test


class ParserTest {
    @Test
    fun primitiesTest() {
        assert(parse("true") is TT)
        assert(parse("false") is FF)
        assert(parse("!a") is Neg)
    }

    @Test
    fun propositionalLogicTest() {
        assert(parse("a and b") is And)
        assert(parse("a or b") is Or)
        assert(parse("a -> b") is Implication)
        assert(parse("a <-> b") is Iff)
    }
    @Test
    fun previousTest() {
        val formulaI = parse("prev[1,2] b")
        val formula = parse("prev b")
        assert(formulaI is Prev && formulaI.interval == (1 to 2))
        assert(formula is Prev)
    }

    @Test
    fun nextTest() {
        val formulaI = parse("next[1,2] b")
        val formula = parse("next b")
        assert(formulaI is Next && formulaI.interval == (1 to 2))
        assert(formula is Next && formula.interval == null)
    }

    @Test
    fun sinceTest() {
        val formulaI = parse("a since [1,2] b")
        val formula = parse("a since b")
        assert(formulaI is Since && formulaI.interval == (1 to 2))
        assert(formula is Since && formula.interval == (0 to null))
    }

    @Test
    fun untilTest() {
        val formulaI = parse("a until [1,2] b")
        val formula = parse("a until b")
        assert(formulaI is Until && formulaI.interval == (1 to 2))
        assert(formula is Until && formula.interval == (0 to null))
    }
}