package edu.cmu.isr.lts.oracles

import de.learnlib.api.oracle.EquivalenceOracle
import de.learnlib.api.query.DefaultQuery
import edu.cmu.isr.lts.DrawAutomaton
import edu.cmu.isr.lts.LTS.CompactDetLTS
import edu.cmu.isr.lts.LTS.CompactNonDetLTS
import edu.cmu.isr.lts.checkSafety
import net.automatalib.automata.fsa.DFA
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.util.automata.predicates.TransitionPredicates
import net.automatalib.util.ts.copy.TSCopy
import net.automatalib.util.ts.traversal.TSTraversalMethod
import net.automatalib.words.Alphabet
import net.automatalib.words.Word

//@TODO: refactor to determine if in error state
class WeakestEquivalenceOracle<I> (val composition: CompactNonDetLTS<I>, private val learningAlphabet: Alphabet<I>) : EquivalenceOracle.DFAEquivalenceOracle<I> {
    override fun findCounterExample(hypothesis: DFA<*, I>?, p1: MutableCollection<out I>?): DefaultQuery<I, Boolean>? {
        val hypothesisCompactDFA = hypothesis as CompactDFA<I>
        val hypothesisAcceptingOnly = CompactDFA<I>(hypothesisCompactDFA.inputAlphabet)
        TSCopy.copy(TSTraversalMethod.BREADTH_FIRST, hypothesisCompactDFA, -1, hypothesisCompactDFA.inputAlphabet, hypothesisAcceptingOnly,
            { hypothesisCompactDFA.isAccepting(it) }, TransitionPredicates.alwaysTrue())
        val hypothesisLTS = CompactDetLTS(hypothesisAcceptingOnly)

//        DrawCompactLTS(hypothesisLTS, hypothesisLTS.inputAlphabet, "coffeeAssumptionLTS")

        //@TODO: check effects of using this alphabet here—can't figure out what to use
        //issue might be that error pruning isn't working
        val result1 = checkSafety(composition, composition.inputAlphabet, hypothesisLTS, learningAlphabet)
//        DrawAutomaton(composition, composition.inputAlphabet, "finalComposition")
        //checking symmetric difference
//        val result2 = checkSafety(hypothesisLTS, learningAlphabet, composition, composition.inputAlphabet)

        if(result1.violation) {
            val violationTrace = Word.fromList(result1.trace)
            println("violation: $violationTrace")
            val prunedViolationList = ArrayList<I>()
            for(symbol in violationTrace) {
                if(symbol in learningAlphabet) {
                    prunedViolationList.add(symbol)
                }
            }
            val prunedViolationTrace = Word.fromList(prunedViolationList)
//            println("pruned violation: $prunedViolationTrace")
            if(!prunedViolationTrace.isEmpty) {
                return DefaultQuery(prunedViolationTrace)
            }
        }

//        if(result2.violation) {
//            val violationTrace = Word.fromList(result2.trace)
//            println("violation2: $violationTrace")
//            val prunedViolationList = ArrayList<I>()
//            for(symbol in violationTrace) {
//                if(symbol in learningAlphabet) {
//                    prunedViolationList.add(symbol)
//                }
//            }
//            val prunedViolationTrace = Word.fromList(prunedViolationList)
////            println("pruned violation2: $prunedViolationTrace")
//            if(!prunedViolationTrace.isEmpty) {
//                return DefaultQuery(prunedViolationTrace)
//            }
//        }
        return null
    }

}
