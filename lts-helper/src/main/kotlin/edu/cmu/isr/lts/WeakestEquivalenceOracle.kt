package edu.cmu.isr.lts

import de.learnlib.api.oracle.EquivalenceOracle
import de.learnlib.api.query.DefaultQuery
import net.automatalib.automata.fsa.DFA
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.words.Alphabet
import net.automatalib.words.Word

//@TODO: refactor to determine if in error state
class WeakestEquivalenceOracle<I> (val composition: CompactNonDetLTS<I>, val learningAlphabet: Alphabet<I>, val tauAlphabet: Alphabet<I>) : EquivalenceOracle.DFAEquivalenceOracle<I> {
    override fun findCounterExample(p0: DFA<*, I>?, p1: MutableCollection<out I>?): DefaultQuery<I, Boolean>? {
        val assumptionLTS = CompactDetLTS(p0 as CompactDFA<I>)
        //@TODO: check effects of using this alphabet here—can't figure out what to use
        //issue might be that error pruning isn't working
            val result = checkSafety(composition, composition.inputAlphabet, assumptionLTS, learningAlphabet)
        if(result.violation) {
            val violationTrace = Word.fromList(result.trace)
            val prunedViolationList = ArrayList<I>()
            for(symbol in violationTrace) {
                if(symbol in learningAlphabet) {
                    prunedViolationList.add(symbol)
                }
            }
            if(prunedViolationList.isEmpty()) {
                return null
            } else {
                return DefaultQuery(Word.fromList(prunedViolationList))
            }
        }
        return null
    }

}