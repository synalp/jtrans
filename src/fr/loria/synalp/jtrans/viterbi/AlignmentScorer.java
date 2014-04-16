package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.util.LogMath;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.facade.FastLinearAligner;
import fr.loria.synalp.jtrans.facade.JTransCLI;
import fr.loria.synalp.jtrans.speechreco.s4.HMMModels;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import static java.lang.System.arraycopy;

/**
 * Learns Gaussians for every unique state and computes alignment likelihoods.
 *
 * To obtain accurate figures, use a single instance of this class across an
 * entire transcription. This way, the Gaussians that have been learned are
 * valid on the entire transcription.
 *
 * If a transcription must be aligned using a sequence of small alignments, use
 * repeated calls to learn() instead of creating a new instance for each
 * sub-alignment.
 */
public class AlignmentScorer {

	private static int logIDCounter = 0;
	private final int logID;

	/** Number of values in FloatData. */
	public static final int FRAME_DATA_LENGTH = 39;

	/** Keep variance values from getting too close to zero. */
	public static final double MIN_VARIANCE = .001;

	public static final int MAX_UNIQUE_STATES = 1000;

	private final float[][] data;
	private final LogMath lm = HMMModels.getLogMath();

	private final int nFrames;
	private int nStates;

	private final int[]        nMatchF; // must be zeroed before use
	private final double[][]   sum;     // must be zeroed before use
	private final double[][]   sumSq;   // must be zeroed before use
	private final double[][]   avg;
	private final double[][]   var;
	private final double[]     detVar;
	private final double[]     likelihood;   // must be zeroed before use

	/**
	 * Timeline of unique states.
	 * Don't confuse unique *states* with StateGraph *nodes*!
	 * (Several nodes may represent the same state)
	 */
	private final int[]        longTimeline;


	private class QStatePool {
		HashMap<HMMState, Integer> map = new HashMap<>();
		List<HMMState> states = new ArrayList<>();

		int getIdx(HMMState s) {
			if (!map.containsKey(s)) {
				int idx = states.size();
				map.put(s, idx);
				states.add(s);
				nStates++;
				return idx;
			} else {
				return map.get(s);
			}
		}

		String getPhone(int idx) {
			HMMState s = states.get(idx);
			return s==null? "null": s.getHMM().getBaseUnit().getName();
		}

		void clear() {
			map.clear();
			states.clear();
		}
	}


	QStatePool pool = new QStatePool();


	private static AlignmentScorer kludgeReferenceScorer;
	private static boolean kludgeModelsUsed = false;

	private enum SystemState {
		UNINITIALIZED,
		LEARNING,
		LEARNING_COMPLETE,
		SCORE_READY,
	}

	private SystemState system = SystemState.UNINITIALIZED;



	public AlignmentScorer(float[][] data) {
		this.nFrames = data.length;
		this.nStates = 0;
		this.data = data;

		logID = logIDCounter++;
		longTimeline = new int[nFrames];

		nMatchF     = new int[MAX_UNIQUE_STATES];
		sum         = new double[MAX_UNIQUE_STATES][FRAME_DATA_LENGTH];
		sumSq       = new double[MAX_UNIQUE_STATES][FRAME_DATA_LENGTH];
		avg         = new double[MAX_UNIQUE_STATES][FRAME_DATA_LENGTH];
		var         = new double[MAX_UNIQUE_STATES][FRAME_DATA_LENGTH];
		detVar      = new double[MAX_UNIQUE_STATES];
		likelihood  = new double[nFrames];

		init();
	}



	public static double sum(double[] array) {
		double sum = 0;
		for (int i = 0; i < array.length; i++) {
			sum += array[i];
		}
		return sum;
	}


	public void init() {
		Arrays.fill(longTimeline, -1);
		Arrays.fill(nMatchF, 0);
		Arrays.fill(likelihood, 0);
		for (int s = 0; s < nStates; s++) {
			Arrays.fill(sum[s], 0);
			Arrays.fill(sumSq[s], 0);
		}
		system = SystemState.LEARNING;
		pool.clear();
	}


