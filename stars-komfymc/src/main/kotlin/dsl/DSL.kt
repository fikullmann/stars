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

fun <A, B> partial(f: (A) -> B, a: A): () -> B = { f(a) }
fun <A, B, C> partial2(f: (A, B) -> C, a: A, b: B): () -> C = { f(a, b) }
class FormulaBuilder(val phi: MutableList<Formula> = mutableListOf()) {
  companion object {
    fun formula(init: FormulaBuilder.() -> Unit): Formula {
      val builder = FormulaBuilder()
      init.invoke(builder)
      return builder.phi[0]
    }

    fun <
        E1 : E,
        E : EntityType<E, T, S, U, D>,
        T : TickDataType<E, T, S, U, D>,
        S : SegmentType<E, T, S, U, D>,
        U : TickUnit<U, D>,
        D : TickDifference<D>> formula(
        init: FormulaBuilder.(Ref<E1>) -> Unit
    ): (Ref<E1>) -> FormulaBuilder {
      return { ref: Ref<E1> ->
        val builder = FormulaBuilder()
        init.invoke(builder, ref)
        builder
      }
    }

    fun <
        E1 : E,
        E : EntityType<E, T, S, U, D>,
        T : TickDataType<E, T, S, U, D>,
        S : SegmentType<E, T, S, U, D>,
        U : TickUnit<U, D>,
        D : TickDifference<D>> formula(
        init: FormulaBuilder.(Ref<E1>, Ref<E1>) -> Unit
    ): (Ref<E1>, Ref<E1>) -> FormulaBuilder {
      return { ref1: Ref<E1>, ref2: Ref<E1> ->
        FormulaBuilder().apply { init(ref1, ref2) }.let { this }
        val builder = FormulaBuilder()
        init.invoke(builder, ref1, ref2)
        builder
      }
    }
  }

  fun <
      E1 : E,
      E : EntityType<E, T, S, U, D>,
      T : TickDataType<E, T, S, U, D>,
      S : SegmentType<E, T, S, U, D>,
      U : TickUnit<U, D>,
      D : TickDifference<D>> ((Ref<E1>) -> FormulaBuilder).holds(ref1: Ref<E1>): Formula =
      this(ref1).phi[0].also { phi.add(it) }
  fun <
      E1 : E,
      E2 : E,
      E : EntityType<E, T, S, U, D>,
      T : TickDataType<E, T, S, U, D>,
      S : SegmentType<E, T, S, U, D>,
      U : TickUnit<U, D>,
      D : TickDifference<D>> ((Ref<E1>, Ref<E2>) -> FormulaBuilder).holds(
      ref1: Ref<E1>,
      ref2: Ref<E2>
  ): Formula = this(ref1, ref2).phi[0].also { phi.add(it) }
  /*  fun <T : Any> Formula.f(formula: (Ref<T>) -> FormulaBuilder, ref1: Ref<T>): Formula =
       formula(ref1).phi[0].also { phi.add(it) }
   fun <T : Any> FormulaBuilder.f(
       formula: (Ref<T>, Ref<T>) -> FormulaBuilder,
       ref1: Ref<T>,
       ref2: Ref<T>
   ): Formula = formula(ref1, ref2).phi[0].also { phi.add(it) }
  */

  private fun <
      E1 : E,
      E : EntityType<E, T, S, U, D>,
      T : TickDataType<E, T, S, U, D>,
      S : SegmentType<E, T, S, U, D>,
      U : TickUnit<U, D>,
      D : TickDifference<D>> binaryFunction(
      init: FormulaBuilder.(Ref<E1>, Ref<E1>) -> Unit,
      ref1: Ref<E1>,
      ref2: Ref<E1>
  ) {
    val builder = FormulaBuilder()
    init.invoke(builder, ref1, ref2)
    phi.add(builder.phi[0])
  }

