package edu.cmu.isr.lts

import net.automatalib.automata.MutableAutomaton
import net.automatalib.automata.UniversalAutomaton
import net.automatalib.automata.fsa.impl.compact.CompactNFA

interface NonDetLTS<S, I> : UniversalAutomaton<S, I, S, Boolean, Void> {
    val errorState: S

    fun isErrorState(state: S): Boolean
}

interface MutableNonDetLTS<S, I> : NonDetLTS<S, I>, MutableAutomaton<S, I, S, Boolean, Void>

class CompactNonDetLTS<I>(nfa: CompactNFA<I>) : CompactNFA<I>(nfa), MutableNonDetLTS<Int, I> {
    private val _errorState: Int

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

    override val errorState: Int
        get() = _errorState

    override fun isErrorState(state: Int): Boolean {
        return !isAccepting(state)
    }
}

fun <I> CompactNFA<I>.asLTS(): CompactNonDetLTS<I> {
    return CompactNonDetLTS(this)
}