	public void learn(Word word, StateGraph graph, int[] timeline, int frameOffset) {
		if (system != SystemState.LEARNING) {
			throw new IllegalStateException("not ready to learn");
		}

		Word.Segment seg = word.getSegment();
		if (seg == null) {
			System.out.println("NULL SEG!!!");
			return;
		}
		int sf = seg.getStartFrame();
		int ef = seg.getEndFrame();

		for (int absf = sf; absf <= ef; absf++) {
			int relf = absf - frameOffset;
			learnNode(graph, timeline[relf], absf);
		}
	}


	public void learn(StateGraph graph, int[] timeline, int frameOffset) {
		if (system != SystemState.LEARNING) {
			throw new IllegalStateException("not ready to learn");
		}

		for (int f = 0; f < timeline.length; f++) {
			int absf = f + frameOffset;
			learnNode(graph, timeline[f], absf);
		}
	}


	private void learnNode(StateGraph graph, int node, int absf) {
		assert longTimeline[absf] < 0 : "longTimeline already filled here";

		if (!graph.isSilentAt(node)) {
			HMMState state = graph.getStateAt(node);
			int myState = pool.getIdx(state);
			longTimeline[absf] = myState;
			learnFrame(absf);
		}
	}


	private void learnFrame(int f) {
		if (system != SystemState.LEARNING) {
			throw new IllegalStateException("not ready to learn");
		}

		int s = longTimeline[f];
		assert s >= 0;

		for (int d = 0; d < FRAME_DATA_LENGTH; d++) {
			float x = data[f][d];
			sum[s][d] += x;
			sumSq[s][d] += x * x;
		}

		nMatchF[s]++;
	}


	/**
	 * @param stretch0 inclusive
	 * @param stretch1 exclusive
	 */
	private void fillVoid(int stretch0, int stretch1, List<Integer> silStates) {
		assert stretch0 < stretch1;
		FastLinearAligner.fillInterpolate(
				silStates, longTimeline, stretch0, stretch1 - stretch0);
		for (int i = stretch0; i < stretch1; i++) {
			learnFrame(i);
		}
	}


	/**
	 * Fill unfilled stretches with silences
	 * @param sil0 state ID of SIL #0
	 * @param sil1 state ID of SIL #1
	 * @param sil2 state ID of SIL #2
	 */
	public void fillVoids(int sil0, int sil1, int sil2) {
		int stretch0 = -1;

		List<Integer> silStates = Arrays.asList(sil0, sil1, sil2);

		for (int f = 0; f < nFrames; f++) {
			if (longTimeline[f] < 0 && stretch0 < 0) {
				// start new stretch
				stretch0 = f;
			} else if (longTimeline[f] >= 0 && stretch0 >= 0) {
				// fill stretch
				fillVoid(stretch0, f, silStates);
				stretch0 = -1;
			}
		}

		if (stretch0 >= 0) {
			fillVoid(stretch0, nFrames, silStates);
		}
	}


	public void finishLearning() {
		if (system != SystemState.LEARNING) {
			throw new IllegalStateException("not ready to finish learning");
		}

//		fillVoids();

		// avg, var, detVar
		for (int s = 0; s < nStates; s++) {
			detVar[s] = 1;
			if (nMatchF[s] == 0) {
				Arrays.fill(avg[s], 0);
				Arrays.fill(var[s], 0);
				continue;
			}
			for (int d = 0; d < FRAME_DATA_LENGTH; d++) {
				avg[s][d] = sum[s][d] / nMatchF[s];
				var[s][d] = Math.max(MIN_VARIANCE,
						sumSq[s][d] / nMatchF[s] - avg[s][d] * avg[s][d]);
				detVar[s] *= var[s][d];
			}
		}

		if (kludgeModelsUsed) {
			throw new Error("Silence models already used");
		}
		else if (kludgeReferenceScorer == null) {
			// First pass
			System.out.println("Providing silence models");
			kludgeReferenceScorer = this;
		}
		else {
			// Second pass
			System.out.println("Consuming silence models");
			assert this != kludgeReferenceScorer;
			assert pool.states.equals(kludgeReferenceScorer.pool.states);

//			for (int i = 0; i < kludgeReferenceScorer.nStates; i++) {
			for (int i = 0; i < 3; i++) {
				assert kludgeReferenceScorer.pool.states.get(i) == null;

				// copy stuff used by score()
				arraycopy(kludgeReferenceScorer.avg[i], 0, avg[i], 0, avg[i].length);
				arraycopy(kludgeReferenceScorer.var[i], 0, var[i], 0, var[i].length);
				detVar[i] = kludgeReferenceScorer.detVar[i];
			}

			kludgeModelsUsed = true;
		}

		system = SystemState.LEARNING_COMPLETE;
	}


