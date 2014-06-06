package fr.loria.synalp.jtrans.align;

import edu.cmu.sphinx.frontend.FloatData;
import fr.loria.synalp.jtrans.project.Anchor;
import fr.loria.synalp.jtrans.project.Token;
import fr.loria.synalp.jtrans.speechreco.s4.S4mfccBuffer;
import fr.loria.synalp.jtrans.train.SpeakerDepModelTrainer;
import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.graph.*;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.List;


/**
 * Base front-end for alignment algorithms.
 * Its main aim is to align a sequence of words to a chunk of audio data.
 */
public abstract class Aligner {

	protected SpeakerDepModelTrainer trainer;
	protected List<FloatData> data;
	protected final File audio;
	protected final ProgressDisplay progress;
	private Runnable refinementIterationHook;


	/**
	 * Compute alignment likelihood in each call to align().
	 */
	private boolean computeLikelihoods = false;

	/**
	 * Refine the baseline alignment with Metropolis-Hastings after completing
	 * Viterbi.
	 */
	private boolean refine = false;


	public Aligner(File audio, ProgressDisplay progress) {
		this.progress = progress;
		this.audio = audio;

		try {
			data = S4mfccBuffer.getAllData(audio, true);
		} catch (IOException | UnsupportedAudioFileException ex) {
			ex.printStackTrace();
			data = null;
		}
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


	public SpeakerDepModelTrainer getTrainer() {
		return trainer;
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
	 * @param endFrame last frame to analyze (inclusive)
	 */
	protected Alignment align(StateGraph graph, int startFrame, int endFrame)
			throws IOException, InterruptedException
	{
		// Space-separated string of words (used as identifier for cache files)
		final String text;

		// build wordStrings and text
		StringBuilder textBuilder = new StringBuilder();
		for (Token w: graph.getWords()) {
			textBuilder.append(w.toString()).append(" ");
		}
		text = textBuilder.toString();

		graph.setProgressDisplay(progress);

		Alignment alignment = getAlignment(graph, text, startFrame, endFrame);

		if (computeLikelihoods) {
			assert trainer != null;
			if (progress != null) {
				progress.setIndeterminateProgress("Computing likelihood...");
			}

			alignment.commitToTokens();

			for (Token w: graph.getWords()) {
				trainer.learn(w, alignment);
			}
		}

		if (refine) {
			if (progress != null) {
				progress.setIndeterminateProgress("Metropolis-Hastings...");
			}

			assert trainer != null;

			final Metropolis refinery =
					new Metropolis(alignment, trainer);

			while (!refinery.hasPlateaued()) {
				alignment = refinery.step();

				if (refinementIterationHook != null) {
					alignment.commitToTokens();
					refinementIterationHook.run();
				}
			}
		}

		alignment.commitToTokens();

		return alignment;
	}


	/**
	 * Aligns tokens between two anchors.
	 * @param reference Use {@link StateGraph} yielded by this aligner as the
	 *                  base graph for the actual alignment. If {@code null},
	 *                  a full StateGraph containing all possible pronunciations
	 *                  will be used.
	 */
	public void align(
			Anchor start,
			Anchor end,
			List<Token> words,
			Aligner reference)
			throws IOException, InterruptedException
	{
		if (words.isEmpty()) {
			return;
		}

		int frameCount = getFrameCount();

		int iFrame = start == null? 0: start.getFrame();
		int fFrame = end   == null? frameCount: end.getFrame()-1;  // see explanation below.

		/* Why did we subtract 1 frame from the final anchor's time?
		Assume that we have 2 contiguous phrases. The same anchor
		serves as the FINAL anchor in the first phrase, and as the INITIAL
		anchor in the second phrase.
		Conceptually, anchors are like points in time, but we work with frames.
		So, anchors technically cover an entire frame. Thus, to avoid that two
		contiguous phrases overlap on 1 frame (i.e. that of the anchor
		they have in common), we subtract 1 frame from the final anchor. */

		if (iFrame >= frameCount) {
			throw new IllegalArgumentException(String.format(
					"Initial frame (%d) beyond frame count (%d)! " +
							"(in phrase: %s)",
					iFrame, frameCount, words));
		}

		if (fFrame >= frameCount) {
			System.err.println("WARNING: shaving frames off final anchor! " +
					"fFrame = " + fFrame + ", frameCount = " + frameCount);
			fFrame = frameCount - 1;
		}

		if (iFrame - fFrame > 1) {
			// initial frame after final frame
			throw new IllegalArgumentException(String.format(
					"Initial frame (%d, %s) after final frame (%d, %s)! " +
							"(in phrase: %s)",
					iFrame, start, fFrame, end, words));
		} else if (iFrame > fFrame) {
			// iFrame may legally be ahead of fFrame by 1 at most if the anchors
			// are too close together (because we have removed 1 frame from the
			// final frame, see above)
			System.err.println(String.format("WARNING: skipping anchors too " +
							"close together: frame %d (initial) vs %d (final) " +
							"(in phrase: %s)",
					iFrame, fFrame, words));
			return;
		}

		StateGraph graph = new StateGraph(words);

		if (null != reference) {
			reference.setComputeLikelihoods(false);
			Alignment al = reference.align(graph, iFrame, fFrame);
			al.clearTokenAlignments();
			graph = new StateGraph(al);
		}

		align(graph, iFrame, fFrame);
	}


	/**
	 * Aligns a StateGraph and produces an HMM state timeline.
	 * @param text space-separated words. Don't use this for anything serious!
	 *             It's mainly useful as an identifier for cache files.
	 *             The actual words are contained in the StateGraph!
	 * @return a frame-by-frame timeline of HMM states
	 */
	protected abstract Alignment getAlignment(
			StateGraph graph,
			String text,
			int startFrame,
			int endFrame)
			throws IOException, InterruptedException;

}
