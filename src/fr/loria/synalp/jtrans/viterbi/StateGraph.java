package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.linguist.acoustic.*;
import edu.cmu.sphinx.util.LogMath;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.speechreco.grammaire.Grammatiseur;
import fr.loria.synalp.jtrans.speechreco.s4.*;
import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.utils.TimeConverter;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * State graph representing a grammar.
 */
public class StateGraph {

	/**
	 * Maximum number of transitions an HMM state may have.
	 */
	/*
	If this value ever has to exceed 127 (overkill), byte reads (for
	transitions) will need to be peppered with "& 0xFF" (no unsigned bytes in
	Java). If it ever has to exceed 255 (way overkill), be sure to change
	the type of the inCount/outCount arrays.
	*/
	public static final int MAX_TRANSITIONS = 64;

	/** Pattern for non-phone grammar tokens. */
	public final static Pattern NONPHONE_PATTERN =
			Pattern.compile("^[^a-zA-Z]$");

	// Extra rules used when building the grammar graph
	private static final String[] SILENCE_RULE =
			{ StatePool.SILENCE_PHONE };

	private static final String[] OPT_SILENCE_RULE =
			{ "[", StatePool.SILENCE_PHONE, "]" };

	/** Filler value for uninitialized log probabilities */
	public final static float UNINITIALIZED_LOG_PROBABILITY =
			Float.NEGATIVE_INFINITY;

	/** Epsilon for comparison of linear probabilities */
	public final static float LIN_PROB_CMP_EPSILON = .0001f;

	/** Pool of unique HMM states. */
	protected StatePool pool;


	/**
	 * All nodes in the grammar.
	 * Each node points to a unique HMM state ID.
	 * There are 3 HMM states per phone, hence 3 nodes per phone.
	 */
	protected int[] nodeStates;

	/**
	 * Number of outbound transitions for each node.
	 * Values in this array should not exceed MAX_TRANSITIONS, otherwise an
	 * index out of bounds exception will eventually be thrown.
	 */
	protected byte[] outCount;

	/**
	 * Node IDs for each outbound transition.
	 * The first entry is *always* the same node (loop).
	 */
	protected int[][] outNode;

	/**
	 * Probability of each outbound transition (in the log domain).
	 * The first entry is *always* the probability of looping on the same node.
	 */
	protected float[][] outProb;

	/** Total number of nodes in the grammar. */
	protected int nNodes;

	/** Total number of words in the grammar. */
	protected int nWords;

	/** Insertion point for new nodes in the nodeStates array. */
	private int insertionPoint;

	/** Alignable words at the basis of this grammar */
	protected List<Word> words;

	/**
	 * Indices of the initial node of each word (i.e. that points to the first
	 * HMM state of the first phone of the word).
	 * All potential pronunciations for a given word are inserted contiguously
	 * in the nodes array. The order in which the nodes are inserted
	 * reflects the order of the words.
	 */
	protected int[] wordBoundaries;

	/** Used to report progress in viterbi() and backtrack() (may be null) */
	protected ProgressDisplay progress = null;


	/**
	 * Tests two linear probabilities for equality.
	 */
	public static boolean linProbEq(double a, double b) {
		assert a >= -LIN_PROB_CMP_EPSILON && a <= 1+LIN_PROB_CMP_EPSILON:
				"linear probability out of bounds: " + a;
		assert b >= -LIN_PROB_CMP_EPSILON && b <= 1+LIN_PROB_CMP_EPSILON:
				"linear probability out of bounds: " + b;
		return Math.abs(a-b) <= LIN_PROB_CMP_EPSILON;
	}

	/**
	 * Trims leading and trailing whitespace then splits around whitespace.
	 */
	public static String[] trimSplit(String text) {
		return text.trim().split("\\s+");
	}


	/**
	 * Sets progress reporting parameters. If you don't care about progress
	 * reporting, you don't have to use this method.
	 * @param progress progress display
	 */
	public void setProgressDisplay(ProgressDisplay progress) {
		this.progress = progress;
	}


