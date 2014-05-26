package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.util.LogMath;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.speechreco.s4.HMMModels;

import java.util.*;


/**
 * StateGraph through which only one sequence of nodes is possible.
 * Note: looping on the same node is not considered to alter the linearity of
 * the graph.
 */
public class StatePath extends StateGraph {

	/**
	 * Converts a linear StateGraph to a StatePath.
	 * @throws IllegalArgumentException if the graph is not linear.
	 */
	public static StatePath asPath(StateGraph graph) {
		if (!graph.isLinear()) {
			throw new IllegalArgumentException("Can't construct StatePath " +
					"from non-linear StateGraph!");
		}

		return new StatePath(graph);
	}


	/**
	 * Constructs a StatePath from a strictly linear StateGraph.
	 * For internal use only; external callers should use asPath() instead.
	 */
	private StatePath(StateGraph graph) {
		super(graph);

		assert graph.isLinear();
		assert isLinear();

		check();
	}


	/**
	 * Constructs a "flattened" state graph from an original state graph and
	 * a timeline of visited nodes.
	 */
	public StatePath(StateGraph graph, int[] timeline) {
		pool = graph.pool;

		//----------------------------------------------------------------------
		// Count

		nNodes = 0;
		nWords = 0;

		int pn = -1;
		int pw = -1;
		for (int frame = 0; frame < timeline.length; frame++) {
			int cn = timeline[frame];
			if (cn == pn) {
				continue;
			}

			int cw = graph.getWordIdxAt(cn, pw);
			if (cw != pw) {
				nWords++;
				pw = cw;
			}

			nNodes++;
			pn = cn;
		}

		//----------------------------------------------------------------------
		// Allocate

		allocArrays(nNodes, nWords);

		//----------------------------------------------------------------------
		// Fill

		int tcn = 0;  // This Current Node
		int tcw = 0;  // This Current Word
		int opn = -1; // Other Previous Node
		int opw = -1; // Other Previous Word

		for (int frame = 0; frame < timeline.length; frame++) {
			int ocn = timeline[frame]; // Other Current Node
			if (ocn == opn) {
				continue;
			}

			nodeStates[tcn]    = graph.nodeStates[ocn];
			// All nodes have 2 outbound transitions, except for the last node
			// which only has 1 (this is corrected after the loop)
			outCount   [tcn]    = 2;
			outNode    [tcn][0] = tcn;
			outNode    [tcn][1] = tcn+1;

			assert graph.outNode[ocn][0] == ocn;
			outProb[tcn][0] = graph.outProb[ocn][0];
			fillUniformNonLoopTransitionProbabilities(tcn);

			int ocw = graph.getWordIdxAt(ocn, opw);
			if (ocw != opw) {
				assert tcw == words.size();
				words.add(graph.words.get(ocw));
				wordBoundaries[tcw] = tcn;
				tcw++;
				opw = ocw;
			}

			tcn++;
			opn = ocn;
		}

		correctLastNodeTransitions();

		check();
	}


	/**
	 * Concatenation constructor. Constructs a StatePath by stringing a chain
	 * of StatePath objects together.
	 */
	public StatePath(StatePath... chain) {
		// Count
		nNodes = 0;
		nWords = 0;
		for (StatePath path: chain) {
			nNodes += path.nNodes;
			nWords += path.nWords;
		}

		// Allocate
		allocArrays(nNodes, nWords);
		pool = new StateSet();

		// Concatenate
		int n = 0;
		for (StatePath path: chain) {
			n = concatenate(path, n);
		}

		correctLastNodeTransitions();

		check();
	}


	public StatePath(StateTimeline timeline) {
		LogMath lm = HMMModels.getLogMath();

		int nonPaddingSegmentCount = 0;
		for (StateTimeline.Segment seg: timeline.segments) {
			if (!seg.isPadding()) {
				nonPaddingSegmentCount++;
			}
		}

		allocArrays(nonPaddingSegmentCount, timeline.getUniqueWordCount());
		pool = new StateSet();

		int n = 0;
		int w = 0;
		Word pWord = null;

		for (StateTimeline.Segment seg: timeline.segments) {
			if (seg.isPadding()) {
				continue;
			}

			int stateIdx = pool.add(seg.state);
			nodeStates[n] = stateIdx;
			outCount[n] = 2;
			outNode[n][0] = n;
			outNode[n][1] = n+1;

			outProb[n] = getSuccessorProbabilities(seg.state, lm);
			assert 2 == outProb[n].length;

			if (null != seg.word && pWord != seg.word) {
				words.add(seg.word);
				wordBoundaries[w++] = n;
				pWord = seg.word;
			}

			n++;
		}

		correctLastNodeTransitions();

		check();
	}


	private void allocArrays(int nNodes, int nWords) {
		this.nNodes = nNodes;
		this.nWords = nWords;

		words = new ArrayList<>(nWords);
		wordBoundaries = new int[nWords];

		nodeStates = new int  [nNodes];
		outCount   = new byte [nNodes];
		// Only 2 transitions will ever be possible for any given node
		// in a path (except for the first node which only has itself)
		outNode    = new int  [nNodes][2];
		outProb    = new float[nNodes][2];

		for (int i = 0; i < nNodes; i++) {
			Arrays.fill(outProb[i], UNINITIALIZED_LOG_PROBABILITY);
		}
	}


	private void check() {
		assert super.isLinear();
		checkTransitions();
	}


	/**
	 * For use by the concatenation constructor only!
	 * Appends nodes and words from another path to this path, and updates node
	 * translation tables.
	 * Should not be used anywhere but in a constructor as it assumes that the
	 * node arrays are not fully filled out.
	 * <p/>
	 * Warning: the last node is left with a "dangling" outbound transition.
	 * That is, the last appended node transitions to the node that follows it,
	 * even though that node doesn't exist yet. This enables linking with the
	 * next path that will be concatenated. However, you will have to remove the
	 * dangling transition after the last iteration.
	 * @param n insertion index for new nodes
	 * @return updated node insertion index
	 */
	private int concatenate(StatePath path, int n) {
		assert words.size() < nWords: "words already filled!";

		// Insert words and adjust word boundaries
		int wordOffset = words.size();
		words.addAll(path.words);
		for (int i = 0; i < path.wordBoundaries.length; i++) {
			wordBoundaries[i + wordOffset] = path.wordBoundaries[i] + n;
		}

		// Make translations for the path's node indices
		int[] poolTrans = pool.addAll(path.pool);
		for (int pathN = 0; pathN < path.nNodes; pathN++) {
			assert pathN == path.nNodes-1 || path.outCount[pathN] == 2;
			nodeStates[n] = poolTrans[path.nodeStates[pathN]];
			outCount[n] = 2;
			outNode[n][0] = n;
			outNode[n][1] = n + 1;
			outProb[n][0] = path.outProb[pathN][0];
			outProb[n][1] = path.outProb[pathN][1];
			n++;
		}

		// Fix dangling transition probabilities
		assert outCount[n-1] == 2;
		outProb[n-1][1] = UNINITIALIZED_LOG_PROBABILITY; // non-loop
		fillUniformNonLoopTransitionProbabilities(n-1);

		return n;
	}


	@Override
	public boolean isLinear() {
		assert super.isLinear(): "StatePath should always be linear!";
		return true;
	}

}
