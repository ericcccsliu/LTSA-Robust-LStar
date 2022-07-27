package edu.cmu.isr.lts

import edu.cmu.isr.lts.LTS.CompactDetLTS
import edu.cmu.isr.lts.LTS.DetLTS
import edu.cmu.isr.lts.LTS.LTS
import edu.cmu.isr.lts.LTS.MutableDetLTS
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.commons.util.Holder
import net.automatalib.serialization.aut.AUTWriter
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalAction
import net.automatalib.util.ts.traversal.TSTraversalVisitor
import net.automatalib.words.Alphabet
import net.automatalib.words.Word
import net.automatalib.words.impl.Alphabets


class SafetyResult<I> {
  var violation: Boolean = false
  var trace: List<I>? = null

  override fun toString(): String {
    return if (violation) "Safety violation: $trace" else "No safety violation"
  }
}

class SafetyVisitor<S, I, T>(private val lts: LTS<S, I, T>,
                             private val result: SafetyResult<I>, private val tauAlphabet: Alphabet<I>? = null, private val observations: HashMap<Word<I>, Boolean>? = null) : TSTraversalVisitor<S, I, T, List<I>> {
  private val visited = mutableSetOf<S>()

  override fun processInitial(state: S, outData: Holder<List<I>>?): TSTraversalAction {
    outData!!.value = emptyList()
    return TSTraversalAction.EXPLORE
  }

  override fun startExploration(state: S, data: List<I>?): Boolean {
    return if (state !in visited) {
      visited.add(state)
      true
    } else {
      false
    }
  }

  override fun processTransition(
    source: S,
    srcData: List<I>?,
    input: I,
    transition: T,
    succ: S,
    outData: Holder<List<I>>?
  ): TSTraversalAction {
    outData!!.value = srcData!! + listOf(input)
    if(observations?.get(Word.fromList(outData.value.filter{tauAlphabet != null && !tauAlphabet.contains(it) })) == false) {
//      println("ignored queries: " + outData.value.filter{tauAlphabet != null && !tauAlphabet.contains(it)})
      return TSTraversalAction.IGNORE
    }
    if (lts.isErrorState(succ)) {
      result.violation = true
      result.trace = outData.value
      return TSTraversalAction.ABORT_TRAVERSAL
    }
    return TSTraversalAction.EXPLORE
  }

}

fun <I> checkSafety(lts: LTS<*, I, *>, inputs1: Alphabet<I>,
                    prop: DetLTS<*, I, *>, inputs2: Alphabet<I>, tauAlphabet: Alphabet<I>? = null, observations: HashMap<Word<I>, Boolean>? = null): SafetyResult<I>
{
  val composition = parallelComposition(lts, inputs1, prop, inputs2)
  val result = SafetyResult<I>()
  val vis = if(observations != null && tauAlphabet != null) { SafetyVisitor(composition, result, tauAlphabet, observations)} else {  SafetyVisitor(composition, result) }
  TSTraversal.breadthFirst(composition, composition.inputAlphabet, vis)
  return result
}

fun <S, I> makeErrorState(prop: MutableDetLTS<S, I, *>, inputs: Alphabet<I>): MutableDetLTS<S, I, *> {
  for (s in prop.states) {
    if (prop.isErrorState(s))
      continue
    for (a in inputs) {
      if (prop.getTransition(s, a) == null) {
        prop.addTransition(s, a, prop.errorState, null)
      }
    }
  }
  return prop
}

fun main() {
  val p = CompactDetLTS(AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b'))
    .withInitial(0)
    .from(0).on('a').to(1)
    .from(1).on('b').to(0)
    .withAccepting(0, 1)
    .create())


  val a = CompactDetLTS(AutomatonBuilders.newDFA(Alphabets.characters('a', 'c'))
    .withInitial(0)
    .from(0).on('a').to(1)
    .from(1)
      .on('b').to(2)
      .on('a').to(1)
    .from(2).on('c').to(0)
    .withAccepting(0, 1, 2)
    .create())

  AUTWriter.writeAutomaton(p, p.inputAlphabet, System.out)

  val pErr = makeErrorState(p, p.inputAlphabet)
  AUTWriter.writeAutomaton(pErr, p.inputAlphabet, System.out)

  println(checkSafety(a, a.inputAlphabet, pErr, p.inputAlphabet))
}