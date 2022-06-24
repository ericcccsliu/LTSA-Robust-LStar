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
    val dfa : CompactDFA<String> = AUTtoDFA<String>("/testfiles/CLIENT_SERVER.aut").getDFA()
    val dfaAnswer = AutomatonBuilders.newDFA(Alphabets.fromArray("call", "service", "reply", "continue"))
        .withInitial(0)
        .from(0).on("call").to(1)
        .from(1)
        .on("service").to(2)
        .from(2).on("reply").to(3)
        .from(3).on("continue").to(0)
        .withAccepting(0,1, 2,3)
        .create()
    println(Automata.testEquivalence(dfa, dfaAnswer, dfaAnswer.inputAlphabet))
}