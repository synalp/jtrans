package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.util.LogMath;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.facade.JTransCLI;
import fr.loria.synalp.jtrans.speechreco.s4.HMMModels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static fr.loria.synalp.jtrans.viterbi.StateSet.isSilenceState;

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
public class ModelTrainer {

	/** Number of values in FloatData. */
	public static final int FRAME_DATA_LENGTH = 39;

	/** Keep variance values from getting too close to zero. */
	public static final double MIN_VARIANCE = .001;


	private final float[][] data;
	private final LogMath lm = HMMModels.getLogMath();

	private final int nFrames;
	private final Map<HMMState, Model> modelMap = new HashMap<>();
	private final HMMState[] compoundTimeline;
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
		compoundTimeline = new HMMState[nFrames];
		clear();
	}


	public static double sum(double[] array) {
		double sum = 0;
		for (int i = 0; i < array.length; i++) {
			sum += array[i];
		}
		return sum;
	}


	public void clear() {
		Arrays.fill(likelihood, 0);
		Arrays.fill(compoundTimeline, null);
		for (Model m: modelMap.values()) {
			m.clear();
		}
		sealed = false;
	}


	Model getModel(HMMState state) {
		Model m = modelMap.get(state);
		if (null == m) {
			m = new Model();
			modelMap.put(state, m);
		}
		return m;
	}



	public void learn(Word word, StateTimeline timeline, int frameOffset) {
		if (sealed) {
			throw new IllegalStateException("can't learn if sealed");
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

			HMMState state = timeline.getStateAtFrame(relf);
			if (!isSilenceState(state)) {
				assert null == compoundTimeline[absf]
						: "frame " + absf + " already processed";
				getModel(state).learnFrame(data[absf]);
				compoundTimeline[absf] = state;
			}
		}
	}


	/**
	 * @param stretch0 inclusive
	 * @param stretch1 exclusive
	 */
	/*
	private void fillVoid(int stretch0, int stretch1, List<Integer> silStates) {
		assert stretch0 < stretch1;
		FastLinearAligner.fillInterpolate(
				silStates, longTimeline, stretch0, stretch1 - stretch0);
		for (int i = stretch0; i < stretch1; i++) {
			learnFrame(i);
		}
	}
	*/


	/**
	 * Fill unfilled stretches with silences
	 * @param sil0 state ID of SIL #0
	 * @param sil1 state ID of SIL #1
	 * @param sil2 state ID of SIL #2
	 */
	/*
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
	*/


	public int seal() {
		if (sealed) {
			throw new IllegalStateException("can't seal if already sealed");
		}

//		fillVoids();

		// avg, var, detVar
		for (Model m: modelMap.values()) {
			m.seal();
		}

		final double logTwoPi = lm.linearToLog(2 * Math.PI);

		int effectiveFrames = 0;

		// likelihood for each frame
		for (int f = 0; f < nFrames; f++) {
			HMMState state = compoundTimeline[f];

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


	public double[] getLikelihoods() {
		if (!sealed) {
			throw new IllegalStateException("can't get likelihoods unless sealed");
		}

		return likelihood;
	}


	public void dump() {
		try {
			PrintWriter w = new PrintWriter(JTransCLI.logID + ".models.txt");
			for (Map.Entry<HMMState, Model> e: modelMap.entrySet()) {
				for (int j = 0; j < 39; j++) {
					HMMState state = e.getKey();
					Model model = e.getValue();
					w.printf("%2d %8s %10f %10f\n", j,
							StatePool.getPhone(state),
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
