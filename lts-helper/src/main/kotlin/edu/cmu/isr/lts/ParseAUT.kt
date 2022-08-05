package edu.cmu.isr.lts

import java.io.InputStream

import net.automatalib.automata.simple.SimpleAutomaton
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.automata.fsa.impl.compact.CompactNFA
import net.automatalib.serialization.aut.AUTParser
import net.automatalib.util.automata.fsa.NFAs

//AUT to automaton parsing class

//when getDFA is called with isProperty == true, we assume that there is only one sink state in the automaton,
//and that state is set as error
class AUTtoAutomaton<I>(fin: String) {
    private val autIs : InputStream = AUTtoAutomaton::class.java.getResourceAsStream(fin) as InputStream
    private val simpleAutomaton: SimpleAutomaton<Int, String> = AUTParser.readAutomaton(autIs).model
    private val nfa: CompactNFA<I> = simpleAutomaton as CompactNFA<I> //this works

    init {
        for (state in nfa.states) {
            //set all states as accepting—only initial state is accepting as default
            nfa.setAccepting(state, true)
        }
    }
    fun getNFA(): CompactNFA<I> {
        return nfa
    }
    fun getDFA(isProperty: Boolean = false): CompactDFA<I> {
        //partial, minimize parameters determined by trial & error :/
        val dfa = NFAs.determinize(nfa, nfa.inputAlphabet, true, false)
        //Assumes error state is the only sink state
        if(isProperty) {
            for(state in dfa){
                if(dfa.getTransitions(state).isEmpty()) {
                    dfa.setAccepting(state, false)
                }
            }
        }
        return dfa
    }
}

//testing
fun main() {
    val dfa : CompactDFA<String> = AUTtoAutomaton<String>("/testfiles/ABP_PROPERTY.aut").getDFA()
    for(state in dfa.states) {
        println(state)
    }
}