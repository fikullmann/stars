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

import tools.aqua.Pdt
import tools.aqua.Proof
import tools.aqua.TP
import tools.aqua.TS
import tools.aqua.dsl.Bind
import tools.aqua.dsl.Ref
import tools.aqua.stars.core.types.EntityType
import tools.aqua.stars.core.types.TickDifference
import tools.aqua.stars.core.types.TickUnit

data class MBinding<
    U : TickUnit<U, D>, D : TickDifference<D>, E1 : EntityType<*, *, *, *, *>, T : Any>(
    val bindVariable: Bind<E1, T>,
    val inner: MState<U, D>,
) : MState<U, D> {
  override fun eval(
      ts: TS<U, D>,
      tp: TP,
      ref: MutableList<Ref<EntityType<*, *, *, *, *>>>
  ): List<Pdt<Proof>> {
    if (tp.i == 0) {
      bindVariable.calculate()
    }
    val p1 = inner.eval(ts, tp, ref)
    return p1
  }
}