	public List<Word> getWords() {
		return words;
	}


	/**
	 * Creates grammar rules from a list of words using the standard
	 * grammatizer.
	 * @param words array of whitespace-trimmed words
	 * @return a 2D array of rule tokens (1st dimension corresponds to word
	 * indices). If a word can't be processed, its rule is set to null.
	 */
	public static String[][] getRules(List<Word> words) {
		String[][] rules = new String[words.size()][];
		Grammatiseur gram = Grammatiseur.getGrammatiseur();

		for (int i = 0; i < words.size(); i++) {
			String rule = gram.getGrammar(words.get(i).toString());

			if (rule == null || rule.isEmpty()) {
				rules[i] = null;
				continue;
			}

			rules[i] = trimSplit(rule);

			for (int j = 0; j < rules[i].length; j++) {
				String r = rules[i][j];

				// Convert phones - don't touch ()[]|
				// TODO: gram should handle the conversion transparently
				if (!NONPHONE_PATTERN.matcher(r).matches()) {
					rules[i][j] = PhoneticForcedGrammar.convertPhone(r);
				}

				assert rules[i][j] != null;
			}
		}

		return rules;
	}


	/**
	 * Counts phones in a list of rules.
	 */
	public static int countPhones(String[][] rules, boolean interWordSilences) {
		// Mandatory final & initial silence
		int count = 2;

		for (String[] ruleTokens: rules) {
			if (null == ruleTokens)
				continue;

			// Optional silence between each word
			if (interWordSilences && count > 2)
				count++;

			for (String token: ruleTokens)
				if (!NONPHONE_PATTERN.matcher(token).matches())
					count++;
		}

		return count;
	}


	public HMMState getStateAt(int nodeIdx) {
		return pool.get(nodeStates[nodeIdx]);
	}


	public boolean isSilentAt(int nodeIdx) {
		return pool.isSilent(nodeStates[nodeIdx]);
	}


	public String getPhoneAt(int nodeIdx) {
		return getStateAt(nodeIdx).getHMM().getBaseUnit().getName();
	}


	/**
	 * Returns the index of the word that belongs to the given node.
	 * Starts searching from the beginning of the word list.
	 * @return -1 if the word can't be found
	 */
	public int getWordIdxAt(int nodeIdx) {
		return getWordIdxAt(nodeIdx, -1);
	}


	/**
	 * Returns the index of the word that belongs to the given node.
	 * Starts searching from a given position.
	 * @param currWord Start searching from the word immediately following this
	 * position. Use -1 to start searching from the beginning of the list!
	 * @return -1 if the word can't be found
	 */
	public int getWordIdxAt(int nodeIdx, int currWord) {
		assert currWord >= -1;

		if (nodeIdx >= nNodes) {
			return -1;
		}

		int currWB = currWord < 0? -1: wordBoundaries[currWord];

		for (int w = currWord+1; w < nWords; w++) {
			int wb = wordBoundaries[w];
			if (wb < 0) {
				continue;
			}

			assert currWord < 0 || wb > currWB: "can't move backwards";

			if (wb > nodeIdx) {
				return currWord;
			}

			currWord = w;
			currWB = wb;
		}

		return currWord;
	}


	/** Returns the total number of nodes in the grammar. */
	public int getNodeCount() {
		return nNodes;
	}


