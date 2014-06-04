package fr.loria.synalp.jtrans.align;

import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.graph.StateGraph;

import java.io.File;
import java.util.Arrays;


/**
 * Aligns HMM states linearly, that is, every HMM state will last roughly the
 * same amount of time. Very fast, but wildly inaccurate; for testing purposes
 * only.
 * <p/>
 * All states in the graph will be used; does not attempt to walk a realistic
 * path in the graph. That is, if a word has several pronunciations that are
 * adjacent in the graph, this aligner will align all of them in a sequence.
 * <p/>
 * To walk a realistic path in the graph (i.e. that uses a single pronunciation
 * for each word), you can use the path yielded by a Viterbi reference aligner
 * ('-r' switch on the command line).
 */
public class LinearAligner extends Aligner {

	public LinearAligner(File audio, ProgressDisplay progress) {
		super(audio, progress);
	}


	protected Alignment getAlignment(
			StateGraph graph,
			String text,
			int startFrame,
			int endFrame)
	{
		final int length = boundCheckLength(startFrame, endFrame);
		int[] nodes = new int[length];
		fillInterpolate(graph.getNodeCount(), nodes, 0, length);
		return graph.alignmentFromNodeTimeline(nodes, startFrame);
	}


	public static void fillInterpolate(
			int valueCount,
			int[] buf,
			int offset,
			int length)
	{
		assert offset+length <= buf.length;
		int[] valueLengths = interpolatedLengths(valueCount, length);
		for (int i = 0; i < valueLengths.length; i++) {
			Arrays.fill(buf, offset, offset+valueLengths[i], i);
			offset += valueLengths[i];
		}
	}


	public static int[] interpolatedLengths(int valueCount, int acrossLength) {
		assert valueCount > 0;
		assert acrossLength > 0;

		if (valueCount > acrossLength) {
			System.err.println("Warning: not enough slots for all values! clipping!");
			valueCount = acrossLength;
		}

		int n = 0;
		float threshold = 1 / (float)valueCount;
		int[] lengths = new int[valueCount];

		for (int t = 0; t < acrossLength; t++) {
			float frac = t / (float)acrossLength;
			if (frac >= threshold) {
				n++;
				threshold = (n+1) / (float)valueCount;
			}
			lengths[n]++;
		}

		return lengths;
	}

}
