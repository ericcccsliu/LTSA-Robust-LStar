package edu.cmu.isr.lts

import edu.cmu.isr.lts.LTS.CompactDetLTS
import edu.cmu.isr.lts.LTS.CompactNonDetLTS
import edu.cmu.isr.lts.LTS.LTS
import net.automatalib.automata.fsa.impl.compact.CompactNFA
import net.automatalib.serialization.aut.AUTWriter
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.util.ts.copy.TSCopy
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalMethod
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets
import java.util.*
import kotlin.collections.ArrayList


class TransOrLoop<S, T>(val loop: S, val trans: T?)


//changed to take in two LTS (may be non-deterministic)
class LTSParallelComposition<S1, S2, I, T1, T2, A1, A2>(
  private val ts1: A1,
  private val inputs1: Alphabet<I>,
  private val ts2: A2,
  private val inputs2: Alphabet<I>,
) : LTS<Pair<S1, S2>, I, Pair<TransOrLoop<S1, T1>, TransOrLoop<S2, T2>>>
    where A1 : LTS<S1, I, T1>,
          A2 : LTS<S2, I, T2>
{
  override val errorState: Pair<S1, S2>
    get() = Pair(ts1.errorState, ts2.errorState)

  override fun isErrorState(state: Pair<S1, S2>): Boolean {
    return ts1.isErrorState(state.first) || ts2.isErrorState(state.second)
  }

  override fun getSuccessor(transition: Pair<TransOrLoop<S1, T1>, TransOrLoop<S2, T2>>): Pair<S1, S2> {
    val t1 = transition.first
    val t2 = transition.second
    val succ = Pair(
      if (t1.trans == null) t1.loop else ts1.getSuccessor(t1.trans),
      if (t2.trans == null) t2.loop else ts2.getSuccessor(t2.trans)
    )
    // Merge all error states into one
    if (isErrorState(succ))
      return errorState
    return succ
  }

  override fun getStateProperty(state: Pair<S1, S2>): Boolean {
    return !isErrorState(state)
  }

  override fun getTransitionProperty(transition: Pair<TransOrLoop<S1, T1>, TransOrLoop<S2, T2>>): Void? {
    return null
  }

  override fun getInitialState(): Pair<S1, S2> {
    return Pair(ts1.getInitialState()!!, ts2.getInitialState()!!)
  }

  override fun getStates(): MutableCollection<Pair<S1, S2>> {
    //cartesian product of states of ts1, ts2
    val states = ArrayList<Pair<S1, S2>>()
    ts1.states.forEach{s1 -> ts2.states.forEach{s2 -> states.add(Pair(s1, s2))}}
    return states
  }

  override fun getInitialStates(): MutableSet<Pair<S1, S2>> {
    val initialStateSet = HashSet<Pair<S1,S2>>()
    initialStateSet.add(getInitialState())
    return initialStateSet
  }

  override fun getTransitions(state: Pair<S1, S2>, input: I): Collection<Pair<TransOrLoop<S1, T1>, TransOrLoop<S2, T2>>> {
      val transitions = ArrayList<Pair<TransOrLoop<S1, T1>, TransOrLoop<S2, T2>>>()
      if(isErrorState(state))
        return Collections.emptySet()

      val s1 = state.first
      val s2 = state.second

      val t1s: Collection<T1>? = if(inputs1.containsSymbol(input)){
        try {
          ts1.getTransitions(s1, input)
        } catch(e: java.lang.IllegalArgumentException) {
          Collections.emptySet()
        }
      } else {
         null
      }
      val t2s: Collection<T2>? =if(inputs2.containsSymbol(input)){
        try {
          ts2.getTransitions(s2, input)
        } catch(e: java.lang.IllegalArgumentException) {
          Collections.emptySet()
        }
      } else {
        null
      }
      if (!t1s.isNullOrEmpty() && !t2s.isNullOrEmpty()) {
        t1s.forEach { t1 -> t2s.forEach { t2 -> transitions.add(Pair(TransOrLoop(s1, t1), TransOrLoop(s2, t2))) } }
        return transitions
      } else if (!t1s.isNullOrEmpty() && t2s == null) {
        t1s.forEach { t1 -> transitions.add(Pair(TransOrLoop(s1, t1), TransOrLoop(s2, null))) }
        return transitions
      } else if (!t2s.isNullOrEmpty() && t1s == null) {
        t2s.forEach { t2 -> transitions.add(Pair(TransOrLoop(s1, null), TransOrLoop(s2, t2))) }
        return transitions
      }


      return Collections.emptySet()
  }
}


fun <I> parallelComposition(lts1: LTS<*, I, *>, inputs1: Alphabet<I>,
                            lts2: LTS<*, I, *>, inputs2: Alphabet<I>): CompactNonDetLTS<I> {
  val inputs = Alphabets.fromCollection(inputs1.union(inputs2))
  val out = CompactNFA(inputs)
  val composition = LTSParallelComposition(lts1, inputs1, lts2, inputs2)

  TSCopy.copy(TSTraversalMethod.BREADTH_FIRST, composition, TSTraversal.NO_LIMIT, inputs, out)
  return CompactNonDetLTS(out)
}


fun main() {
  val a = CompactDetLTS(AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b', 'c'))
    .withInitial(0)
    .from(0).on('a').to(1)
    .from(1)
      .on('b').to(0)
      .on('c').to(2)
    .from(2).on('b').to(0)
    .withAccepting(0, 1, 2)
    .create())


  val b = CompactDetLTS(AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b', 'c'))
    .withInitial(0)
    .from(0).on('a').to(1)
    .from(1)
      .on('b').to(-1)
      .on('c').to(-1)
    .withAccepting(0, 1)
    .create())

  val c = parallelComposition(a, a.inputAlphabet, b, b.inputAlphabet)

  AUTWriter.writeAutomaton(c, c.inputAlphabet, System.out)
  println(c.isAccepting(0))
  println(c.isAccepting(1))
  println(c.isAccepting(2))
  println(c.errorState)

}