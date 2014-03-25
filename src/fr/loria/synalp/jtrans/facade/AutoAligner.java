package fr.loria.synalp.jtrans.facade;

import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.speechreco.s4.S4ForceAlignBlocViterbi;
import fr.loria.synalp.jtrans.speechreco.s4.S4mfccBuffer;
import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.viterbi.StateGraph;
import fr.loria.synalp.jtrans.viterbi.SwapDeflater;
import fr.loria.synalp.jtrans.viterbi.SwapInflater;

import java.io.*;
import java.util.List;

public class AutoAligner {

	private final File audio;
	private final int appxTotalFrames;
	private final S4mfccBuffer mfcc;
	private final ProgressDisplay progress;
	private final SwapDeflater swapWriter;
	private final SwapInflater swapReader;


	/**
	 * Maximum number of bytes for a Viterbi backtrack stack to reside in
	 * memory. Above this threshold, the stack will be swapped to disk.
	 * Only applies to FULL_BACKTRACK_VITERBI.
	 */
	public static final int SWAP_THRESHOLD_BYTES = 1024*1024*16;


	/**
	 * Delete Viterbi backpointer swap files when the JVM terminates.
	 */
	public static boolean DELETE_BACKTRACK_SWAP_FILES = true;


	/**
	 * Compute alignment likelihood in each call to align().
	 */
	public static boolean COMPUTE_LIKELIHOODS = false;


	public AutoAligner(File audio, int appxTotalFrames, ProgressDisplay progress) throws IOException {
		this.audio = audio;
		this.appxTotalFrames = appxTotalFrames;
		this.progress = progress;

		mfcc = new S4mfccBuffer();
		AudioFileDataSource afds = new AudioFileDataSource(3200, null);
		afds.setAudioFile(audio, null);
		mfcc.setSource(S4ForceAlignBlocViterbi.getFrontEnd(afds));

		swapWriter = SwapDeflater.getSensibleSwapDeflater(true);
		swapReader = new SwapInflater();
	}


	/**
	 * Align words between startWord and endWord using Sphinx and the revised
	 * Viterbi algorithm. Slow, and disk space hungry, but accurate.
	 *
	 * The result is not merged into the main alignment (use merge()).
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

		graph.setWordAlignments(words, timeline, startFrame);

		if (COMPUTE_LIKELIHOODS) {
			if (progress != null) {
				progress.setIndeterminateProgress("Computing likelihood...");
			}
			return graph.alignmentLikelihood(timeline, mfcc, startFrame);
		} else {
			return Double.NEGATIVE_INFINITY;
		}
	}


	/**
	 * Sets up I/O streams, aligns a StateGraph and returns an HMM state
	 * timeline.
	 */
	private int[] getTimeline(
			StateGraph graph,
			String text,
			int startFrame,
			int endFrame)
			throws IOException, InterruptedException
	{
		final OutputStream out;
		final SwapInflater.InputStreamFactory inFactory;

		// Get a ballpark measurement of the final size of the uncompressed
		// backpointer table to determine if we're going to swap to disk or
		// keep it all in RAM

		int appxEndFrame = endFrame < 0? appxTotalFrames: endFrame;
		long projectedSize =
				(long)(appxEndFrame-startFrame+1) * graph.getStateCount();
		assert projectedSize >= 0: "integer overflow";

		if (projectedSize <= SWAP_THRESHOLD_BYTES) {
			out = new ByteArrayOutputStream();
			inFactory = new SwapInflater.InputStreamFactory() {
				@Override
				public InputStream make() throws IOException {
					return new ByteArrayInputStream(
							((ByteArrayOutputStream)out).toByteArray());
				}
			};
		} else {
			final File swapFile = Cache.getCacheFile("backtrack", "swp",
					audio, text, startFrame, endFrame);

			if (DELETE_BACKTRACK_SWAP_FILES) {
				swapFile.deleteOnExit();
			}

			System.out.println("Swap file: " + swapFile);
			System.out.println("Projected backpointer size (uncompressed): "
					+ projectedSize/1024/1024 + " MB");

			out = new FileOutputStream(swapFile);
			inFactory = new SwapInflater.InputStreamFactory() {
				@Override
				public InputStream make() throws IOException {
					return new FileInputStream(swapFile);
				}
			};
		}

		//----------------------------------------------------------------------
		// Run alignment

		swapWriter.init(graph.getStateCount(), out);
		graph.viterbi(mfcc, swapWriter, startFrame, endFrame);

		swapReader.init(swapWriter.getIndex(), inFactory);
		return graph.backtrack(swapReader);
	}

}
