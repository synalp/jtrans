package fr.loria.synalp.jtrans.viterbi;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * StateGraph through which only one sequence of nodes is possible.
 * Note: looping on the same node is not considered to alter the linearity of
 * the graph.
 */
public class StatePath extends StateGraph {

	/**
	 * Maps node indices from the original StateGraph to node indices in this
	 * StatePath.
	 */
	private int[] nodeTranslations;


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

		words = new ArrayList<>(nWords);
		wordBoundaries = new int[nWords];

		nodeStates = new int  [nNodes];
		inCount    = new byte [nNodes];
		inNode     = new int  [nNodes][MAX_TRANSITIONS];
		inProb     = new float[nNodes][MAX_TRANSITIONS];

		nodeTranslations = new int[graph.nNodes];
		Arrays.fill(nodeTranslations, -1);

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

			nodeTranslations[ocn] = tcn;

			nodeStates[tcn]    = graph.nodeStates[ocn];
			inCount   [tcn]    = 2;
			inNode    [tcn][0] = tcn;
			inNode    [tcn][1] = tcn-1;

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

		assert super.isLinear(): "flattened StateGraph should be linear";
	}


	@Override
	public boolean isLinear() {
		assert super.isLinear();
		return true;
	}

}
