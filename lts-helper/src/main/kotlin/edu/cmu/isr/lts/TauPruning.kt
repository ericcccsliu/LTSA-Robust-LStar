package edu.cmu.isr.lts

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashMultimap
import edu.cmu.isr.lts.LTS.CompactDetLTS
import edu.cmu.isr.lts.LTS.CompactNonDetLTS
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.automata.fsa.impl.compact.CompactNFA
import net.automatalib.util.automata.predicates.TransitionPredicates
import net.automatalib.util.ts.copy.TSCopy
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalMethod
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets
import java.util.ArrayDeque
import java.util.HashMap
import java.util.HashSet

class TauPruning (val composition: CompactNonDetLTS<String>, private val tauAlphabet: Alphabet<String>) {
    private val backtrackingMap = HashMap<Int, HashMultimap<String, Int>>()

    init {
        for(state in composition.states) {
            for(input in composition.inputAlphabet) {
                for(endState in composition.getSuccessors(state, input)) {
                    if(backtrackingMap.contains(endState)){
                        backtrackingMap[endState]?.put(input, state)
                    } else {
                        backtrackingMap[endState] = HashMultimap.create()
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
//        println("initial backtracking: " + (backtrackingMap[initialErrorState]?.filterKeys{tauAlphabet.contains(it)}))
        nextStates.add(composition.errorState)
        while(nextStates.isNotEmpty()) { //@TODO: this is broken????????
            val currentState = nextStates.pollFirst()
            //skip iteration if this state has already been seen
            if(errorStates.contains(currentState)) {
                continue
            }
            val incomingTransitions = backtrackingMap[currentState]
            if(incomingTransitions != null) {
//                val tauTransitions = incomingTransitions.filterKeys {
//                    tauAlphabet.contains(it)
//                }
//                tauTransitions.forEach {
//                    nextStates.addLast(it.value)
//                }
                for(transition in tauAlphabet) {
                    if(incomingTransitions.containsKey(transition)) {
                        nextStates.addAll(incomingTransitions[transition])
                    }
                }
            }
            errorStates.add(currentState)
        }
        println("backtracked error states: $errorStates")

        //condense error states by constructing CompactNonDetLTS
        val prunedNFA = CompactNFA(composition.inputAlphabet)
        println("errorStateSet: " + composition.states.filter{!composition.isAccepting(it)})
        val mapping = TSCopy.copy(TSTraversalMethod.BREADTH_FIRST, composition, TSTraversal.NO_LIMIT, composition.inputAlphabet, prunedNFA, {!errorStates.contains(it) || it == initialErrorState}, TransitionPredicates.alwaysTrue())

        DrawAutomaton(composition, composition.inputAlphabet, "composition" )
        println("composition error state: " + composition.errorState)
        DrawAutomaton(prunedNFA, prunedNFA.inputAlphabet, "prunedNFA")

        val prunedNFALTS = CompactNonDetLTS(prunedNFA)

        for(state in errorStates) {
            val incomingTransitions = backtrackingMap[state]?.asMap()?.filter { !tauAlphabet.contains(it.key) }
            incomingTransitions?.forEach {entry ->
                println("transition: " + entry.value + ", " + entry.key + ", " + state)
                entry.value.forEach{originState -> if(!errorStates.contains(originState) && mapping.get(originState) != null) prunedNFALTS.addTransition(mapping.get(originState), entry.key, prunedNFALTS.errorState)}
            }
        }
//        println("prunedNFA states: " + prunedNFA.states)
//        println("prunedNFA initial states: " + prunedNFA.initialStates)
//        println("prunedNFA accepting states: " + prunedNFA.states.filter{prunedNFA.isAccepting(it)})
//        DrawAutomaton(prunedNFALTS, prunedNFALTS.inputAlphabet, "prunedNFALTS")
        return prunedNFALTS
    }
}

fun main() {
    val testNFA = AUTtoDFA<String>("/testfiles/coffee_pruning_test.aut").getNFA()
    testNFA.setAccepting(4, false)
    val testNFALTS = CompactNonDetLTS(testNFA)
    DrawAutomaton(testNFALTS, testNFALTS.inputAlphabet, "coffee_property")
    println(testNFALTS.errorState)
    val tauAlphabet = Alphabets.fromList(listOf("mBrew"))
    val testNFALTSPruned = TauPruning(testNFALTS, tauAlphabet).getResult()
    DrawAutomaton(testNFALTSPruned, testNFALTSPruned.inputAlphabet, "coffee_property_PRUNED")

}