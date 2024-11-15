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

class Eaux<U : TickUnit<U, D>, D : TickDifference<D>>(
    override val endTS: U? = null,
    private var sAlphas: MutableList<Pair<TS<U, D>, SatProof>> = mutableListOf(),
    private var vAlphas: MutableList<Pair<TS<U, D>, Proof>> = mutableListOf(),
) : FutureAux<U, D>() {
  /** updates the Eaux structure at arrival of a new TS/TP */
  fun updateEaux(interval: Interval<D>, nts: TS<U, D>, ntp: TP, p1: Proof): MutableList<Proof> {
    shiftEaux(interval, nts, ntp)
    addTsTpFuture(interval, nts, ntp)
    addSubps(interval, nts, ntp, p1)

    shiftEaux(interval, nts, ntp, endTS)
    return findOptimalProofs(interval, nts)
  }

  fun copy() =
      Eaux(
              endTS,
              sAlphas.map { it.copy() }.toMutableList(),
              vAlphas.map { it.copy() }.toMutableList(),
          )
          .also { aux ->
            aux.tsTpOut = tsTpOut.toMutableList()
            aux.tsTpIn = tsTpIn.toMutableList()
            aux.optimalProofs = aux.optimalProofs.map { it.copy() }.toMutableList()
          }

  fun update(
      interval: Interval<D>,
      nts: TS<U, D>,
      ntp: TP,
      p1: Proof
  ): Pair<MutableList<Proof>, Eaux<U, D>> {
    val copy = copy()
    val result = copy.updateEaux(interval, nts, ntp, p1)
    return result to copy
  }

  private fun addSubps(interval: Interval<D>, nts: TS<U, D>, ntp: TP, proof: Proof) {
    val firstTS = firstTsTp()?.first?.i ?: throw NoExistingTsTp()
    when (proof) {
      is SatProof -> {
        if (nts.i >= (interval.startVal?.let { firstTS + it } ?: firstTS)) {
          sAlphas.add(nts to proof)
          sAlphas.sortBy { it.second.size() }
        }
      }
      is ViolationProof -> {
        if (nts.i >= (interval.startVal?.let { firstTS + it } ?: firstTS)) {
          vAlphas.add(nts to proof)
        }
      }
      else -> throw InvalidProofObject()
    }
  }

  private fun shiftEaux(interval: Interval<D>, nts: TS<U, D>, ntp: TP, end: U? = null) {
    val tsTps = readyTsTps(interval, nts, end)
    tsTps.forEach { (ts, tp) ->
      if (sAlphas.isNotEmpty()) {
        optimalProofs.add(ts to SatEventually(tp, sAlphas.first().second))
      } else {
        val ltp = vAlphas.lastOrNull()?.second?.at() ?: ltp(tp)
        optimalProofs.add(ts to VEventually(tp, ltp, vAlphas.map { it.second }.toMutableList()))
      }
      adjustEaux(interval, nts, ntp)
    }
  }

  private fun adjustEaux(interval: Interval<D>, nts: TS<U, D>, ntp: TP) {
    dropFirstTsTp()
    val (firstTs, firstTp) = firstTsTp() ?: (nts to ntp)

    sAlphas.removeIf { (ts, p) ->
      ts < (interval.startVal?.let { firstTs + it } ?: firstTs) || p.at() < firstTp
    }
    vAlphas.removeIf { (ts, p) ->
      ts < (interval.startVal?.let { firstTs + it } ?: firstTs) || p.at() < firstTp
    }
    shiftTsTpFuture(interval, firstTs, ntp)
  }
}
