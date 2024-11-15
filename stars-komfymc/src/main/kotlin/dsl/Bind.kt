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

import tools.aqua.stars.core.types.*

sealed interface RefBind

class Bind<E1 : EntityType<*, *, *, *, *>, T1 : Any>(
    val ref: Ref<E1>,
    private val term: (E1) -> T1,
    val binding: MutableMap<RefId, T1> = mutableMapOf(),
) : RefBind {
  fun with(entity: E1): T1 =
      binding[RefId(entity.id)] ?: throw Exception("The binding was not previously configured.")

  fun calculate() {
    ref.allAtTick().forEach { e -> binding[RefId(e.id)] = term(e) }
  }
}
