package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.util.LogMath;
import fr.loria.synalp.jtrans.speechreco.s4.HMMModels;
import fr.loria.synalp.jtrans.speechreco.s4.S4mfccBuffer;

import java.util.Arrays;
import java.util.List;

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
	private boolean            learning = false;


	public AlignmentScorer(float[][] data) {
		this.nFrames = data.length;
		this.nStates = MAX_UNIQUE_STATES;  // TODO pool.size() would be better
		// TODO pool.size() currently starts at 0 and increases (anchored alignment)
		this.data = data;

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


	public AlignmentScorer(List<FloatData> dataList) {
		this(S4mfccBuffer.to2DArray(dataList));
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
		learning = true;
	}


	public void learn(StateGraph graph, int[] timeline, int frameOffset) {
		if (!learning) {
			throw new IllegalStateException("not ready to learn");
		}

		// sum, sumSq, nMatchF
		for (int f = 0; f < timeline.length; f++) {
			int absf = f + frameOffset;
			assert longTimeline[absf] < 0 : "longTimeline already filled here";

			int s = graph.getUniqueStateIdAt(timeline[f]);
			longTimeline[absf] = s;

			for (int d = 0; d < FRAME_DATA_LENGTH; d++) {
				float x = data[absf][d];
				sum[s][d] += x;
				sumSq[s][d] += x * x;
				nMatchF[s]++;
			}
		}
	}


	public void finishLearning() {
		if (!learning) {
			throw new IllegalStateException("not ready to finish learning");
		}

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

		learning = false;
	}


	public double[] score() {
		if (learning) {
			throw new IllegalStateException("still learning");
		}

		// likelihood for each frame
		for (int f = 0; f < nFrames; f++) {
			int s = longTimeline[f];

			if (s < 0) {
				continue;
			}

			double K = -lm.linearToLog(Math.sqrt(
					2*Math.PI * Math.abs(detVar[s])));

			double dot = 0;

			for (int d = 0; d < FRAME_DATA_LENGTH; d++) {
				double numer = data[f][d] - avg[s][d];
				dot += numer * numer / var[s][d];
			}

			likelihood[f] += K - .5 * dot;
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

}
