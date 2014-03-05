package fr.loria.synalp.jtrans.viterbi;

import fr.loria.synalp.jtrans.utils.BufferUtils;

import java.io.*;
import java.util.Arrays;
import java.util.zip.Deflater;

/**
 * Multithreaded compressed swap writer for Viterbi backtracking.
 *
 * A single instance may be reused for several different graphs to avoid wasting
 * time re-allocating buffers.
 *
 * @see StateGraph
 */
public class SwapDeflater {

	/**
	 * Whenever the deflater is being reinitialized, we try to stick to this
	 * value for the sizes of the buffers.
	 */
	private final int approxBytesPerPage;

	private final Deflater def;

	/** Compressed data buffer that the flush thread writes into */
	private final byte[] compBuffer;

	/** Number of states for the graph currently being analyzed */
	private int nStates;

	/** Maximum number of frames before flushing the page and starting over */
	private int maxFramesPerPage;

	/** Where the swap gets written */
	private OutputStream out;

	/** Contains offsets of pages in the swap file among other things */
	private PageIndex index;

	// NEVER EVER USE previousRun.length! Since the buffer might be
	// reused, its length may not be what you expect. Use nStates instead
	/**
	 * Values of bestInTrans from the previous frame
	 * (used for pre-compression filtering).
	 * MUST BE FILLED WITH ZEROES BEFORE STARTING A NEW PAGE!!!
 	 */
	private byte[] previousRun;

	/** Uncompressed data buffer that the main thread writes into */
	private byte[] frontBuffer;

	/** Number of frames stored in the front buffer */
	private int frontBufferFrames;

	/** Uncompressed data buffer that the flush thread reads from */
	private byte[] backBuffer;

	/**
	 * Number of frames stored in the back buffer.
	 * A value of 0 signifies that the back buffer has been processed by the
	 * flush thread and the buffers are ready to be swapped.
	 */
	private int backBufferFrames;

	/** Thread that compresses data in the background */
	private Thread flushThread;

	/** Tells flushThread to stop consuming pages */
	private boolean done = false;


	private class FlushThread extends Thread {
		private FlushThread() {
			super("SwapDeflater flusher");
		}

		@Override
		public void run() {
			try {
				while (!done) {
					consumePage();
				}
			} catch (IOException ex) {
				throw new Error(ex);
			} catch (InterruptedException ex) {
				throw new Error(ex);
			}
			System.out.println("Flush thread dead");
		}
	}


	/**
	 * @param approxBytesPerPage Ballpark measurement of the size of a single
	 * uncompressed page (in bytes). The compression process will need at least
	 * twice as much bytes in memory. Don't go overboard with this value: past
	 * a certain size, larger pages yield marginally better compression,
	 * but they typically adversely affect performance.
	 * @param deflater Unless you're seriously strapped for disk space, we
	 * recommend using Deflater.BEST_SPEED with HUFFMAN_ONLY.
	 */
	public SwapDeflater(int approxBytesPerPage, Deflater deflater)
			throws IOException
	{
		this.approxBytesPerPage = approxBytesPerPage;
		def = deflater;
		compBuffer  = new byte[1048576];
	}


	/**
	 * @param nStates number of states in the vector
	 * @param out swap output stream (can be a file, but can also reside
	 *            in RAM if you have enough of it)
	 */
	public void init(int nStates, OutputStream out) {
		assert backBufferFrames == 0;
		assert frontBufferFrames == 0;
		assert flushThread == null || !flushThread.isAlive();

		this.nStates = nStates;
		this.out = out;

		index = new PageIndex(nStates);

		maxFramesPerPage = Math.max(1, approxBytesPerPage / nStates);
		int pageLength = maxFramesPerPage * nStates;
		System.out.println("Page length: " + pageLength + " bytes ("
				+ maxFramesPerPage + " frames)");

		frontBuffer = BufferUtils.grow(frontBuffer, pageLength);
		backBuffer = BufferUtils.grow(backBuffer, pageLength);
		previousRun = BufferUtils.grow(previousRun, nStates);
		resetFilter();

		frontBufferFrames = 0;
		backBufferFrames = 0;
		flushThread = null;
		done = false;
	}


