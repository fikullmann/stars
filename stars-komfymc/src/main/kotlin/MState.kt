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

import tools.aqua.auxStructures.*


/*
sealed interface MState<U : TickUnit<U, D>, D : TickDifference<D>> {
    fun eval(
    ts: TS<U, D>,
    tp: TP,
    ref: MutableList<Ref<EntityType<*, *, *, *, *>>>
    ): List<Pdt<Proof>>
}

class MTT<U : TickUnit<U, D>, D : TickDifference<D>> : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>>
        = listOf(Leaf<Proof>(SatTT(tp)))
}

class MFF<U : TickUnit<U, D>, D : TickDifference<D>> : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>>
        = listOf(Leaf<Proof>(VFF(tp)))
}
interface Pred {
    fun call(): Boolean
}

data class MUnaryPred<U : TickUnit<U, D>, D : TickDifference<D>,
        T : EntityType<*, *, *, *, *>>(val ref: Ref<T>, val phi: (T) -> Boolean) : Pred, MState<U, D> {
    override fun call() = phi.invoke(ref.now())
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        if (formula.ref.fixed) {
            return listOf(Leaf(if(formula.call()) SatPred(tp) else VPred(tp)))
        } else {
            val part = formula.ref.cycleEntitiesAtTick(tp, formula)
            return listOf(Node(formula.ref, part))
        }
    }
}
data class MBinaryPred<U : TickUnit<U, D>, D : TickDifference<D>,
        E1 : EntityType<*, *, *, *, *>, E2 : EntityType<*, *, *, *, *>>(
    val ref1: Ref<E1>,
    val ref2: Ref<E2>,
    val phi: (E1, E2) -> Boolean
) : Pred, MState<U, D> {
    fun fix1() = MUnaryPred<U, D, E2>(ref2, partial2First(phi, ref1.now()))
    fun fix2() = MUnaryPred<U, D, E1>(ref1, partial2Second(phi, ref2.now()))
    override fun call() = phi.invoke(ref1.now(), ref2.now())
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val pair = formula.ref1.fixed to formula.ref2.fixed
        when (pair) {
            true to true -> return listOf(Leaf(if(formula.call()) SatPred(tp) else VPred(tp)))
            true to false -> return formula.fix1().eval(ts, tp, ref)
            false to true -> return formula.fix2().eval(ts, tp, ref)
            else -> {
                val order = ref.indexOf(ref1 as Ref<EntityType<*, *, *, *, *>>) < ref.indexOf(ref2 as Ref<EntityType<*, *, *, *, *>>)
                val firstRef = if (order) formula.ref1 else formula.ref2
                val secondRef = if (order) formula.ref2 else formula.ref1
                val part = firstRef.cycleBinaryEntitiesAtTick(tp, formula, secondRef)
                return listOf(Node(firstRef, part))
            }
        }
    }
}

data class MNegate<U : TickUnit<U, D>, D : TickDifference<D>>(val inner: MState<U, D>) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val expls = inner.eval(ts, tp, ref)
        val fExpls = expls.map { expl -> expl.apply1(ref, ::evalNeg) }
        return fExpls
    }
}

data class MAnd<U : TickUnit<U, D>, D : TickDifference<D>>(val lhs: MState<U, D>, val rhs: MState<U, D>, val buf: Buf2 = Buf2()) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val p1 = lhs.eval(ts, tp, ref)
        val p2 = rhs.eval(ts, tp, ref)
        formula.buf.add(p1, p2)
        val fExpls = formula.buf.take(ref, ::evalAnd)
        return fExpls
    }
}


data class MOr<U : TickUnit<U, D>, D : TickDifference<D>>(val lhs: MState<U, D>, val rhs: MState<U, D>, val buf: Buf2 = Buf2()) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val p1 = lhs.eval(ts, tp, ref)
        val p2 = rhs.eval(ts, tp, ref)
        formula.buf.add(p1, p2)
        val fExpls = formula.buf.take(ref, ::evalOr)
        return fExpls
    }
}


data class MIff<U : TickUnit<U, D>, D : TickDifference<D>>(val lhs: MState<U, D>, val rhs: MState<U, D>, val buf: Buf2 = Buf2()) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val p1 = lhs.eval(ts, tp, ref)
        val p2 = rhs.eval(ts, tp, ref)
        formula.buf.add(p1, p2)
        val fExpls = formula.buf.take(ref, ::evalIff)
        return fExpls
    }
}


data class MImpl<U : TickUnit<U, D>, D : TickDifference<D>>(val lhs: MState<U, D>, val rhs: MState<U, D>, val buf: Buf2 = Buf2()) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val p1 = lhs.eval(ts, tp, ref)
        val p2 = rhs.eval(ts, tp, ref)
        formula.buf.add(p1, p2)
        val fExpls = formula.buf.take(ref, ::evalImpl)
        return fExpls
    }
}


data class MPrev<U : TickUnit<U, D>, D : TickDifference<D>>(
    val interval: Interval<D>,
    val inner: MState<U, D>,
    var first: Boolean = true,
    val buft: BufPrevNext<U, D> = BufPrevNext(),
) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val expl = inner.eval(ts, tp, ref)
        formula.buft.add(expl, ts)
        val prevvedExpl =
            formula.buft
                .take(
                    ref,
                    true,
                    formula.interval,
                )
                .toMutableList()
        if (formula.first) {
            prevvedExpl.add(0, Leaf(VPrev0))
            formula.first = false
        }
        return prevvedExpl
    }
}


data class MNext<U : TickUnit<U, D>, D : TickDifference<D>>(
    val interval: Interval<D>,
    val inner: MState<U, D>,
    var first: Boolean = true,
    val buft: BufPrevNext<U, D> = BufPrevNext(),
) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val expl = inner.eval(ts, tp, ref).toMutableList()
        if (formula.first && expl.isNotEmpty()) {
            expl.removeFirst()
            formula.first = false
        }
        formula.buft.add(expl, ts)
        val nextedExpl = formula.buft.take(ref, false, formula.interval).toMutableList()
        formula.buft.clearInner()
        return nextedExpl
    }
}


data class MOnce<U : TickUnit<U, D>, D : TickDifference<D>>(
    val interval: Interval<D>,
    val inner: MState<U, D>,
    val buft: BufT<U, D> = BufT<U, D>(),
    var mAuxPdt: Pdt<Oaux<U, D>> = Leaf(Oaux())
) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val p1 = inner.eval(ts, tp, ref)
        formula.buft.add(p1, ts to tp)

        val result = mutableListOf<Pdt<Proof>>()
        for ((expl, ts1, tp1) in formula.buft) {
            val (pPdt, auxPdt) =
                split(
                    apply2(ref, expl, formula.mAuxPdt) { p, aux ->
                        aux.update1(formula.interval, ts1, tp1, p)
                    })
            result.add(pPdt)
            formula.mAuxPdt = auxPdt
        }
        formula.buft.clearInner()
        return result
    }
}


data class MHistorically<U : TickUnit<U, D>, D : TickDifference<D>>(
    val interval: Interval<D>,
    val inner: MState<U, D>,
    val buft: BufT<U, D> = BufT<U, D>(),
    var hAux: Pdt<Haux<U, D>> = Leaf(Haux())
) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val p1 = inner.eval(ts, tp, ref)
        formula.buft.add(p1, ts to tp)
        val result = mutableListOf<Pdt<Proof>>()
        for ((expl, ts1, tp1) in formula.buft) {
            val (pPdt, auxPdt) =
                split(
                    apply2(ref, expl, formula.hAux) { p, aux ->
                        aux.update1(formula.interval, ts1, tp1, p)
                    })
            result.add(pPdt)
            formula.hAux = auxPdt
        }
        formula.buft.clearInner()
        return result
    }
}


data class MEventually<U : TickUnit<U, D>, D : TickDifference<D>>(
    val interval: Interval<D>,
    val inner: MState<U, D>,
    val buft: BufT<U, D> = BufT<U, D>(),
    var eAux: Pdt<Eaux<U, D>> = Leaf(Eaux())
) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val p1 = inner.eval(ts, tp, ref)
        formula.buft.add(p1, ts to tp)

        val result = mutableListOf<Pdt<Proof>>()
        for ((expl, ts1, tp1) in formula.buft) {
            val (listsPdt, auxPdt) =
                split(
                    apply2(ref, expl, formula.eAux) { p, aux ->
                        aux.update(formula.interval, ts1, tp1, p)
                    })
            result.addAll(splitList(listsPdt))
            formula.eAux = auxPdt
        }
        return result
    }
}


data class MAlways<U : TickUnit<U, D>, D : TickDifference<D>>(
    val interval: Interval<D>,
    val inner: MState<U, D>,
    val buft: BufT<U, D> = BufT<U, D>(),
    var aAux: Pdt<Aaux<U, D>> = Leaf(Aaux<U, D>())
) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val p1 = inner.eval(ts, tp, ref)
        formula.buft.add(p1, ts to tp)
        val result = mutableListOf<Pdt<Proof>>()
        for ((expl, ts1, tp1) in formula.buft) {
            val (listsPdt, auxPdt) =
                split(
                    apply2(ref, expl, formula.aAux as Pdt<Aaux<U, D>>) { p, aux ->
                        aux.update(formula.interval as Interval<D>, ts1 as TS<U, D>, tp1, p)
                    })
            result.addAll(splitList(listsPdt))
            formula.aAux = auxPdt
        }
        return result
    }
}


data class MSince<U : TickUnit<U, D>, D : TickDifference<D>>(
    val interval: Interval<D>,
    val lhs: MState<U, D>,
    val rhs: MState<U, D>,
    val buf2t: Buf2T<U, D> = Buf2T(),
    var saux: Pdt<Saux<U, D>> = Leaf(Saux<U, D>())
) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val pL = lhs.eval(ts, tp, ref)
        val pR = rhs.eval(ts, tp, ref)
        formula.buf2t.add(pL, pR, ts to tp)
        val result = mutableListOf<Pdt<Proof>>()
        for ((expl, ts1, tp1) in formula.buf2t) {
            val (expl1, expl2) = expl
            val (pPdt, auxPdt) =
                split(
                    apply3(ref, expl1, expl2, formula.saux) { p1, p2, aux ->
                        aux.update1(formula.interval, ts1, tp1, p1, p2)
                    })
            result.add(pPdt)
            formula.saux = auxPdt
        }
        return result
    }
}


data class MUntil<U : TickUnit<U, D>, D : TickDifference<D>>(
    val interval: Interval<D>,
    val lhs: MState<U, D>,
    val rhs: MState<U, D>,
    val buf2t: Buf2T<U, D> = Buf2T(),
    var uaux: Pdt<Uaux<U, D>> = Leaf(Uaux<U, D>())
) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val pL = lhs.eval(ts, tp, ref)
        val pR = rhs.eval(ts, tp, ref)
        formula.buf2t.add(pL, pR, ts to tp)
        val result = mutableListOf<Pdt<Proof>>()
        for ((expl, ts1, tp1) in formula.buf2t) {
            val (expl1, expl2) = expl
            val (pPdt, auxPdt) =
                split(
                    apply3(ref, expl1, expl2, formula.uaux) { p1, p2, aux ->
                        aux.update1(formula.interval, ts1, tp1, p1, p2)
                    })
            result.addAll(splitList(pPdt))
            formula.uaux = auxPdt
        }
        return result
    }
}


data class MExists<U : TickUnit<U, D>, D : TickDifference<D>>(
    val ref: Ref<*>,
    val inner: MState<U, D>,
    // val proofs: MutableList<Tree> = mutableListOf(),
) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val newVar = formula.ref as Ref<EntityType<*, *, *, *, *>>
        ref.add(newVar)
        val expls1 = inner.eval(ts, tp, ref)
        val fExpls = expls1.map { expl -> hide(ref, expl, ::existsLeaf, ::existsNode, newVar) }
        return fExpls
    }
}


data class MForall<U : TickUnit<U, D>, D : TickDifference<D>>(
    val ref: Ref<*>,
    val inner: MState<U, D>,
    // val proofs: MutableList<Proof> = mutableListOf(),
    // val tsTp: MutableList<Pair<TS, TP>> = mutableListOf()
) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val newVar = formula.ref as Ref<EntityType<*, *, *, *, *>>
        ref.add(newVar)
        val expls1 = inner.eval(ts, tp, ref)
        val fExpls = expls1.map { expl -> hide(ref, expl, ::forallLeaf, ::forallNode, newVar) }
        return fExpls
    }
}


data class MPastMinPrev<U : TickUnit<U, D>, D : TickDifference<D>>(
    val interval: Interval<D>,
    val factor: Double,
    val inner: MState<U, D>,
    val buft: BufT<U, D> = BufT<U, D>(),
    var aux: Pdt<PastMinAux<U, D>> = Leaf(PastMinAux<U, D>())
) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val p1 = inner.eval(ts, tp, ref)
        formula.buft.add(p1, ts to tp)

        val result = mutableListOf<Pdt<Proof>>()
        for ((expl, ts1, tp1) in formula.buft) {
            val (pPdt, auxPdt) =
                split(
                    apply2(ref, expl, formula.aux) { p, aux ->
                        aux.update1(formula.interval, ts1, tp1, formula.factor, p)
                    })
            result.add(pPdt)
            formula.aux = auxPdt
        }
        return result
    }
}


data class MPastMaxPrev<U : TickUnit<U, D>, D : TickDifference<D>>(
    val interval: Interval<D>,
    val factor: Double,
    val inner: MState<U, D>,
    val buft: BufT<U, D> = BufT<U, D>(),
    var aux: Pdt<PastMaxAux<U, D>> = Leaf(PastMaxAux<U, D>())
) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val p1 = inner.eval(ts, tp, ref)
        formula.buft.add(p1, ts to tp)

        val result = mutableListOf<Pdt<Proof>>()
        for ((expl, ts1, tp1) in formula.buft) {
            val (pPdt, auxPdt) =
                split(
                    apply2(ref, expl, formula.aux) { p, aux ->
                        aux.update1(formula.interval, ts1, tp1, formula.factor, p)
                    })
            result.add(pPdt)
            formula.aux = auxPdt
        }
        return result
    }
}


data class MMinPrev<U : TickUnit<U, D>, D : TickDifference<D>>(
    val interval: Interval<D>,
    val factor: Double,
    val inner: MState<U, D>,
    val buft: BufT<U, D> = BufT<U, D>(),
    var aux: Pdt<MinAux<U, D>> = Leaf(MinAux<U, D>())
) : MState<U, D> {
    override fun eval(
        ts: TS<U, D>,
        tp: TP,
        ref: MutableList<Ref<EntityType<*, *, *, *, *>>>,
    ): List<Pdt<Proof>> {
        val p1 = inner.eval(ts, tp, ref)
        buft.add(p1, ts to tp)
        val result = mutableListOf<Pdt<Proof>>()
        for ((expl, ts1, tp1) in buft) {
            val (listsPdt, auxPdt) =
                split(
                    apply2(ref, expl, aux) { p, aux ->
                        aux.update(interval, ts1, tp1, factor, p)
                    })
            result.addAll(splitList(listsPdt))
            aux = auxPdt
        }
        return result
    }
}

data class MMaxPrev<U : TickUnit<U, D>, D : TickDifference<D>>(
    val interval: Interval<D>,
    val factor: Double,
    val inner: MState<U, D>,
    val buft: BufT<U, D> = BufT<U, D>(),
    var aux: Pdt<MaxAux<U, D>> = Leaf(MaxAux<U, D>())
) : MState<U, D> {
    override fun eval(ts: TS<U, D>, tp: TP, ref: MutableList<Ref<EntityType<*, *, *, *, *>>>): List<Pdt<Proof>> {
        val formula = this
        val p1 = inner.eval(ts, tp, ref)
        formula.buft.add(p1, ts to tp)
        val result = mutableListOf<Pdt<Proof>>()
        for ((expl, ts1, tp1) in formula.buft) {
            val (listsPdt, auxPdt) =
                split(
                    apply2(ref, expl, formula.aux) { p, aux ->
                        aux.update(formula.interval, ts1, tp1, formula.factor, p)
                    })
            result.addAll(splitList(listsPdt))
            formula.aux = auxPdt
        }
        return result
    }
}


class Buf2(
    val lhs: MutableList<Pdt<Proof>> = mutableListOf(),
    val rhs: MutableList<Pdt<Proof>> = mutableListOf()
) {
  fun add(p1s: List<Pdt<Proof>>, p2s: List<Pdt<Proof>>) {
    lhs.addAll(p1s)
    rhs.addAll(p2s)
  }

  fun take(
      ref: MutableList<Ref<EntityType<*, *, *, *, *>>>,
      f: (Proof, Proof) -> Proof
  ): List<Pdt<Proof>> {
    val result = mutableListOf<Pdt<Proof>>()
    if (lhs.isNotEmpty() && rhs.isNotEmpty()) {
      val expl1 = lhs.removeFirst()
      val expl2 = rhs.removeFirst()
      result.add(apply2(ref, expl1, expl2) { p1, p2 -> f(p1, p2) })
    }
    return result
  }
}

class BufPrevNext<U : TickUnit<U, D>, D : TickDifference<D>>(
    val inner: MutableList<Pdt<Proof>> = mutableListOf(),
    val tss: MutableList<TS<U, D>> = mutableListOf()
) {
  fun clearInner() {
    inner.clear()
  }

  fun add(p1s: List<Pdt<Proof>>, ts: TS<U, D>) {
    inner.addAll(p1s)
    tss.add(ts)
  }

  fun take(
      ref: MutableList<Ref<EntityType<*, *, *, *, *>>>,
      prev: Boolean,
      interval: Interval<D>
  ): List<Pdt<Proof>> {
    val result = mutableListOf<Pdt<Proof>>()
    if (inner.isNotEmpty() && tss.size > 1) {
      val expl = inner.removeFirst()
      val ts1 = tss.removeFirst()
      val ts2 = tss.first()
      result.add(expl.apply1(ref) { p -> MPrevNext.update(prev, interval, p, ts1, ts2) })
    }
    return result
  }
}

class BufT<U : TickUnit<U, D>, D : TickDifference<D>>(
    val inner: MutableList<Pdt<Proof>> = mutableListOf(),
    val tsTp: MutableList<Pair<TS<U, D>, TP>> = mutableListOf()
) : Iterable<Triple<Pdt<Proof>, TS<U, D>, TP>> {
  fun clearInner() {
    inner.clear()
  }

  fun add(p1s: List<Pdt<Proof>>, ts: Pair<TS<U, D>, TP>) {
    inner.addAll(p1s)
    tsTp.add(ts)
  }

  fun take(): Triple<Pdt<Proof>, TS<U, D>, TP> {
    val expl = inner.removeFirst()
    val (ts, tp) = tsTp.removeFirst()
    return Triple(expl, ts, tp)
  }

  override fun iterator(): Iterator<Triple<Pdt<Proof>, TS<U, D>, TP>> = BufTIterator(this)
}

class BufTIterator<U : TickUnit<U, D>, D : TickDifference<D>>(val self: BufT<U, D>) : Iterator<Triple<Pdt<Proof>, TS<U, D>, TP>> {
  override fun hasNext(): Boolean = self.inner.isNotEmpty() && self.tsTp.isNotEmpty()

  override fun next(): Triple<Pdt<Proof>, TS<U, D>, TP> = self.take()
}

class Buf2T<U : TickUnit<U, D>, D : TickDifference<D>>(
    val lhs: MutableList<Pdt<Proof>> = mutableListOf(),
    val rhs: MutableList<Pdt<Proof>> = mutableListOf(),
    val tsTp: MutableList<Pair<TS<U, D>, TP>> = mutableListOf()
) : Iterable<Triple<Pair<Pdt<Proof>, Pdt<Proof>>, TS<U, D>, TP>> {
  fun add(p1s: List<Pdt<Proof>>, p2s: List<Pdt<Proof>>, ts: Pair<TS<U, D>, TP>) {
    lhs.addAll(p1s)
    rhs.addAll(p2s)
    tsTp.add(ts)
  }

  fun take(): Triple<Pair<Pdt<Proof>, Pdt<Proof>>, TS<U, D>, TP> {
    val expl1 = lhs.removeFirst()
    val expl2 = rhs.removeFirst()
    val (ts, tp) = tsTp.removeFirst()
    return Triple(expl1 to expl2, ts, tp)
  }

  override fun iterator(): Iterator<Triple<Pair<Pdt<Proof>, Pdt<Proof>>, TS<U, D>, TP>> =
      Buf2TIterator(this)
}

class Buf2TIterator<U : TickUnit<U, D>, D : TickDifference<D>>(val self: Buf2T<U, D>) : Iterator<Triple<Pair<Pdt<Proof>, Pdt<Proof>>, TS<U, D>, TP>> {
  override fun hasNext(): Boolean =
      self.lhs.isNotEmpty() && self.rhs.isNotEmpty() && self.tsTp.isNotEmpty()

  override fun next(): Triple<Pair<Pdt<Proof>, Pdt<Proof>>, TS<U, D>, TP> = self.take()
}

fun evalNeg(p1: Proof): Proof {
    return when {
        p1 is SatProof -> VNeg(p1)
        p1 is ViolationProof -> SatNeg(p1)
        else -> ErrorProof
    }
}

fun evalAnd(p1: Proof, p2: Proof): Proof {
    return when {
        p1 is SatProof && p2 is SatProof -> SatAnd(p1, p2)
        p1 is SatProof && p2 is ViolationProof -> VAndR(p2)
        p1 is ViolationProof && p2 is SatProof -> VAndL(p1)
        p1 is ViolationProof && p2 is ViolationProof ->
            if (p1.size() <= p2.size()) VAndL(p1) else VAndR(p2)
        else -> ErrorProof
    }
}

fun evalOr(p1: Proof, p2: Proof): Proof {
    return when {
        p1 is SatProof && p2 is SatProof -> if (p1.size() <= p2.size()) SatOrL(p1) else SatOrR(p2)
        p1 is SatProof && p2 is ViolationProof -> SatOrL(p1)
        p1 is ViolationProof && p2 is SatProof -> SatOrR(p2)
        p1 is ViolationProof && p2 is ViolationProof -> VOr(p1, p2)
        else -> ErrorProof
    }
}

fun evalIff(p1: Proof, p2: Proof): Proof {
    return when {
        p1 is SatProof && p2 is SatProof -> SatIffSS(p1, p2)
        p1 is SatProof && p2 is ViolationProof -> VIffSV(p1, p2)
        p1 is ViolationProof && p2 is SatProof -> VIffVS(p1, p2)
        p1 is ViolationProof && p2 is ViolationProof -> SatIffVV(p1, p2)
        else -> ErrorProof
    }
}

fun evalImpl(p1: Proof, p2: Proof): Proof {
    return when {
        p1 is SatProof && p2 is SatProof -> SatImplR(p2)
        p1 is SatProof && p2 is ViolationProof -> VImpl(p1, p2)
        p1 is ViolationProof && p2 is SatProof ->
            if (p1.size() < p2.size()) SatImplL(p1) else SatImplR(p2)
        p1 is ViolationProof && p2 is ViolationProof -> SatImplL(p1)
        else -> ErrorProof
    }
}

fun <T : EntityType<*, *, *, *, *>> existsLeaf(ref: Ref<T>, p1: Proof): Proof {
    return when (p1) {
        is SatProof -> SatExists(ref, null, p1)
        is ViolationProof ->
            VExists(ref, listOf(RefId(ref.allAtTick().map { it.id }.firstOrNull() ?: -1) to p1))
        else -> ErrorProof
    }
}

fun <T : EntityType<*, *, *, *, *>> existsNode(
    ref: Ref<T>,
    part: List<Pair<RefId, Proof>>
): Proof {
    if (part.any { it.second is SatProof }) {
        val sats = part.filter { it.second is SatProof }
        return minpList(
            sats.map { (refId, proof) ->
                assert(proof is SatProof)
                SatExists(ref, refId, proof as SatProof)
            })
    } else {
        return VExists(ref, part.map { (set, proof) -> set to (proof as ViolationProof) })
    }
}

fun <T : EntityType<*, *, *, *, *>> forallLeaf(ref: Ref<T>, p1: Proof): Proof {
    return when (p1) {
        is SatProof ->
            SatForall(ref, listOf(RefId(ref.allAtTick().map { it.id }.firstOrNull() ?: -1) to p1))
        is ViolationProof -> VForall(ref, null, p1)
        else -> ErrorProof
    }
}

fun <T : EntityType<*, *, *, *, *>> forallNode(
    ref: Ref<T>,
    part: List<Pair<RefId, Proof>>
): Proof {
    if (part.all { it.second is SatProof }) {
        return SatForall(ref, part.map { (set, proof) -> set to (proof as SatProof) })
    } else {
        val viols = part.filter { it.second is ViolationProof }
        return minpList(
            viols.map { (refId, proof) ->
                assert(proof is ViolationProof)
                VForall(ref, refId, proof as ViolationProof)
            })
    }
}

 */
