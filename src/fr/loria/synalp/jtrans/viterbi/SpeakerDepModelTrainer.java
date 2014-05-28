package fr.loria.synalp.jtrans.viterbi;

import fr.loria.synalp.jtrans.elements.Word;

import java.util.ArrayList;
import java.util.List;

/**
 * Speaker-dependent model trainer.
 * @see ModelTrainer
 */
public class SpeakerDepModelTrainer {

	protected List<ModelTrainer> trainers;

	public SpeakerDepModelTrainer(int speakers, float[][] dataArray) {
		trainers = new ArrayList<>(speakers);

		for (int i = 0; i < speakers; i++) {
			trainers.add(new ModelTrainer(dataArray));
		}
	}


	public void learn(Word w, StateTimeline timeline, int startFrame) {
		trainers.get(w.getSpeaker()).learn(w, timeline, startFrame);
	}


	public void clear() {
		for (ModelTrainer mt: trainers) {
			mt.clear();
		}
	}


	public void seal() {
		for (ModelTrainer mt: trainers) {
			mt.seal();
		}
	}


	public double getCumulativeLikelihood() {
		double sum = 0;
		for (ModelTrainer mt: trainers) {
			sum += ModelTrainer.sum(mt.getLikelihoods());
		}
		return sum;
	}

}
