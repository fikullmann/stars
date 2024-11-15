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

package tools.aqua.MStates

import tools.aqua.*
import tools.aqua.auxStructures.Eaux
import tools.aqua.auxStructures.Oaux
import tools.aqua.dsl.Ref
import tools.aqua.stars.core.types.EntityType
import tools.aqua.stars.core.types.TickDifference
import tools.aqua.stars.core.types.TickUnit

data class MOnce<U : TickUnit<U, D>, D : TickDifference<D>>(
    val interval: Interval<D>,
    val inner: MState<U, D>,
    val buft: BufT<U, D> = BufT<U, D>(),
    var mAuxPdt: Pdt<Oaux<U, D>> = Leaf(Oaux())
) : MState<U, D> {
  override fun eval(
      ts: TS<U, D>,
      tp: TP,
      ref: MutableList<Ref<EntityType<*, *, *, *, *>>>
  ): List<Pdt<Proof>> {
    val formula = this
    val p1 = inner.eval(ts, tp, ref)
    formula.buft.add(p1, ts to tp)

    val result = mutableListOf<Pdt<Proof>>()
    for ((expl, ts1, tp1) in formula.buft) {
      val (pPdt, auxPdt) =
          split(
              apply2(ref, expl, formula.mAuxPdt) { p, aux ->
                aux.update1(formula.interval, ts1, tp1, p)
              })
      result.add(pPdt)
      formula.mAuxPdt = auxPdt
    }
    formula.buft.clearInner()
    return result
  }
}

data class MEventually<U : TickUnit<U, D>, D : TickDifference<D>>(
    val interval: Interval<D>,
    val inner: MState<U, D>,
    val buft: BufT<U, D> = BufT<U, D>(),
    var eAux: Pdt<Eaux<U, D>> = Leaf(Eaux())
) : MState<U, D> {
  override fun eval(
      ts: TS<U, D>,
      tp: TP,
      ref: MutableList<Ref<EntityType<*, *, *, *, *>>>
  ): List<Pdt<Proof>> {
    val formula = this
    val p1 = inner.eval(ts, tp, ref)
    formula.buft.add(p1, ts to tp)

    val result = mutableListOf<Pdt<Proof>>()
    for ((expl, ts1, tp1) in formula.buft) {
      val (listsPdt, auxPdt) =
          split(
              apply2(ref, expl, formula.eAux) { p, aux ->
                aux.update(formula.interval, ts1, tp1, p)
              })
      result.addAll(splitList(listsPdt))
      formula.eAux = auxPdt
    }
    return result
  }
}