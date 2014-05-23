package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.util.LogMath;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.facade.FastLinearAligner;
import fr.loria.synalp.jtrans.facade.JTransCLI;
import fr.loria.synalp.jtrans.speechreco.s4.HMMModels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import static java.lang.System.arraycopy;
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

	public static final int MAX_UNIQUE_STATES = 1000;

	private final float[][] data;
	private final LogMath lm = HMMModels.getLogMath();

	private final int nFrames;

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


	StatePool pool = new StateSet();


	private enum SystemState {
		UNINITIALIZED,
		LEARNING,
		LEARNING_COMPLETE,
		SCORE_READY,
	}

	private SystemState system = SystemState.UNINITIALIZED;



	public ModelTrainer(float[][] data) {
		this.nFrames = data.length;
		this.data = data;

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
		for (int s = 0; s < MAX_UNIQUE_STATES; s++) {
			Arrays.fill(sum[s], 0);
			Arrays.fill(sumSq[s], 0);
		}
		system = SystemState.LEARNING;
		pool.clear();
	}


	public void learn(Word word, StateTimeline timeline, int frameOffset) {
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
			learnState(timeline.getStateAtFrame(relf), absf);
		}
	}


	/**
	 * @param graph
	 * @param timeline timeline of nodes in the state graph
	 * @param frameOffset timeline starts at this frame
	 */
	public void learn(StateGraph graph, int[] timeline, int frameOffset) {
		if (system != SystemState.LEARNING) {
			throw new IllegalStateException("not ready to learn");
		}

		for (int f = 0; f < timeline.length; f++) {
			int absf = f + frameOffset;
			learnState(graph.getStateAt(timeline[f]), absf);
		}
	}


	private void learnState(HMMState state, int absf) {
		assert longTimeline[absf] < 0 : "longTimeline already filled here";

		if (!isSilenceState(state)) {
			int myState = pool.indexOf(state);
			if (myState < 0) {
				myState = pool.add(state);
			}
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
		for (int s = 0; s < pool.size(); s++) {
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


	public static ModelTrainer merge(List<ModelTrainer> scorers) {
		ModelTrainer merger =
				new ModelTrainer(scorers.get(0).data);

		merger.pool = new StateList();

		// states 0, 1, 2: common silences
		merger.pool.add(null);
		merger.pool.add(null);
		merger.pool.add(null);
		assert merger.pool.size() == 3;

		for (ModelTrainer scorer: scorers) {
			assert scorer.nFrames == merger.nFrames;
			assert scorer.data == merger.data;

			// offset at which the new states are inserted
			int off = merger.pool.size();
			int[] translation = merger.pool.addAll(scorer.pool);
			for (int i = 0; i < translation.length; i++) {
				assert translation[i] == off + i:
						i + " " + translation[i] + " " + off;
			}

//			System.out.println("Merge: added " + scorer.pool.states.size() + " states from scorer");

			arraycopy(scorer.nMatchF, 0, merger.nMatchF, off, scorer.pool.size());
			arraycopy(scorer.sum,     0, merger.sum,     off, scorer.pool.size());
			arraycopy(scorer.sumSq,   0, merger.sumSq,   off, scorer.pool.size());
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
				assert scorer.pool.get(sus).equals(merger.pool.get(mus));
				assert scorer.nMatchF[sus] == merger.nMatchF[mus];
				assert scorer.sum    [sus] == merger.sum    [mus];
				assert scorer.sumSq  [sus] == merger.sumSq  [mus];

				merger.longTimeline[f] = mus;
			}
		}

		merger.fillVoids(0, 1, 2);
		merger.finishLearning();

		return merger;
	}


	public void dump() {
		try {
			PrintWriter w = new PrintWriter(JTransCLI.logID + ".models.txt");
			for (int i = 0; i < pool.size(); i++) {
				for (int j = 0; j < 39; j++) {
					w.printf("%3d %2d %8s %10f %10f\n", i, j,
							pool.getPhone(i), avg[i][j], var[i][j]);
				}
			}
			w.close();
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}


	public static ModelTrainer merge(ModelTrainer... scorers) {
		return merge(Arrays.asList(scorers));
	}


	public int[] getLongTimeline() {
		return longTimeline;
	}

}
