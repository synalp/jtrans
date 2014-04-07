package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.linguist.acoustic.*;
import edu.cmu.sphinx.util.LogMath;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.facade.Cache;
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


	/** Pool of unique HMM states. */
	private final StatePool pool;


	/**
	 * All nodes in the grammar.
	 * Each node points to a unique HMM state ID.
	 * There are 3 HMM states per phone, hence 3 nodes per phone.
	 */
	private int[] nodeStates;

	/**
	 * Number of incoming transitions for each node.
	 * Values in this array should not exceed MAX_TRANSITIONS, otherwise an
	 * index out of bounds exception will eventually be thrown.
	 */
	private final byte[] inCount;

	/**
	 * Node IDs for each incoming transition.
	 * The first entry is *always* the same state (loop).
	 */
	private final int[][] inNode;

	/**
	 * Probability of each incoming transition.
	 * The first entry is *always* the probability of looping on the same state.
	 */
	private final float[][] inProb;

	/** Total number of non-empty phones contained in the grammar. */
	private final int nPhones;

	/** Total number of nodes in the grammar. */
	private final int nNodes;

	/** Insertion point for new nodes in the nodeStates array. */
	private int insertionPoint;

	/** Human-readable words at the basis of this grammar */
	private final String[] words;

	/**
	 * Indices of the initial node of each word (i.e. that points to the first
	 * HMM state of the first phone of the word).
	 * All potential pronunciations for a given word are inserted contiguously
	 * in the nodes array. The order in which the nodes are inserted
	 * reflects the order of the words.
	 */
	private final int[] wordBoundaries;

	private final int[] wordSpeakers;

	/** Used to report progress in viterbi() and backtrack() (may be null) */
	private ProgressDisplay progress = null;


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


	/**
	 * Creates grammar rules from a list of words using the standard
	 * grammatizer.
	 * @param words array of whitespace-trimmed words
	 * @return a 2D array of rule tokens (1st dimension corresponds to word
	 * indices). If a word can't be processed, its rule is set to null.
	 */
	public static String[][] getRules(String[] words) {
		String[][] rules = new String[words.length][];
		Grammatiseur gram = Grammatiseur.getGrammatiseur();

		for (int i = 0; i < words.length; i++) {
			String rule = gram.getGrammar(words[i]);

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
	public static int countPhones(String[][] rules) {
		// Mandatory final & initial silence
		int count = 2;

		for (String[] ruleTokens: rules) {
			if (null == ruleTokens)
				continue;

			// Optional silence between each word
			if (count > 2)
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


	public int getUniqueStateIdAt(int nodeIdx) {
		return nodeStates[nodeIdx];
	}


	public String getPhoneAt(int nodeIdx) {
		return getStateAt(nodeIdx).getHMM().getBaseUnit().getName();
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
					addIncomingTransition(posNewState0, parentId);
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
	 * Creates a new incoming transition, but does not set its probability.
	 * @param dest arrival node
	 * @param src departure node
	 */
	private void addIncomingTransition(int dest, int src) {
		assert inCount[dest] < MAX_TRANSITIONS - 1: "too many transitions";
		inNode[dest][inCount[dest]++] = src;
	}


	/**
	 * Inserts three emitting HMM states (corresponding to a phone) as three
	 * nodes in the graph. Binds the three nodes together. The first node has
	 * no inbound transitions and the third node has no outbound transitions.
	 */
	private int insertStateTriplet(String phone) {
		pool.check(phone);

		// add nodes for each state in the phone
		for (int i = 0; i < 3; i++) {
			int j = insertionPoint + i;

			int stateId = pool.getId(phone, i);
			nodeStates[j] = stateId;
			HMMState state = pool.get(stateId);

			addIncomingTransition(j, j);
			if (i > 0) {
				addIncomingTransition(j, j-1);
			}

			for (HMMStateArc arc: state.getSuccessors()) {
				HMMState arcState = arc.getHMMState();
				if (i == 2 && arcState.isExitState())
					continue;

				float p = arc.getLogProbability();
				if (arcState == state) {
					inProb[j][0] = p;
				} else {
					inProb[j+1][1] = p;
				}
			}
		}

		insertionPoint += 3;
		return insertionPoint - 1;
	}


	/**
	 * Sets uniform inter-phone probabilities on the first state of an HMM.
	 * @param nodeId ID of the first state of an HMM
	 */
	private void setUniformInterPhoneTransitionProbabilities(int nodeId) {
		assert nodeId % 3 == 0 : "not a first state (i.e. multiple of 3)";
		assert nodeId > 0 : "node #0 doesn't have any incoming transitions";
		assert inCount[nodeId] >= 2 : "not linked to the rest of the graph";
		assert inProb[nodeId][0] != 0f : "loop probability must be initialized";
		assert inProb[nodeId][1] == 0f : "non-loop probabilities must be uninitialized";

		LogMath lm = HMMModels.getLogMath();
		double linearLoopProb = lm.logToLinear(inProb[nodeId][0]);
		float p = lm.linearToLog(
				(1f - linearLoopProb) / (double)(inCount[nodeId] - 1));

		for (byte j = 1; j < inCount[nodeId]; j++)
			inProb[nodeId][j] = p;
	}


	/**
	 * Constructs a state graph from words, and the rules associated with each
	 * of them.
	 * @param words an array of words
	 * @param rules a 2D array of rule tokens. The first dimension maps to the
	 *              index of the word corresponding to the rule.
	 */
	public StateGraph(
			StatePool pool,
			String[][] rules,
			String[] words,
			int[] speakers)
	{
		this.words = words;
		this.pool = pool;

		wordBoundaries = new int[words.length];
		wordSpeakers = speakers;

		nPhones = countPhones(rules);
		nNodes  = 3 * nPhones;

		nodeStates = new int  [nNodes];
		inCount    = new byte [nNodes];
		inNode     = new int  [nNodes][MAX_TRANSITIONS];
		inProb     = new float[nNodes][MAX_TRANSITIONS];

		//----------------------------------------------------------------------
		// Build state graph

		Set<Integer> tails = new HashSet<Integer>();

		// add initial mandatory silence
		parseRule(SILENCE_RULE, tails);

		int nonEmptyRules = 0;

		for (int i = 0; i < words.length; i++) {
			if (null == rules[i]) {
				System.err.println("Skipping word without a rule: " + words[i]);
				wordBoundaries[i] = -1;
				continue;
			}

			if (nonEmptyRules > 0) {
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
		// All node have been inserted

		assert insertionPoint == nNodes : "predicted node count not met : "
				+ "actual " + insertionPoint + ", expected " + nNodes;
		assert inCount[0] == 1 : "first node can only have 1 incoming transition";

		// correct inter-phone transition probabilities
		for (int i = 3; i < nNodes; i += 3) {
			setUniformInterPhoneTransitionProbabilities(i);
		}

		// last state can loop forever
		inProb[nNodes -1][0] = 0f; // log domain
	}


	/**
	 * Constructs a state graph from an array of words.
	 * Rules will be looked up in the standard grammar.
	 */
	public StateGraph(StatePool pool, String[] words, int[] speakers) {
		this(pool, getRules(words), words, speakers);
	}


	/**
	 * Constructs a state graph for easy testing from whitespace-separated
	 * words. Rules will be looked up in the standard grammar. Uses an
	 * independent state pool.
	 */
	public static StateGraph quick(String text) {
		String[] words = trimSplit(text);
		int[] speakers = new int[words.length];
		return new StateGraph(new StatePool(), words, speakers);
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
			for (byte j = 0; j < inCount[i]; j++) {
				w.write(String.format("\nnode%d -> node%d [ label=%f ]",
						inNode[i][j], i, lm.logToLinear(inProb[i][j])));
			}
		}

		w.write("\n}");
		w.flush();
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

				assert inCount[i] >= 1;

				// Probability to reach a node given the previous vector v
				// i.e. max(P(k -> i) * v[k]) for each predecessor k of node #i
				float bestReachProb;

				// Initialize with first incoming transition
				bestReachProb = inProb[i][0] + vpf[inNode[i][0]]; // log domain
				bestInTrans[i] = 0;

				// Find best probability among all incoming transitions
				for (byte j = 1; j < inCount[i]; j++) {
					float p = inProb[i][j] + vpf[inNode[i][j]]; // log domain
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
		int leadNode = nNodes - 1;
		int[] timeline = new int[swapReader.getFrameCount()];
		for (int f = timeline.length-1; f >= 0; f--) {
			byte transID = swapReader.getIncomingTransition(f, leadNode);
			leadNode = inNode[leadNode][transID];
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
			int frameState = timeline[f];
			while (w+1 < words.length && wordBoundaries[w+1] < frameState) {
				w++;
			}
			if (w != pw) {
				System.out.println(String.format("%8.2f %16s %8d",
						TimeConverter.frame2sec(f),
						words[w],
						wordBoundaries[w]));
				pw = w;
			}
		}
	}


	public void setWordAlignments(
			List<Word> alignable,
			int[] timeline,
			int offset)
	{
		assert alignable.size() == wordBoundaries.length;

		for (Word w: alignable) {
			w.clearAlignment();
		}

		int cw = -1;              // current word idx
		int ps = -1;              // previous state idx
		Word word = null;         // current word
		Word.Phone phone = null;  // current phone

		for (int f = 0; f < timeline.length; f++) {
			int cs = timeline[f]; // current state idx
			int now = offset+f; // absolute frame number

			int pw = cw; // previous word idx
			while (cw+1 < words.length && wordBoundaries[cw+1] <= cs) {
				cw++;
			}

			if (cw != pw) {
				word = alignable.get(cw);
				word.setSegment(now, now);
			} else if (null != word) {
				word.getSegment().setEndFrame(now);
			}

			if (f == 0 || ps/3 != cs/3) {
				if (null != word) {
					phone = new Word.Phone(
							getPhoneAt(cs), new Word.Segment(now, now));
					word.addPhone(phone);
				}
				ps = cs;
			} else if (null != phone) {
				phone.getSegment().setEndFrame(now);
			}
		}
	}


	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("USAGE: StateGraph <SOUNDFILE.WAV> <\"transcription\">");
			System.exit(1);
		}

		final String wavpath = args[0];
		final String words = new Scanner(new File(args[1])).useDelimiter("\\Z")
				.next().replaceAll("[\\n\\r\u001f]", " ");

		StateGraph gv = StateGraph.quick(words);
		System.out.println("PHONE COUNT: " + gv.nPhones);
		System.out.println("GRAPH SIZE: " + gv.nNodes);
		gv.dumpDot(new FileWriter("grammar_vector.dot"));

		System.out.println("Starting Viterbi...");

		File swapFile = Cache.getCacheFile("backtrack", "swp", words);
		File indexFile = Cache.getCacheFile("backtrack", "idx", words);

		boolean quick = false;
		PageIndex index;

		if (!quick) {
			long t0 = System.currentTimeMillis();
			SwapDeflater swapper = SwapDeflater.getSensibleSwapDeflater(true);
			swapper.init(gv.nNodes, new FileOutputStream(swapFile));
			gv.viterbi(S4mfccBuffer.getAllData(new File(wavpath)), swapper, 0, -1);
			System.out.println("VITERBI TOOK " + (System.currentTimeMillis()-t0)/1000L + " SECONDS");
			index = swapper.getIndex();
			index.serialize(new FileOutputStream(indexFile));
		} else {
			index = PageIndex.deserialize(new FileInputStream(indexFile));
		}

		System.out.println("FRAME COUNT: " + index.getFrameCount());

		System.out.println("Backtracking...");
		SwapInflater swapReader = new SwapInflater();
		swapReader.init(index, swapFile);
		int[] timeline = gv.backtrack(swapReader);
		gv.prettyPrintTimeline(timeline);
	}

}
