package edu.cmu.isr.lts

import de.learnlib.util.Experiment
import edu.cmu.isr.lts.LTS.CompactDetLTS
import edu.cmu.isr.lts.LTS.CompactNonDetLTS
import edu.cmu.isr.lts.oracles.WeakestEquivalenceOracle
import net.automatalib.automata.Automaton
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
        get() = getAssumptionAlphabet(sysNFA, propertyDFA, envNFA)

    private val tauAlphabet: Alphabet<String>
        get() = getTauAlphabet(sysNFA, propertyDFA, envNFA)

    private val sysLTS: CompactNonDetLTS<String> = CompactNonDetLTS(sysNFA)
    private val propertyLTS: CompactDetLTS<String> = CompactDetLTS(propertyDFA)
    val composition = parallelComposition(sysLTS, sysLTS.inputAlphabet, propertyLTS, propertyLTS.inputAlphabet)

    private val targetNFA = TauPruning(composition, tauAlphabet).getResult()

    val lStarResult: CompactDFA<String>
        get() {
            val lStarAlgorithm = WeakestAssumptionLStar(targetNFA, sysLTS, assumptionAlphabet)
            val experiment =
                Experiment.DFAExperiment(
                    lStarAlgorithm,
                    WeakestEquivalenceOracle(targetNFA, assumptionAlphabet),
                    assumptionAlphabet
                )
            experiment.run()
            //@TODO: make this a function to clean up code
            val finalHypothesisCompactDFA = experiment.finalHypothesis as CompactDFA
            val hypothesisAcceptingOnly = CompactDFA(finalHypothesisCompactDFA.inputAlphabet)
            TSCopy.copy(TSTraversalMethod.BREADTH_FIRST, finalHypothesisCompactDFA, -1, finalHypothesisCompactDFA.inputAlphabet, hypothesisAcceptingOnly,
                { finalHypothesisCompactDFA.isAccepting(it) }, TransitionPredicates.alwaysTrue())
            return hypothesisAcceptingOnly
        }

    val subsetConstructionResult: CompactDFA<String>
        get() {
            return extractPropertyDFA(targetNFA, assumptionAlphabet, tauAlphabet)
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

fun main() {
    val experiment = WeakestAssumption("/testfiles/coffee_sys.aut", "/testfiles/coffee_property.aut", "/testfiles/coffee_env.aut")
    val lStarResult = experiment.lStarResult
    val subsetConstructionResult = experiment.subsetConstructionResult
    DrawAutomaton(lStarResult, lStarResult.inputAlphabet, "coffee_LSTAR")
    DrawAutomaton(subsetConstructionResult, subsetConstructionResult.inputAlphabet, "coffee_SUBSET")
    println("separating word: " + DeterministicEquivalenceTest.findSeparatingWord(lStarResult, subsetConstructionResult, subsetConstructionResult.inputAlphabet))
    println("equivalence: " + Automata.testEquivalence(lStarResult, subsetConstructionResult, subsetConstructionResult.inputAlphabet))
}