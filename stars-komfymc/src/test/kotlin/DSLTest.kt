
import tools.aqua.domain.FormulaBuilder.Companion.formula
import tools.aqua.domain.Entity
import tools.aqua.domain.Lane
import tools.aqua.domain.Vehicle
import kotlin.test.Test

class DSLTest {
    @Test
    fun oneVehicle() {
        val l = Lane(1, 20.0, 350.0)

        val entities = mutableListOf<Entity>()
        val v1 = Vehicle(1, 1, 38.0, l, "xyz", true).also { entities.add(it) }
        val v2 = Vehicle(2, 1, 60.0, l, "xyz", false).also { entities.add(it) }
        val v1T2 = v1.copy(tickData = 2, positionOnLane = 50.0).also { entities.add(it) }
        val v2T2 = v2.copy(tickData = 2, positionOnLane = 103.0).also { entities.add(it) }


        formula {
            always(1 to 2) {
                tt()
            }
        }
    }
}