package fr.loria.synalp.jtrans.anchorless;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.Deflater;

class SwapDeflater {

	// TODO essayer avec un DeflaterOutputStream et des appels à finish()

	private final int frameLength;
	private final int maxFramesPerPage;

	private OutputStream out;
	private Deflater def;
	private PageIndex index;
	private int framesProcessed;
	private int framesFlushed;
	private long compressedBytesWritten;

	private ByteBuffer rawBuf;
	byte[] compBuf;
	int[] previousBestParent;


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
				(framesProcessed - framesFlushed) * frameLength);
		def.finish();
		assert !def.finished();

		while (!def.finished()) {
			int len = def.deflate(compBuf);
			if (len <= 0) {
				System.out.print("c");
				continue;
			}
			System.out.print(def.needsInput()? "!": ".");
			compressedBytesWritten += len;
			out.write(compBuf, 0, len);
		}

		assert def.finished();
		index.putPage(framesProcessed - framesFlushed, (int) def.getBytesWritten());
		framesFlushed = framesProcessed;

		System.out.println(String.format(
				"[Frame %d] backtrack footprint: %s",
				framesProcessed,
				compressedBytesWritten));

		rawBuf.rewind();
	}


	public void write(int[] n) throws IOException {
		assert previousBestParent.length == n.length;

		for (int i = 0; i < previousBestParent.length; i++) {
			rawBuf.putInt(n[i] - previousBestParent[i]);
		}

		System.arraycopy(n, 0, previousBestParent, 0, previousBestParent.length);

		framesProcessed++;

		if ((framesProcessed - framesFlushed) % maxFramesPerPage == 0) {
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
