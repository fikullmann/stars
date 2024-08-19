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
import kotlin.math.abs

abstract open class TAux(
    var tsTpIn: MutableList<Pair<TS, TP>> = mutableListOf(),
    var tsTpOut: MutableList<Pair<TS, TP>> = mutableListOf(),
)
{
    fun firstTsTp() = tsTpOut.firstOrNull() ?: tsTpIn.firstOrNull()
    fun dropFirstTsTp() {
        if (tsTpOut.isNotEmpty()) {
            tsTpOut.removeFirst()
        } else tsTpIn.removeFirst()
    }
    /**
     * updates tsTpIn and Out so the correct pairs are in In and Out depending on current TSTP
     */
    fun shiftTsTpsPast(interval: BoundedInterval, iStart: Double, ts: TS, tp: TP) {
        // top branch is not actually needed..(?)
        if (abs(iStart) < 0.0001) {
            tsTpIn.add(ts to tp)
            tsTpIn.removeIf { (tsL, _) -> tsL.i < interval.startVal }
        } else {
            tsTpOut.add(ts to tp)
            tsTpOut.forEach { (tsL, tpL) ->
                if (tsL.i <= interval.endVal) { tsTpIn.add(tsL to tpL) }
            }
            tsTpOut.removeIf { (tsL, _) -> tsL.i <= interval.endVal }
            tsTpIn.removeIf { (tsL, _) -> tsL.i < interval.startVal }
        }
    }
    fun shiftTsTpFuture(interval: BoundedInterval, firstTs: TS, tpCur: TP) {
        tsTpIn.forEach { (ts, tp) ->
            if (ts.i < firstTs.i + interval.startVal && tp < tpCur) {
                tsTpOut.add(ts to tp)
            }
        }
        tsTpOut.removeIf { (ts, tp) -> ts < firstTs && tp < tpCur}
        tsTpIn.removeIf { (ts, tp) -> ts.i < firstTs.i + interval.startVal && tp < tpCur }
    }
    fun addTsTpFuture(interval: BoundedInterval, nts: TS, ntp: TP) {
        if (tsTpOut.isNotEmpty() || tsTpIn.isNotEmpty()) {
            val firstTs = firstTsTp()?.first ?: throw UnboundedFuture()
            if (nts < firstTs + interval.startVal) {
                tsTpOut.add(nts to ntp)
            } else {
                tsTpIn.add(nts to ntp)
            }
        } else {
            if (abs(interval.startVal) < 0.0001) {
                tsTpIn.add(nts to ntp)
            } else {
                tsTpOut.add(nts to ntp)
            }
        }
    }
    fun readyTsTps(interval: BoundedInterval, nts: TS): MutableList<Pair<TS, TP>> = buildList {
            tsTpIn.forEach { (ts, tp) -> if (ts + interval.endVal < nts) add(ts to tp) }
            tsTpOut.forEach { (ts, tp) -> if (ts + interval.endVal < nts) add(ts to tp) }
        }.toMutableList()
    fun readyTsTps(interval: BoundedInterval, nts: TS, endTS: Double?): List<Pair<TS, TP>> = if (endTS == nts.i) { (tsTpOut + tsTpIn) } else buildList {
        tsTpIn.forEach { (ts, tp) -> if (ts + interval.endVal < nts) add(ts to tp) }
        tsTpOut.forEach { (ts, tp) -> if (ts + interval.endVal < nts) add(ts to tp) }
    }

    fun tsTpOf(stp: TP): TS = tsTpOut.find { (_, tp) ->
            tp == stp
        }?.first ?: tsTpIn.find { (_, tp) ->
            tp == stp
        }?.first ?: throw NoExistingTsTp()

    fun etp(tp: TP): TP {
        return when (tsTpIn.firstOrNull()) {
            null -> tsTpOut.firstOrNull()?.second ?: tp
            else -> tsTpIn.first().second
        }
    }
    fun ltp(tp: TP): TP = tsTpOut.lastOrNull()?.second ?: tp
}