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

abstract open class TAux<U : TickUnit<U, D>, D : TickDifference<D>>(
    var tsTpIn: MutableList<Pair<TS<U, D>, TP>> = mutableListOf(),
    var tsTpOut: MutableList<Pair<TS<U, D>, TP>> = mutableListOf(),
) {
  fun firstTsTp() = tsTpOut.firstOrNull() ?: tsTpIn.firstOrNull()

  fun dropFirstTsTp() {
    if (tsTpOut.isNotEmpty()) {
      tsTpOut.removeFirst()
    } else tsTpIn.removeFirst()
  }
  /** updates tsTpIn and Out so the correct pairs are in In and Out depending on current TSTP */
  fun shiftTsTpsPast(interval: RealInterval<U, D>, iStart: D?, ts: TS<U, D>, tp: TP) {
    // top branch is not actually needed..(?)
    if (iStart == null) {
      tsTpIn.add(ts to tp)
      tsTpIn.removeIf { (tsL, _) -> tsL.i < interval.startVal }
    } else {
      tsTpOut.add(ts to tp)
      tsTpOut.forEach { (tsL, tpL) ->
        if (tsL.i <= interval.endVal) {
          tsTpIn.add(tsL to tpL)
        }
      }
      tsTpOut.removeIf { (tsL, _) -> tsL.i <= interval.endVal }
      tsTpIn.removeIf { (tsL, _) -> tsL.i < interval.startVal }
    }
  }

  fun readyTsTps(interval: Interval<D>, nts: TS<U, D>, endTS: D?): List<Pair<TS<U, D>, TP>> =
      if (endTS == nts.i) {
        (tsTpOut + tsTpIn)
      } else {
        if (interval is BoundedInterval<D>) {
          buildList {
            tsTpIn.forEach { (ts, tp) -> if (ts + interval.endVal < nts) add(ts to tp) }
            tsTpOut.forEach { (ts, tp) -> if (ts + interval.endVal < nts) add(ts to tp) }
          }
        } else {
          listOf()
        }
      }

  fun tsTpOf(stp: TP): TS<U, D> =
      tsTpOut.find { (_, tp) -> tp == stp }?.first
          ?: tsTpIn.find { (_, tp) -> tp == stp }?.first
          ?: throw NoExistingTsTp()

  fun etp(tp: TP): TP {
    return when (tsTpIn.firstOrNull()) {
      null -> tsTpOut.firstOrNull()?.second ?: tp
      else -> tsTpIn.first().second
    }
  }

  fun ltp(tp: TP): TP = tsTpOut.lastOrNull()?.second ?: tp
}

abstract open class FutureAux<U : TickUnit<U, D>, D : TickDifference<D>>(
    var tsTpIn: MutableList<Pair<TS<U, D>, TP>> = mutableListOf(),
    var tsTpOut: MutableList<Pair<TS<U, D>, TP>> = mutableListOf(),
    var optimalProofs: MutableList<Pair<TS<U, D>, Proof>> = mutableListOf(),
    open val endTS: U? = null,
) {
  fun firstTsTp() = tsTpOut.firstOrNull() ?: tsTpIn.firstOrNull()

  fun dropFirstTsTp() {
    if (tsTpOut.isNotEmpty()) {
      tsTpOut.removeFirst()
    } else tsTpIn.removeFirst()
  }

  fun findOptimalProofs(interval: Interval<D>, nts: TS<U, D>): MutableList<Proof> {
    if (endTS == nts.i) {
      return optimalProofs.map { it.second }.toMutableList()
    } else {
      if (interval is BoundedInterval) {
        val result = optimalProofs.filter { (ts, _) -> (ts + interval.endVal < nts) }
        optimalProofs.removeIf { (ts, _) -> (ts + interval.endVal < nts) }
        return result.map { it.second }.toMutableList()
      } else return mutableListOf()
    }
  }

  fun shiftTsTpFuture(interval: Interval<D>, firstTs: TS<U, D>, tpCur: TP) {
    tsTpIn.forEach { (ts, tp) ->
      if (ts < (interval.startVal?.let { firstTs + it } ?: firstTs) && tp < tpCur) {
        tsTpOut.add(ts to tp)
      }
    }
    tsTpOut.removeIf { (ts, tp) -> ts < firstTs && tp < tpCur }
    tsTpIn.removeIf { (ts, tp) ->
      ts < (interval.startVal?.let { firstTs + it } ?: firstTs) && tp < tpCur
    }
  }

  fun addTsTpFuture(interval: Interval<D>, nts: TS<U, D>, ntp: TP) {
    if (tsTpOut.isNotEmpty() || tsTpIn.isNotEmpty()) {
      val firstTS = firstTsTp()?.first ?: throw UnboundedFuture()
      if (nts < (interval.startVal?.let { firstTS + it } ?: firstTS)) {
        tsTpOut.add(nts to ntp)
      } else {
        tsTpIn.add(nts to ntp)
      }
    } else {
      if (interval.startVal == null) {
        tsTpIn.add(nts to ntp)
      } else {
        tsTpOut.add(nts to ntp)
      }
    }
  }

  fun readyTsTps(interval: BoundedInterval<D>, nts: TS<U, D>): MutableList<Pair<TS<U, D>, TP>> =
      buildList {
            tsTpIn.forEach { (ts, tp) -> if (ts + interval.endVal < nts) add(ts to tp) }
            tsTpOut.forEach { (ts, tp) -> if (ts + interval.endVal < nts) add(ts to tp) }
          }
          .toMutableList()

  fun readyTsTps(interval: Interval<D>, nts: TS<U, D>, endTS: U?): List<Pair<TS<U, D>, TP>> =
      if (endTS == nts.i) (tsTpOut + tsTpIn)
      else {
        if (interval is BoundedInterval) {
          buildList {
            tsTpIn.forEach { (ts, tp) -> if (ts + interval.endVal < nts) add(ts to tp) }
            tsTpOut.forEach { (ts, tp) -> if (ts + interval.endVal < nts) add(ts to tp) }
          }
        } else listOf()
      }

  fun tsTpOf(stp: TP): TS<U, D> =
      tsTpOut.find { (_, tp) -> tp == stp }?.first
          ?: tsTpIn.find { (_, tp) -> tp == stp }?.first
          ?: throw NoExistingTsTp()

  fun ltp(tp: TP): TP = tsTpOut.lastOrNull()?.second ?: tp
}
