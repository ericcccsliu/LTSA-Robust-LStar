package edu.cmu.isr.lts

import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandler
import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandlers
import de.learnlib.algorithms.lstar.closing.ClosingStrategies
import de.learnlib.algorithms.lstar.closing.ClosingStrategy
import de.learnlib.algorithms.lstar.dfa.ExtensibleLStarDFA
import de.learnlib.api.oracle.MembershipOracle
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.words.Alphabet
import net.automatalib.words.Word
import java.util.*

class WeakestAssumptionLStar<I>(
    machine: CompactDFA<I>, property: CompactDFA<I>, alphabet: Alphabet<I>, membershipOracle: MembershipOracle<I, Boolean>,
    initialSuffixes: List<Word<I>>, cexHandler: ObservationTableCEXHandler<Any?, Any?>, closingStrategy: ClosingStrategy<Any?, Any?> //any bad practice
)
        : ExtensibleLStarDFA<I> (alphabet, WeakestMembershipOracle<I>(machine, property), initialSuffixes, cexHandler, closingStrategy) {
            constructor(machine: CompactDFA<I>, property: CompactDFA<I>, alphabet: Alphabet<I>) :
                this(machine, property, alphabet, WeakestMembershipOracle<I>(machine, property), Collections.emptyList(), ObservationTableCEXHandlers.CLASSIC_LSTAR, ClosingStrategies.CLOSE_FIRST)

}