package fr.loria.synalp.jtrans.train;

import fr.loria.synalp.jtrans.project.Token;
import fr.loria.synalp.jtrans.utils.BinarySegmentation;
import fr.loria.synalp.jtrans.utils.BufferUtils;
import fr.loria.synalp.jtrans.align.Alignment;

import static fr.loria.synalp.jtrans.align.FastLinearAligner.fillInterpolate;

import java.util.ArrayList;
import java.util.List;

/**
 * Speaker-dependent model trainer.
 * @see ModelTrainer
 */
public class SpeakerDepModelTrainer {

	protected final int frames;

	/** Speaker-dependent trainers for non-silence models */
	protected List<ModelTrainer> trainers;

	/**
	 * Speaker-independent trainer for silence models.
	 * Uses numbers among 0, 1, and 2 as silence state identifiers.
	 */
	protected ModelTrainer silenceTrainer;


	public static boolean LEARN_SILENCES = true;


	public SpeakerDepModelTrainer(int speakers, float[][] data) {
		frames = data.length;

		trainers = new ArrayList<>(speakers);

		for (int i = 0; i < speakers; i++) {
			trainers.add(new ModelTrainer(data));
		}

		silenceTrainer = new ModelTrainer(data);
	}


	public void learn(Token w, Alignment alignment) {
		trainers.get(w.getSpeaker()).learn(w, alignment);
	}


	public void clear() {
		for (ModelTrainer mt: trainers) {
			mt.clear();
		}
		silenceTrainer.clear();
	}


	public void seal() {
		for (ModelTrainer mt: trainers) {
			mt.seal();
		}

		if (LEARN_SILENCES) {
			BinarySegmentation silenceStencil = new BinarySegmentation();
			silenceStencil.union(0, frames); // fill entire segmentation

			for (ModelTrainer mt : trainers) {
				silenceStencil.intersect(mt.getNullStencil());
			}

			// Mini-timeline for silences
			int[] buf = new int[256];

			for (int i = 0; i < silenceStencil.size(); i++) {
				int off = (int) silenceStencil.get(i).getStart();
				int len = (int) silenceStencil.get(i).length();
				buf = BufferUtils.grow(buf, len);

				// Spread 3 silence "states" across the length
				fillInterpolate(3, buf, 0, len);

				for (int j = 0; j < len; j++) {
					// In silenceTrainer, silence "states" are identified by a
					// number among 0, 1, and 2, so just use the value in buf.
					silenceTrainer.learnStateAtFrame(buf[j], off + j);
				}
			}

			silenceTrainer.seal();
		}
	}


	public double getCumulativeLikelihood() {
		double sum = LEARN_SILENCES
				? ModelTrainer.sum(silenceTrainer.getLikelihoods())
				: 0;

		for (ModelTrainer mt: trainers) {
			sum += ModelTrainer.sum(mt.getLikelihoods());
		}

		return sum;
	}

}
