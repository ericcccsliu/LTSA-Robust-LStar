package edu.cmu.isr.lts

import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandler
import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandlers
import de.learnlib.algorithms.lstar.closing.ClosingStrategies
import de.learnlib.algorithms.lstar.closing.ClosingStrategy
import de.learnlib.algorithms.lstar.dfa.ExtensibleLStarDFA
import de.learnlib.util.Experiment.DFAExperiment
import edu.cmu.isr.lts.LTS.CompactDetLTS
import edu.cmu.isr.lts.LTS.CompactNonDetLTS
import edu.cmu.isr.lts.oracles.WeakestEquivalenceOracle
import edu.cmu.isr.lts.oracles.WeakestMembershipOracle
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.automata.fsa.impl.compact.CompactNFA
import net.automatalib.words.Alphabet
import net.automatalib.words.Word
import net.automatalib.words.impl.Alphabets
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
