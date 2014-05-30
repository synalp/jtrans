package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.util.LogMath;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.facade.JTransCLI;
import fr.loria.synalp.jtrans.speechreco.s4.HMMModels;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Refines an HMM state timeline by shifting transitions with the
 * Metropolis-Hastings algorithm.
 */
public class TransitionRefinery {

	private Alignment timeline;
	private double cLhd;

	private Random random;
	private SpeakerDepModelTrainer trainer;
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
			Alignment baseline,
			SpeakerDepModelTrainer trainer)
	{
		timeline = new Alignment(baseline);

		random = new Random();
		this.trainer = trainer;

/*
		for (int i = 0; i < 10000000; i++) {
			shift();
		}
*/

		cLhd = train(baseline);
	}


	private double train(Alignment timeline) {
		trainer.clear();

		for (Word w: timeline.getUniqueWords()) {
			trainer.learn(w, timeline, 0);
		}

		trainer.seal();

		return trainer.getCumulativeLikelihood();
	}


	/**
	 * Refines an HMM state timeline with the Metropolis-Hastings algorithm.
	 */
	public Alignment step() throws IOException {
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
		Alignment proposal = new Alignment(timeline);

		for (int i = 0; i < 100; i++) {
			proposal.wiggle(random, 1);
		}

		double newCLhd = train(proposal);
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

		if (accept) {
			cLhd = newCLhd;
			timeline = proposal;
		}

		System.out.println("Acceptance status: " + status);

		return status;
	}

}
