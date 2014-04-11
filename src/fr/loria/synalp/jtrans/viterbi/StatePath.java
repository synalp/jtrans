package fr.loria.synalp.jtrans.viterbi;

import java.util.*;


/**
 * StateGraph through which only one sequence of nodes is possible.
 * Note: looping on the same node is not considered to alter the linearity of
 * the graph.
 */
public class StatePath extends StateGraph {

	/**
	 * Maps node indices from the original StateGraph objects to node indices
	 * in this StatePath.
	 */
	private Map<StateGraph, int[]> nodeTranslations;


	/**
	 * Creates a new translation array for the given graph.
	 * @return ready-to-fill translation array
	 */
	private int[] newTranslation(StateGraph graph) {
		assert !nodeTranslations.containsKey(graph);
		int[] nt = new int[graph.nNodes];
		Arrays.fill(nt, -1);
		nodeTranslations.put(graph, nt);
		return nt;
	}


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

		nodeTranslations = new HashMap<>(1);
		int[] nt = newTranslation(graph);
		for (int i = 0; i < graph.nNodes; i++) {
			nt[i] = i;
		}
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
		// Only 2 inbound transitions will ever be possible for any given node
		// in a path (except for the first node which only has itself)
		inNode     = new int  [nNodes][2];
		inProb     = new float[nNodes][2];

		nodeTranslations = new HashMap<>(1);
		int[] translations = newTranslation(graph);

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

			translations[ocn]  = tcn;
			nodeStates[tcn]    = graph.nodeStates[ocn];
			// All nodes have 2 inbound transitions, except for node #0 which
			// only has 1 (this is corrected after the loop)
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

		// Correct first node
		inCount[0] = 1;
		assert inNode[0][0] == 0;
		inProb[0][0] = 0; // log domain
		inNode[0][1] = -1;

		assert super.isLinear(): "flattened StateGraph should be linear";
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
		words = new ArrayList<>(nWords);
		wordBoundaries = new int[nWords];
		nodeStates = new int  [nNodes];
		inCount    = new byte [nNodes];
		inNode     = new int  [nNodes][MAX_TRANSITIONS];
		inProb     = new float[nNodes][MAX_TRANSITIONS];
		nodeTranslations = new HashMap<>();
		pool = new StatePool();

		// Concatenate
		int n = 0;
		for (StatePath path: chain) {
			n = concatenate(path, n);
		}
	}


	/**
	 * For use by the concatenation constructor only!
	 * Appends nodes and words from another path to this path, and updates node
	 * translation tables.
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
		int[] nodeTrans = newTranslation(path);
		for (int pathN = 0; pathN < path.nNodes; pathN++) {
			nodeTrans[pathN] = n;
			nodeStates[n] = poolTrans[path.nodeStates[pathN]];
			inCount[n] = 2;
			inNode[n][0] = n;
			inNode[n][1] = n - 1;
			n++;
		}

		// Make translations for node indices in all "parent" graphs of the
		// path (which, in turn, adds new "parent" graphs to this)
		for (Map.Entry<StateGraph, int[]> e: path.nodeTranslations.entrySet()) {
			int[] newNT = newTranslation(e.getKey());
			int[] oldNT = e.getValue();

			for (int i = 0; i < oldNT.length; i++) {
				newNT[i] = oldNT[i] < 0
						? -1
						: nodeTrans[oldNT[i]];
			}
		}

		return n;
	}


	@Override
	public boolean isLinear() {
		assert super.isLinear(): "StatePath should always be linear!";
		return true;
	}


	/**
	 * Returns the index of the node (in this graph) corresponding to the node
	 * referenced by graphNodeIdx (in the original graph).
	 */
	public int translateNode(StateGraph graph, int graphNodeIdx) {
		if (nodeTranslations == null) {
			throw new IllegalStateException("node translations don't exist " +
					"- were they cleared prematurely?");
		}

		assert graphNodeIdx >= 0;
		assert graphNodeIdx < graph.nNodes;

		return nodeTranslations.get(graph)[graphNodeIdx];
	}


	/**
	 * Drops references to the original StateGraph objects to give the JVM a
	 * chance to garbage-collect them. Caution: you won't be able to "translate"
	 * nodes after calling this method.
	 */
	public void dropTranslations() {
		nodeTranslations = null;
	}

}
