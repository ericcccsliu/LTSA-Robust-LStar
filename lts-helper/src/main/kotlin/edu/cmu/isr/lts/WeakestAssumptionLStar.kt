package edu.cmu.isr.lts

import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandler
import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandlers
import de.learnlib.algorithms.lstar.closing.ClosingStrategies
import de.learnlib.algorithms.lstar.closing.ClosingStrategy
import de.learnlib.algorithms.lstar.dfa.ExtensibleLStarDFA
import de.learnlib.util.Experiment.DFAExperiment
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.commons.util.IOUtil
import net.automatalib.serialization.dot.GraphDOT
import net.automatalib.visualization.VisualizationHelper
import net.automatalib.visualization.VisualizationHelper.NodeAttrs
import net.automatalib.visualization.dot.DOT
import net.automatalib.words.Alphabet
import net.automatalib.words.Word
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


//time to break things
fun main() {
    val abpSysDFA : CompactDFA<String> = AUTtoDFA<String>("/testfiles/ABP_SYS.aut").getDFA()
    val abpPropertyDFA : CompactDFA<String> = AUTtoDFA<String>("/testfiles/ABP_PROPERTY.aut").getDFA()
    val lStarAlgorithm = WeakestAssumptionLStar(abpSysDFA, abpPropertyDFA, abpSysDFA.inputAlphabet)
    val experiment = DFAExperiment(lStarAlgorithm, WeakestEquivalenceOracle(abpSysDFA, abpPropertyDFA), abpSysDFA.inputAlphabet)
    experiment.run()
    val finalHyp: CompactDFA<String> = experiment.finalHypothesis as CompactDFA<String>

    val dotFile: File = File("automaton.dot")
    val dotOutputFile: File = File("automaton-as-png.png")
    dotFile.createNewFile()
    dotOutputFile.createNewFile()

    GraphDOT.write(
        finalHyp,
        finalHyp.inputAlphabet,
        IOUtil.asBufferedUTF8Writer(dotFile),
        InvalidVisualizationHelper()
    )
    DOT.runDOT(dotFile, "png", dotOutputFile)

}