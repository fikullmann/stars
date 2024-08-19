package dslFormulas

import org.junit.jupiter.api.Test
import tools.aqua.SatProof
import tools.aqua.dsl.FormulaBuilder.Companion.formula
import tools.aqua.dsl.Ref
import tools.aqua.dsl.holds
import tools.aqua.eval
import tools.aqua.stars.core.tsc.builder.all
import tools.aqua.stars.core.tsc.builder.leaf
import tools.aqua.stars.core.tsc.builder.root
import tools.aqua.stars.data.av.dataclasses.*
import kotlin.math.abs
import kotlin.math.sign


class CarlaTest() {
    val hasMidTrafficDensity = formula { v: Ref<Vehicle> ->
        minPrevalence(0.6) {
            pred(v) { it.tickData.vehiclesInBlock(it.lane.road.block).size in 6..15 }
        }
    }
    val hasHighTrafficDensity = formula { v: Ref<Vehicle> ->
        minPrevalence(0.6) {
            pred(v) { it.tickData.vehiclesInBlock(it.lane.road.block).size > 15 }
        }
    }

    val hasLowTrafficDensity = formula { v: Ref<Vehicle> ->
        neg { hasHighTrafficDensity.holds(v) or hasMidTrafficDensity.holds(v) }
    }

// val changedLane

    val onSameRoad = formula { v1: Ref<Vehicle>, v2: Ref<Vehicle> ->
        pred(v1, v2) { (e1, e2) -> e1.lane.road == e2.lane.road }
    }

    val oncoming = formula { v1: Ref<Vehicle> ->
        exists { v2: Ref<Vehicle> ->
            eventually {
                onSameRoad.holds(v1, v2) and pred(v1, v2) { (e1, e2) ->
                    e1.lane.laneId.sign == e2.lane.laneId.sign
                }
            }
        }
    }

    val isInJunction = formula { v: Ref<Vehicle> ->
        minPrevalence(0.8) {
            pred(v) { it.lane.road.isJunction }
        }
    }

    val isInSingleLane = formula { v: Ref<Vehicle> ->
        neg { isInJunction.holds(v) } and minPrevalence(0.8) {
            pred(v) { e -> e.lane.road.lanes.filter { e.lane.laneId.sign == it.laneId.sign }.size == 1 }
        }
    }
    val isInMultiLane = formula { v: Ref<Vehicle> ->
        neg { isInJunction.holds(v) or isInSingleLane.holds(v) }
    }

    val sunset = formula { v: Ref<Vehicle> ->
        minPrevalence(0.6) {
            pred(v) { it.tickData.daytime == Daytime.Sunset }
        }
    }
    val noon = formula { v: Ref<Vehicle> ->
        minPrevalence(0.6) {
            pred(v) { it.tickData.daytime == Daytime.Noon }
        }
    }
    val weatherClear = formula { v: Ref<Vehicle> ->
        minPrevalence(0.6) {
            pred(v) { it.tickData.weather.type == WeatherType.Clear }
        }
    }
    val weatherCloudy = formula { v: Ref<Vehicle> ->
        minPrevalence(0.6) {
            pred(v) { it.tickData.weather.type == WeatherType.Cloudy }
        }
    }
    val weatherWet = formula { v: Ref<Vehicle> ->
        minPrevalence(0.6) {
            pred(v) { it.tickData.weather.type == WeatherType.Wet }
        }
    }
    val weatherWetCloudy = formula { v: Ref<Vehicle> ->
        minPrevalence(0.6) {
            pred(v) { it.tickData.weather.type == WeatherType.WetCloudy }
        }
    }
    val weatherSoftRain = formula { v: Ref<Vehicle> ->
        minPrevalence(0.6) {
            pred(v) { it.tickData.weather.type == WeatherType.SoftRainy }
        }
    }
    val weatherMidRain = formula { v: Ref<Vehicle> ->
        minPrevalence(0.6) {
            pred(v) { it.tickData.weather.type == WeatherType.MidRainy }
        }
    }