	/**
	 * Parses a grammar rule and adds the corresponding states to the vector.
	 * The states are bound together as needed, effectively creating a graph.
	 * @param tokenIter iterator on rule tokens
	 * @param tails IDs of nodes that have no outbound transitions yet (they
	 *              are always the 3rd state in a phone). New nodes will be
	 *              chained to tail nodes. IMPORTANT: this set is modified by
	 *              this method! After the method has run, the set contains the
	 *              new tails in the graph.
	 * @return token an unknown token that stopped the parsing of the grammar,
	 * or null if the entire token stream has been parsed successfully
	 */
	private String parseRule(Iterator<String> tokenIter, Set<Integer> tails) {
		while (tokenIter.hasNext()) {
			String token = tokenIter.next();

			if (!NONPHONE_PATTERN.matcher(token).matches()) {
				// Compulsory 1-token path
				// Replace existing tails

				// Create the actual nodes
				int posNewState0 = insertionPoint;
				insertStateTriplet(token);

				// Bind tails to the 1st state that just got created
				for (Integer parentId: tails) {
					addOutboundTransition(posNewState0, parentId,
							UNINITIALIZED_LOG_PROBABILITY);
					// probability will be corrected in
					// fillUniformNonLoopTransitionProbabilities()
				}

				// New sole tail is 3rd state that just got inserted
				tails.clear();
				tails.add(posNewState0 + 2);
			}

			else if (token.equals("(")) {
				// Compulsory multiple choice path
				// Replace existing tails

				Set<Integer> tailsCopy = new HashSet<Integer>(tails);
				tails.clear();

				do {
					Set<Integer> newTails = new HashSet<Integer>(tailsCopy);
					token = parseRule(tokenIter, newTails);
					tails.addAll(newTails);
				} while (token.equals("|"));
				assert token.equals(")");
			}

			else if (token.equals("[")) {
				// Optional path
				// Append new tails to existing tails

				Set<Integer> subTails = new HashSet<Integer>(tails);
				token = parseRule(tokenIter, subTails);
				tails.addAll(subTails);
				assert token.equals("]");
			}

			else {
				return token;
			}
		}

		return null;
	}


	/**
	 * Convenience method to parse a rule from an array of tokens.
	 * See the main parseRule method for more information.
	 * @return token an unknown token that stopped the parsing of the grammar,
	 * or null if the entire token stream has been parsed successfully
	 */
	private String parseRule(String[] ruleTokens, Set<Integer> tails) {
		return parseRule(Arrays.asList(ruleTokens).iterator(), tails);
	}


	/**
	 * Creates a new outbound transition.
	 * @param dest arrival node
	 * @param src departure node
	 * @param p log probability
	 */
	private void addOutboundTransition(int dest, int src, float p) {
		outNode[src][outCount[src]] = dest;
		outProb[src][outCount[src]] = p;
		outCount[src]++;
	}


	/**
	 * Inserts three emitting HMM states (corresponding to a phone) as three
	 * nodes in the graph. Binds the three nodes together. The first node has
	 * no inbound transitions and the third node has no outbound transitions.
	 */
	private int insertStateTriplet(String phone) {
		pool.check(phone);

		LogMath lm = HMMModels.getLogMath();

		// add nodes for each state in the phone
		for (int i = 0; i < 3; i++) {
			int j = insertionPoint + i;

			int stateId = pool.getId(phone, i);
			nodeStates[j] = stateId;
			HMMState state = pool.get(stateId);
			assert state.isEmitting();

			HMMStateArc[] succ = state.getSuccessors();
			assert succ.length == 2: "HMMState must have two successors";
			assert linProbEq(1,
					lm.logToLinear(succ[0].getLogProbability()) +
					lm.logToLinear(succ[1].getLogProbability()))
					: "linear probabilities must sum to 1";

			HMMStateArc loop = succ[0].getHMMState() == state
					? succ[0]
					: succ[1];
			assert loop.getHMMState() == state: "can't find loop arc";
			HMMStateArc nonLoop = succ[loop==succ[0]? 1: 0];

			addOutboundTransition(j, j, loop.getLogProbability());

			if (i < 2) {
				addOutboundTransition(j+1, j, nonLoop.getLogProbability());
			}
		}

		insertionPoint += 3;
		return insertionPoint - 1;
	}


