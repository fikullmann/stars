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

import tools.aqua.auxStructures.EmptyVariableReferences
import tools.aqua.auxStructures.IncorrectVariableReferences
import tools.aqua.dsl.Ref
import tools.aqua.dsl.RefId
import tools.aqua.stars.core.types.EntityType


abstract class Pdt<Aux> {
    abstract fun <Result> apply1(vars: MutableList<Ref<EntityType<*, *, *, *, *>>>, f: (Aux) -> Result): Pdt<Result>
}

class Leaf<Aux>(val value: Aux) : Pdt<Aux>() {
    override fun <Result> apply1(vars: MutableList<Ref<EntityType<*, *, *, *, *>>>, f: (Aux) -> Result): Pdt<Result> {
        return Leaf(f(value))
    }
}

class Node<Aux>(val variable: Ref<out EntityType<*, *, *, *, *>>, val partition: List<Pair<RefId, Pdt<Aux>>>) : Pdt<Aux>() {
    override fun <Result> apply1(vars: MutableList<Ref<EntityType<*, *, *, *, *>>>, f: (Aux) -> Result): Pdt<Result> {
        if (vars.isNotEmpty()) {
            for (variable in vars) {
                if (this.variable == variable) {
                    return Node(variable, partition.map { it.first to it.second.apply1(vars, f) })
                }
            }
            throw IncorrectVariableReferences()
        }
        throw EmptyVariableReferences()
    }
}

fun at(pdt: Pdt<Proof>): TP {
    return when (pdt) {
        is Leaf<Proof> -> pdt.value.at()
        is Node<Proof> -> at((pdt.partition.first().second))
        else -> throw Exception()
    }
}

fun <P, Aux> split(pdt: Pdt<Pair<P, Aux>>): Pair<Pdt<P>, Pdt<Aux>> {
    when (pdt) {
        is Leaf<Pair<P, Aux>> -> return Leaf(pdt.value.first) to Leaf(pdt.value.second)
        is Node<Pair<P, Aux>> -> {
            val proofPartition = mutableListOf<Pair<RefId, Pdt<P>>>()
            val auxPartition = mutableListOf<Pair<RefId, Pdt<Aux>>>()
            for (p in pdt.partition) {
                split(p.second).let { (proof, aux) ->
                    proofPartition.add(p.first to proof)
                    auxPartition.add(p.first to aux)
                }
            }
            return Node(pdt.variable, proofPartition) to Node(pdt.variable, auxPartition)
        }
        else -> throw Exception()
    }
}
fun <P> splitList(pdt: Pdt<MutableList<P>>): List<Pdt<P>> {
    when (pdt) {
        is Leaf -> return pdt.value.map { Leaf(it) }
        is Node -> {
            val childPartition = mutableListOf<List<Pdt<P>>>()
            val partSplitList = pdt.partition.map { it.first to splitList(it.second) }
            val partFst = partSplitList.map { it.first }
            val partSnd = partSplitList.map { it.second }
            for (i in 0..<partSnd.first().size) {
                childPartition.add(partSnd.map { it[i] })
            }
            return childPartition.map { Node(pdt.variable, partFst.zip(it)) }
        }
        else -> throw Exception("Pdt can only be Node or Leaf, but somehow it was neither.")
    }
}

fun <P, Result> apply1(vars: MutableList<Ref<EntityType<*, *, *, *, *>>>, tree: Pdt<P>, f: (P) -> Result): Pdt<Result> {
    if (tree is Leaf<P>) {
        return Leaf(f(tree.value))
    } else if (vars.isNotEmpty() && tree is Node<P>) {
        for (variable in vars) {
            if (tree.variable == variable) {
                return Node(variable, tree.partition.map { it.first to apply1(vars, it.second, f) })
            }
        }
        throw IncorrectVariableReferences()
    }
    throw EmptyVariableReferences()
}

