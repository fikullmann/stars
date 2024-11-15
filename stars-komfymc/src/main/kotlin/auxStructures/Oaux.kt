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

package tools.aqua.auxStructures

import tools.aqua.*
import tools.aqua.stars.core.types.TickDifference
import tools.aqua.stars.core.types.TickUnit

class Oaux<U : TickUnit<U, D>, D : TickDifference<D>>(
    private var tsZero: TS<U, D>? = null,
    val sAlphasIn: MutableList<Pair<TS<U, D>, SatProof>> = mutableListOf(),
    val sAlphasOut: MutableList<Pair<TS<U, D>, SatProof>> = mutableListOf(),
    val vAlphasIn: MutableList<Pair<TS<U, D>, ViolationProof>> = mutableListOf(),
    val vAlphasOut: MutableList<Pair<TS<U, D>, ViolationProof>> = mutableListOf()
) : TAux<U, D>() {
  fun copy() =
      Oaux(
              tsZero?.copy() ?: null,
              sAlphasIn.map { it.copy() }.toMutableList(),
              sAlphasOut.map { it.copy() }.toMutableList(),
              vAlphasIn.map { it.copy() }.toMutableList(),
              vAlphasOut.map { it.copy() }.toMutableList(),
          )
          .also {
            it.tsTpOut = tsTpOut.toMutableList()
            it.tsTpIn = tsTpIn.toMutableList()
          }

  fun updateOaux(interval: Interval<D>, ts: TS<U, D>, tp: TP, p: Proof): Proof {
    val currTsZero = tsZero ?: ts
    tsZero = currTsZero
    addSubps(ts, p)
    if (ts.i < (interval.startVal?.let { currTsZero.i + it } ?: currTsZero.i)) {
      tsTpOut.add(ts to tp)
      return VOnceOutL(tp)
    } else {
      var l = currTsZero.i
      if (interval is BoundedInterval && currTsZero < (ts - interval.endVal)) {
        l = (ts.i - interval.endVal)
      }
      val r = interval.startVal?.let { ts.i - it } ?: ts.i
      shiftOaux(RealInterval(l, r), interval.startVal, ts, tp)
      return evalOaux(tp)
    }
  }

  fun update1(interval: Interval<D>, ts: TS<U, D>, tp: TP, p: Proof): Pair<Proof, Oaux<U, D>> {
    val copy = copy()
    val result = copy.updateOaux(interval, ts, tp, p)
    return result to copy
  }

  private fun addSubps(ts: TS<U, D>, p: Proof) {
    when (p) {
      is SatProof -> sAlphasOut.add(ts to p)
      is ViolationProof -> vAlphasOut.add(ts to p)
      else -> throw InvalidProofObject()
    }
  }

  private fun shiftOaux(interval: RealInterval<U, D>, iStart: D?, ts: TS<U, D>, tp: TP) {
    shiftTsTpsPast(interval, iStart, ts, tp)

    val newInSat = sAlphasOut.filter { (ts, _) -> interval.contains(ts.i) }
    sAlphasOut.removeIf { (ts, _) -> ts.i <= interval.endVal }
    if (newInSat.isNotEmpty()) {
      sAlphasIn.addAll(newInSat)
      sAlphasIn.sortBy { it.second.size() }
    }

    val newInVio = vAlphasOut.filter { (ts, _) -> interval.contains(ts.i) }
    vAlphasOut.removeIf { (ts, _) -> ts.i <= interval.endVal }
    vAlphasIn.addAll(newInVio)

    sAlphasIn.removeIf { (tsL, _) -> tsL.i < interval.startVal }
    sAlphasOut.removeIf { (tsL, _) -> tsL.i <= interval.endVal }
    vAlphasIn.removeIf { (tsL, _) -> tsL.i < interval.startVal }
    vAlphasOut.removeIf { (tsL, _) -> tsL.i <= interval.endVal }
  }

  private fun evalOaux(tp: TP): Proof {
    if (sAlphasIn.isNotEmpty()) {
      return SatOnce(tp, sAlphasIn.first().second)
    } else {
      val etp =
          when (vAlphasIn.isEmpty()) {
            true -> etp(tp)
            false -> vAlphasIn.first().second.at()
          }
      return VOnce(tp, etp, vAlphasIn.map { it.second }.toMutableList())
    }
  }
}