	/**
	 * Sets uniform probabilities on the non-looping transitions of a node.
	 * That is, every non-looping transition will be set to the same probability
	 * so that all outbound probabilities sum to one in the linear scale.
	 * (Reminder: internally, we use log probabilities, not linear.)
	 * <p/>
	 * The requested node's transition count ({@code outCount[nodeIdx]}) and
	 * the probability of looping ({@code outProb[nodeIdx][0]}) must be set
	 * prior to calling this method!
	 */
	protected void fillUniformNonLoopTransitionProbabilities(int nodeIdx) {
		int count = outCount[nodeIdx];
		float loopLogP = outProb[nodeIdx][0];

		assert count >= 2
				: "not linked to the rest of the graph";

		assert loopLogP != UNINITIALIZED_LOG_PROBABILITY
				: "loop probability must be initialized";

		LogMath lm = HMMModels.getLogMath();
		float p = lm.linearToLog(
				(1 - lm.logToLinear(loopLogP)) / (double) (count - 1));

		for (int i = 1; i < outCount[nodeIdx]; i++) {
			assert outProb[nodeIdx][i] == UNINITIALIZED_LOG_PROBABILITY
					: "non-loop probabilities must be uninitialized";
			outProb[nodeIdx][i] = p;
		}
	}


	protected void correctLastNodeTransitions() {
		int last = nNodes - 1;
		assert outNode[last][0] == last;
		outCount[last] = 1;
		/* DON'T set the loop probability of the last node to 1 (linear)!
		Intuitively, it would make sense to do so, but Viterbi will make sure
		not to go past the last node anyway. Leaving outProb as is for the last
		node is required by StatePath's concatenation operator (to string
		several paths together properly). */
	}


	/**
	 * Checks the legality of all transitions in the graph.
	 * @throws IllegalStateException if any transition is illegal
	 */
	protected void checkTransitions() {
		LogMath lm = HMMModels.getLogMath();

		for (int n = 0; n < nNodes-1; n++) {
			if (outCount[n] < 2) {
				throw new IllegalStateException(
						"node #" + n + " isolated from graph");
			}

			if (outNode[n][0] != n) {
				throw new IllegalStateException(
						"node #" + n + "'s first transition should be a loop");
			}

			// forbid backwards transitions
			for (int t = 0; t < outCount[n]; t++) {
				if (outNode[n][t] < n) {
					throw new IllegalStateException("illegal backwards " +
							"transition from node " + n + " to " + outNode[n][t]);
				}
			}

			float sum = 0;
			for (int t = 0; t < outCount[n]; t++) {
				sum += lm.logToLinear(outProb[n][t]);
			}

			if (!linProbEq(1, sum)) {
				throw new IllegalStateException(String.format(
						"linear probabilities of outbound transitions of " +
						"node #%d do not sum to one (sum=%f)", n, sum));
			}
		}

		// Special case for the last node. Don't check the sum of its transition
		// probabilities (see correctLastNodeTransitions() to learn why)
		int last = nNodes - 1;
		if (outCount[last] != 1 || outNode[last][0] != last) {
			throw new IllegalStateException("last node must have exactly " +
					"1 transition, i.e. a loop on itself");
		}
	}


	/**
	 * Constructs a state graph from words, and the rules associated with each
	 * of them.
	 * @param words an array of words
	 * @param rules a 2D array of rule tokens. The first dimension maps to the
	 *              index of the word corresponding to the rule.
	 * @param interWordSilences insert optional silences between each word
	 */
	public StateGraph(
			StatePool pool,
			String[][] rules,
			List<Word> words,
			boolean interWordSilences)
	{
		this.words = words;
		this.pool = pool;

		nWords  = words.size();
		int nPhones = countPhones(rules, interWordSilences);
		nNodes  = 3 * nPhones;

		wordBoundaries = new int[nWords];

		nodeStates = new int  [nNodes];
		outCount   = new byte [nNodes];
		outNode    = new int  [nNodes][MAX_TRANSITIONS];
		outProb    = new float[nNodes][MAX_TRANSITIONS];

		for (int i = 0; i < nNodes; i++) {
			Arrays.fill(outProb[i], UNINITIALIZED_LOG_PROBABILITY);
		}

		//----------------------------------------------------------------------
		// Build state graph

		Set<Integer> tails = new HashSet<Integer>();

		// add initial mandatory silence
		parseRule(SILENCE_RULE, tails);

		int nonEmptyRules = 0;

		for (int i = 0; i < nWords; i++) {
			if (null == rules[i]) {
				System.err.println("Skipping word without a rule: " + words.get(i));
				wordBoundaries[i] = -1;
				continue;
			}

			if (interWordSilences && nonEmptyRules > 0) {
				// optional silence between two words
				parseRule(OPT_SILENCE_RULE, tails);
			}

			// Word actually starts after optional silence
			wordBoundaries[i] = insertionPoint;

			String token = parseRule(rules[i], tails);
			assert token == null : "rule couldn't be parsed entirely";

			nonEmptyRules++;
		}

		// add final mandatory silence
		parseRule(SILENCE_RULE, tails);

		//----------------------------------------------------------------------
		// All nodes have been inserted

		assert insertionPoint == nNodes : "predicted node count not met : "
				+ "actual " + insertionPoint + ", expected " + nNodes;

		correctLastNodeTransitions();

		// correct inter-phone transition probabilities
		for (int i = 2; i < nNodes-1; i += 3) {
			fillUniformNonLoopTransitionProbabilities(i);
		}

		checkTransitions();
	}