	public int score() {
		if (system != SystemState.LEARNING_COMPLETE) {
			throw new IllegalStateException("still learning");
		}

		final double logTwoPi = lm.linearToLog(2 * Math.PI);

		int effectiveFrames = 0;

		// likelihood for each frame
		for (int f = 0; f < nFrames; f++) {
			int s = longTimeline[f];

			if (s < 0) {
				likelihood[f] = 0;
				continue;
			} else {
				effectiveFrames++;
			}

			double dot = 0;
			for (int d = 0; d < FRAME_DATA_LENGTH; d++) {
				double numer = data[f][d] - avg[s][d];
				dot += numer * numer / var[s][d];
			}

			assert detVar[s] > 0;

			// -log(1 / sqrt(2 pi detVar)) = -(log(2 pi)/2 + log(detVar)/2)
			likelihood[f] = -.5 * (dot + logTwoPi + lm.linearToLog(detVar[s]));
		}


		try {
			final String plotName = JTransCLI.logID + "_perframe_" +
					(kludgeModelsUsed? "clear": "gold") + ".txt";
			PrintWriter perFrame;
			perFrame = new PrintWriter(new BufferedWriter(new FileWriter(plotName)));
			System.err.println("Plot: " + plotName);

			for (int f = 0; f < likelihood.length; f++) {
				perFrame.printf("%6d %6.6f %4d %s\n",
						f, likelihood[f], longTimeline[f], pool.getPhone(longTimeline[f]));
			}

			perFrame.flush();
			perFrame.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}


		system = SystemState.SCORE_READY;

		return effectiveFrames;
	}


	public double[] getLikelihoods() {
		if (system != SystemState.SCORE_READY) {
			throw new IllegalStateException("score not ready");
		}

		return likelihood;
	}


	public static AlignmentScorer merge(List<AlignmentScorer> scorers) {
		AlignmentScorer merger =
				new AlignmentScorer(scorers.get(0).data);

		// states 0, 1, 2: common silences
		merger.pool.states.add(null);
		merger.pool.states.add(null);
		merger.pool.states.add(null);
		assert merger.pool.states.size() == 3;

		for (AlignmentScorer scorer: scorers) {
			assert scorer.nStates == scorer.pool.states.size();
			assert scorer.nFrames == merger.nFrames;
			assert scorer.data == merger.data;

			// offset at which the new states are inserted
			int off = merger.pool.states.size();
			merger.pool.states.addAll(scorer.pool.states);
			System.out.println("Merge: added " + scorer.pool.states.size() + " states from scorer");

			arraycopy(scorer.nMatchF, 0, merger.nMatchF, off, scorer.nStates);
			arraycopy(scorer.sum,     0, merger.sum,     off, scorer.nStates);
			arraycopy(scorer.sumSq,   0, merger.sumSq,   off, scorer.nStates);
			// No need to copy avg, var, detVar;
			// they will be filled out by finishLearning()

			for (int f = 0; f < scorer.nFrames; f++) {
				int sus = scorer.longTimeline[f];

				if (sus < 0) {
					continue;
				}

				int mus = off + sus;

				assert merger.longTimeline[f] < 0: "overwriting longTimeline";
				assert !scorer.pool.getPhone(sus).equals("SIL"): "unexpected SIL";
				assert scorer.pool.states.get(sus).equals(merger.pool.states.get(mus));
				assert scorer.nMatchF[sus] == merger.nMatchF[mus];
				assert scorer.sum    [sus] == merger.sum    [mus];
				assert scorer.sumSq  [sus] == merger.sumSq  [mus];

				merger.longTimeline[f] = mus;
			}
		}

		merger.nStates = merger.pool.states.size();
		merger.fillVoids(0, 1, 2);
		merger.finishLearning();

		return merger;
	}


	public static AlignmentScorer merge(AlignmentScorer... scorers) {
		return merge(Arrays.asList(scorers));
	}


	public int[] getLongTimeline() {
		return longTimeline;
	}

}
