package edu.cmu.isr.lts

import java.io.InputStream

import net.automatalib.automata.simple.SimpleAutomaton
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.automata.fsa.impl.compact.CompactNFA
import net.automatalib.serialization.aut.AUTParser
import net.automatalib.util.automata.fsa.NFAs


class AUTtoDFA<I>(fin: String) {
    private val autIs : InputStream = AUTtoDFA::class.java.getResourceAsStream(fin) as InputStream
    private val simpleAutomaton: SimpleAutomaton<Int, String> = AUTParser.readAutomaton(autIs).model
    private val nfa: CompactNFA<I> = simpleAutomaton as CompactNFA<I>
    //potential issue: AUT file may not be deterministic
    fun getDFA() : CompactDFA<I> {
        return NFAs.determinize(nfa);
    }
}

fun main(args: Array<String>) {
    val dfa : CompactDFA<Int> = AUTtoDFA<Int>("/testfiles/CLIENT_SERVER.aut").getDFA()
    println(dfa.initialState)
}