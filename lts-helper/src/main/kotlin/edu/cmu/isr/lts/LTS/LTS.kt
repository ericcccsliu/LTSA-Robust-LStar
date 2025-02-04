package edu.cmu.isr.lts.LTS

import net.automatalib.automata.UniversalAutomaton

//implementation of LTS as specified in robustness paper
interface LTS<S, I, T> : UniversalAutomaton<S, I, T, Boolean, Void> {
    val errorState: S
    fun isErrorState(state: S): Boolean
    fun getInitialState(): S
}
