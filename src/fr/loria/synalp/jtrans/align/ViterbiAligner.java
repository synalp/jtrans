package fr.loria.synalp.jtrans.align;

import fr.loria.synalp.jtrans.utils.Cache;
import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.graph.StateGraph;
import fr.loria.synalp.jtrans.graph.swap.SwapDeflater;
import fr.loria.synalp.jtrans.graph.swap.SwapInflater;

import java.io.*;


/**
 * Aligns HMM states using Sphinx and the revised Viterbi algorithm.
 * Slow, and disk space hungry, but accurate.
 *
 * @see StateGraph#viterbi
 */
public class ViterbiAligner extends Aligner {
    public static String saveForwardBackward = null;

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


	public ViterbiAligner(File audio, ProgressDisplay progress)
		throws IOException
	{
		super(audio, progress);

		swapWriter = SwapDeflater.getSensibleSwapDeflater(true);
		swapReader = new SwapInflater();
	}


	@Override
	public Alignment getAlignment(
			final StateGraph graph,
			final String text,
			final int startFrame,
			final int endFrame)
			throws InterruptedException
	{
		Cache.ObjectFactory factory = new Cache.ObjectFactory() {
			public int[] make() throws InterruptedException {
				try {
					return getRawTimeline(graph, text, startFrame, endFrame);
				} catch (IOException ex) {
					ex.printStackTrace();
					return null;
				}
			}
		};

		int[] tl = (int[])Cache.cachedObject(
				"viterbi",
				"timeline",
				factory,
				audio, text, graph.getNodeCount(), startFrame, endFrame);

		return graph.alignmentFromNodeTimeline(tl, startFrame);
	}


	protected int[] getRawTimeline(
			StateGraph graph,
			String text,
			int startFrame,
			int endFrame)
			throws IOException, InterruptedException
	{
		int length = boundCheckLength(startFrame, endFrame);

		final OutputStream out;
		final SwapInflater.InputStreamFactory inFactory;

		// Get a ballpark measurement of the final size of the uncompressed
		// backpointer table to determine if we're going to swap to disk or
		// keep it all in RAM

		long projectedSize = (long)length * graph.getNodeCount();
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
        int[] timeline;
		if (ViterbiAligner.saveForwardBackward!=null) {
            timeline = graph.forward(data, startFrame, endFrame);
        } else {
            graph.viterbi(data, swapWriter, startFrame, endFrame);
            swapReader.init(swapWriter.getIndex(), inFactory);
		    timeline = graph.backtrack(swapReader);
        }
		assert timeline.length == length;

		return timeline;
	}

}
