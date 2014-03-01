package fr.loria.synalp.jtrans.anchorless;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.Deflater;

/**
 * Multithreaded compressed swap writer
 */
class SwapDeflater {

	private final int maxFramesPerPage;

	private final OutputStream out;
	private final Deflater def;
	private final PageIndex index;

	private int[] previousBestParent;

	/** Uncompressed data buffer that the main thread writes into */
	private ByteBuffer frontBuffer;

	/** Number of frames stored in the front buffer */
	private int frontBufferFrames;

	/** Uncompressed data buffer that the flush thread reads from */
	private ByteBuffer backBuffer;

	/**
	 * Number of frames stored in the back buffer.
	 * A value of 0 signifies that the back buffer has been processed by the
	 * flush thread and the buffers are ready to be swapped.
	 */
	private int backBufferFrames;

	/** Compressed data buffer that the flush thread writes into */
	private byte[] compBuffer;

	/** Thread that compresses data in the background */
	private Thread flushThread;

	/** Tells flushThread to stop consuming pages */
	private boolean done = false;


	private class FlushThread extends Thread {
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
	 * @param nStates number of states in the vector
	 * @param out swap output stream (can be a file, but can also reside
	 *            in RAM if you have enough of it)
	 */
	public SwapDeflater(
			int approxBytesPerPage,
			Deflater deflater,
			int nStates,
			OutputStream out)
			throws IOException
	{
		index = new PageIndex(nStates);

		maxFramesPerPage = approxBytesPerPage / index.bytesPerFrame;
		assert maxFramesPerPage >= 1;
		System.out.println("Max Frames Per Page : " + maxFramesPerPage);

		this.out = out;

		def = deflater;

		frontBuffer = ByteBuffer.allocate(maxFramesPerPage * index.bytesPerFrame);
		backBuffer  = ByteBuffer.allocate(maxFramesPerPage * index.bytesPerFrame);

		compBuffer = new byte[1048576];
		previousBestParent = new int[nStates];
	}


	/**
	 * Creates a SwapDeflater with sensible memory and compression settings.
	 * @param nStates number of states in the vector
	 * @param out swap output stream (can be a file, but can also reside
	 *            in RAM if you have enough of it)
	 * @param compress use compression. Disabling compression dramatically
	 * speeds up the swapping process, but the trade-off is that swap files
	 * become enormous when working on long recordings.
	 */
	public static SwapDeflater getSensibleSwapDeflater(
			int nStates,
			OutputStream out,
			boolean compress)
		throws IOException
	{
		Deflater deflater = new Deflater(
				compress? Deflater.BEST_SPEED: Deflater.NO_COMPRESSION);
		deflater.setStrategy(Deflater.HUFFMAN_ONLY);
		return new SwapDeflater(1024*1024*16, deflater, nStates, out);
	}


	public PageIndex getIndex() {
		return index;
	}


	// Thread safety: don't access instance variables frontBuffer/backBuffer
	// nor backBufferFrames directly in this method, because they may change
	// at any time in the main thread. Use the provided parameters instead.
	private void fullFlush(ByteBuffer rawBuf, int framesInBuf) throws IOException {
		assert rawBuf.hasArray();

		def.reset();
		def.setInput(rawBuf.array(), rawBuf.arrayOffset(),
				framesInBuf * index.bytesPerFrame);
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

		rawBuf.rewind();

		index.putPage(framesInBuf, (int)def.getBytesWritten());

		System.out.println(String.format(
				"[Frame %d] backtrack footprint: %s",
				index.getFrameCount(),
				index.getCompressedBytes()));
	}


	public void write(int[] n) throws IOException, InterruptedException {
		assert previousBestParent.length == n.length;

		if (index.useShorts) {
			for (int i = 0; i < previousBestParent.length; i++) {
				frontBuffer.putShort((short) (n[i] - previousBestParent[i]));
			}
		} else {
			for (int i = 0; i < previousBestParent.length; i++) {
				frontBuffer.putInt(n[i] - previousBestParent[i]);
			}
		}

		System.arraycopy(n, 0, previousBestParent, 0, previousBestParent.length);

		frontBufferFrames++;

		if (frontBufferFrames % maxFramesPerPage == 0) {
			System.out.print("J");
			producePage();
			Arrays.fill(previousBestParent, 0);
		}
	}


	// Called from main thread
	private synchronized void producePage() throws InterruptedException {
		if (flushThread == null) {
			flushThread = new FlushThread();
			flushThread.start();
		}

		// Wait for flushThread to finish working on the back buffer
		while (backBufferFrames > 0) {
			wait();
		}

		// Swap buffers
		ByteBuffer tmp = frontBuffer;
		frontBuffer = backBuffer;
		backBuffer = tmp;

		backBufferFrames = frontBufferFrames;
		frontBufferFrames = 0;

		// Allow flushThread to use the back buffer
		notify();
	}


	// Called from flushThread
	private synchronized void consumePage() throws IOException, InterruptedException {
		// Wait for the main thread to release the back buffer
		while (backBufferFrames == 0) {
			wait();
		}

		// Do the heavy lifting
		fullFlush(backBuffer, backBufferFrames);

		// Release back buffer
		backBufferFrames = 0;

		// Tell main thread we're done with the back buffer
		notify();
	}


	public void close() throws IOException, InterruptedException {
		producePage();
		done = true;
		flushThread.join();
		out.flush();
		out.close();
		System.out.println("Swap closed");
	}

}
