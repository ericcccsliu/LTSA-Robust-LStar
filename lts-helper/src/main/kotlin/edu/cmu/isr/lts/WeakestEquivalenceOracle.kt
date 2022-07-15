package edu.cmu.isr.lts

import de.learnlib.api.oracle.EquivalenceOracle
import de.learnlib.api.query.DefaultQuery
import net.automatalib.automata.fsa.DFA
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.util.automata.predicates.TransitionPredicates
import net.automatalib.util.ts.copy.TSCopy
import net.automatalib.util.ts.copy.TSCopyVisitor
import net.automatalib.util.ts.traversal.TSTraversalMethod
import net.automatalib.words.Alphabet
import net.automatalib.words.Word
import java.util.function.Predicate

//@TODO: refactor to determine if in error state
class WeakestEquivalenceOracle<I> (val composition: CompactNonDetLTS<I>, private val learningAlphabet: Alphabet<I>, val tauAlphabet: Alphabet<I>) : EquivalenceOracle.DFAEquivalenceOracle<I> {
    override fun findCounterExample(hypothesis: DFA<*, I>?, p1: MutableCollection<out I>?): DefaultQuery<I, Boolean>? {
        val hypothesisCompactDFA = hypothesis as CompactDFA<I>
        val hypothesisAcceptingOnly = CompactDFA<I>(hypothesisCompactDFA.inputAlphabet)
        TSCopy.copy(TSTraversalMethod.BREADTH_FIRST, hypothesisCompactDFA, -1, hypothesisCompactDFA.inputAlphabet, hypothesisAcceptingOnly,
            { hypothesisCompactDFA.isAccepting(it) }, TransitionPredicates.alwaysTrue())
        val hypothesisLTS = CompactDetLTS(hypothesisAcceptingOnly)

        DrawCompactLTS(hypothesisLTS, hypothesisLTS.inputAlphabet, "coffeeAssumptionLTS")

        //@TODO: check effects of using this alphabet here—can't figure out what to use
        //issue might be that error pruning isn't working
        val result = checkSafety(composition, composition.inputAlphabet, hypothesisLTS, learningAlphabet)
        if(result.violation) {
            val violationTrace = Word.fromList(result.trace)
            println("violation $violationTrace")
            val prunedViolationList = ArrayList<I>()
            for(symbol in violationTrace) {
                if(symbol in learningAlphabet) {
                    prunedViolationList.add(symbol)
                }
            }
            return if(prunedViolationList.isEmpty()) {
                null
            } else {
                DefaultQuery(Word.fromList(prunedViolationList))
            }
        }
        return null
    }

}