	/**
	 * Empty constructor.
	 * Subclasses are responsible for initializing everything!
	 */
	protected StateGraph() {
	}


	/**
	 * Constructs a state graph from an array of words.
	 * Rules will be looked up in the standard grammar.
	 */
	public StateGraph(StatePool pool, List<Word> words) {
		this(pool, getRules(words), words, true);
	}


	/**
	 * Copy constructor.
	 * Fields are deep-copied except for `words` and `pool`.
	 */
	public StateGraph(StateGraph graph) {
		pool = graph.pool;
		nNodes = graph.nNodes;
		nWords = graph.nWords;
		insertionPoint = graph.insertionPoint;

		nodeStates = Arrays.copyOf(graph.nodeStates, nNodes);
		outCount = Arrays.copyOf(graph.outCount, nNodes);

		outNode = new int[nNodes][];
		outProb = new float[nNodes][];

		for (int i = 0; i < nNodes; i++) {
			outNode[i] = Arrays.copyOf(graph.outNode[i], MAX_TRANSITIONS);
			outProb[i] = Arrays.copyOf(graph.outProb[i], MAX_TRANSITIONS);
		}

		words = new ArrayList<>(graph.words);
		wordBoundaries = Arrays.copyOf(graph.wordBoundaries, nWords);
	}


	/**
	 * Constructs a state graph for easy testing from whitespace-separated
	 * words. Rules will be looked up in the standard grammar. Uses an
	 * independent state pool.
	 */
	public static StateGraph quick(String text) {
		List<Word> words = new ArrayList<>();
		for (String str: trimSplit(text)) {
			words.add(new Word(str));
		}
		return new StateGraph(new StatePool(), words);
	}


	/**
	 * Dumps a GraphViz/DOT representation of the vector.
	 */
	public void dumpDot(Writer w) throws IOException {
		LogMath lm = HMMModels.getLogMath();

		w.write("digraph {");

		for (int i = 0; i < nNodes; i++) {
			w.write(String.format("\nnode%d [ label=\"%s %d\" ]", i,
					getPhoneAt(i), getStateAt(i).getState()));
			for (byte j = 0; j < outCount[i]; j++) {
				w.write(String.format("\nnode%d -> node%d [ label=%f ]",
						i, outNode[i][j], lm.logToLinear(outProb[i][j])));
			}
		}

		w.write("\n}");
		w.flush();
	}


	/**
	 * Converts outbound transitions to inbound transitions.
	 * Mainly useful for Viterbi.
	 */
	protected class InboundTransitionBridge {
		final int[]     inCount = new int  [nNodes];
		final int[][]   inNode  = new int  [nNodes][MAX_TRANSITIONS];
		final float[][] inProb  = new float[nNodes][MAX_TRANSITIONS];

