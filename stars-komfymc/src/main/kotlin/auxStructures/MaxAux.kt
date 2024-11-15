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

data class MaxAux<U : TickUnit<U, D>, D : TickDifference<D>>(
    private var sAlphas: MutableList<Pair<TS<U, D>, SatProof>> = mutableListOf(),
    private var vAlphas: MutableList<Pair<TS<U, D>, ViolationProof>> = mutableListOf(),
    override val endTS: U? = null,
) : FutureAux<U, D>() {
  fun copy() =
      MaxAux(
              sAlphas.map { it.copy() }.toMutableList(),
              vAlphas.map { it.copy() }.toMutableList(),
              endTS)
          .also { aux ->
            aux.tsTpOut = tsTpOut.toMutableList()
            aux.tsTpIn = tsTpIn.toMutableList()
            aux.optimalProofs = aux.optimalProofs.map { it.copy() }.toMutableList()
          }
  /** updates the Eaux structure at arrival of a new TS/TP */
  fun updateMaxAux(
      interval: Interval<D>,
      nts: TS<U, D>,
      ntp: TP,
      factor: Double,
      p1: Proof
  ): MutableList<Proof> {
    shiftMaxAux(interval, nts, ntp, factor)
    addTsTpFuture(interval, nts, ntp)
    addSubps(interval, nts, ntp, p1)

    shiftMaxAux(interval, nts, ntp, factor, endTS)
    return findOptimalProofs(interval, nts)
  }

  /** updates the Eaux structure at arrival of a new TS/TP */
  fun update(
      interval: Interval<D>,
      nts: TS<U, D>,
      ntp: TP,
      factor: Double,
      p1: Proof
  ): Pair<MutableList<Proof>, MaxAux<U, D>> {
    val copy = copy()
    val result = copy.updateMaxAux(interval, nts, ntp, factor, p1)
    return result to copy
  }

  private fun addSubps(interval: Interval<D>, nts: TS<U, D>, ntp: TP, proof: Proof) {
    val firstTS = firstTsTp()?.first ?: throw NoExistingTsTp()
    when (proof) {
      is SatProof -> {
        if (nts >= (interval.startVal?.let { firstTS + it } ?: firstTS)) {
          sAlphas.add(nts to proof)
          sAlphas.sortBy { it.second.size() }
        }
      }
      is ViolationProof -> {
        if (nts >= (interval.startVal?.let { firstTS + it } ?: firstTS)) vAlphas.add(nts to proof)
      }
      else -> throw InvalidProofObject()
    }
  }

  private fun shiftMaxAux(
      interval: Interval<D>,
      nts: TS<U, D>,
      ntp: TP,
      factor: Double,
      end: U? = null
  ) {
    val tsTps = readyTsTps(interval, nts, end)
    tsTps.forEach { (ts, tp) ->
      if (vAlphas.isEmpty()) {
        optimalProofs.add(ts to VMaxAllSat(tp))
      } else {
        val correct = sAlphas.size.toDouble() / (sAlphas.size + vAlphas.size)
        if (correct < factor) {
          val ltp = sAlphas.lastOrNull()?.second?.at() ?: ltp(tp)
          optimalProofs.add(
              ts to SatMaxPrev(tp, ltp, correct, sAlphas.map { it.second }.toMutableList()))
        } else {
          optimalProofs.add(
              ts to
                  VMaxPrev(
                      tp,
                      vAlphas.last().second.at(),
                      correct,
                      vAlphas.map { it.second }.toMutableList()))
        }
      }
      adjustMaxAux(interval, nts, ntp)
    }
  }

  private fun adjustMaxAux(interval: Interval<D>, nts: TS<U, D>, ntp: TP) {
    dropFirstTsTp()
    val (firstTS, firstTp) = firstTsTp() ?: (nts to ntp)

    sAlphas.removeIf { (ts, p) ->
      ts < (interval.startVal?.let { firstTS + it } ?: firstTS) || p.at() < firstTp
    }
    vAlphas.removeIf { (ts, p) ->
      ts < (interval.startVal?.let { firstTS + it } ?: firstTS) || p.at() < firstTp
    }
    shiftTsTpFuture(interval, firstTS, ntp)
  }
}