	/**
	 * Resets the pre-compression filter.
	 * Must be done before starting a new page, otherwise the semi-random
	 * accessibility of the page system will be broken!
	 */
	private void resetFilter() {
		Arrays.fill(previousRun, (byte)0);
	}


	/**
	 * Creates a SwapDeflater with sensible memory and compression settings.
	 * @param compress use compression. Disabling compression dramatically
	 * speeds up the swapping process, but the trade-off is that swap files
	 * become enormous when working on long recordings.
	 */
	public static SwapDeflater getSensibleSwapDeflater(boolean compress)
			throws IOException
	{
		Deflater deflater = new Deflater(
				compress? Deflater.BEST_SPEED: Deflater.NO_COMPRESSION);
		deflater.setStrategy(Deflater.HUFFMAN_ONLY);
		return new SwapDeflater(1024*1024*16, deflater);
	}


	public PageIndex getIndex() {
		return index;
	}


	// Thread safety: don't access instance variables frontBuffer/backBuffer
	// nor backBufferFrames directly in this method, because they may change
	// at any time in the main thread. Use the provided parameters instead.
	private void fullFlush(byte[] rawBuf, int framesInBuf) throws IOException {
		def.reset();
		def.setInput(rawBuf, 0, framesInBuf * nStates);
		def.finish();
		assert !def.finished();

		while (!def.finished()) {
			int len = def.deflate(compBuffer);
			if (len > 0) {
				out.write(compBuffer, 0, len);
			}
			System.out.print(len <= 0? "?": def.needsInput()? "!": ".");
		}

		assert def.finished();

		index.putPage(framesInBuf, (int)def.getBytesWritten());

		System.out.println(String.format(
				"[Frame %d] backtrack footprint: %s",
				index.getFrameCount(),
				index.getCompressedBytes()));
	}


	public void write(byte[] n) throws IOException, InterruptedException {
		assert nStates == n.length;

		final int fbOffset = frontBufferFrames*nStates;

		// Filter
		for (int i = 0; i < nStates; i++) {
			frontBuffer[fbOffset+i] = (byte)(n[i] - previousRun[i]);
		}

		System.arraycopy(n, 0, previousRun, 0, nStates);

		frontBufferFrames++;

		if (frontBufferFrames % maxFramesPerPage == 0) {
			System.out.print("J");
			producePage();
			resetFilter();
		}
	}


	// Called from main thread
	private synchronized void producePage() throws InterruptedException {
		if (flushThread == null) {
			flushThread = new FlushThread();
			flushThread.start();

			// Wait for thread to start up
			wait();
		}

		// Wait for flushThread to finish working on the back buffer
		while (backBufferFrames > 0) {
			wait();
		}

		// Swap buffers
		byte[] tmp = frontBuffer;
		frontBuffer = backBuffer;
		backBuffer = tmp;

		backBufferFrames = frontBufferFrames;
		frontBufferFrames = 0;

		// Allow flushThread to use the back buffer
		notify();
	}


	// Called from flushThread
	private synchronized void consumePage() throws IOException, InterruptedException {
		// Tell main thread we're ready to work with the back buffer
		notify();

		// Wait for the main thread to release the back buffer
		while (!done && backBufferFrames == 0) {
			wait();
		}

		if (done) {
			assert backBufferFrames == 0;
			return;
		}

		// Do the heavy lifting
		fullFlush(backBuffer, backBufferFrames);

		// Release back buffer
		backBufferFrames = 0;
	}


	// Called from main thread
	public void close() throws IOException, InterruptedException {
		producePage();

		synchronized (this) {
			// wait for flush thread to consume the rest
			while (backBufferFrames > 0) {
				wait();
			}
			done = true;
			notify();
		}

		flushThread.join();
		flushThread = null;
		out.flush();
		out.close();
		System.out.println("Swap closed");
	}

}
