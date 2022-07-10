package edu.cmu.isr.lts

import de.learnlib.api.oracle.MembershipOracle
import de.learnlib.api.query.Query
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.automata.simple.SimpleDeterministicAutomaton
import net.automatalib.words.Word
import net.automatalib.words.impl.Alphabets

class WeakestMembershipOracle<I>(private val learningTarget: CompactNonDetLTS<I>, private val system: CompactNonDetLTS<I>) : MembershipOracle.DFAMembershipOracle<I> {
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
        if(query != null) {
            val input: Word<I> = query.input
            val inputAutomaton = CompactDFA<I>(Alphabets.fromList(input.asList()))

            for ((index, symbol) in input.withIndex()) {
                inputAutomaton.addTransition(index, symbol, index + 1)
            }

            val result: SafetyResult<I> = checkSafety(CompactDetLTS(inputAutomaton), Alphabets.fromList(input.asList()), learningTarget, learningTarget.inputAlphabet)
            if(result.violation) {
                query.answer(false)
            } else {
                query.answer(true)
            }
        }
    }

}

