package edu.cmu.isr.lts

import com.google.common.collect.Sets
import edu.cmu.isr.lts.LTS.CompactNonDetLTS
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.ts.UniversalDTS
import net.automatalib.util.automata.predicates.TransitionPredicates
import net.automatalib.util.ts.copy.TSCopy
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalMethod
import net.automatalib.words.Alphabet

//implements property extraction step described in "Assumption Generation for Software Component Verification"
class PropertyExtractionDTS (private val target: CompactNonDetLTS<String>, private val tauAlphabet: Alphabet<String>):
    UniversalDTS<Set<Int>, String, Set<Int>?, Boolean, Void> {

    override fun getInitialState(): Set<Int> {
        //tau closure of initial state
        val initialState = Sets.newHashSet(target.getInitialState())
        return tauClosure(initialState)
    }

//    override fun getStates(): MutableCollection<Set<Int>> {
//        val stateSet: Set<Int> = target.states.toSet()
//        return Sets.powerSet(stateSet)
//    }

    override fun getTransition(state: Set<Int>, input: String): Set<Int>? {
        //assume inputted state is already tau closed
        var nextState = HashSet<Int>()
        for(individualState in state) {
            nextState.addAll(target.getSuccessors(individualState, input))
        }
        nextState = tauClosure(nextState) as HashSet<Int>
        //if the set of states contains error, then entire set is error
        //null is error state //empty set is sink state
        if(nextState.contains(target.errorState)) return null
        return nextState
    }

    override fun getTransitionProperty(p0: Set<Int>?): Void? {
        return null
    }

    //use to distinguish sink state
    override fun getStateProperty(state: Set<Int>?): Boolean {
        //null is error state //empty set is sink state
        if(state == null) {
            return false
        }
        return true
    }

    override fun getSuccessor(transition: Set<Int>?): Set<Int>? {
        return transition
    }

    private fun tauClosure(state: Set<Int>): Set<Int> {
        val finalStates = HashSet<Int>()
        val q = ArrayDeque<Int>()

        q.addAll(state)
        while(q.isNotEmpty()) {
            val curr = q.first()
            q.removeFirst()
            if(!finalStates.contains(curr)){
                q.addAll(tauReachable(curr))
                finalStates.add(curr)
            }
        }
        return finalStates
    }

    private fun tauReachable(state: Int): Set<Int> {
        val tauReachableStates = HashSet<Int>()
        for(word in tauAlphabet){
            if(target.getSuccessors(state, word).isNotEmpty()){
                tauReachableStates.addAll(target.getSuccessors(state,word))
                //may be too verbose
            }
        }
        return tauReachableStates
    }

}

fun extractPropertyDFA(target: CompactNonDetLTS<String>, alphabet: Alphabet<String>, tauAlphabet: Alphabet<String>) : CompactDFA<String> {
    val propertyDTS = PropertyExtractionDTS(target, tauAlphabet)
    val outputDFA = CompactDFA(alphabet)

    TSCopy.copy(TSTraversalMethod.BREADTH_FIRST, propertyDTS, TSTraversal.NO_LIMIT, alphabet, outputDFA)
    return outputDFA
}