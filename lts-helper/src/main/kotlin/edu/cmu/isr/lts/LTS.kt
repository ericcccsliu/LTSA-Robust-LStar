package edu.cmu.isr.lts

import net.automatalib.automata.UniversalAutomaton

interface LTS<S, I, T> : UniversalAutomaton<S, I, T, Boolean, Void> {
    val errorState: S
    fun isErrorState(state: S): Boolean
    fun getInitialState(): S
}
