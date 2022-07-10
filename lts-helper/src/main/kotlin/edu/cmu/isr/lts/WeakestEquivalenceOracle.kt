package edu.cmu.isr.lts

import de.learnlib.api.oracle.EquivalenceOracle
import de.learnlib.api.query.DefaultQuery
import net.automatalib.automata.fsa.DFA
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.words.Word

//@TODO: refactor to determine if in error state
class WeakestEquivalenceOracle<I> (machineLTS: CompactNonDetLTS<I>, propertyLTS: CompactDetLTS<I>) : EquivalenceOracle.DFAEquivalenceOracle<I> {
    val composition = parallelComposition(machineLTS, machineLTS.inputAlphabet, propertyLTS, propertyLTS.inputAlphabet)
    override fun findCounterExample(p0: DFA<*, I>?, p1: MutableCollection<out I>?): DefaultQuery<I, Boolean>? {
        val assumptionLTS = CompactDetLTS(p0 as CompactDFA<I>)
        val result = checkSafety(assumptionLTS, assumptionLTS.inputAlphabet, composition, composition.inputAlphabet)
        if(result.violation) {
            return DefaultQuery(Word.fromList(result.trace))
        }
        return null
    }

}