package fr.loria.synalp.jtrans.anchorless;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.linguist.acoustic.*;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import fr.loria.synalp.jtrans.speechreco.grammaire.Grammatiseur;
import fr.loria.synalp.jtrans.speechreco.s4.*;
import fr.loria.synalp.jtrans.utils.TimeConverter;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Sweeping grammar vector for the experimental anchorless algorithm.
 */
public class GrammarVector {

	/**
	 * If this value ever has to exceed 255 (overkill!), be sure to change
	 * the type of the nTrans array.
	 */
	public static final int MAX_TRANSITIONS = 10;

	/** All HMM states in the grammar.
	 * TODO: speed up scoring with a set of unique states */
	private HMMState[] states;

	/** Number of transitions for each state */
	private byte[] nTrans;

	/** Transition matrix: successor IDs */
	private int[][] succ;

	/** Transition matrix: probabilities */
	private float[][] prob;

	/** Total number of non-empty phones contained in the grammar. */
	private final int nPhones;

	/** Total number of HMM states in the grammar. */
	private final int nStates;

	/** Insertion point for new states in the states array. */
	private int insertionPoint;

	/** Pattern for non-phone grammar tokens. */
	public final static Pattern NONPHONE_PATTERN =
			Pattern.compile("^[^a-zA-Z]$");


	/**
	 * Trims leading and trailing whitespace then splits around whitespace.
	 */
	public static String[] trimSplit(String text) {
		return text.trim().split("\\s+");
	}


	/**
	 * Counts phones in a list of words.
	 */
	public static int countPhones(List<String> words) {
		// mandatory initial and final silences
		// plus one optional silence between each word
		int count = 1 + words.size();

		for (String w: words) {
			String rule = Grammatiseur.getGrammatiseur().getGrammar(w);
			for (String token: trimSplit(rule))
				if (!NONPHONE_PATTERN.matcher(token).matches())
					count++;
		}

		return count;
	}


