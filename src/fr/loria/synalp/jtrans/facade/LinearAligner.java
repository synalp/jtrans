package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.viterbi.StateGraph;

import java.io.File;
import java.io.IOException;


/**
 * Aligns HMM states linearly, that is, every HMM state will last roughly the
 * same amount of time.
 * Very fast, but wildly inaccurate; only useful for debugging.
 */
public class LinearAligner extends AutoAligner {

	public LinearAligner(
			File audio,
			int appxTotalFrames,
			ProgressDisplay progress)
	{
		super(audio, appxTotalFrames, progress);
	}


	protected int[] getTimeline(
			StateGraph graph,
			String text,
			int startFrame,
			int endFrame)
			throws IOException, InterruptedException
	{
		final int length = endFrame - startFrame + 1;
		return fillInterpolate(graph.getStateCount(), new int[length]);
	}


	public static int[] fillInterpolate(int rangeLength, int[] buf) {
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
			buf[i] = v;
		}

		return buf;
	}

}
