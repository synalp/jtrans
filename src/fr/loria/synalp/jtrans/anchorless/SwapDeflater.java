package fr.loria.synalp.jtrans.anchorless;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.Deflater;

class SwapDeflater {

	private final int frameLength;
	private final int maxFramesPerPage;

	private final OutputStream out;
	private final Deflater def;
	private final PageIndex index;

	/** Uncompressed data buffer that the main thread writes into */
	private ByteBuffer frontBuffer;

	/** Number of frames stored in the front buffer */
	private int frontBufferFrames;

	/** Thread that compresses data in the background */
	private Thread flushThread;

	/** Uncompressed data buffer that the flush thread reads from */
	private ByteBuffer backBuffer;

	/** Compressed data buffer that the flush thread writes into */
	private byte[] compBuffer;

	private int[] previousBestParent;


	private class FlushThread extends Thread {
		ByteBuffer buffer;
		int frames;

		public FlushThread(ByteBuffer buf, int fib) {
			buffer = buf;
			frames = fib;
		}

		@Override
		public void run() {
			try {
				fullFlush(buffer, frames);
			} catch (IOException ex) {
				throw new Error(ex);
			}
		}
	}


	public SwapDeflater(int maxFPP, int nStates, OutputStream out)
			throws IOException
	{
		frameLength = 4 * nStates;
		maxFramesPerPage = maxFPP;

		this.out = out;

		def = new Deflater(Deflater.BEST_SPEED);
		def.setStrategy(Deflater.HUFFMAN_ONLY);

		index = new PageIndex();

		frontBuffer = ByteBuffer.allocate(maxFramesPerPage * frameLength);
		backBuffer  = ByteBuffer.allocate(maxFramesPerPage * frameLength);

		compBuffer = new byte[1048576];
		previousBestParent = new int[nStates];
	}


	// Thread safety: don't access instance variables frontBuffer/backBuffer
	// nor frontBufferFrames directly in this method, because they may change
	// at any time in the main thread. Use the provided parameters instead.
	private synchronized void fullFlush(ByteBuffer rawBuf, int framesInBuf) throws IOException {
		assert rawBuf.hasArray();

		def.reset();
		def.setInput(rawBuf.array(), rawBuf.arrayOffset(),
				framesInBuf * frameLength);
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

		for (int i = 0; i < previousBestParent.length; i++) {
			frontBuffer.putInt(n[i] - previousBestParent[i]);
		}

		System.arraycopy(n, 0, previousBestParent, 0, previousBestParent.length);

		frontBufferFrames++;

		if (frontBufferFrames % maxFramesPerPage == 0) {
			System.out.print("J");

			synchronized (this) {
				flushThread = new FlushThread(frontBuffer, frontBufferFrames);
				flushThread.start();

				// swap buffers
				ByteBuffer tmp = frontBuffer;
				frontBuffer = backBuffer;
				backBuffer = tmp;
				frontBufferFrames = 0;

				Arrays.fill(previousBestParent, 0);
			}
		}
	}


	public void close() throws IOException, InterruptedException {
		fullFlush(frontBuffer, frontBufferFrames);
		out.flush();
		out.close();
	}


	public PageIndex getIndex() {
		return index;
	}

}
