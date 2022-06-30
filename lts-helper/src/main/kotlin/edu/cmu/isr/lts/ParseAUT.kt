package edu.cmu.isr.lts

import java.io.InputStream

import net.automatalib.automata.simple.SimpleAutomaton
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.automata.fsa.impl.compact.CompactNFA
import net.automatalib.serialization.aut.AUTParser
import net.automatalib.util.automata.Automata
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.util.automata.fsa.NFAs
import net.automatalib.words.impl.Alphabets

//NOTE: make sink/error state the last state
class AUTtoDFA<I>(fin: String) {
    private val autIs : InputStream = AUTtoDFA::class.java.getResourceAsStream(fin) as InputStream
    private val simpleAutomaton: SimpleAutomaton<Int, String> = AUTParser.readAutomaton(autIs).model
    private val nfa: CompactNFA<I> = simpleAutomaton as CompactNFA<I>
    fun getDFA(): CompactDFA<I> {
        for (state in nfa.states) {
            //set all states as accepting—only initial state is accepting as default
            nfa.setAccepting(state, true)
        }
        return NFAs.determinize(nfa, nfa.inputAlphabet, true, false);
        //partial, minimize parameters determined by trial & error :/
    }
}

//testing
fun main(args: Array<String>) {
    val dfa : CompactDFA<String> = AUTtoDFA<String>("/testfiles/ABP_PROPERTY.aut").getDFA()
    for(state in dfa.states) {
        println(state)
    }
}