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
import tools.aqua.stars.core.types.*

/**
 * Ref is used in the DSL to symbolize entities of a specific type. Function now() is used inside
 * predicates and terms of the DSL. nextTick() and allAtTick() are helper functions for the model
 * checker.
 *
 * @see makeRef
 */
class Wrap<E1: Any>(
    private val kClass: KClass<E1>
) {
  /**
   * returns the entity with the given id at the given tickData. before now() is called, the id must
   * be set and the correct tickdatatype must be specified for Ref
   */
  fun resolve(): E1 = TODO()

  companion object {
    /** helper function to create instances of Ref without needing an explicit class parameter */
    inline operator fun <reified E1 : Any> invoke(): Wrap<E1> = Wrap(E1::class)
    inline operator fun <reified E1 : Any> invoke(id: Int): Wrap<E1> = Wrap(E1::class)

    inline operator fun <reified E1 : Any> invoke(entity: E1): Wrap<E1> = Wrap(E1::class)
  }
}