  private fun buildNeg(): Neg = assert(phi.size == 1).let { Neg(phi.first()) }
  private fun buildAnd(): And = assert(phi.size == 2).let { And(phi[0], phi[1]) }
  private fun buildOr(): Or = assert(phi.size == 2).let { Or(phi[0], phi[1]) }
  private fun buildImpl(): Implication = assert(phi.size == 2).let { Implication(phi[0], phi[1]) }
  private fun buildIff(): Iff = assert(phi.size == 2).let { Iff(phi[0], phi[1]) }
  private fun buildPrev(interval: Pair<Double, Double>?): Prev {
    assert(phi.size == 1)
    return Prev(interval, phi.first())
  }

  private fun buildNext(interval: Pair<Double, Double>?): Next {
    assert(phi.size == 1)
    return Next(interval, phi.first())
  }

  private fun buildOnce(interval: Pair<Double, Double>?): Once {
    assert(phi.size == 1)
    return Once(interval, phi.first())
  }

  private fun buildHistorically(interval: Pair<Double, Double>?): Historically {
    assert(phi.size == 1)
    return Historically(interval, phi.first())
  }

  private fun buildEventually(interval: Pair<Double, Double>?): Eventually {
    assert(phi.size == 1)
    return Eventually(interval, phi.first())
  }

  private fun buildAlways(interval: Pair<Double, Double>? = null): Always {
    assert(phi.size == 1)
    return Always(interval, inner = phi[0])
  }

  private fun buildSince(interval: Pair<Double, Double>? = null): Since {
    assert(phi.size == 2)
    return Since(interval, lhs = phi[0], rhs = phi[1])
  }

  private fun buildUntil(interval: Pair<Double, Double>? = null): Until {
    assert(phi.size == 2)
    return Until(interval, lhs = phi[0], rhs = phi[1])
  }

  fun <
      E1 : E,
      E : EntityType<E, T, S, U, D>,
      T : TickDataType<E, T, S, U, D>,
      S : SegmentType<E, T, S, U, D>,
      U : TickUnit<U, D>,
      D : TickDifference<D>> buildForall(ref: Ref<E1>): Forall<E1> {
    assert(phi.size == 1)
    return Forall(ref, phi[0])
  }

  fun <
      E1 : E,
      E : EntityType<E, T, S, U, D>,
      T : TickDataType<E, T, S, U, D>,
      S : SegmentType<E, T, S, U, D>,
      U : TickUnit<U, D>,
      D : TickDifference<D>> buildExists(ref: Ref<E1>): Exists<E1> {
    assert(phi.size == 1)
    return Exists(ref, phi[0])
  }

  private fun buildMinPrevalence(interval: Pair<Double, Double>?, fraction: Double): MinPrevalence {
    assert(phi.size == 1)
    return MinPrevalence(interval, fraction, phi[0])
  }

  private fun buildMaxPrevalence(interval: Pair<Double, Double>?, fraction: Double): MaxPrevalence {
    assert(phi.size == 1)
    return MaxPrevalence(interval, fraction, phi[0])
  }

  private fun buildPastMinPrevalence(interval: Pair<Double, Double>?, fraction: Double): PastMinPrevalence {
    assert(phi.size == 1)
    return PastMinPrevalence(interval, fraction, phi[0])
  }

  private fun buildPastMaxPrevalence(interval: Pair<Double, Double>?, fraction: Double): PastMaxPrevalence {
    assert(phi.size == 1)
    return PastMaxPrevalence(interval, fraction, phi[0])
  }

  fun <Type: Any> buildBinding(term: Term<Type>, wrap: Wrap<Type>): Binding<Type> {
    assert(phi.size == 1)
    return Binding(term, wrap, phi[0])
  }

