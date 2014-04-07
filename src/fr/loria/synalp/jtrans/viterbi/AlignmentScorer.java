package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.util.LogMath;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.facade.FastLinearAligner;
import fr.loria.synalp.jtrans.speechreco.s4.HMMModels;
import fr.loria.synalp.jtrans.speechreco.s4.S4mfccBuffer;

import java.util.Arrays;
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

	public static final int MAX_UNIQUE_STATES = 100;

	private final float[][] data;
	private final LogMath lm = HMMModels.getLogMath();

	private final int nFrames;
	private final int nStates;

	private final int[]        nMatchF; // must be zeroed before use
	private final double[][]   sum;     // must be zeroed before use
	private final double[][]   sumSq;   // must be zeroed before use
	private final double[][]   avg;
	private final double[][]   var;
	private final double[]     detVar;
	private final double[]     likelihood;   // must be zeroed before use

	private final int[]        longTimeline;

	private enum SystemState {
		UNINITIALIZED,
		LEARNING,
		LEARNING_COMPLETE,
		SCORE_READY,
	}

	private SystemState system = SystemState.UNINITIALIZED;



	public AlignmentScorer(float[][] data, int nStates) {
		this.nFrames = data.length;
		this.nStates = nStates;
		this.data = data;

		logID = logIDCounter++;
		longTimeline = new int[nFrames];

		nMatchF     = new int[nStates];
		sum         = new double[nStates][FRAME_DATA_LENGTH];
		sumSq       = new double[nStates][FRAME_DATA_LENGTH];
		avg         = new double[nStates][FRAME_DATA_LENGTH];
		var         = new double[nStates][FRAME_DATA_LENGTH];
		detVar      = new double[nStates];
		likelihood  = new double[nFrames];

		init();
	}


	public AlignmentScorer(List<FloatData> dataList, int nStates) {
		this(S4mfccBuffer.to2DArray(dataList), nStates);
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
			assert longTimeline[absf] < 0;

			int s = graph.getUniqueStateIdAt(timeline[relf]);
			longTimeline[absf] = s;

			learnFrame(absf);
		}
	}


	public void learn(StateGraph graph, int[] timeline, int frameOffset) {
		if (system != SystemState.LEARNING) {
			throw new IllegalStateException("not ready to learn");
		}

		// sum, sumSq, nMatchF
		for (int f = 0; f < timeline.length; f++) {
			int absf = f + frameOffset;
			assert longTimeline[absf] < 0 : "longTimeline already filled here";

			int s = graph.getUniqueStateIdAt(timeline[f]);
			longTimeline[absf] = s;

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

		system = SystemState.SCORE_READY;

		return effectiveFrames;
	}


	public double[] getLikelihoods() {
		if (system != SystemState.SCORE_READY) {
			throw new IllegalStateException("score not ready");
		}

		return likelihood;
	}


	/**
	 * Computes the likelihood of an alignment.
	 * @param timeline alignment that maps frames to nodes in the graph
	 * @return likelihoods per frame
	 */
	public double[] alignmentLikelihood(StateGraph graph, int[] timeline) {
		assert timeline.length == nFrames;
		init();
		learn(graph, timeline, 0);
		finishLearning();
		score();
		return likelihood;
	}


	public double cumulativeAlignmentLikelihood(StateGraph graph, int[] timeline) {
		return sum(alignmentLikelihood(graph, timeline));
	}


	public static AlignmentScorer merge(List<AlignmentScorer> scorers) {
		int totalStateCount = 3;
		for (AlignmentScorer scorer: scorers) {
			totalStateCount += scorer.nStates;
		}

		AlignmentScorer merger =
				new AlignmentScorer(scorers.get(0).data, totalStateCount);

		int off = 3;
		for (AlignmentScorer scorer: scorers) {
			assert scorer.nFrames == merger.nFrames;
			assert scorer.data == merger.data;

			arraycopy(scorer.nMatchF, 0, merger.nMatchF, off, scorer.nStates);
			arraycopy(scorer.sum,     0, merger.sum,     off, scorer.nStates);
			arraycopy(scorer.sumSq,   0, merger.sumSq,   off, scorer.nStates);
			// No need to copy avg, var, detVar;
			// they will be filled out by finishLearning()

			for (int f = 0; f < scorer.nFrames; f++) {
				int stateAt = scorer.longTimeline[f];
				if (stateAt >= 0) {
					assert merger.longTimeline[f] < 0;
					merger.longTimeline[f] = off + stateAt;
				}
			}

			off += scorer.nStates;
		}

		assert totalStateCount == off;

		merger.fillVoids(0, 1, 2);

		return merger;
	}


	public static AlignmentScorer merge(AlignmentScorer... scorers) {
		return merge(Arrays.asList(scorers));
	}


	public int[] getLongTimeline() {
		return longTimeline;
	}

}
