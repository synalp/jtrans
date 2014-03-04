package fr.loria.synalp.jtrans.viterbi;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.InflaterInputStream;

/**
 * Compressed swap reader for Viterbi backtracking
 * @see StateGraph
 */
public class SwapInflater {

	private final File file;
	private ByteBuffer pageBuf;
	private PageIndex.Entry currentPage;
	private PageIndex index;


	public SwapInflater(File file, PageIndex index) {
		this.file = file;
		this.index = index;
	}


	public int getFrameCount() {
		return index.getFrameCount();
	}


	public byte getIncomingTransition(int frame, int state) throws IOException {
		if (currentPage == null || !currentPage.within(frame)) {
			inflatePage(index.getPage(frame));
		}

		return pageBuf.get(idx(frame, state));
	}


	private int idx(int frame, int state) {
		return index.nStates * (frame-currentPage.frame0) + state;
	}


	private void inflatePage(PageIndex.Entry page) throws IOException {
		System.out.print("Inflating page " + page.number + " @ offset " + page.offset + "... ");
		assert page != currentPage;
		currentPage = page;

		int unpackedPageLength = page.frameCount * index.nStates;
		if (pageBuf == null || pageBuf.capacity() < unpackedPageLength) {
			System.out.print("Reallocating... ");
			pageBuf = ByteBuffer.allocate(unpackedPageLength);
		} else {
			pageBuf.rewind();
		}

		InputStream is;

		is = new FileInputStream(file);
		long skipped = is.skip(page.offset);
		assert skipped == page.offset;
		assert page.offset + page.compressedChunkLength <= file.length();

		is = new InflaterInputStream(is);

		int rdtot = 0;
		while (rdtot < unpackedPageLength) {
			rdtot += is.read(pageBuf.array(),
					pageBuf.arrayOffset() + rdtot,
					unpackedPageLength - rdtot);
		}
		is.close();
		assert rdtot == unpackedPageLength;

		int f = page.frame0+1;
		final int fn = page.frame0 + page.frameCount;
		for (; f < fn; f++) {
			for (int s = 0; s < index.nStates; s++) {
				byte p = pageBuf.get(idx(f-1, s));
				byte c = pageBuf.get(idx(f, s));
				pageBuf.put(idx(f, s), (byte) (p + c));
			}
		}

		System.out.println("OK");
	}

}