	/**
	 * Parses a grammar rule and adds the corresponding states to the vector.
	 * The states are bound together as needed, effectively creating a graph.
	 * @param tokenIter iterator on rule tokens
	 * @param tails IDs of states that have no outbound transitions yet (they
	 *              are always the 3rd state in a phone). New states will be
	 *              chained to tail states. IMPORTANT: this set is modified by
	 *              this method! After the method has run, the set contains the
	 *              new tails in the graph.
	 * @return token an unknown token that stopped the parsing of the grammar,
	 * or null if the entire token stream has been parsed successfully
	 */
	private String parseRule(
			Iterator<String> tokenIter,
			Set<Integer> tails,
			AcousticModel acMod,
			UnitManager unitMgr)
	{
		while (tokenIter.hasNext()) {
			String token = tokenIter.next();

			if (!NONPHONE_PATTERN.matcher(token).matches()) {
				// Compulsory 1-token path
				// Replace existing tails

				String phone = PhoneticForcedGrammar.convertPhone(token);

				// Bind tails to the 1st state that is going to be created
				for (Integer parentId: tails) {
					succ[parentId][nTrans[parentId]++] = insertionPoint;
				}

				// New sole tail is future 3rd state
				tails.clear();
				tails.add(insertionPoint + 2);

				// Create the actual states
				insertStateTriplet(phone, acMod, unitMgr);
			}

			else if (token.equals("(")) {
				// Compulsory multiple choice path
				// Replace existing tails

				Set<Integer> tailsCopy = new HashSet<Integer>(tails);
				tails.clear();

				do {
					Set<Integer> newTails = new HashSet<Integer>(tailsCopy);
					token = parseRule(tokenIter, newTails, acMod, unitMgr);
					tails.addAll(newTails);
				} while (token.equals("|"));
				assert token.equals(")");
			}

			else if (token.equals("[")) {
				// Optional path
				// Append new tails to existing tails

				Set<Integer> subTails = new HashSet<Integer>(tails);
				token = parseRule(tokenIter, subTails, acMod, unitMgr);
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
	 * Convenience method to parse a rule from a string.
	 * See the main parseRule method for more information.
	 */
	private String parseRule(
			String rule,
			Set<Integer> tails,
			AcousticModel acMod,
			UnitManager unitMgr)
	{
		return parseRule(
				Arrays.asList(trimSplit(rule)).iterator(),
				tails,
				acMod,
				unitMgr);
	}


	/**
	 * Inserts three emitting HMM states corresponding to a phone.
	 * Binds the three states together. The first state has no inbound
	 * transitions and the third state has no outbound transitions.
	 */
	private int insertStateTriplet(
			String phone,
			final AcousticModel acMod,
			final UnitManager unitMgr)
	{
		System.out.println("inserting state triplet for '" + phone + "'");

		// find HMM for this phone
		HMM hmm = acMod.lookupNearestHMM(
				unitMgr.getUnit(phone), HMMPosition.UNDEFINED, false);

		// add phone states
		for (int i = 0; i < 3; i++) {
			int j = insertionPoint + i;
			HMMState state = hmm.getState(i);

			assert state.isEmitting();
			assert !state.isExitState();
			assert state.getSuccessors().length == 2;

			states[j] = state;
			succ[j][0] = j;
			if (i < 2) {
				succ[j][1] = j+1;
				nTrans[j] = 2;
			} else {
				nTrans[j] = 1;
			}

			for (HMMStateArc arc: state.getSuccessors()) {
				HMMState arcState = arc.getHMMState();
				if (i == 2 && arcState.isExitState())
					continue;

				float p = arc.getLogProbability();
				if (arcState == state) {
					prob[j][0] = p;
				} else {
					assert i != 2;
					assert !arcState.isExitState();
					assert arcState == hmm.getState(i+1);
					prob[j][1] = p;
				}
			}
		}

		insertionPoint += 3;
		return insertionPoint - 1;
	}


	/**
	 * Sets uniform inter-phone probabilities on the last state of an HMM.
	 * @param stateId ID of the last (3rd) state of an HMM
	 */
	private void setUniformInterPhoneTransitionProbabilities(int stateId) {
		assert stateId % 3 == 2 : "must be a third state";

		if (nTrans[stateId] < 2)
			return;

		assert prob[stateId][0] != 0f : "loop probability can't be 0";
		assert prob[stateId][1] == 0f : "non-loop probabilities must be 0";

		LogMath lm = HMMModels.getLogMath();
		double linearLoopProb = lm.logToLinear(prob[stateId][0]);
		float p = lm.linearToLog(
				(1f - linearLoopProb) / (double)(nTrans[stateId] - 1));

		for (byte j = 1; j < nTrans[stateId]; j++)
			prob[stateId][j] = p;
	}


	/**
	 * Constructs a grammar vector from a list of words.
	 */
	public GrammarVector(
			List<String> words,
			AcousticModel acMod,
			UnitManager unitMgr)
	{
		nPhones = countPhones(words);
		nStates = 3 * nPhones;

		states = new HMMState[nStates];
		nTrans = new byte    [nStates];
		succ   = new int     [nStates][MAX_TRANSITIONS];
		prob   = new float   [nStates][MAX_TRANSITIONS];

		//----------------------------------------------------------------------
		// Build state graph

		Grammatiseur gram = Grammatiseur.getGrammatiseur();
		Set<Integer> tails = new HashSet<Integer>();
		boolean firstWord = true;

		// add initial mandatory silence
		parseRule("SIL", tails, acMod, unitMgr);

		// TODO: setUniformInterPhoneTransitionProbabilities

		for (String w: words) {
			if (firstWord) {
				firstWord = false;
			} else {
				// optional silence between two words
				parseRule("[ SIL ]", tails, acMod, unitMgr);
			}

			String rule = gram.getGrammar(w);
			System.out.println("Rule: " + rule);
			assert rule != null;
			assert !rule.isEmpty();

			String token = parseRule(rule, tails, acMod, unitMgr);
			assert token == null;
		}

		// add final mandatory silence
		parseRule("SIL", tails, acMod, unitMgr);

		assert insertionPoint == nStates;
	}


	/**
	 * Constructs a grammar vector from a piece of text.
	 * @param text words separated by spaces
	 */
	public GrammarVector(String text, AcousticModel acMod, UnitManager unitMgr)
	{
		this(Arrays.asList(trimSplit(text)), acMod, unitMgr);
	}


	/**
	 * Dumps a GraphViz/DOT representation of the vector.
	 */
	public void dumpDot(Writer w) throws IOException {
		LogMath lm = HMMModels.getLogMath();

		w.write("digraph {");

		for (int i = 0; i < nStates; i++) {
			HMMState s = states[i];
			w.write(String.format("\nnode%d [ label=\"%s %d\" ]", i,
					s.getHMM().getBaseUnit().getName(), s.getState()));
			for (byte j = 0; j < nTrans[i]; j++) {
				w.write(String.format("\nnode%d -> node%d [ label=%f ]",
						i, succ[i][j], lm.logToLinear(prob[i][j])));
			}
		}

		w.write("\n}");
		w.flush();
	}


	/**
	 * @return an array containing the best state for each frame
	 */
	public int[] viterbi(S4mfccBuffer mfcc) {
		float[] pv         = new float[nStates]; // previous vector
		float[] cv         = new float[nStates]; // current vector

		// Emission probability (frame score)
		float[] pEmission  = new float[nStates];

		// Probability to reach a state given the previous vector
		// max(pTransition(parent->s) * pv[s]) for each parent of state 's'
		float[] pReachMax  = new float[nStates];

		// State that yielded pReachMax for each state
		int  [] bestParent = new int  [nStates];

		Deque<int[]> backtrack = new ArrayDeque<int[]>();

		//----------------------------------------------------------------------

		// Initialize vector
		// We only have one initial state (state #0), probability 1
		// Probabilities are in the log domain
		Arrays.fill(cv, Float.NEGATIVE_INFINITY);
		cv[0] = 0;

		for (int f = 0; !mfcc.noMoreFramesAvailable; f++) {
			Data frame = mfcc.getData();
			if (frame instanceof DataStartSignal || frame instanceof DataEndSignal)
				continue;

			// Score frame according to each state in the vector
			System.out.println("Scoring Frame: " + f);
			for (int i = 0; i < nStates; i++)
				pEmission[i] = states[i].getScore(frame);

			// TODO these fills may not be very efficient
			Arrays.fill(pReachMax, Float.NEGATIVE_INFINITY);
			Arrays.fill(bestParent, -1);

			for (int parent = 0; parent < nStates; parent++) {
				// TODO: skip if pv[parent] is negative infinity?
				for (byte snt = 0; snt < nTrans[parent]; snt++) {
					int s = succ[parent][snt];
					float pReach = prob[parent][snt] + pv[parent]; // log domain
					if (pReach > pReachMax[s]) {
						pReachMax[s] = pReach;
						bestParent[s] = parent;
					}
				}
			}

			for (int s = 0; s < nStates; s++) {
				cv[s] = pEmission[s] + pReachMax[s]; // log domain
			}

			float[] recycled = pv;
			pv = cv;
			cv = recycled; // Avoid creating new arrays, recycle old pv as cv

			int[] bestParentCopy = new int[nStates];
			System.arraycopy(bestParent, 0, bestParentCopy, 0, nStates);
			backtrack.add(bestParentCopy);
		}

		for (int s = 0; s < nStates; s++) {
			System.out.println("CV[" + s + "] " + cv[s]);
		}

		System.out.println("Backtracking...");
		System.out.println(String.format(
				"Appx. footprint of backtrack stack: %dKB",
				backtrack.size() * (8+4+nStates*4) / 1024));
		System.out.println(Integer.SIZE);

		int pathLead = nStates-1;
		int[] timeline = new int[backtrack.size()];
		while (!backtrack.isEmpty()) {
			pathLead = backtrack.pop()[pathLead];
			timeline[backtrack.size()] = pathLead;
		}

		System.out.println("Note: only initial states are shown below");
		System.out.println("    TIME   STATE#     UNIT");
		for (int i = 0; i < timeline.length; i++) {
			if (i == 0 || timeline[i-1]/3 != timeline[i]/3) {
				System.out.println(String.format("%8.2f %8d %8s",
						TimeConverter.frame2sec(i),
						timeline[i],
						states[timeline[i]].getHMM().getBaseUnit()));
			}
		}

		return timeline;
	}


	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("USAGE: GrammarVector <SOUNDFILE.WAV> <\"transcription\">");
			System.exit(1);
		}

		final String wavpath = args[0];
		final String words   = args[1];

		ConfigurationManager cm = new ConfigurationManager("sr.cfg");
		UnitManager unitmgr = (UnitManager)cm.lookup("unitManager");
		assert unitmgr != null;

		TiedStateAcousticModel acmod = (TiedStateAcousticModel) HMMModels.getAcousticModels();

		GrammarVector gv = new GrammarVector(words, acmod, unitmgr);
		System.out.println("PHONE COUNT: " + gv.nPhones);
		System.out.println("GRAPH SIZE: " + gv.nStates);
		gv.dumpDot(new FileWriter("grammar_vector.dot"));

		AudioFileDataSource afds = (AudioFileDataSource)cm.lookup("audioFileDataSource");
		afds.setAudioFile(new File(wavpath), null);
		S4mfccBuffer mfcc = new S4mfccBuffer();
		mfcc.setSource(S4ForceAlignBlocViterbi.getFrontEnd(afds));

		System.out.println("Starting Viterbi...");
		gv.viterbi(mfcc);
		System.out.println("done");
	}

}
