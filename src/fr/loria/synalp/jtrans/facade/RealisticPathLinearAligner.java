package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.viterbi.StateTimeline;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * "Smart" linear aligner that walks the same path in the state graph as the
 * Viterbi aligner would. Every HMM state will last roughly the same amount of
 * time.
 *
 * Slow and inaccurate, but more realistic than {@link FastLinearAligner}.
 * For testing purposes only, e.g. to compare with likelihoods yielded by
 * ViterbiAligner.
 *
 * @see FastLinearAligner
 */
public class RealisticPathLinearAligner extends CheatingAligner {

	public RealisticPathLinearAligner(File audio, ProgressDisplay progress)
			throws IOException
	{
		super(audio, progress);
	}


	@Override
	protected StateTimeline getTimeline(StateTimeline baseline) {
		List<Integer> values = new ArrayList<>();

		/*
		for (int i = 0; i < baseline.length; i++) {
			int node = baseline[i];
			if (i == 0 || baseline[i-1] != node) {
				values.add(node);
			}
		}

		return FastLinearAligner.fillInterpolate(
				values, new int[baseline.length], 0, baseline.length);
		*/

		throw new Error("Reimplement me!");
	}

}
