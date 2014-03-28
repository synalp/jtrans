package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.viterbi.StateGraph;
import fr.loria.synalp.jtrans.viterbi.SwapDeflater;
import fr.loria.synalp.jtrans.viterbi.SwapInflater;

import java.io.*;


/**
 * Aligns HMM states using Sphinx and the revised Viterbi algorithm.
 * Slow, and disk space hungry, but accurate.
 *
 * @see StateGraph#viterbi
 */
public class ViterbiAligner extends AutoAligner {

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


	private final SwapDeflater swapWriter;
	private final SwapInflater swapReader;


	public ViterbiAligner(File audio, int appxTotalFrames, ProgressDisplay progress)
		throws IOException
	{
		super(audio, appxTotalFrames, progress);

		swapWriter = SwapDeflater.getSensibleSwapDeflater(true);
		swapReader = new SwapInflater();
	}


	protected int[] getTimeline(
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
				(long)(appxEndFrame-startFrame+1) * graph.getNodeCount();
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

		swapWriter.init(graph.getNodeCount(), out);
		graph.viterbi(mfcc, swapWriter, startFrame, endFrame);

		swapReader.init(swapWriter.getIndex(), inFactory);
		return graph.backtrack(swapReader);
	}

}
