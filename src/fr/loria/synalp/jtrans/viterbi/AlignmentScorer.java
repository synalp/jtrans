package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.util.LogMath;
import fr.loria.synalp.jtrans.speechreco.s4.HMMModels;
import fr.loria.synalp.jtrans.speechreco.s4.S4mfccBuffer;

import java.util.Arrays;

/**
 * Computes alignment likelihoods. To avoid frenetic memory allocation activity,
 * an instance of this class is meant to be reused, provided the alignments use
 * the same states across the same number of frames.
 */
public class AlignmentScorer {

	/** Number of values in FloatData. */
	public static final int FRAME_DATA_LENGTH = 39;

	/** Keep variance values from getting too close to zero. */
	public static final double MIN_VARIANCE = .001;

	private final StateGraph graph;
	private final StatePool pool;
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


	/**
	 * @param data MFCC data
	 */
	public AlignmentScorer(StateGraph graph, StatePool pool, float[][] data) {
		this.graph = graph;
		this.pool = pool;
		this.nFrames = data.length;
		this.nStates = pool.size();
		this.data = data;

		nMatchF     = new int[nStates];
		sum         = new double[nStates][FRAME_DATA_LENGTH];
		sumSq       = new double[nStates][FRAME_DATA_LENGTH];
		avg         = new double[nStates][FRAME_DATA_LENGTH];
		var         = new double[nStates][FRAME_DATA_LENGTH];
		detVar      = new double[nStates];
		likelihood  = new double[nFrames];
	}


	/**
	 * @param mfcc audio data
	 * @param frameOffset jump to this frame before reading audio data
	 */
	public static float[][] getData(
			int nFrames,
			S4mfccBuffer mfcc,
			int frameOffset)
	{
		float data[][] = new float[nFrames][FRAME_DATA_LENGTH];

		mfcc.gotoFrame(frameOffset);

		// Get data
		for (int f = 0; f < nFrames;) {
			Data d = mfcc.getData();
			if (d instanceof DataEndSignal) {
				throw new Error("Out of data!!! "
						+ (nFrames - f) + " frames missing!");
			}

			float[] values;
			try {
				values = FloatData.toFloatData(d).getValues();
			} catch (IllegalArgumentException ex) {
				// not a FloatData/DoubleData
				continue;
			}
			assert values.length == FRAME_DATA_LENGTH;
			System.arraycopy(values, 0, data[f], 0, FRAME_DATA_LENGTH);
			f++; // successful
		}

		return data;
	}


	public static double sum(double[] array) {
		double sum = 0;
		for (int i = 0; i < array.length; i++) {
			sum += array[i];
		}
		return sum;
	}


	/**
	 * Computes the likelihood of an alignment.
	 * @param timeline alignment that maps frames to nodes in the graph
	 * @return likelihoods per frame
	 */
	public double[] alignmentLikelihood(int[] timeline) {
		assert timeline.length == nFrames;

		Arrays.fill(nMatchF, 0);
		Arrays.fill(likelihood, 0);
		for (int s = 0; s < nStates; s++) {
			Arrays.fill(sum[s], 0);
			Arrays.fill(sumSq[s], 0);
		}

		// sum, sumSq, nMatchF
		for (int f = 0; f < nFrames; f++) {
			for (int d = 0; d < FRAME_DATA_LENGTH; d++) {
				float x = data[f][d];
				int s = graph.getUniqueStateIdAt(timeline[f]);
				sum[s][d] += x;
				sumSq[s][d] += x * x;
				nMatchF[s]++;
			}
		}

		// avg, var, detVar
		for (int s = 0; s < nStates; s++) {
			detVar[s] = 1;
			for (int d = 0; d < FRAME_DATA_LENGTH; d++) {
				avg[s][d] = sum[s][d] / nMatchF[s];
				var[s][d] = Math.max(MIN_VARIANCE,
						sumSq[s][d] / nMatchF[s] - avg[s][d] * avg[s][d]);
				detVar[s] *= var[s][d];
			}
		}

		// likelihood for each frame
		for (int f = 0; f < nFrames; f++) {
			int s = graph.getUniqueStateIdAt(timeline[f]);

			double K = -lm.linearToLog(Math.sqrt(
					2*Math.PI * Math.abs(detVar[s])));

			double dot = 0;

			for (int d = 0; d < FRAME_DATA_LENGTH; d++) {
				double numer = data[f][d] - avg[s][d];
				dot += numer * numer / var[s][d];
			}

			likelihood[f] += K - .5 * dot;

			/*
			System.out.println("Frame " + f +
					": State #" + s + ":" + states[s].getHMM().getBaseUnit().getName() +
					",\tK = " + K +
					",\tDot = " + dot +
					",\tL = " + likelihood[f]);
			*/
		}

		// debug
		/*
		for (int s = 0; s < nStates; s++) {
			System.out.println("STATE #" + s + ":"
					+ states[s].getHMM().getBaseUnit().getName());

			for (int f = 0; f < nFrames; f++) {
				if (s == timeline[f]) {
					System.out.println(String.format(
							"\tF = %4d\tL = %f ", f, likelihood[f]));
				}
			}
		}
		*/

		return likelihood;
	}


	public double cumulativeAlignmentLikelihood(int[] timeline) {
		return sum(alignmentLikelihood(timeline));
	}

}
