package edu.cmu.isr.lts

import edu.cmu.isr.lts.LTS.LTS
import net.automatalib.automata.Automaton
import net.automatalib.commons.util.IOUtil
import net.automatalib.serialization.dot.GraphDOT
import net.automatalib.visualization.VisualizationHelper
import net.automatalib.visualization.dot.DOT
import net.automatalib.words.Alphabet
import java.io.File
import java.util.*


//ctrl + c/v'd from an example
class InvalidVisualizationHelper<N, E> : VisualizationHelper<N, E> {
    private val random = Random(123)
    override fun getNodeProperties(node: N, properties: MutableMap<String, String>): Boolean {
        properties[VisualizationHelper.NodeAttrs.SHAPE] = "thisisnotavalidshape" + random.nextInt()
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
    val dotOutputFile = File("visualizations/$name.png")
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