		InboundTransitionBridge() {
			// force loop as first transition
			for (int n = 0; n < nNodes; n++) {
				inCount[n] = 1;
				inNode[n][0] = n;
				inProb[n][0] = outProb[n][0];
				Arrays.fill(inProb[n], 1, inProb[n].length,
						UNINITIALIZED_LOG_PROBABILITY);
			}

			// fill non-loop transitions
			for (int n = 0; n < nNodes; n++) {
				assert outNode[n][0] == n;
				for (int t = 1; t < outCount[n]; t++) {
					int in = outNode[n][t];
					int it = inCount[in];
					inNode[in][it] = n;
					inProb[in][it] = outProb[n][t];
					inCount[in]++;
				}
			}
		}
	}


	/**
	 * Finds the most likely predecessor of each node for each audio frame
	 * (using the Viterbi algorithm).
	 *
	 * Each iteration builds upon the likelihoods found in the previous
	 * iteration, as well as the score given by every HMM state for each audio
	 * frame.
	 *
	 * After the last iteration, we obtain a full table of the most likely
	 * predecessors of each state and for each frame. Since there is only one
	 * possible final state, this table tells us which state most likely
	 * transitioned to the final state in the second-to-last frame. From there,
	 * we can find the most likely predecessor of *that* state in the
	 * third-to-last frame... and so on and so forth until we have traced the
	 * most likely path back to the initial state.
	 *
	 * All this method actually does is computing the likelihoods for each frame
	 * and storing them in a swap file. The pathfinding process is completed by
	 * backtrack().
	 *
	 * @see StateGraph#backtrack second part of the pathfinding process
	 * @see SwapInflater
	 * @param data all frames in the audio source
	 * @param swapWriter object that commits likelihoods to a swap file
	 *                   or buffer
	 * @param startFrame first frame to analyze
	 * @param endFrame last frame to analyze
	 */
	public void viterbi(
			List<FloatData> data,
			SwapDeflater swapWriter,
			int startFrame,
			int endFrame)
			throws IOException, InterruptedException
	{
		if (endFrame >= data.size()) {
			throw new IllegalArgumentException("endFrame >= data.size()");
		}

		assert startFrame <= endFrame;
		assert startFrame >= 0;
		assert endFrame >= 0;

		int frameCount = 1 + endFrame - startFrame;

		InboundTransitionBridge in = new InboundTransitionBridge();

		// Probability vectors
		float[] vpf = new float[nNodes]; // vector for previous frame (read-only)
		float[] vcf = new float[nNodes]; // vector for current frame (write-only)

		// ID of the incoming transition that yielded bestReachProb for each state
		byte[] bestInTrans = new byte[nNodes];

		// Initialize probability vector
		// We only have one initial node (node #0), probability 1
		Arrays.fill(vpf, Float.NEGATIVE_INFINITY);
		vpf[0] = 0; // Probabilities are in the log domain

		for (int f = startFrame; f <= endFrame; f++) {
			if (progress != null) {
				progress.setProgress(String.format(
						"Viterbi forward pass: frame %d of %d (deflated swap: %d MB)",
						f-startFrame,
						frameCount,
						swapWriter.getIndex().getCompressedBytes() / 1024 / 1024),
						(float) (f-startFrame) / (float) frameCount);
			}

			for (int i = 0; i < nNodes; i++) {
				// Emission probability (frame score)
				// We could cache this for unique states, but in practice
				// ScoreCachingSenone already does it for us.
				float emission = getStateAt(i).getScore(data.get(f));

				assert in.inCount[i] >= 1;

				// Probability to reach a node given the previous vector v
				// i.e. max(P(k -> i) * v[k]) for each predecessor k of node #i
				float bestReachProb;

				// Initialize with first incoming transition
				// If last node, loop forever (log prob 0).
				// (Please see correctLastNodeTransitions() for an explanation
				// of why the last node's log prob isn't just set to 0.)
				bestReachProb = (i == nNodes-1? 0: in.inProb[i][0])
						+ vpf[in.inNode[i][0]]; // log domain
				bestInTrans[i] = 0;

				// Find best probability among all incoming transitions
				for (byte j = 1; j < in.inCount[i]; j++) {
					float p = in.inProb[i][j] + vpf[in.inNode[i][j]]; // log domain
					if (p > bestReachProb) {
						bestReachProb = p;
						bestInTrans[i] = j;
					}
				}

				vcf[i] = emission + bestReachProb; // log domain
			}

			swapWriter.write(bestInTrans);

			// swap vectors
			float[] temp = vcf;
			vcf = vpf;
			vpf = temp;
		}

		swapWriter.close();
	}


