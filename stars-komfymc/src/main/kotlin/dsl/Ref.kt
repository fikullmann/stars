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

package tools.aqua.dsl

import kotlin.reflect.KClass
import tools.aqua.*
import tools.aqua.MStates.MBinaryPred
import tools.aqua.MStates.Pred
import tools.aqua.stars.core.types.*

/**
 * Ref is used in the DSL to symbolize entities of a specific type. Function now() is used inside
 * predicates and terms of the DSL. nextTick() and allAtTick() are helper functions for the model
 * checker.
 *
 * @see makeRef
 */
class Ref<E1 : EntityType<*, *, *, *, *>>(
    private val kClass: KClass<E1>,
    id: Int? = null,
    val fixed: Boolean = false
) : RefBind {
  var id: Int? = id
    set(value) {
      if (!fixed) field = value else throw Exception("The Id of a fixed Ref can not be changed.")
    }

  var tickIdx: Int = -1
    set(value) {
      field = value
      entities = tickDataType[value].entities.filterIsInstance(kClass.java)
    }

  var entity: E1? = null
  private var entities: List<E1>? = listOf()

  /**
   * returns the entity with the given id at the given tickData. before now() is called, the id must
   * be set and the correct tickdatatype must be specified for Ref
   */
  fun now(): E1 {
    setToGlobalTick()
    if (fixed) {
      assert(id != null)
      return entities?.firstOrNull { it.id == id }
          ?: throw Exception(
              "There are of this id at the current tick. Make sure the correct id and globalTick is given.")
    } else {
      assert(entity != null)
      return (entity as E1)
          ?: throw Exception(
              "$id There are no entities of this id at the current tick. Make sure the correct id and globalTick is given.")
    }
  }
  /**
   * returns all entities at the given tick: correct tickdatatype must be specified for Ref before
   * calling
   */
  fun allAtTick(): List<E1> {
    if (tickIdx != globalTickIdx) setToGlobalTick()
    return entities
        ?: throw Exception(
            "" +
                "There are no entities at all. Give Ref entities through Ref.setSegment(segment) before calling this method.")
  }

  /** cycles through all the entities of a tick and calls phi after correctly assigning Ref. */
  fun cycleEntitiesAtTick(tp: TP, formula: Pred) =
      allAtTick().fold(mutableListOf<Pair<RefId, Pdt<Proof>>>()) { acc, e ->
        entity = e
        acc.apply {
          add(RefId(entity!!.id) to Leaf(if (formula.call()) SatPred(tp) else VPred(tp)))
        }
      }

  fun cycleBinaryEntitiesAtTick(
    tp: TP,
    formula: MBinaryPred<*, *, *, *>,
    ref2: Ref<out EntityType<*, *, *, *, *>>
  ) =
      allAtTick().fold(mutableListOf<Pair<RefId, Pdt<Proof>>>()) { acc, e ->
        entity = e
        val part2 = ref2.cycleEntitiesAtTick(tp, formula)
        acc.apply { add(RefId(entity!!.id) to Node(ref2, part2)) }
      }

  /** sets tick to the next tick, updates entities accordingly */
  private fun setToGlobalTick() {
    if (tickIdx != globalTickIdx) {
      tickIdx = globalTickIdx
      entities = tickDataType[tickIdx].entities.filterIsInstance(kClass.java)
    }
  }

  companion object {
    var globalTickIdx: Int = -1
    lateinit var tickDataType: List<TickDataType<*, *, *, *, *>>

    fun <
        E : EntityType<E, T, S, U, D>,
        T : TickDataType<E, T, S, U, D>,
        S : SegmentType<E, T, S, U, D>,
        U : TickUnit<U, D>,
        D : TickDifference<D>> setSegment(segment: S) {
      tickDataType = segment.tickData
      globalTickIdx = 0
    }

    fun <
        E : EntityType<E, T, S, U, D>,
        T : TickDataType<E, T, S, U, D>,
        S : SegmentType<E, T, S, U, D>,
        U : TickUnit<U, D>,
        D : TickDifference<D>> cycle(segment: S, action: (index: Int, tick: U) -> Unit) {
      setSegment(segment)
      tickDataType.forEachIndexed { idx, tickDataType ->
        globalTickIdx = idx
        action(idx, tickDataType.currentTick as U)
      }
    }
    /** helper function to create instances of Ref without needing an explicit class parameter */
    inline operator fun <
        reified E1 : E,
        E : EntityType<E, T, S, U, D>,
        T : TickDataType<E, T, S, U, D>,
        S : SegmentType<E, T, S, U, D>,
        U : TickUnit<U, D>,
        D : TickDifference<D>> invoke(): Ref<E1> = Ref(E1::class)

    inline operator fun <
        reified E1 : EntityType<*, *, *, *, *>,
    > invoke(id: Int): Ref<E1> = Ref(E1::class, id, true)

    inline operator fun <
        reified E1 : E,
        E : EntityType<E, T, S, U, D>,
        T : TickDataType<E, T, S, U, D>,
        S : SegmentType<E, T, S, U, D>,
        U : TickUnit<U, D>,
        D : TickDifference<D>> invoke(entity: E1): Ref<E1> = Ref(E1::class, entity.id, true)
  }
}

data class RefId(val i: Int)