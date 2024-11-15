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

package tools.aqua

import tools.aqua.stars.core.types.TickDifference
import tools.aqua.stars.core.types.TickUnit

/** Timestamp */
@JvmInline
value class TS<U : TickUnit<U, D>, D : TickDifference<D>>(val i: U) : Comparable<TS<U, D>> {
  override fun compareTo(other: TS<U, D>) = i.compareTo(other.i)

  operator fun plus(other: D) = TS(i + other)

  operator fun minus(other: D) = TS(i - other)

  // operator fun minus(other: TS<U, D>) = TS<U, D>(i - other.i)
}

/** Timepoint */
@JvmInline
value class TP(val i: Int) : Comparable<TP> {
  override fun compareTo(other: TP): Int {
    return i - other.i
  }

  operator fun plus(other: Int) = TP(i + other)

  operator fun minus(other: Int) = TP(i - other)
}

abstract class Interval<D : TickDifference<D>>(open val startVal: D? = null) {
  /** returns true when t is inside the Interval */
  abstract fun contains(t: D): Boolean

  /** returns true when Timepoint t is before the Interval */
  abstract fun below(t: D): Boolean

  /** returns true when Timepoint t is after the Interval */
  abstract fun above(t: D): Boolean
}

data class InfInterval<D : TickDifference<D>>(override val startVal: D? = null) : Interval<D>() {
  override fun contains(t: D) = startVal?.let { (t >= startVal) } ?: true

  override fun below(t: D) = startVal?.let { t < startVal } ?: false

  override fun above(t: D) = false
}

data class BoundedInterval<D : TickDifference<D>>(override val startVal: D?, val endVal: D) :
    Interval<D>() {
  override fun contains(t: D) = startVal?.let { (t in startVal..endVal) } ?: (t <= endVal)

  override fun below(t: D) = startVal?.let { t < startVal } ?: false

  override fun above(t: D) = t > endVal
}

data class RealInterval<U : TickUnit<U, D>, D : TickDifference<D>>(val startVal: U, val endVal: U) {
  fun contains(t: U) = t in startVal..endVal

  fun below(t: U) = t < startVal

  fun above(t: U) = t > endVal
}
