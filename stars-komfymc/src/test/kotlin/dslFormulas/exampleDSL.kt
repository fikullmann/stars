/*
 * Copyright 2024 The STARS Project Authors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")

package dslFormulas

import kotlin.math.abs
import kotlin.math.sign
import org.junit.jupiter.api.Test
import tools.aqua.dsl.FormulaBuilder.Companion.formula
import tools.aqua.dsl.Ref
import tools.aqua.stars.data.av.dataclasses.*

class exampleDSL {
  @Test
  fun monitors() {
    val hasMidTrafficDensity = formula {
      forall() { x: Ref<Vehicle> ->
        binding(term(x) { v -> v.positionOnLane }) { b -> tt() } and
        eventually {
          (const(6) leq
              term { x.now().let { v -> v.tickData.vehiclesInBlock(v.lane.road.block).size } }) and
              (term { x.now().let { v -> v.tickData.vehiclesInBlock(v.lane.road.block).size } } leq
                  const(15))
        }
      }
    }
    val hasMidTrafficDensityPred = formula {
      exists { x: Ref<Vehicle> ->
        eventually { pred(x) { v -> v.tickData.vehiclesInBlock(v.lane.road.block).size in 6..15 } }
      }
    }
    formula { v: Ref<Vehicle> ->
      minPrevalence(0.6) {
        neg(hasMidTrafficDensity) or
            (term { v.now().effVelocityInKmPH } leq
                term { v.now().lane.speedLimits[v.now().positionOnLane.toInt()].speedLimit })
      }
    }

    val changedLane = formula { v: Ref<Vehicle> ->
      binding(term(v) { v.now().lane }) { l -> eventually { pred(v) { v -> l.with(v) != v.lane } } }
    }
    val didCrossRedLightDSL = formula { v: Ref<Vehicle> ->
      eventually {
        tt() and
            binding(term(v) { v -> v.lane.road }) { l ->
              next { pred(v) { v -> l.with(v) != v.lane.road } }
            }
      }
    }
  }

  @Test
  fun varyingInOut() {
    val outFormula = formula { x: Ref<Vehicle> -> tt() }
    val noAnd = formula { exists { x: Ref<Vehicle> -> tt() } }
    val predAnd = formula { exists { x: Ref<Vehicle> -> tt() and ff() } }
    val outAnd = formula {
      exists { x: Ref<Vehicle> -> outFormula.holds(x) and outFormula.holds(x) }
    }
  }

  @Test
  fun overtaking() {
    val onSameRoadDSL = formula { v1: Ref<Vehicle>, v2: Ref<Vehicle> ->
      pred(v1, v2) { fst, snd -> fst.lane.road == snd.lane.road }
    }

    val sameDirectionDSL = formula { v1: Ref<Vehicle>, v2: Ref<Vehicle> ->
      onSameRoadDSL.holds(v1, v2) and
          pred(v1, v2) { v1, v2 -> v1.lane.laneId.sign == v2.lane.laneId.sign }
    }
    val isBehindDSL = formula { r1: Ref<Vehicle>, r2: Ref<Vehicle> ->
      sameDirectionDSL.holds(r1, r2) and
          pred(r1, r2) { r1, r2 -> (r1.positionOnLane + 2.0) < r2.positionOnLane }
    }
    val bothOver10MphDSL = formula { r1: Ref<Vehicle>, r2: Ref<Vehicle> ->
      pred(r1, r2) { r1, r2 -> r1.effVelocityInMPH > 10 } and
          pred(r1, r2) { r1, r2 -> r2.effVelocityInMPH > 10 }
    }
    val besidesDSL = formula { r1: Ref<Vehicle>, r2: Ref<Vehicle> ->
      sameDirectionDSL.holds(r1, r2) and
          pred(r1, r2) { r1, r2 -> abs(r1.positionOnLane - r2.positionOnLane) <= 2.0 }
    }
    val overtakingDSL = formula { r1: Ref<Vehicle>, r2: Ref<Vehicle> ->
      eventually {
        isBehindDSL.holds(r1, r2) and
            bothOver10MphDSL.holds(r1, r2) and
            next {
              until {
                isBehindDSL.holds(r1, r2) and bothOver10MphDSL.holds(r1, r2)
                besidesDSL.holds(r1, r2) and
                    bothOver10MphDSL.holds(r1, r2) and
                    next {
                      until {
                        besidesDSL.holds(r1, r2) and bothOver10MphDSL.holds(r1, r2)
                        isBehindDSL.holds(r1, r2) and bothOver10MphDSL.holds(r1, r2)
                      }
                    }
              }
            }
      }
    }
  }
}
