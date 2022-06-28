package edu.cmu.isr.lts

import de.learnlib.api.oracle.MembershipOracle
import de.learnlib.api.query.Query
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.words.Word

class WeakestMembershipOracle<I>(machine: CompactDFA<I>, property: CompactDFA<I>) : MembershipOracle.DFAMembershipOracle<I> {
    private val machineLTS: CompactDetLTS<I> = machine.asLTS()
    private val propertyLTS: CompactDetLTS<I> = property.asLTS()
    val composition = parallelComposition(machineLTS, machineLTS.inputAlphabet, propertyLTS, propertyLTS.inputAlphabet)

    override fun processQueries(p0: MutableCollection<out Query<I, Boolean>>?) {
        if (p0 != null) {
            for(query: Query<I, Boolean> in p0) {
                processQuery(query)
            }
        }
    }
    override fun processQuery(query: Query<I, Boolean>?) {
        // @TODO
        // run string in query over composition, resolve false if ends in error state
        val input: Word<I>? = query?.input

        //null check
        if(query != null) {
            if (composition.accepts(input)) {
                query.answer(true)
            } else {
                query.answer(false)
            }
        }
    }

}

