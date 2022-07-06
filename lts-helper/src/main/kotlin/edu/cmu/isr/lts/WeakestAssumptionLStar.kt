package edu.cmu.isr.lts

import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandler
import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandlers
import de.learnlib.algorithms.lstar.closing.ClosingStrategies
import de.learnlib.algorithms.lstar.closing.ClosingStrategy
import de.learnlib.algorithms.lstar.dfa.ExtensibleLStarDFA
import de.learnlib.util.Experiment.DFAExperiment
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.automata.fsa.impl.compact.CompactNFA
import net.automatalib.automata.simple.SimpleDeterministicAutomaton
import net.automatalib.commons.util.IOUtil
import net.automatalib.serialization.dot.GraphDOT
import net.automatalib.util.ts.copy.TSCopy
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalMethod
import net.automatalib.visualization.VisualizationHelper
import net.automatalib.visualization.VisualizationHelper.NodeAttrs
import net.automatalib.visualization.dot.DOT
import net.automatalib.words.Alphabet
import net.automatalib.words.Word
import net.automatalib.words.impl.Alphabets
import java.io.File
import java.util.*


//use alphabet of property union alphabet of machine
class WeakestAssumptionLStar<I>(
    machine: CompactDFA<I>, property: CompactDFA<I>, alphabet: Alphabet<I>,
    initialSuffixes: List<Word<I>>, cexHandler: ObservationTableCEXHandler<Any?, Any?>, closingStrategy: ClosingStrategy<Any?, Any?> //any bad practice
)
        : ExtensibleLStarDFA<I> (alphabet, WeakestMembershipOracle<I>(machine, property), initialSuffixes, cexHandler, closingStrategy) {
            constructor(machine: CompactDFA<I>, property: CompactDFA<I>, alphabet: Alphabet<I>) :
                this(machine, property, alphabet, Collections.emptyList(), ObservationTableCEXHandlers.CLASSIC_LSTAR, ClosingStrategies.CLOSE_FIRST)
}
class Experiment (sysPath: String, propertyPath: String, envPath: String) {
    private val sysDFA : CompactDFA<String> = AUTtoDFA<String>(sysPath).getDFA()
    private val propertyDFA : CompactDFA<String> = AUTtoDFA<String>(propertyPath).getDFA(true)
    private val envDFA : CompactDFA<String> = AUTtoDFA<String>(envPath).getDFA()

    val learningAlphabet: Alphabet<String>
        get() = getLearningAlphabet(sysDFA, propertyDFA, envDFA)

    private val tauAlphabet: Alphabet<String>
        get() = getTauAlphabet(sysDFA, propertyDFA, envDFA)

    private val sysLTS: CompactDetLTS<String> = sysDFA.asLTS()
    private val propertyLTS: CompactDetLTS<String> = propertyDFA.asLTS()
    val composition: CompactDetLTS<String> = parallelComposition(sysLTS, sysLTS.inputAlphabet, propertyLTS, propertyLTS.inputAlphabet)

    //maps end state to map of transition, initial state
    private val backtrackingMap = HashMap<Int, HashMap<String, Int>>()

    //iterate over composition, add new error states to list
    //copy composition DFA over to composition NFA, condensing error states into one
    //@TODO: consider where to process changed transitions
    //@TODO: check to make sure this doesn't get stuck in a self-loop
    init {
        for(state in composition.states) {
            for(input in composition.inputAlphabet) {
                if(composition.getSuccessor(state, input) != SimpleDeterministicAutomaton.IntAbstraction.INVALID_STATE) {
                    val endState: Int = composition.getSuccessor(state, input)
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

    private val lStarAlgorithm = WeakestAssumptionLStar(sysDFA, propertyDFA, learningAlphabet)

    val result: CompactDFA<String>
        get() {
            val experiment =
                DFAExperiment(lStarAlgorithm, WeakestEquivalenceOracle(sysDFA, propertyDFA), learningAlphabet)
            experiment.run()
            return experiment.finalHypothesis as CompactDFA<String>
        }

    private fun pruneErrorState(composition: CompactDetLTS<String>): CompactNonDetLTS<String> {
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
        val prunedNFA = CompactNFA<String>(composition.inputAlphabet)
        TSCopy.copy(TSTraversalMethod.BREADTH_FIRST, composition, TSTraversal.NO_LIMIT, composition.inputAlphabet, prunedNFA)
        for(state in errorStates) {
            if(state == initialErrorState) {
                continue
            }
            prunedNFA.removeAllTransitions(state)
            val incomingTransitions = backtrackingMap[state]
            incomingTransitions?.forEach {
                prunedNFA.addTransition(it.value, it.key, initialErrorState)
            }
        }
        return prunedNFA.asLTS()
    }
    private fun getLearningAlphabet(system: CompactDFA<String>, property: CompactDFA<String>, environment: CompactDFA<String>) : Alphabet<String> {
        val systemSet = HashSet(system.inputAlphabet)
        val propertySet = HashSet(property.inputAlphabet)
        val environmentSet = HashSet(environment.inputAlphabet)
        systemSet.addAll(propertySet)
        environmentSet.retainAll(systemSet)
        return Alphabets.fromCollection(environmentSet)
    }

    private fun getTauAlphabet(system: CompactDFA<String>, property: CompactDFA<String>, environment: CompactDFA<String>): Alphabet<String> {
        val systemSet = HashSet(system.inputAlphabet)
        val propertySet = HashSet(property.inputAlphabet)
        val environmentSet = HashSet(environment.inputAlphabet)
        systemSet.addAll(propertySet)
        systemSet.removeAll(environmentSet)
        return Alphabets.fromCollection(systemSet)
    }
}

//ctrl + c/v'd from an example
private class InvalidVisualizationHelper<N, E> : VisualizationHelper<N, E> {
    private val random = Random(123)
    override fun getNodeProperties(node: N, properties: MutableMap<String, String>): Boolean {
        properties[NodeAttrs.SHAPE] = "thisisnotavalidshape" + random.nextInt()
        return true
    }

    override fun getEdgeProperties(src: N, edge: E, tgt: N, properties: Map<String, String>): Boolean {
        return true
    }
}
fun DrawAutomaton(automaton: CompactDFA<String>) {
    val dotFile = File("automaton.dot")
    val dotOutputFile = File("automaton-as-png.png")
    dotFile.createNewFile()
    dotOutputFile.createNewFile()

    GraphDOT.write(
        automaton,
        automaton.inputAlphabet,
        IOUtil.asBufferedUTF8Writer(dotFile),
        InvalidVisualizationHelper()
    )
    DOT.runDOT(dotFile, "png", dotOutputFile)
}


//time to break things
fun main() {
//    val abpSysDFA : CompactDFA<String> = AUTtoDFA<String>("/testfiles/ABP_SYS.aut").getDFA()
//    val abpPropertyDFA : CompactDFA<String> = AUTtoDFA<String>("/testfiles/ABP_PROPERTY.aut").getDFA(true)
//    val lStarAlgorithm = WeakestAssumptionLStar(abpSysDFA, abpPropertyDFA, abpSysDFA.inputAlphabet)
//
//
}