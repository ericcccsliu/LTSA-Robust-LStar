package edu.cmu.isr.lts

import de.learnlib.api.oracle.MembershipOracle
import de.learnlib.api.query.Query
import net.automatalib.commons.util.Holder
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalAction
import net.automatalib.util.ts.traversal.TSTraversalVisitor
import net.automatalib.words.Alphabet
import net.automatalib.words.Word

class WeakestMembershipOracle<I>(private val learningTarget: CompactNonDetLTS<I>, private val learningAlphabet: Alphabet<I>) : MembershipOracle.DFAMembershipOracle<I> {
    override fun processQueries(p0: MutableCollection<out Query<I, Boolean>>?) {
        if (p0 != null) {
            for(query: Query<I, Boolean> in p0) {
                processQuery(query)
            }
        }
    }
    override fun processQuery(query: Query<I, Boolean>?) {
        // @TODO
        // run string in query over composition, resolve false if ends in error state
        query?.answer(checkMembership(query.input))
    }

    private fun checkMembership(trace: Word<I>): Boolean{
        print(trace.toString() + " result:")
        if(trace.isEmpty){
            println(" true")
            return true
        }

        val traceDFACreator = AutomatonBuilders.newDFA(learningAlphabet).withInitial(0)

        for((index, symbol) in trace.withIndex()) {
            traceDFACreator.from(index).on(symbol).to(index + 1)
        }
        val traceDFA = traceDFACreator.create()
        for(state in traceDFA) {
            traceDFA.setAccepting(state, true)
        }
        val traceLTS = CompactDetLTS(traceDFA)

        for(state in traceLTS){
            traceLTS.setAccepting(state, true)
        }

        val composition = parallelComposition(traceLTS, learningAlphabet, learningTarget, learningTarget.inputAlphabet)
        if(isErrorReachable(composition, learningTarget.inputAlphabet)){
            println(" false")
            return false
        }
        println(" true")
        return true
    }

    private fun isErrorReachable(target: LTS<Int, I, Int>, alphabet: Alphabet<I>): Boolean {
        val query = ErrorReachability()
        val visitor = MembershipVisitor(target, query)
        TSTraversal.breadthFirst(target, alphabet, visitor)
        return query.result
    }

}

class ErrorReachability {
    var result: Boolean = false
}

class MembershipVisitor<I>(private val target: LTS<Int, I, Int>, private val query: ErrorReachability): TSTraversalVisitor<Int, I, Int, Boolean> {
    private val visited = HashSet<Int>()
    override fun processInitial(state: Int, outData: Holder<Boolean>?): TSTraversalAction {
        return TSTraversalAction.EXPLORE
    }

    override fun startExploration(state: Int, result: Boolean?): Boolean {
        return if (state !in visited) {
            visited.add(state)
            true
        } else {
            false
        }
    }

    override fun processTransition(
        source: Int,
        srcData: Boolean?,
        input: I,
        transition: Int?,
        successor: Int,
        outData: Holder<Boolean>?
    ): TSTraversalAction {
        if(target.isErrorState(successor)) {
            query.result = true
            return TSTraversalAction.ABORT_TRAVERSAL
        }
        return TSTraversalAction.EXPLORE

    }

}