package fr.loria.synalp.jtrans.facade;

import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.speechreco.s4.S4ForceAlignBlocViterbi;
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

	protected final File audio;
	protected final int appxTotalFrames;
	protected final S4mfccBuffer mfcc;
	protected final ProgressDisplay progress;
	protected Runnable refinementIterationHook;


	/**
	 * Compute alignment likelihood in each call to align().
	 */
	public static boolean COMPUTE_LIKELIHOODS = false;


	/**
	 * Refine the baseline alignment with Metropolis-Hastings after completing
	 * Viterbi.
	 */
	public static boolean METROPOLIS_HASTINGS_POST_PROCESSING = false;


	public AutoAligner(File audio, int appxTotalFrames, ProgressDisplay progress) {
		this.audio = audio;
		this.appxTotalFrames = appxTotalFrames;
		this.progress = progress;

		mfcc = new S4mfccBuffer();
		AudioFileDataSource afds = new AudioFileDataSource(3200, null);
		afds.setAudioFile(audio, null);
		mfcc.setSource(S4ForceAlignBlocViterbi.getFrontEnd(afds));
	}


	/**
	 * Sets a hook to be run after every iteration of the Metropolis-Hastings
	 * refinement process.
	 */
	public void setRefinementIterationHook(Runnable r) {
		refinementIterationHook = r;
	}


	/**
	 * Align words between startWord and endWord.
	 *
	 * @param endFrame last frame to analyze. Use a negative number to use all
	 *                 frames in the audio source
	 *
	 * @return likelihood of the alignment (or Double.NEGATIVE_INFINITY if
	 * COMPUTE_LIKELIHOODS is false)
	 */
	public double align(
			final List<Word> words,
			final int startFrame,
			final int endFrame)
			throws IOException, InterruptedException
	{
		if (progress != null) {
			progress.setIndeterminateProgress("Setting up state graph...");
		}

		// String representations of each word (used to build StateGraph)
		final String[] wordStrings = new String[words.size()];

		// Space-separated string of words (used as identifier for cache files)
		final String text;

		// build wordStrings and text
		StringBuilder textBuilder = new StringBuilder();
		for (int i = 0; i < words.size(); i++) {
			Word w = words.get(i);
			wordStrings[i] = w.toString();
			textBuilder.append(w.toString()).append(" ");
		}
		text = textBuilder.toString();

		final StateGraph graph = new StateGraph(wordStrings);
		graph.setProgressDisplay(progress, appxTotalFrames);

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

		int[] timeline = (int[])Cache.cachedObject("hmmtimeline", "timeline",
				new TimelineFactory(),
				audio, text, startFrame, endFrame);

		AlignmentScorer scorer = null;
		if (METROPOLIS_HASTINGS_POST_PROCESSING || COMPUTE_LIKELIHOODS) {
			scorer = new AlignmentScorer(graph,
					AlignmentScorer.getData(timeline.length, mfcc, startFrame));
		}

		if (METROPOLIS_HASTINGS_POST_PROCESSING) {
			if (progress != null) {
				progress.setIndeterminateProgress("Metropolis-Hastings...");
			}

			assert scorer != null;
			final TransitionRefinery refinery = new TransitionRefinery(
					timeline, scorer);

			while (!refinery.hasPlateaued()) {
				timeline = refinery.step();

				if (refinementIterationHook != null) {
					graph.setWordAlignments(words, timeline, startFrame);
					refinementIterationHook.run();
				}
			}
		}

		graph.setWordAlignments(words, timeline, startFrame);

		if (COMPUTE_LIKELIHOODS) {
			assert scorer != null;
			if (progress != null) {
				progress.setIndeterminateProgress("Computing likelihood...");
			}
			return scorer.cumulativeAlignmentLikelihood(timeline);
		} else {
			return Double.NEGATIVE_INFINITY;
		}
	}


	/**
	 * Aligns a StateGraph and produces an HMM state timeline.
	 * @param text space-separated words (mainly useful as an identifier for
	 *             cache files)
	 * @return a frame-by-frame timeline of HMM states
	 */
	protected abstract int[] getTimeline(
			StateGraph graph,
			String text,
			int startFrame,
			int endFrame)
			throws IOException, InterruptedException;

}
