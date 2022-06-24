package edu.cmu.isr.lts

import de.learnlib.filter.cache.dfa.DFAHashCacheOracle
import de.learnlib.api.oracle.MembershipOracle
import de.learnlib.api.query.Query
import net.automatalib.automata.fsa.impl.compact.CompactDFA

class WeakestMembershipOracle<I, D>(machine: CompactDFA<I>, property: CompactDFA<I>) : MembershipOracle<I, D> {
    val machineLTS: CompactDetLTS<I> = machine.asLTS()
    val propertyLTS: CompactDetLTS<I> = property.asLTS()

    override fun processQueries(p0: MutableCollection<out Query<I, D>>?) {
        // @TODO
    }

    override fun processQuery(query: Query<I, D>?) {
        // @TODO
    }

}