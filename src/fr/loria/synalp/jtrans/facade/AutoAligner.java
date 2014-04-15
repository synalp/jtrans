package fr.loria.synalp.jtrans.facade;

import edu.cmu.sphinx.frontend.FloatData;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.speechreco.s4.S4mfccBuffer;
import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.viterbi.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Base front-end for alignment algorithms.
 * Its main aim is to align a sequence of words to a chunk of audio data.
 */
public abstract class AutoAligner {

	protected List<AlignmentScorer> scorers;
	protected final List<FloatData> data;
	protected final File audio;
	protected final ProgressDisplay progress;
	private Runnable refinementIterationHook;

	List<StateGraph> concatGraphs = new ArrayList<>();
	List<int[]> concatTimelines = new ArrayList<>();


	/**
	 * Compute alignment likelihood in each call to align().
	 */
	private boolean computeLikelihoods = false;

	/**
	 * Refine the baseline alignment with Metropolis-Hastings after completing
	 * Viterbi.
	 */
	private boolean refine = false;


	public AutoAligner(File audio, ProgressDisplay progress) {
		this.progress = progress;
		this.audio = audio;

		data = S4mfccBuffer.getAllData(audio);
	}


	public void setRefine(boolean doRefine) {
		this.refine = doRefine;
	}


	public void setComputeLikelihoods(boolean computeLikelihoods) {
		this.computeLikelihoods = computeLikelihoods;
	}


	public void setScorers(int speakers) {
		scorers = new ArrayList<>(speakers);
		float[][] dataArray = S4mfccBuffer.to2DArray(data);

		for (int i = 0; i < speakers; i++) {
			scorers.add(new AlignmentScorer(dataArray,
					AlignmentScorer.MAX_UNIQUE_STATES));
			// TODO pool.size() would be better
			// TODO pool.size() currently starts at 0 and increases (anchored alignment)
		}
	}


	public int getFrameCount() {
		return data.size();
	}


	/**
	 * Sets a hook to be run after every iteration of the Metropolis-Hastings
	 * refinement process.
	 */
	public void setRefinementIterationHook(Runnable r) {
		refinementIterationHook = r;
	}


	public int boundCheckLength(int startFrame, int endFrame) {
		assert startFrame <= endFrame;
		assert startFrame >= 0;
		assert endFrame >= 0;
		assert endFrame < data.size();
		return endFrame - startFrame + 1;
	}


	/**
	 * Aligns words in a StateGraph.
	 * @param endFrame last frame to analyze
	 */
	public void align(
			final StateGraph graph,
			final int startFrame,
			final int endFrame)
			throws IOException, InterruptedException
	{
		// Space-separated string of words (used as identifier for cache files)
		final String text;

		// build wordStrings and text
		StringBuilder textBuilder = new StringBuilder();
		for (Word w: graph.getWords()) {
			textBuilder.append(w.toString()).append(" ");
		}
		text = textBuilder.toString();

		graph.setProgressDisplay(progress);

		// Cache wrapper class for getTimeline()
		class TimelineFactory implements Cache.ObjectFactory {
			public int[] make() {
				try {
					return getTimeline(graph, text, startFrame, endFrame);
				} catch (IOException ex) {
					ex.printStackTrace();
					return null;
				} catch (InterruptedException ex) {
					ex.printStackTrace();
					return null;
				}
			}
		}

		int[] timeline = (int[])Cache.cachedObject(
				getTimelineCacheDirectoryName(),
				"timeline",
				new TimelineFactory(),
				audio, text, graph.getNodeCount(), startFrame, endFrame);

		concatGraphs.add(graph);
		concatTimelines.add(timeline);

		if (computeLikelihoods) {
			assert scorers != null;
			if (progress != null) {
				progress.setIndeterminateProgress("Computing likelihood...");
			}

			graph.setWordAlignments(timeline, startFrame);

			for (Word w: graph.getWords()) {
				scorers.get(w.getSpeaker())
						.learn(w, graph, timeline, startFrame);
			}
		}

		if (refine) {
			if (progress != null) {
				progress.setIndeterminateProgress("Metropolis-Hastings...");
			}

			assert scorers != null;

			final TransitionRefinery refinery = new TransitionRefinery(
					graph, timeline, scorers);

			while (!refinery.hasPlateaued()) {
				timeline = refinery.step();

				if (refinementIterationHook != null) {
					graph.setWordAlignments(timeline, startFrame);
					refinementIterationHook.run();
				}
			}
		}

		graph.setWordAlignments(timeline, startFrame);
	}


	/**
	 * Aligns a StateGraph and produces an HMM state timeline.
	 * @param text space-separated words. Don't use this for anything serious!
	 *             It's mainly useful as an identifier for cache files.
	 *             The actual words are contained in the StateGraph!
	 * @return a frame-by-frame timeline of HMM states
	 */
	protected abstract int[] getTimeline(
			StateGraph graph,
			String text,
			int startFrame,
			int endFrame)
			throws IOException, InterruptedException;


	/**
	 * Returns a name for the directory containing cached timelines produced
	 * by/for this aligner. Different alignment algorithms should not share the
	 * same cache!
	 */
	protected String getTimelineCacheDirectoryName() {
		final String suffix = "Aligner";
		String name = getClass().getSimpleName();
		assert name.endsWith(suffix);
		name = name.substring(0, name.length() - suffix.length());
		return "timeline_" + name.toLowerCase();
	}


	public void printScores() {
		AlignmentScorer scorer = AlignmentScorer.merge(scorers);
		scorer.finishLearning();
		scorer.score();
		double sum = AlignmentScorer.sum(scorer.getLikelihoods());
		System.out.println("Overall likelihood " + sum);
	}


	public StatePath getConcatenatedPath() {
		// TODO: should throw an error on overlaps
		StatePath[] paths = new StatePath[concatGraphs.size()];
		for (int i = 0; i < concatGraphs.size(); i++) {
			paths[i] = new StatePath(concatGraphs.get(i), concatTimelines.get(i));
		}
		return new StatePath(paths);
	}

}
