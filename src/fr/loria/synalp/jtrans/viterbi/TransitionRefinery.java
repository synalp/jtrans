package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.util.LogMath;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.facade.JTransCLI;
import fr.loria.synalp.jtrans.speechreco.s4.HMMModels;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Refines an HMM state timeline by shifting transitions with the
 * Metropolis-Hastings algorithm.
 */
public class TransitionRefinery {

	private int[] timeline;
	private int[] backup;
	private double cLhd;

	private Random random;
	private StateGraph graph;
	private List<ModelTrainer> trainers;
	private LogMath log = HMMModels.getLogMath();

	int rejectionStreak = 0;
	int rejections = 0;
	int acceptances = 0;
	int luckyAcceptances = 0;
	int iterations;
	private PrintWriter plot;


	/**
	 * Maximum number of rejected steps in a row before aborting the
	 * Metropolis-Hastings refinement.
	 */
	public static final int METROPOLIS_REJECTION_STREAK_CAP = 500;


	/** Proposal acceptance/rejection status */
	private enum Accept {
		/** Accepted on merit alone. Proposal better than original. */
		MERIT,

		/** Accepted on luck. Proposal worse than original, but got randomly
		 * accepted anyway. */
		LUCK,

		/** Rejected. Proposal worse than original. */
		REJECTED,
	}


	/**
	 * @param baseline Baseline alignment (as found e.g. with viterbi()).
	 */
	public TransitionRefinery(
			StateGraph graph,
			int[] baseline,
			List<ModelTrainer> trainers)
	{
		timeline = new int[baseline.length];
		backup = new int[baseline.length];
		System.arraycopy(baseline, 0, timeline, 0, timeline.length);

		random = new Random();
		this.graph = graph;
		this.trainers = trainers;

/*
		for (int i = 0; i < 10000000; i++) {
			shift();
		}
*/

		cLhd = computeCumulativeLikelihood();
	}


	private double computeCumulativeLikelihood() {
		// TODO: too much boilerplate -- do these steps really need to be separated?

		for (ModelTrainer s: trainers) {
			s.init();
		}

		for (Word w: graph.getWords()) {
			trainers.get(w.getSpeaker()).learn(w, graph, timeline, 0);
		}

		ModelTrainer merged = ModelTrainer.merge(trainers);
		merged.score();

		return ModelTrainer.sum(merged.getLikelihoods());
	}


	/**
	 * Finds the next transition between two states.
	 * @param offset start searching at this frame
	 * @param timeline HMM state timeline
	 * @return the number of the frame preceding the transition
	 */
	public static int nextTransition(int offset, int[] timeline) {
		final int upper = timeline.length - 2;
		while (offset < upper) {
			if (timeline[offset] != timeline[offset+1]) {
				return offset;
			}
			offset++;
		}
		return offset >= upper? -1: offset;
	}


	protected void shift() {
		// first frame in the region surrounding the transition
		int winL = -1;

		// last frame in the region surrounding the transition
		int winR = -1;

		// frames to the left of the transition in the middle of the region
		int oldLeftFrames = 0;

		while (winR < 0) {
			// don't use last 2 values (see nextTransition())
			winL = nextTransition(random.nextInt(timeline.length - 2), timeline);
			if (winL < 0) continue;
			// the region starts to the right of the "initial" transition
			winL++;

			int winC = nextTransition(winL, timeline);
			if (winC < 0) continue;

			oldLeftFrames = 1 + winC - winL;

			winR = nextTransition(winC + 1, timeline);
			assert winC != winR;
		}

		int left  = timeline[winL];
		int right = timeline[winR];
		assert left != right;

		int windowLength = 1 + winR-winL;

		if (windowLength <= 2) {
			// can't shift anything without erasing a state
			return;
		}

		int leftFrames = oldLeftFrames;
		while (leftFrames == oldLeftFrames) {
			// don't erase first nor last state in the region
			leftFrames = 1 + random.nextInt(windowLength - 1);
		}

		assert leftFrames >= 1;
		assert leftFrames <= windowLength-1;

		Arrays.fill(timeline, winL, winL+leftFrames, left);
		Arrays.fill(timeline, winL+leftFrames, winR+1, right);
	}


	/**
	 * Refines an HMM state timeline with the Metropolis-Hastings algorithm.
	 */
	public int[] step() throws IOException {
		iterations++;

		if (plot == null) {
			final String plotName = JTransCLI.logID + "_likelihood.txt";
			plot = new PrintWriter(new BufferedWriter(new FileWriter(plotName)));
			System.err.println("Plot: " + plotName);
		}

		Accept status = metropolisHastings();

		if (status == Accept.REJECTED) {
			rejections++;
			rejectionStreak++;
		} else {
			acceptances++;
			if (status == Accept.LUCK) {
				luckyAcceptances++;
			}
			rejectionStreak = 0;
		}


		plot.println(cLhd);

		if (hasPlateaued() || iterations % 100 == 0) {
			plot.flush();
			System.err.println(String.format(
					"Rejections: %d, Acceptances: %d (of which %d lucky) (%f%%)",
					rejections, acceptances, luckyAcceptances,
					100f * acceptances / (rejections+acceptances)));
		}

		return timeline;
	}


	public boolean hasPlateaued() {
		//return rejectionStreak >= METROPOLIS_REJECTION_STREAK_CAP;
		return false;
	}


	/**
	 * Refines a random transition in the timeline.
	 */
	private Accept metropolisHastings() {
		System.arraycopy(timeline, 0, backup, 0, timeline.length);

		for (int i = 0; i < 100; i++) {
			shift();
		}

		double newCLhd = computeCumulativeLikelihood();

		boolean accept = newCLhd > cLhd;
		final Accept status;

		if (accept) {
			status = Accept.MERIT;
		} else {
			double ratio = log.logToLinear((float) (newCLhd - cLhd));
			assert ratio >= 0 && ratio <= 1;
			double dice = random.nextDouble();
			accept = dice <= ratio;
			status = accept? Accept.LUCK: Accept.REJECTED;

			System.out.println(String.format(
					"Dice: %.5f, Ratio: %.20f, newLhd: %.10f, oldLhd: %.10f, logdiff:%.10f",
					dice, ratio, newCLhd, cLhd, newCLhd - cLhd));
		}

		if (!accept) {
			int[] swap = timeline;
			timeline = backup;
			backup = swap;
		} else {
			cLhd = newCLhd;
		}

		System.out.println("Acceptance status: " + status);

		return status;
	}

}
