package edu.cmu.isr.lts

import de.learnlib.util.Experiment
import edu.cmu.isr.lts.LTS.CompactDetLTS
import edu.cmu.isr.lts.LTS.CompactNonDetLTS
import edu.cmu.isr.lts.oracles.WeakestEquivalenceOracle
import edu.cmu.isr.lts.oracles.WeakestMembershipOracle
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.automata.fsa.impl.compact.CompactNFA
import net.automatalib.util.automata.Automata
import net.automatalib.util.automata.equivalence.DeterministicEquivalenceTest
import net.automatalib.util.automata.predicates.TransitionPredicates
import net.automatalib.util.ts.copy.TSCopy
import net.automatalib.util.ts.traversal.TSTraversalMethod
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets
import java.util.HashSet
import kotlin.system.measureTimeMillis

class WeakestAssumption (sysPath: String, propertyPath: String, envPath: String) {
    private val sysNFA : CompactNFA<String> = AUTtoDFA<String>(sysPath).getNFA()
    private val propertyDFA : CompactDFA<String> = AUTtoDFA<String>(propertyPath).getDFA(true)
    private val envNFA : CompactNFA<String> = AUTtoDFA<String>(envPath).getNFA()

    init{
        DrawAutomaton(sysNFA, sysNFA.inputAlphabet, "system")
        DrawAutomaton(propertyDFA, propertyDFA.inputAlphabet, "property")
        DrawAutomaton(envNFA, envNFA.inputAlphabet, "env")
    }

    private val assumptionAlphabet: Alphabet<String>
    private val tauAlphabet: Alphabet<String>
    init {
        assumptionAlphabet = getAssumptionAlphabet(sysNFA, propertyDFA, envNFA)
        tauAlphabet = getTauAlphabet(sysNFA, propertyDFA, envNFA)
    }

    val sysLTS: CompactNonDetLTS<String> = CompactNonDetLTS(sysNFA)
    val propertyLTS: CompactDetLTS<String> = CompactDetLTS(propertyDFA)
    val composition = parallelComposition(sysLTS, sysLTS.inputAlphabet, propertyLTS, propertyLTS.inputAlphabet)

    private val targetNFA = TauPruning(composition, tauAlphabet).getResult()
    init {
        println("tauAlphabet $tauAlphabet")
        println("assumptionAlphabet $assumptionAlphabet")
        println("target error state " + targetNFA.errorState)
        DrawAutomaton(targetNFA, targetNFA.inputAlphabet, "target")

        //issue in tau pruning???
    }
    val subsetConstructionTime: Long
    val subsetConstructionResult: CompactDFA<String>
    init {
        subsetConstructionTime = measureTimeMillis {
            subsetConstructionResult = extractPropertyDFA(targetNFA, assumptionAlphabet, tauAlphabet)
        }
    }

    val lStarTime: Long
    val lStarResult: CompactDFA<String>
    init {
        lStarTime = measureTimeMillis {
            addSinkState(targetNFA, assumptionAlphabet)
            val membershipOracle = WeakestMembershipOracle(targetNFA, assumptionAlphabet)
            val lStarAlgorithm = WeakestAssumptionLStar(targetNFA, sysLTS, assumptionAlphabet, membershipOracle)
            val experiment =
                Experiment.DFAExperiment(
                    lStarAlgorithm,
                    WeakestEquivalenceOracle(targetNFA, assumptionAlphabet, tauAlphabet, membershipOracle.observations),
                    assumptionAlphabet
                )
            experiment.run()
            //@TODO: make this a function to clean up code
            val finalHypothesisCompactDFA = experiment.finalHypothesis as CompactDFA
            val hypothesisAcceptingOnly = CompactDFA(finalHypothesisCompactDFA.inputAlphabet)
            TSCopy.copy(TSTraversalMethod.BREADTH_FIRST,
                finalHypothesisCompactDFA,
                -1,
                finalHypothesisCompactDFA.inputAlphabet,
                hypothesisAcceptingOnly,
                { finalHypothesisCompactDFA.isAccepting(it) },
                TransitionPredicates.alwaysTrue()
            )
            lStarResult = hypothesisAcceptingOnly
        }
    }

    private fun getAssumptionAlphabet(system: CompactNFA<String>, property: CompactDFA<String>, environment: CompactNFA<String>) : Alphabet<String> {
        //@TODO: use sets?
        val systemSet = HashSet(system.inputAlphabet)
        val propertySet = HashSet(property.inputAlphabet)
        val environmentSet = HashSet(environment.inputAlphabet)
        systemSet.addAll(propertySet)
        environmentSet.retainAll(systemSet)
        return Alphabets.fromCollection(environmentSet)
    }