  fun FormulaBuilder.tt(): TT = TT.also { phi.add(it) }
  fun FormulaBuilder.ff(): FF = FF.also { phi.add(it) }
  fun <
      E1 : E,
      E : EntityType<E, T, S, U, D>,
      T : TickDataType<E, T, S, U, D>,
      S : SegmentType<E, T, S, U, D>,
      U : TickUnit<U, D>,
      D : TickDifference<D>> FormulaBuilder.pred(
      ref1: Ref<E1>,
      init: (E1) -> Boolean = { true }
  ): Formula = UnaryPredicate(ref1, partial(init, ref1.now())).also { phi.add(it) }
  fun <
      E1 : E,
      E2 : E,
      E : EntityType<E, T, S, U, D>,
      T : TickDataType<E, T, S, U, D>,
      S : SegmentType<E, T, S, U, D>,
      U : TickUnit<U, D>,
      D : TickDifference<D>> FormulaBuilder.pred(
      ref1: Ref<E1>,
      ref2: Ref<E2>,
      init: (Pair<E1, E2>) -> Boolean = { true }
  ): Formula = BinaryPredicate(ref1, ref2, partial(init, ref1.now() to ref2.now())).also { phi.add(it) }
  fun FormulaBuilder.neg(input: Formula): Neg {
    return Neg(input).also { phi.add(it) }
  }

  fun FormulaBuilder.neg(init: FormulaBuilder.() -> Unit = {}): Neg {
    return FormulaBuilder().apply(init).buildNeg().also { phi.add(it) }
  }

  infix fun Formula.and(other: Formula): And =
      And(this, other).also {
        phi.removeLast()
        phi.removeLast()
        phi.add(it)
      }

  infix fun Formula.or(other: Formula): Or =
      Or(this, other).also {
        phi.clear()
        phi.add(it)
      }

  infix fun Formula.impl(other: Formula): Implication =
      Implication(this, other).also {
        phi.clear()
        phi.add(it)
      }

  infix fun Formula.iff(other: Formula): Iff =
      Iff(this, other).also {
        phi.clear()
        phi.add(it)
      }