fun <P, Aux, Result> apply2(refs: MutableList<Ref<EntityType<*, *, *, *, *>>>, pdt1: Pdt<P>, pdt2: Pdt<Aux>, f: (P, Aux) -> Result): Pdt<Result> {
    val vars = refs.toMutableList()
    return when {
        pdt1 is Leaf<P> && pdt2 is Leaf<Aux> -> Leaf(f(pdt1.value, pdt2.value))
        pdt1 is Leaf<P> && pdt2 is Node<Aux> -> Node(pdt2.variable, pdt2.partition.map {
            it.first to apply1(vars, it.second, partial2First(f, pdt1.value))
        })
        pdt1 is Node<P> && pdt2 is Leaf<Aux> -> Node(pdt1.variable, pdt1.partition.map { (refId, tree) ->
            refId to apply1(vars, tree, partial2Second(f, pdt2.value))
        })
        vars.isNotEmpty() && pdt1 is Node<P> && pdt2 is Node<Aux> -> {
            compareVariables(vars, pdt1, pdt2, f)
        }
        else -> throw EmptyVariableReferences()
    }
}

private fun <P, Aux, Result> compareVariables(vars: MutableList<Ref<EntityType<*, *, *, *, *>>>, pdt1: Node<P>, pdt2: Node<Aux>, f: (P, Aux) -> Result): Pdt<Result> {
    val variable = vars.removeFirst()
    val equalPair = (pdt1.variable == variable) to (pdt2.variable == variable)
    return when (equalPair) {
        true to true -> Node(variable, merge2(pdt1.partition, pdt2.partition).map { (refId1, part1, part2 ) ->
            refId1 to apply2(vars, part1, part2, f)
        })
        true to false -> Node(variable, pdt1.partition.map { (refId1, tree1) ->
            refId1 to apply2(vars, tree1, pdt2, f)
        })
        false to true -> Node(variable, pdt2.partition.map { (refId2, tree2) ->
            refId2 to apply2(vars, pdt1, tree2, f)
        })
        else -> apply2(vars, pdt1, pdt2, f)
    }
}

fun <P1, P2> merge2( part1: List<Pair<RefId, Pdt<P1>>>, part2: List<Pair<RefId, Pdt<P2>>>): List<Triple<RefId, Pdt<P1>, Pdt<P2>>> {
    val result = mutableListOf<Triple<RefId, Pdt<P1>, Pdt<P2>>>()
    part1.mapNotNull { (refId1, pdt1 ) ->
        part2.firstOrNull { (refId2, _) ->
            refId1 == refId2
        }?.let { (_, pdt2) ->
            result.add(Triple(refId1, pdt1, pdt2))
        }
    }
    return result
}

fun <P1, P3> merge3(part1: List<Pair<RefId, Pdt<P1>>>, part2: List<Pair<RefId, Pdt<P1>>>, part3: List<Pair<RefId, Pdt<P3>>>): List<Pair<RefId, Triple<Pdt<P1>, Pdt<P1>, Pdt<P3>>>> {
    val result = mutableListOf<Pair<RefId, Triple<Pdt<P1>, Pdt<P1>, Pdt<P3>>>>()
    part1.mapNotNull { (refId1, pdt1) ->
        val found2 = part2.firstOrNull { refId1 == it.first }
        val found3 = part3.firstOrNull { refId1 == it.first }
        if (found2 != null && found3 != null) {
            result.add(refId1 to Triple(pdt1, found2.second, found3.second))
        } else null
    }
    return result
}