    private fun getTauAlphabet(system: CompactNFA<String>, property: CompactDFA<String>, environment: CompactNFA<String>): Alphabet<String> {
        //@TODO: use sets?
        val systemSet = HashSet(system.inputAlphabet)
        val propertySet = HashSet(property.inputAlphabet)
        val environmentSet = HashSet(environment.inputAlphabet)
        systemSet.addAll(propertySet)
        systemSet.removeAll(environmentSet)
        return Alphabets.fromCollection(systemSet)
    }
}

fun <I> addSinkState(targetNFA: CompactNonDetLTS<I>, learningAlphabet: Alphabet<I>) {
    val sinkState = targetNFA.addState(true)
    for(input in learningAlphabet) {
        targetNFA.addTransition(sinkState, input, sinkState)
    }
    for(state in targetNFA.states) {
        for(input in learningAlphabet) {
            if(targetNFA.getSuccessors(state, input).isEmpty()) {
                targetNFA.addTransition(state, input, sinkState)
            }
        }
    }
}

fun main() {
     val experiment = WeakestAssumption("/testfiles/coffee_sys.aut", "/testfiles/coffee_property.aut", "/testfiles/coffee_env.aut")
//    val experiment = WeakestAssumption("/testfiles/ABP_SYS.aut", "/testfiles/ABP_PROPERTY.aut", "/testfiles/ABP_ENV.aut")
//    val experiment = WeakestAssumption("/testfiles/1LINE_PUMP_SYS.aut", "/testfiles/1LINE_PUMP_PROPERTY.aut", "/testfiles/1LINE_PUMP_ENV.aut")
//    val experiment = WeakestAssumption("/testfiles/2LINES_SYS.aut", "/testfiles/2LINES_PROPERTY.aut", "/testfiles/2LINES_ENV.aut")
    val lStarResult = experiment.lStarResult
    val subsetConstructionResult = experiment.subsetConstructionResult
    DrawAutomaton(lStarResult, lStarResult.inputAlphabet, "LSTAR")
    DrawAutomaton(subsetConstructionResult, subsetConstructionResult.inputAlphabet, "SUBSET")
    println("separating word: " + DeterministicEquivalenceTest.findSeparatingWord(lStarResult, subsetConstructionResult, subsetConstructionResult.inputAlphabet))
    println("equivalence: " + Automata.testEquivalence(lStarResult, subsetConstructionResult, subsetConstructionResult.inputAlphabet))

    val lStarResultLTS = CompactDetLTS(lStarResult)
    val compositionlStar = parallelComposition(lStarResultLTS, lStarResultLTS.inputAlphabet, experiment.sysLTS, experiment.sysLTS.inputAlphabet)
    print("safety of lStarResult: ")
    val lStarSafetyResult = checkSafety(compositionlStar, compositionlStar.inputAlphabet, experiment.propertyLTS, experiment.propertyLTS.inputAlphabet)
    if(lStarSafetyResult.violation) {
        println("violation—" + lStarSafetyResult.trace)
    } else {
        println("safe")
    }

    val subsetResultLTS = CompactDetLTS(subsetConstructionResult)
    val compositionSubset = parallelComposition(subsetResultLTS, subsetResultLTS.inputAlphabet, experiment.sysLTS, experiment.sysLTS.inputAlphabet)
    print("safety of lStarResult: ")
    val  subsetSafetyResult = checkSafety(compositionSubset, compositionSubset.inputAlphabet, experiment.propertyLTS, experiment.propertyLTS.inputAlphabet)
    if(subsetSafetyResult.violation) {
        println("violation—" + subsetSafetyResult.trace)
    } else {
        println("safe")
    }

    makeErrorState(lStarResultLTS, lStarResultLTS.inputAlphabet)
    val subsetSatisfieslStarResult = checkSafety(subsetResultLTS, subsetResultLTS.inputAlphabet, lStarResultLTS, lStarResultLTS.inputAlphabet)
    makeErrorState(subsetResultLTS, subsetResultLTS.inputAlphabet)
    val lStarSatisfiesSubsetResult = checkSafety(CompactDetLTS(lStarResult), lStarResult.inputAlphabet, subsetResultLTS, subsetResultLTS.inputAlphabet)

    print("subset |= lStar: ")
    if(subsetSatisfieslStarResult.violation) {
        println(subsetSatisfieslStarResult.trace)
    } else {
        println("true")
    }

    print("lStar |= subset: ")
    if(lStarSatisfiesSubsetResult.violation) {
        println(lStarSatisfiesSubsetResult.trace)
    } else {
        println("true")
    }
    println()
    println("lStar Runtime: " + experiment.lStarTime)
    println("Subset Construction Runtime: " + experiment.subsetConstructionTime)
}