  fun FormulaBuilder.prev(
      interval: Pair<Double, Double>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Prev {
    return FormulaBuilder().apply(init).buildPrev(interval).also { phi.add(it) }
  }

  fun FormulaBuilder.next(
      interval: Pair<Double, Double>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Next {
    return FormulaBuilder().apply(init).buildNext(interval).also { phi.add(it) }
  }

  fun FormulaBuilder.once(
      interval: Pair<Double, Double>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Once {
    return FormulaBuilder().apply(init).buildOnce(interval).also { phi.add(it) }
  }

  fun FormulaBuilder.historically(
      interval: Pair<Double, Double>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Historically {
    return FormulaBuilder().apply(init).buildHistorically(interval).also { phi.add(it) }
  }

  fun eventually(
      interval: Pair<Double, Double>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Eventually {
    return FormulaBuilder().apply(init).buildEventually(interval).also { phi.add(it) }
  }

  fun FormulaBuilder.always(
      interval: Pair<Double, Double>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Always {
    return FormulaBuilder().apply(init).buildAlways(interval).also { phi.add(it) }
  }

  fun FormulaBuilder.since(
      interval: Pair<Double, Double>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Since {
    return FormulaBuilder().apply(init).buildSince(interval).also { phi.add(it) }
  }

  fun FormulaBuilder.until(
      interval: Pair<Double, Double>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Until {
    return FormulaBuilder().apply(init).buildUntil(interval).also { phi.add(it) }
  }

  inline fun <
      reified E1 : E,
      E : EntityType<E, T, S, U, D>,
      T : TickDataType<E, T, S, U, D>,
      S : SegmentType<E, T, S, U, D>,
      U : TickUnit<U, D>,
      D : TickDifference<D>> FormulaBuilder.forall(
      init: FormulaBuilder.(Ref<E1>) -> Unit = {}
  ): Forall<E1> {
    val ref = Ref<E1, E, T, S, U, D>()
    return FormulaBuilder().apply { init(ref) }.buildForall(ref).also { phi.add(it) }
  }

  inline fun <
      reified E1 : E,
      E : EntityType<E, T, S, U, D>,
      T : TickDataType<E, T, S, U, D>,
      S : SegmentType<E, T, S, U, D>,
      U : TickUnit<U, D>,
      D : TickDifference<D>> FormulaBuilder.exists(
      init: FormulaBuilder.(Ref<E1>) -> Unit = {}
  ): Exists<E1> {
    val ref = Ref<E1, E, T, S, U, D>()
    return FormulaBuilder().apply { init(ref) }.buildExists(ref).also { phi.add(it) }
  }

  fun FormulaBuilder.minPrevalence(
      fraction: Double,
      interval: Pair<Double, Double>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): MinPrevalence {
    return FormulaBuilder().apply(init).buildMinPrevalence(interval, fraction).also { phi.add(it) }
  }

  fun FormulaBuilder.maxPrevalence(
      fraction: Double,
      interval: Pair<Double, Double>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): MaxPrevalence {
    return FormulaBuilder().apply(init).buildMaxPrevalence(interval, fraction).also { phi.add(it) }
  }

  fun FormulaBuilder.pastMinPrevalence(
      fraction: Double,
      interval: Pair<Double, Double>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): PastMinPrevalence {
    return FormulaBuilder().apply(init).buildPastMinPrevalence(interval, fraction).also { phi.add(it) }
  }

  fun FormulaBuilder.pastMaxPrevalence(
      fraction: Double,
      interval: Pair<Double, Double>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): PastMaxPrevalence {
    return FormulaBuilder().apply(init).buildPastMaxPrevalence(interval, fraction).also { phi.add(it) }
  }

  inline fun <reified Type: Any> FormulaBuilder.binding(
      term: Term<Type>,
      init: FormulaBuilder.(Wrap<Type>) -> Unit = {}
  ): Binding<Type> {
    val wrap = Wrap<Type>()
    return FormulaBuilder().apply { init(wrap) }.buildBinding(term, wrap).also { phi.add(it) }
  }

  infix fun <Type> Term<Type>.leq(other: Term<Type>): Leq<Type> =
      Leq(this, other).also { phi.add(it) }

  infix fun <Type> Term<Type>.lt(other: Term<Type>): Lt<Type> = Lt(this, other).also { phi.add(it) }

  infix fun <Type> Term<Type>.geq(other: Term<Type>): Geq<Type> =
      Geq(this, other).also { phi.add(it) }

  infix fun <Type> Term<Type>.gt(other: Term<Type>): Gt<Type> = Gt(this, other).also { phi.add(it) }

  infix fun <Type> Term<Type>.eq(other: Term<Type>): Eq<Type> = Eq(this, other).also { phi.add(it) }

  infix fun <Type> Term<Type>.ne(other: Term<Type>): Ne<Type> = Ne(this, other).also { phi.add(it) }
  fun <Type> term(init: () -> Type): Variable<Type> = Variable(init)
  fun <Type> const(value: Type): Constant<Type> = Constant(value)
}

fun <
    E1 : E,
    E : EntityType<E, T, S, U, D>,
    T : TickDataType<E, T, S, U, D>,
    S : SegmentType<E, T, S, U, D>,
    U : TickUnit<U, D>,
    D : TickDifference<D>> ((Ref<E1>) -> FormulaBuilder).holds(ref1: Ref<E1>): Formula =
    this(ref1).phi[0]

fun <
    E1 : E,
    E2 : E,
    E : EntityType<E, T, S, U, D>,
    T : TickDataType<E, T, S, U, D>,
    S : SegmentType<E, T, S, U, D>,
    U : TickUnit<U, D>,
    D : TickDifference<D>> ((Ref<E1>, Ref<E2>) -> FormulaBuilder).holds(
    ref1: Ref<E1>,
    ref2: Ref<E2>
): Formula = this(ref1, ref2).phi[0]
