package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.viterbi.StateGraph;

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
public class RealisticPathLinearAligner extends AutoAligner {

	private ViterbiAligner pathfinder;


	public RealisticPathLinearAligner(File audio, ProgressDisplay progress)
			throws IOException
	{
		super(audio, progress);
		pathfinder = new ViterbiAligner(audio, progress);
	}


	@Override
	protected int[] getTimeline(
			StateGraph graph,
			String text,
			int startFrame,
			int endFrame)
			throws IOException, InterruptedException
	{
		final int length = boundCheckLength(startFrame, endFrame);

		int[] refTL = pathfinder.getTimeline(graph, text, startFrame, endFrame);

		List<Integer> values = new ArrayList<Integer>();

		for (int i = 0; i < refTL.length; i++) {
			int state = refTL[i];
			if (i == 0 || refTL[i-1] != state) {
				values.add(state);
			}
		}

		return FastLinearAligner.fillInterpolate(values, new int[length]);
	}

}
