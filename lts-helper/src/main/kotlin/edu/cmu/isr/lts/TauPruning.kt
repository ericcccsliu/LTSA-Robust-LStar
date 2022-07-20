package edu.cmu.isr.lts

import edu.cmu.isr.lts.LTS.CompactDetLTS
import edu.cmu.isr.lts.LTS.CompactNonDetLTS
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.automata.fsa.impl.compact.CompactNFA
import net.automatalib.util.automata.predicates.TransitionPredicates
import net.automatalib.util.ts.copy.TSCopy
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalMethod
import net.automatalib.words.Alphabet
import java.util.ArrayDeque
import java.util.HashMap
import java.util.HashSet

class TauPruning (val composition: CompactNonDetLTS<String>, private val tauAlphabet: Alphabet<String>) {
    private val backtrackingMap = HashMap<Int, HashMap<String, Int>>()

    init {
        for(state in composition.states) {
            for(input in composition.inputAlphabet) {
                for(endState in composition.getSuccessors(state, input)) {
                    if(backtrackingMap.contains(endState)){
                        backtrackingMap[endState]?.put(input, state)
                    } else {
                        backtrackingMap[endState] = HashMap()
                        backtrackingMap[endState]?.put(input, state)
                    }
                }
            }
        }
    }

    fun getResult(): CompactNonDetLTS<String> {
        //bfs tau error state backtracking
        val errorStates = HashSet<Int>()
        val nextStates = ArrayDeque<Int>()
        //keep track of the initial error state so we have something for all the backtracked error states to condense into
        val initialErrorState = composition.errorState
        nextStates.add(composition.errorState)
        while(nextStates.isNotEmpty()) {
            val currentState = nextStates.first()
            nextStates.removeFirst()
            //skip iteration if this state has already been seen
            if(errorStates.contains(currentState)) {
                continue
            }
            val incomingTransitions = backtrackingMap[currentState]
            if(incomingTransitions != null) {
                val tauTransitions = incomingTransitions.filterKeys {
                    tauAlphabet.contains(it)
                }
                tauTransitions.forEach {
                    nextStates.addLast(it.value)
                }
            }
            errorStates.add(currentState)
        }

        //condense error states by constructing CompactNonDetLTS
        val prunedNFA = CompactNFA(composition.inputAlphabet)
        TSCopy.copy(TSTraversalMethod.BREADTH_FIRST, composition, TSTraversal.NO_LIMIT, composition.inputAlphabet, prunedNFA, {!errorStates.contains(it) || it == initialErrorState}, TransitionPredicates.alwaysTrue())
        for(state in errorStates) {
            val incomingTransitions = backtrackingMap[state]
            incomingTransitions?.forEach {
                prunedNFA.addTransition(it.value, it.key, initialErrorState)
            }
        }
        return CompactNonDetLTS(prunedNFA)
    }
}