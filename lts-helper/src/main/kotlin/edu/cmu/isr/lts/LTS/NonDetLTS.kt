package edu.cmu.isr.lts.LTS

import net.automatalib.automata.fsa.impl.compact.CompactNFA

//implementation of nondeterministic LTS
interface NonDetLTS<S, I> : LTS<S, I, S>

class CompactNonDetLTS<I>(nfa: CompactNFA<I>) : CompactNFA<I>(nfa), NonDetLTS<Int, I> {
    private val _errorState: Int
    private val initialState: Int

    init {
        // Check that there should be at most one error state, that is marked as unacceptable.
        val unacceptable = states.filter { !isAccepting(it) }
        if (unacceptable.size > 1)
            throw Error("There should be one error state in LTS which might be unreachable.")
        _errorState = if (unacceptable.isEmpty())
            addState(false)
        else
            unacceptable[0]
    }

    init {
        if(initialStates.size > 1 || initialStates.size == 0)
            throw Error("There should be one and only one initial state in LTS")
        initialState = initialStates.elementAt(0)
    }

    override val errorState: Int
        get() = _errorState

    override fun isErrorState(state: Int): Boolean {
        return !isAccepting(state)
    }

    override fun getInitialState(): Int {
        return initialState
    }

}

//fun <I> CompactNFA<I>.asLTS(): CompactNonDetLTS<I> {
//    return CompactNonDetLTS(this)
//}