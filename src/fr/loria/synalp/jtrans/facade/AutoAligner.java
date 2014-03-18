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
	 */
	public void align(List<Word> words, int startFrame, int endFrame)
			throws IOException, InterruptedException
	{
		//----------------------------------------------------------------------
		// Initialize graph and swap streams

		if (progress != null) {
			progress.setIndeterminateProgress("Setting up state graph...");
		}

		String[] wordStrings = new String[words.size()];
		for (int i = 0; i < words.size(); i++) {
			wordStrings[i] = words.get(i).toString();
		}

		StateGraph graph = new StateGraph(wordStrings);
		graph.setProgressDisplay(progress, appxTotalFrames);

		final OutputStream out;
		final SwapInflater.InputStreamFactory inFactory;

		// Get a ballpark measurement of the final size of the uncompressed
		// backpointer table to determine if we're going to swap to disk or
		// keep it all in RAM

		int appxEndFrame = endFrame < 0? appxTotalFrames: endFrame;
		int projectedSize = (appxEndFrame-startFrame+1) * graph.getStateCount();
		boolean keepInRAM = projectedSize <= SWAP_THRESHOLD_BYTES;

		if (keepInRAM) {
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
					audio, words, startFrame, endFrame);
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
		int[] timeline = graph.backtrack(swapReader);

		graph.setWordAlignments(words, timeline, startFrame);
	}

}
