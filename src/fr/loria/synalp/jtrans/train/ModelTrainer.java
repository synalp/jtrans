package fr.loria.synalp.jtrans.train;

import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.util.LogMath;
import fr.loria.synalp.jtrans.JTrans;
import fr.loria.synalp.jtrans.project.Token;
import fr.loria.synalp.jtrans.utils.BinarySegmentation;
import fr.loria.synalp.jtrans.speechreco.s4.HMMModels;
import fr.loria.synalp.jtrans.align.Alignment;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static fr.loria.synalp.jtrans.graph.StatePool.isSilenceState;

/**
 * Learns Gaussians for every unique state and computes alignment likelihoods.
 * <p/>
 * To obtain accurate figures, use a single instance of this class across an
 * entire transcription. This way, the Gaussians that have been learned are
 * valid on the entire transcription.
 * <p/>
 * If a transcription must be aligned using a sequence of small alignments, use
 * repeated calls to learn() instead of creating a new instance for each
 * sub-alignment.
 * <p/>
 * In this class, "states" can be any Object; they don't actually need to be
 * HMMStates from Sphinx. State objects just serve as identifiers. You can use
 * unique Strings or Integers as state identifiers if you like.
 */
public class ModelTrainer {

	/** Number of values in FloatData. */
	public static final int FRAME_DATA_LENGTH = 39;

	/** Keep variance values from getting too close to zero. */
	public static final double MIN_VARIANCE = .001;


	private final float[][] data;
	private final LogMath lm = HMMModels.getLogMath();

	private final int nFrames;
	private final Map<Object, Model> modelMap = new HashMap<>();
	private final Object[] compoundTimeline;
	private final double[] likelihood;   // must be zeroed before use
	private boolean sealed = false;


	static class Model {
		int nMatchF;
		double[] sum    = new double[FRAME_DATA_LENGTH];
		double[] sumSq  = new double[FRAME_DATA_LENGTH];
		double[] avg    = new double[FRAME_DATA_LENGTH];
		double[] var    = new double[FRAME_DATA_LENGTH];
		double detVar;
		boolean sealed = false;

		void clear() {
			nMatchF = 0;
			Arrays.fill(sum, 0);
			Arrays.fill(sumSq, 0);
			sealed = false;
		}

		void learnFrame(float[] frameData) {
			assert !sealed;

			nMatchF++;

			for (int d = 0; d < FRAME_DATA_LENGTH; d++) {
				float x = frameData[d];
				sum[d] += x;
				sumSq[d] += x * x;
			}
		}

		void seal() {
			assert !sealed;

			detVar = 1;
			if (nMatchF == 0) {
				Arrays.fill(avg, 0);
				Arrays.fill(var, 0);
				return;
			}

			for (int d = 0; d < FRAME_DATA_LENGTH; d++) {
				double a = sum[d] / nMatchF;
				avg[d] = a;
				var[d] = Math.max(MIN_VARIANCE, sumSq[d] / nMatchF - a*a);
				detVar *= var[d];
			}

			sealed = true;
		}

		double frameLikelihood(float[] frameData, LogMath lm, double logTwoPi) {
			assert sealed;
			assert detVar > 0;

			double dot = 0;
			for (int d = 0; d < FRAME_DATA_LENGTH; d++) {
				double numer = frameData[d] - avg[d];
				dot += numer * numer / var[d];
			}

			// -log(1 / sqrt(2 pi detVar)) = -(log(2 pi)/2 + log(detVar)/2)
			return -.5 * (dot + logTwoPi + lm.linearToLog(detVar));
		}
	}


	public ModelTrainer(float[][] data) {
		this.data = data;
		nFrames = data.length;
		likelihood = new double[nFrames];
		compoundTimeline = new Object[nFrames];
		clear();
	}


	public static double sum(double[] array) {
		double sum = 0;
		for (int i = 0; i < array.length; i++) {
			sum += array[i];
		}
		return sum;
	}


	/**
	 * Clears all models, making this trainer available for learning again.
	 * This way, you can reuse this trainer instead of wasting time
	 * re-allocating memory for a new one.
	 */
	public void clear() {
		Arrays.fill(likelihood, 0);
		Arrays.fill(compoundTimeline, null);
		for (Model m: modelMap.values()) {
			m.clear();
		}
		sealed = false;
	}


	Model getModel(Object state) {
		Model m = modelMap.get(state);
		if (null == m) {
			m = new Model();
			modelMap.put(state, m);
		}
		return m;
	}


	/**
	 * Trains models for all non-silent states in an aligned word.
	 */
	public void learn(Token word, Alignment alignment) {
		if (sealed) {
			throw new IllegalStateException("can't learn if sealed");
		}

		if (!word.isAlignable()) {
			return;
		}

		Token.Segment seg = word.getSegment();
		if (null == seg) {
			System.out.println("Unaligned word: " + word);
			return;
		}

		for (int f = seg.getStartFrame(); f <= seg.getEndFrame(); f++) {
			HMMState state = alignment.getStateAtFrame(f);
			if (!isSilenceState(state)) {
				learnStateAtFrame(state, f);
			}
		}
	}


	/**
	 * Trains a state model for the data in a given frame.
	 * @param state unique state identifier
	 * @param f frame number
	 */
	public void learnStateAtFrame(Object state, int f) {
		if (sealed) {
			throw new IllegalStateException("can't learn if sealed");
		}

		assert null == compoundTimeline[f]
				: "frame " + f + " already processed";

		getModel(state).learnFrame(data[f]);
		compoundTimeline[f] = state;
	}


	/**
	 * Finalizes gaussians for all models and computes per-frame likelihoods.
	 * @return effective number of frames on which the models were trained
	 */
	public int seal() {
		if (sealed) {
			throw new IllegalStateException("can't seal if already sealed");
		}

		// avg, var, detVar
		for (Model m: modelMap.values()) {
			m.seal();
		}

		final double logTwoPi = lm.linearToLog(2 * Math.PI);

		int effectiveFrames = 0;

		// likelihood for each frame
		for (int f = 0; f < nFrames; f++) {
			Object state = compoundTimeline[f];

			if (null == state) {
				likelihood[f] = 0;
			} else {
				likelihood[f] = modelMap.get(state)
						.frameLikelihood(data[f], lm, logTwoPi);
				effectiveFrames++;
			}
		}

		sealed = true;

		return effectiveFrames;
	}


	/**
	 * Returns time stretches where this trainer has not been used.
	 */
	public BinarySegmentation getNullStencil() {
		// TODO: not efficient

		BinarySegmentation bseg = new BinarySegmentation();

		for (int f = 0; f < compoundTimeline.length; f++) {
			if (null == compoundTimeline[f]) {
				bseg.union(f, 1);
			}
		}

		return bseg;
	}


	/**
	 * Returns per-frame likelihoods computed by {@link #seal()}.
	 */
	public double[] getLikelihoods() {
		if (!sealed) {
			throw new IllegalStateException("can't get likelihoods unless sealed");
		}

		return likelihood;
	}


	public void dump() {
		try {
			PrintWriter w = new PrintWriter(JTrans.logID + ".models.txt");
			for (Map.Entry<Object, Model> e: modelMap.entrySet()) {
				for (int j = 0; j < 39; j++) {
					Object state = e.getKey();
					Model model = e.getValue();
					w.printf("%2d %8s %10f %10f\n", j,
							state,
							model.avg[j],
							model.var[j]);
				}
			}
			w.close();
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}

}
