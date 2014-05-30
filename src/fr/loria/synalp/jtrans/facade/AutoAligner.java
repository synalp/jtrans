package fr.loria.synalp.jtrans.facade;

import edu.cmu.sphinx.frontend.FloatData;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.speechreco.s4.S4mfccBuffer;
import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.viterbi.*;

import java.io.*;
import java.util.List;


/**
 * Base front-end for alignment algorithms.
 * Its main aim is to align a sequence of words to a chunk of audio data.
 */
public abstract class AutoAligner {

	protected SpeakerDepModelTrainer trainer;
	protected final List<FloatData> data;
	protected final File audio;
	protected final ProgressDisplay progress;
	private Runnable refinementIterationHook;

	/**
	 * Concatenation of timelines obtained via successive calls to
	 * {@link #align}.
	 */
	private Alignment concatenated = new Alignment(0);


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


	public void initTrainers(int speakers) {
		trainer = new SpeakerDepModelTrainer(
				speakers, S4mfccBuffer.to2DArray(data));
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
	 * @param concatenate Whether to concatenate the resulting timeline
	 *                    internally (don't do that with overlapping speech!),
	 *                    to prepare a call to getConcatenatedPath().
	 *                    When concatenating, calls to align() must reflect the
	 *                    chronological order of the pieces of text to align!
	 */
	public void align(
			final StateGraph graph,
			final int startFrame,
			final int endFrame,
			boolean concatenate)
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

		Alignment alignment = getCachedTimeline(
				graph, text, startFrame, endFrame);

		if (concatenate) {
			/* Technically, we don't *need* to pad the concatenated timeline,
			since we only need the state sequence (in getConcatenatedPath()),
			which isn't affected by timing info. But it makes sense to pad it
			anyway, just so it is chronologically correct if we ever need it. */
			concatenated.pad(startFrame);
			concatenated.concatenate(alignment);
		}

		if (computeLikelihoods) {
			assert trainer != null;
			if (progress != null) {
				progress.setIndeterminateProgress("Computing likelihood...");
			}

			alignment.commitToWords();

			for (Word w: graph.getWords()) {
				trainer.learn(w, alignment);
			}
		}

		if (refine) {
			if (progress != null) {
				progress.setIndeterminateProgress("Metropolis-Hastings...");
			}

			assert trainer != null;

			final TransitionRefinery refinery =
					new TransitionRefinery(alignment, trainer);

			while (!refinery.hasPlateaued()) {
				alignment = refinery.step();

				if (refinementIterationHook != null) {
					alignment.commitToWords();
					refinementIterationHook.run();
				}
			}
		}

		alignment.commitToWords();
	}


	public Alignment getCachedTimeline(
			final StateGraph graph,
			final String text,
			final int startFrame,
			final int endFrame)
	{
		// Cache wrapper class for getTimeline()
		class TimelineFactory implements Cache.ObjectFactory {
			public Alignment make() {
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

		/*
		return (StateTimeline)Cache.cachedObject(
				getTimelineCacheDirectoryName(),
				"timeline",
				new TimelineFactory(),
				audio, text, graph.getNodeCount(), startFrame, endFrame);
		*/
		System.out.println("Reimplement me! Cached timeline");

		return new TimelineFactory().make();
	}


	/**
	 * Aligns a StateGraph and produces an HMM state timeline.
	 * @param text space-separated words. Don't use this for anything serious!
	 *             It's mainly useful as an identifier for cache files.
	 *             The actual words are contained in the StateGraph!
	 * @return a frame-by-frame timeline of HMM states
	 */
	protected abstract Alignment getTimeline(
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


	/*
	public void dumpMergedTrainer() {
		ModelTrainer trainer = ModelTrainer.merge(trainer);
		trainer.score();
		trainer.dump();
	}
	*/


	/**
	 * Returns concatenation of timelines obtained via successive calls to
	 * {@link #align}.
	 */
	public Alignment getConcatenatedTimeline() {
		// TODO: should throw an error on overlaps
		return new Alignment(concatenated);
	}

}