fun <P, Aux, Result> apply3(refs: MutableList<Ref<EntityType<*, *, *, *, *>>>, pdt1: Pdt<P>, pdt2: Pdt<P>, pdt3: Pdt<Aux>, f: (P, P, Aux) -> Result): Pdt<Result> {
    val vars = refs.toMutableList()
    when {
        pdt1 is Leaf<P> && pdt2 is Leaf<P> && pdt3 is Leaf<Aux> -> return Leaf(f(pdt1.value, pdt2.value, pdt3.value))
        pdt1 is Node<P> && pdt2 is Leaf<P> && pdt3 is Leaf<Aux> -> return Node(pdt1.variable, pdt1.partition.map { (refId, tree) ->
            refId to apply1(vars, tree, partial3BC(f, pdt2.value, pdt3.value))
        })
        pdt1 is Leaf<P> && pdt2 is Node<P> && pdt3 is Leaf<Aux> -> return Node(pdt2.variable, pdt2.partition.map { (refId, tree) ->
            refId to apply1(vars, tree, partial3AC(f, pdt1.value, pdt3.value))
        })
        pdt1 is Leaf<P> && pdt2 is Leaf<P> && pdt3 is Node<Aux> -> return Node(pdt3.variable, pdt3.partition.map {
            it.first to apply1(vars, it.second, partial3AB(f, pdt1.value, pdt2.value))
        })
        vars.isNotEmpty() && pdt1 is Node<P> && pdt2 is Node<P> && pdt3 is Leaf<Aux> -> return compareVariables(vars, pdt1, pdt2, partial3C(f, pdt3.value))
        vars.isNotEmpty() && pdt1 is Node<P> && pdt2 is Leaf<P> && pdt3 is Node<Aux> -> return compareVariables(vars, pdt1, pdt3, partial3B(f, pdt2.value))
        vars.isNotEmpty() && pdt1 is Leaf<P> && pdt2 is Node<P> && pdt3 is Node<Aux> -> return compareVariables(vars, pdt2, pdt3, partial3A(f, pdt1.value))
        vars.isNotEmpty() && pdt1 is Node<P> && pdt2 is Node<P> && pdt3 is Node<Aux> -> {
            val variable = vars.removeFirst()
            val equalTriple = (pdt1.variable == variable) to (pdt2.variable == variable) to (pdt3.variable == variable)
            return when(equalTriple){
                (true to true) to true -> Node(variable, merge3(pdt1.partition, pdt2.partition, pdt3.partition).map { (refId1, trees) ->
                    refId1 to apply3(vars, trees.first, trees.second, trees.third, f)
                })
                (true to true) to false -> Node(variable, merge2(pdt1.partition, pdt2.partition).map { (refId1, tree1, tree2) ->
                    refId1 to apply3(vars, tree1, tree2, pdt3, f)
                })
                (true to false) to true -> Node(variable, merge2(pdt1.partition, pdt3.partition).map { (refId1, tree1, tree3) ->
                    refId1 to apply3(vars, tree1, pdt2, tree3, f)
                })
                (false to true) to true -> Node(variable, merge2(pdt2.partition, pdt3.partition).map { (refId2, tree2, tree3) ->
                    refId2 to apply3(vars, pdt1, tree2, tree3, f)
                })
                (false to false) to true -> Node(variable, pdt3.partition.map { (refId3, tree3) ->
                    refId3 to apply3(vars, pdt1, pdt2, tree3, f)
                })
                (false to true) to false -> Node(variable, pdt2.partition.map { (refId2, tree2) ->
                    refId2 to apply3(vars, pdt1, tree2, pdt3, f)
                })
                (true to false) to false -> Node(variable, pdt1.partition.map { (refId1, tree1) ->
                    refId1 to apply3(vars, tree1, pdt2, pdt3, f)
                })
                else -> apply3(vars, pdt1, pdt2, pdt3, f)
            }
        }
        else -> throw EmptyVariableReferences()
    }
}

fun hide(
    refs: MutableList<Ref<EntityType<*, *, *, *, *>>>,
    pdt: Pdt<Proof>,
    fleaf: (Ref<EntityType<*, *, *, *, *>>, Proof) -> Proof,
    fnode: (Ref<EntityType<*, *, *, *, *>>, List<Pair<RefId, Proof>>) -> Proof,
    newVar: Ref<EntityType<*, *, *, *, *>>
): Pdt<Proof> {
    val vars = refs.toMutableList()
    when {
        pdt is Leaf -> return Leaf(fleaf(newVar, pdt.value))
        vars.size == 1 && pdt is Node<Proof> -> return Leaf(fnode(newVar, pdt.partition.mapNotNull { (refId, leaf) ->
            if (leaf is Leaf<Proof>) {
                refId to leaf.value
            } else null
        }))
        vars.isNotEmpty() && pdt is Node<Proof> -> {
            val first = vars.removeFirst()
            return if (first == pdt.variable) {
                Node(first, pdt.partition.map { (refId, tree) ->
                    refId to hide(vars, tree, fleaf, fnode, newVar)
                })
            } else {
                hide(vars, pdt, fleaf, fnode, newVar)
            }
        }
        else -> throw EmptyVariableReferences()
    }
}
