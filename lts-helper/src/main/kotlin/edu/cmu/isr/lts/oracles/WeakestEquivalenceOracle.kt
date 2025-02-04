package edu.cmu.isr.lts.oracles

import de.learnlib.api.oracle.EquivalenceOracle
import de.learnlib.api.query.DefaultQuery
import edu.cmu.isr.lts.LTS.CompactDetLTS
import edu.cmu.isr.lts.LTS.CompactNonDetLTS
import edu.cmu.isr.lts.checkSafety
import edu.cmu.isr.lts.makeErrorState
import net.automatalib.automata.fsa.DFA
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.automata.fsa.impl.compact.CompactNFA
import net.automatalib.util.automata.predicates.TransitionPredicates
import net.automatalib.util.ts.copy.TSCopy
import net.automatalib.util.ts.traversal.TSTraversalMethod
import net.automatalib.words.Alphabet
import net.automatalib.words.Word

//@TODO: refactor to determine if in error state
class WeakestEquivalenceOracle<I> (val composition: CompactNonDetLTS<I>, private val learningAlphabet: Alphabet<I>, private val tauAlphabet: Alphabet<I>, val observations: HashMap<Word<I>, Boolean>) : EquivalenceOracle.DFAEquivalenceOracle<I> {

    private val compositionWithoutErrorLTS: CompactNonDetLTS<I>
    init {
        val compositionWithoutError = CompactNFA(composition.inputAlphabet)
        TSCopy.copy(TSTraversalMethod.BREADTH_FIRST,
            composition,
            -1,
            composition.inputAlphabet,
            compositionWithoutError,
            { it != composition.errorState },
            TransitionPredicates.alwaysTrue()
        )
        compositionWithoutErrorLTS = CompactNonDetLTS(compositionWithoutError)
    }

    override fun findCounterExample(hypothesis: DFA<*, I>?, p1: MutableCollection<out I>?): DefaultQuery<I, Boolean>? {
        val hypothesisCompactDFA = hypothesis as CompactDFA<I>
        val hypothesisAcceptingOnly = CompactDFA<I>(hypothesisCompactDFA.inputAlphabet)
        TSCopy.copy(TSTraversalMethod.BREADTH_FIRST, hypothesisCompactDFA, -1, hypothesisCompactDFA.inputAlphabet, hypothesisAcceptingOnly,
            { hypothesisCompactDFA.isAccepting(it) }, TransitionPredicates.alwaysTrue())
        val hypothesisLTS = CompactDetLTS(hypothesisAcceptingOnly)
        val result1 = checkSafety(composition, composition.inputAlphabet, hypothesisLTS, learningAlphabet)
        val hypothesisErrorLTS = makeErrorState(hypothesisLTS, hypothesisLTS.inputAlphabet) as CompactDetLTS<I>

//        checking symmetric difference
        val result2 = checkSafety(compositionWithoutErrorLTS, compositionWithoutErrorLTS.inputAlphabet, hypothesisErrorLTS, learningAlphabet, tauAlphabet, observations)
        var violationTrace1: Word<I>? = null
        var violationTrace2: Word<I>? = null

        if(result1.violation) {
            val violationTrace = Word.fromList(result1.trace)
            val prunedViolationList = ArrayList<I>()
            for(symbol in violationTrace) {
                if(symbol in learningAlphabet) {
                    prunedViolationList.add(symbol)
                }
            }
            val prunedViolationTrace = Word.fromList(prunedViolationList)
            if(!prunedViolationTrace.isEmpty) {
                violationTrace1 = prunedViolationTrace
            }
        }

        if(result2.violation) {
            val violationTrace = Word.fromList(result2.trace)
            val prunedViolationList = ArrayList<I>()
            for(symbol in violationTrace) {
                if(symbol in learningAlphabet) {
                    prunedViolationList.add(symbol)
                }
            }
            val prunedViolationTrace = Word.fromList(prunedViolationList)
            if(!prunedViolationTrace.isEmpty) {
                violationTrace2 = prunedViolationTrace
            }
        }
        if(violationTrace1 == null && violationTrace2 != null) {
            return DefaultQuery(violationTrace2)
        } else if (violationTrace1 != null && violationTrace2 == null) {
            return DefaultQuery(violationTrace1)
        } else if (violationTrace1 != null && violationTrace2 != null) {
            return if(violationTrace1.length() <= violationTrace2.length()) {
                DefaultQuery(violationTrace1)
            } else {
                DefaultQuery(violationTrace2)
            }
        }
        return null
    }

}


/*
accounting for already-queried traces in building hypothesis with error:
    record nonaccepting (error) states in hypothesis
    convert hypothesis to lts with all states accepting
    makeerrorstate on hypothesislts
    delete initial nonaccepting states
 */

/*
if a trace ends in error, trace with tau alphabet interspersed should not be in weakest assumption
 */