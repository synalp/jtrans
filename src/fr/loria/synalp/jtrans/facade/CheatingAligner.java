package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.viterbi.Alignment;
import fr.loria.synalp.jtrans.viterbi.StateGraph;

import java.io.File;
import java.io.IOException;

/**
 * Aligner that walks the same path in the state graph as the Viterbi aligner
 * would.
 *
 * Shares ViterbiAligner's cache when determining the reference path.
 *
 * For testing purposes only, e.g. to compare with likelihoods yielded by
 * ViterbiAligner.
 */
public abstract class CheatingAligner extends AutoAligner {

	protected ViterbiAligner pathfinder;


	public CheatingAligner(File audio, ProgressDisplay progress)
		throws IOException
	{
		super(audio, progress);
		pathfinder = new ViterbiAligner(audio, progress);
	}


	@Override
	protected Alignment getAlignment(
			StateGraph graph,
			String text,
			int startFrame,
			int endFrame)
			throws IOException, InterruptedException
	{
		return tweak(pathfinder.getAlignment(
				graph, text, startFrame, endFrame));
	}


	protected abstract Alignment tweak(Alignment baseline);

}
