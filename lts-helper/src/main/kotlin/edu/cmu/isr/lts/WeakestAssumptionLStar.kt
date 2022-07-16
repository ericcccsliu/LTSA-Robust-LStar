package edu.cmu.isr.lts

import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandler
import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandlers
import de.learnlib.algorithms.lstar.closing.ClosingStrategies
import de.learnlib.algorithms.lstar.closing.ClosingStrategy
import de.learnlib.algorithms.lstar.dfa.ExtensibleLStarDFA
import de.learnlib.util.Experiment.DFAExperiment
import net.automatalib.automata.Automaton
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.automata.fsa.impl.compact.CompactNFA
import net.automatalib.commons.util.IOUtil
import net.automatalib.serialization.dot.GraphDOT
import net.automatalib.util.automata.predicates.TransitionPredicates
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
    learningTarget: CompactNonDetLTS<I>, learningAlphabet: Alphabet<I>, system: CompactNonDetLTS<I>,
    initialSuffixes: List<Word<I>>, cexHandler: ObservationTableCEXHandler<Any?, Any?>, closingStrategy: ClosingStrategy<Any?, Any?> //any bad practice
)
        : ExtensibleLStarDFA<I> (learningAlphabet, WeakestMembershipOracle<I>(learningTarget, learningAlphabet), initialSuffixes, cexHandler, closingStrategy) {
            constructor(learningTarget: CompactNonDetLTS<I>, system: CompactNonDetLTS<I>, alphabet: Alphabet<I>) :
                this(learningTarget, alphabet, system, Collections.emptyList(), ObservationTableCEXHandlers.CLASSIC_LSTAR, ClosingStrategies.CLOSE_FIRST)
}
class Experiment (sysPath: String, propertyPath: String, envPath: String) {
    //@TODO: finish refactor for nondeterministic system
    private val sysNFA : CompactNFA<String> = AUTtoDFA<String>(sysPath).getNFA()
    private val propertyDFA : CompactDFA<String> = AUTtoDFA<String>(propertyPath).getDFA(true)
    private val envNFA : CompactNFA<String> = AUTtoDFA<String>(envPath).getNFA()

    private val learningAlphabet: Alphabet<String>
        get() = getLearningAlphabet(sysNFA, propertyDFA, envNFA)

    private val tauAlphabet: Alphabet<String>
        get() = getTauAlphabet(sysNFA, propertyDFA, envNFA)

    private val sysLTS: CompactNonDetLTS<String> = CompactNonDetLTS(sysNFA)
    private val propertyLTS: CompactDetLTS<String> = CompactDetLTS(propertyDFA)
    init {
        println("Property error state: " + propertyLTS.errorState)
    }

    val composition = parallelComposition(sysLTS, sysLTS.inputAlphabet, propertyLTS, propertyLTS.inputAlphabet)
    init {
        DrawCompactLTS(sysLTS, sysLTS.inputAlphabet, "sysLTS")
        DrawCompactLTS(propertyLTS, propertyLTS.inputAlphabet, "propertyLTS")
        DrawAutomaton(composition, composition.inputAlphabet, "composition")
        println("composition error state: " + composition.errorState)
        println("sys input alphabet: " + sysLTS.inputAlphabet.toString())
        println("property input alphabet" + propertyLTS.inputAlphabet.toString())
    }

    //maps end state to map of transition, initial state
    private val backtrackingMap = HashMap<Int, HashMap<String, Int>>()

    //iterate over composition, add new error states to list
    //copy composition DFA over to composition NFA, condensing error states into one
    //@TODO: consider where to process changed transitions
    //@TODO: check to make sure this doesn't get stuck in a self-loop
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

    private val targetNFA = pruneErrorState(composition)

    init {
        DrawCompactLTS(targetNFA, targetNFA.inputAlphabet, "target")
        println("target error state: " + targetNFA.errorState)
    }

    private val lStarAlgorithm = WeakestAssumptionLStar(targetNFA, sysLTS, learningAlphabet)

    val result: CompactDFA<String>
        get() {
            val experiment =
                DFAExperiment(lStarAlgorithm, WeakestEquivalenceOracle(targetNFA, learningAlphabet, tauAlphabet), learningAlphabet)
            experiment.run()
            return experiment.finalHypothesis as CompactDFA<String>
        }

    private fun pruneErrorState(composition: CompactNonDetLTS<String>): CompactNonDetLTS<String> {
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
        TSCopy.copy(TSTraversalMethod.BREADTH_FIRST, composition, TSTraversal.NO_LIMIT, composition.inputAlphabet, prunedNFA, {!errorStates.contains(it) || it == initialErrorState}, TransitionPredicates.alwaysTrue())
        for(state in errorStates) {
//            //ignore initial error state because everything else condenses into it
//            if(state == initialErrorState) {
//                continue
//            }
//            //@TODO: remove state??? rn just removing all the transitions from it—does that do the same thing???? idk
//            prunedNFA.removeAllTransitions(state)
            val incomingTransitions = backtrackingMap[state]
            incomingTransitions?.forEach {
                prunedNFA.addTransition(it.value, it.key, initialErrorState)
            }
        }
        return CompactNonDetLTS(prunedNFA)
    }
    private fun getLearningAlphabet(system: CompactNFA<String>, property: CompactDFA<String>, environment: CompactNFA<String>) : Alphabet<String> {
        val systemSet = HashSet(system.inputAlphabet)
        val propertySet = HashSet(property.inputAlphabet)
        val environmentSet = HashSet(environment.inputAlphabet)
        systemSet.addAll(propertySet)
        environmentSet.retainAll(systemSet)
        return Alphabets.fromCollection(environmentSet)
    }

    private fun getTauAlphabet(system: CompactNFA<String>, property: CompactDFA<String>, environment: CompactNFA<String>): Alphabet<String> {
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
fun <I> DrawCompactLTS(LTS: LTS<Int, I, Int>, alphabet: Alphabet<I>, name: String) {
    DrawAutomaton(LTS, alphabet, name)
}

fun <S, I, T> DrawAutomaton(automaton: Automaton<S, I, T>, alphabet: Alphabet<I>, name: String) {
    val dotFile = File("dot/$name.dot")
    val dotOutputFile = File("visualizations/$name-as-png.png")
    dotFile.createNewFile()
    dotOutputFile.createNewFile()

    GraphDOT.write(
        automaton,
        alphabet,
        IOUtil.asBufferedUTF8Writer(dotFile),
        InvalidVisualizationHelper()
    )
    DOT.runDOT(dotFile, "png", dotOutputFile)
}


//time to break things :)
fun main() {
    println("starting...")

    val result = Experiment("/testfiles/ABP_SYS.aut", "/testfiles/ABP_PROPERTY.aut", "/testfiles/ABP_ENV.aut").result
    println("finished!")
    DrawCompactLTS(CompactDetLTS(result), result.inputAlphabet, "result")
}