package fr.loria.synalp.jtrans.anchorless;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.Deflater;

class SwapDeflater {

	private final int frameLength;
	private final int maxFramesPerPage;

	private OutputStream out;
	private Deflater def;
	private PageIndex index;

	private ByteBuffer rawBuf;
	private int framesInBuf;
	private byte[] compBuf;
	private int[] previousBestParent;


	public SwapDeflater(int maxFPP, int nStates, OutputStream out)
			throws IOException
	{
		frameLength = 4 * nStates;
		maxFramesPerPage = maxFPP;

		this.out = out;

		def = new Deflater(Deflater.BEST_SPEED);
		def.setStrategy(Deflater.HUFFMAN_ONLY);

		index = new PageIndex();

		rawBuf = ByteBuffer.allocate(maxFramesPerPage * frameLength);
		compBuf = new byte[1048576];
		previousBestParent = new int[nStates];
	}


	private void fullFlush() throws IOException {
		assert rawBuf.hasArray();

		def.reset();
		def.setInput(rawBuf.array(), rawBuf.arrayOffset(),
				framesInBuf * frameLength);
		def.finish();
		assert !def.finished();

		while (!def.finished()) {
			int len = def.deflate(compBuf);
			if (len > 0) {
				out.write(compBuf, 0, len);
			}
			System.out.print(len <= 0? "?": def.needsInput()? "!": ".");
		}

		assert def.finished();

		index.putPage(framesInBuf, (int)def.getBytesWritten());
		framesInBuf = 0;

		System.out.println(String.format(
				"[Frame %d] backtrack footprint: %s",
				index.getFrameCount(),
				index.getCompressedBytes()));

		rawBuf.rewind();
	}


	public void write(int[] n) throws IOException {
		assert previousBestParent.length == n.length;

		for (int i = 0; i < previousBestParent.length; i++) {
			rawBuf.putInt(n[i] - previousBestParent[i]);
		}

		System.arraycopy(n, 0, previousBestParent, 0, previousBestParent.length);

		framesInBuf++;

		if (framesInBuf % maxFramesPerPage == 0) {
			fullFlush();
			Arrays.fill(previousBestParent, 0);
		}
	}


	public void close() throws IOException {
		fullFlush();
		out.flush();
		out.close();
	}


	public PageIndex getIndex() {
		return index;
	}

}
