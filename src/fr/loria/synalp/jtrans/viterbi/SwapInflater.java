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


	public int get(int frame, int state) throws IOException {
		if (currentPage == null || !currentPage.within(frame)) {
			inflatePage(index.getPage(frame));
		}

		if (index.useShorts) {
			int s = pageBuf.getShort(idx(frame, state)) & 0xFFFF;
			return s == 65535? -1: s;
		} else {
			return pageBuf.getInt(idx(frame, state));
		}
	}


	private int idx(int frame, int state) {
		return index.bytesPerFrame * (frame-currentPage.frame0) +
				index.bytesPerState * state;
	}


	private void inflatePage(PageIndex.Entry page) throws IOException {
		System.out.print("Inflating page " + page.number + " @ offset " + page.offset + "... ");
		assert page != currentPage;
		currentPage = page;

		int unpackedPageLength = page.frameCount * index.bytesPerFrame;
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
		if (index.useShorts) {
			for (; f < fn; f++) {
				for (int s = 0; s < index.nStates; s++) {
					int p = pageBuf.getShort(idx(f-1, s));
					int c = pageBuf.getShort(idx(f, s));
					pageBuf.putShort(idx(f, s), (short)(p+c));
				}
			}
		} else {
			for (; f < fn; f++) {
				for (int s = 0; s < index.nStates; s++) {
					int p = pageBuf.getInt(idx(f-1, s));
					int c = pageBuf.getInt(idx(f, s));
					pageBuf.putInt(idx(f, s), p+c);
				}
			}
		}


		System.out.println("OK");
	}

}
