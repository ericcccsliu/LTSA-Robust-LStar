package edu.cmu.isr.lts

import net.automatalib.automata.UniversalAutomaton
import net.automatalib.ts.UniversalDTS

interface LTS<S, I, T> : UniversalAutomaton<S, I, T, Boolean, Void> {
    val errorState: S
    fun isErrorState(state: S): Boolean


    //additional

    fun getInitialState(): S?
}

//interface LTS<S, I, T> {
//    val errorState: S
//    val initialState: S?
//
//    fun isErrorState(state: S): Boolean
//    fun getTransition(s1: S, input: I): T?
//    fun getSuccessor(trans: T): S
//}