package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.viterbi.StateGraph;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Aligns HMM states linearly, that is, every HMM state will last roughly the
 * same amount of time. All states in the graph will be used; does not attempt
 * to walk a realistic path in the graph.
 *
 * Very fast, but wildly inaccurate; for testing purposes only.
 *
 * @see RealisticPathLinearAligner
 */
public class FastLinearAligner extends AutoAligner {

	public FastLinearAligner(
			File audio,
			ProgressDisplay progress)
	{
		super(audio, progress);
	}


	protected int[] getTimeline(
			StateGraph graph,
			String text,
			int startFrame,
			int endFrame)
	{
		final int length = boundCheckLength(startFrame, endFrame);
		return fillInterpolate(graph.getNodeCount(), new int[length]);
	}


	public static int[] fillInterpolate(List<Integer> values, int[] buf) {
		int rangeLength = values.size();

		assert rangeLength > 0;
		assert buf.length > 0;

		if (rangeLength > buf.length) {
			System.err.println("Warning: not enough slots for range; clipping");
			rangeLength = buf.length;
		}

		int v = 0;
		float nextR = 1 / (float)rangeLength;

		for (int i = 0; i < buf.length; i++) {
			float r = i / (float)buf.length;
			if (r >= nextR) {
				v++;
				nextR = (v+1) / (float)rangeLength;
			}
			buf[i] = values.get(v);
		}

		return buf;
	}


	public static int[] fillInterpolate(int rangeLength, int[] buf) {
		List<Integer> values = new ArrayList<Integer>();
		for (int i = 0; i < rangeLength; i++) {
			values.add(i);
		}
		return fillInterpolate(values, buf);
	}

}