	/**
	 * Finds the most likely path between the initial and final nodes using the
	 * table of most likely predecessors found by viterbi().
	 *
	 * @see StateGraph#viterbi first part of the pathfinding process
	 * @param swapReader reader for the swap file produced by viterbi()
	 * @return A time line of the most likely node at each frame. Given as an
	 * array of node IDs, with array indices being frame numbers relative to
	 * the first frame given to StateGraph#viterbi.
	 */
	public int[] backtrack(SwapInflater swapReader) throws IOException {
		InboundTransitionBridge in = new InboundTransitionBridge();

		int leadNode = nNodes - 1;
		int[] timeline = new int[swapReader.getFrameCount()];
		for (int f = timeline.length-1; f >= 0; f--) {
			byte transID = swapReader.getIncomingTransition(f, leadNode);
			leadNode = in.inNode[leadNode][transID];
			timeline[f] = leadNode;
			assert leadNode >= 0;

			if (progress != null) {
				progress.setProgress("Viterbi backward pass: frame " + f,
						((float)f/(float)(timeline.length-1)));
			}
		}

		return timeline;
	}


	private void prettyPrintTimeline(int[] timeline) {
		System.out.println("Note: only initial states are shown below");
		System.out.println("    TIME   STATE#     UNIT");
		for (int f = 0; f < timeline.length; f++) {
			if (f == 0 || timeline[f-1]/3 != timeline[f]/3) {
				System.out.println(String.format("%8.2f %8d %8s",
						TimeConverter.frame2sec(f),
						timeline[f],
						getPhoneAt(timeline[f])));
			}
		}

		System.out.println("\n    TIME         WORD       BOUNDARY");
		int pw = -1;
		int w = -1;
		for (int f = 0; f < timeline.length; f++) {
			w = getWordIdxAt(timeline[f], w);
			if (w != pw) {
				System.out.println(String.format("%8.2f %16s %8d",
						TimeConverter.frame2sec(f),
						words.get(w),
						wordBoundaries[w]));
				pw = w;
			}
		}
	}


	public void setWordAlignments(
			int[] timeline,
			int offset)
	{
		for (Word w: words) {
			w.clearAlignment();
		}

		int cw = -1;              // current word idx
		int pn = -1;              // previous node idx
		Word word = null;         // current word
		Word.Phone phone = null;  // current phone

		for (int f = 0; f < timeline.length; f++) {
			int cn = timeline[f]; // current node idx
			int now = offset+f; // absolute frame number

			int pw = cw; // previous word idx
			cw = getWordIdxAt(cn, cw);

			if (cw != pw) {
				word = words.get(cw);
				word.setSegment(now, now);
			} else if (null != word) {
				word.getSegment().setEndFrame(now);
			}

			if (f == 0 || pn/3 != cn/3) {
				if (null != word) {
					phone = new Word.Phone(
							getPhoneAt(cn), new Word.Segment(now, now));
					word.addPhone(phone);
				}
				pn = cn;
			} else if (null != phone) {
				phone.getSegment().setEndFrame(now);
			}
		}
	}


	/**
	 * Returns true if there is only one possible sequence of nodes (path).
	 * In other words, a graph is linear if all nodes transition to no more than
	 * one other node besides themselves.
	 * Note: looping on the same node is not considered to alter the linearity
	 * of the graph.
	 */
	public boolean isLinear() {
		for (int i = 0; i < nNodes; i++) {
			assert outCount[i] > 0: "must have at least one transition (loop)";
			assert outNode[i][0] == i: "first transition must be a loop";

			if (outCount[i] > 2) {
				return false;
			}
		}

		return true;
	}

}