    val weatherHardRain = formula { v: Ref<Vehicle> ->
        minPrevalence(0.6) {
            pred(v) { it.tickData.weather.type == WeatherType.HardRainy }
        }
    }


    val soBetween = formula { ego: Ref<Vehicle> ->
        exists { v1: Ref<Vehicle> ->
            exists { v2: Ref<Vehicle> ->
                (pred(ego, v2) { (e1, e2) -> e1.id != e2.id } and
                        pred(v1, v2) { (e1, e2) -> e1.id != e2.id }) and
                        (pred(ego, v2) { (e1, e2) -> e1.lane.uid == e2.lane.uid } or
                                pred(v1, v2) { (e1, e2) -> e1.lane.uid == e2.lane.uid }) and
                        (pred(ego, v2) { (e1, e2) -> e1.lane.uid != e2.lane.uid } or
                                pred(ego, v2) { (e1, e2) -> e1.positionOnLane < e2.positionOnLane }) and
                        (pred(v1, v2) { (e1, e2) -> e1.lane.uid != e2.lane.uid } or
                                pred(v1, v2) { (e1, e2) -> e1.positionOnLane > e2.positionOnLane })
            }
        }
    }

    val isBehind = formula { r1: Ref<Vehicle>, r2: Ref<Vehicle> ->
        pred(r1, r2) { r1.now().lane.road == r2.now().lane.road } and
                pred(r1, r2) { r1.now().lane.laneId.sign == r2.now().lane.laneId.sign } and
                pred(r1, r2) { abs(r1.now().positionOnLane - r2.now().positionOnLane) <= 2.0 } and
                pred(r1, r2) { (r1.now().positionOnLane + 2.0) < r2.now().positionOnLane }
    }
    val bothOver10Mph = formula { r1: Ref<Vehicle>, r2: Ref<Vehicle> ->
        pred(r1, r2) { r1.now().effVelocityInMPH > 10 } and
                pred(r1, r2) { r2.now().effVelocityInMPH > 10 }
    }
    val besides = formula { r1: Ref<Vehicle>, r2: Ref<Vehicle> ->
        pred(r1, r2) { r1.now().lane.road == r2.now().lane.road } and
                pred(r1, r2) { r1.now().lane.laneId.sign == r2.now().lane.laneId.sign } and
                pred(r1, r2) { abs(r1.now().positionOnLane - r2.now().positionOnLane) <= 2.0 } and
                pred(r1, r2) { abs(r2.now().positionOnLane - r1.now().positionOnLane) <= 2.0 }
    }
    val overtaking = formula { r1: Ref<Vehicle> ->
        exists { r2: Ref<Vehicle> ->
            isBehind.holds(r1, r2) and
                    bothOver10Mph.holds(r1, r2) and
                    next {
                        until {
                            isBehind.holds(r1, r2) and
                                    isBehind.holds(r1, r2) and
                                    isBehind.holds(r1, r2) and
                                    bothOver10Mph.holds(r1, r2)
                            besides.holds(r1, r2) and
                                    bothOver10Mph.holds(r1, r2) and
                                    next {
                                        until {
                                            besides.holds(r1, r2) and bothOver10Mph.holds(r1, r2)
                                            isBehind.holds(r1, r2) and bothOver10Mph.holds(r1, r2)
                                        }
                                    }
                        }
                    }
        }
    }

    val obeyedSpeedLimit = formula { t1: Ref<Vehicle> ->
        always {
            pred(t1) { it.effVelocityInMPH <= it.lane.speedAt(it.positionOnLane) }
        }
    }

    @Test
    fun asdfjkl() {
        root<Actor, TickData, Segment, TickDataUnitSeconds, TickDataDifferenceSeconds> {
            all("TSC Root") {
                leaf("someone between") {
                    condition = { ctx ->
                        eval(ctx.segment, soBetween.holds(Ref(ctx.primaryEntityId))).first() is SatProof
                    }
                }
                leaf("obeyed speed limit") {
                    condition = { ctx ->
                        eval(ctx.segment, obeyedSpeedLimit.holds(Ref<Vehicle>(ctx.primaryEntityId))).first() is SatProof
                    }
                }
            }
        }
    }
}