package fr.loria.synalp.jtrans.viterbi;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.InflaterInputStream;

/**
 * Compressed swap reader for Viterbi backtracking
 * @see StateGraph
 */
public class SwapInflater {

	private byte[] pageBuf;
	private PageIndex.Entry currentPage;
	private PageIndex index;
	private InputStreamFactory inputStreamFactory;


	public interface InputStreamFactory {
		public InputStream make() throws IOException;
	}


	public SwapInflater(PageIndex index, InputStreamFactory factory) {
		this.index = index;
		this.inputStreamFactory = factory;
	}


	public SwapInflater(PageIndex index, final File file) {
		this(index, new InputStreamFactory() {
			@Override
			public InputStream make() throws IOException {
				return new FileInputStream(file);
			}
		});
	}


	public SwapInflater(PageIndex index, final byte[] buf) {
		this(index, new InputStreamFactory() {
			@Override
			public InputStream make() throws IOException {
				return new ByteArrayInputStream(buf);
			}
		});
	}


	public int getFrameCount() {
		return index.getFrameCount();
	}


	public byte getIncomingTransition(int frame, int state) throws IOException {
		if (currentPage == null || !currentPage.within(frame)) {
			inflatePage(index.getPage(frame));
		}

		return pageBuf[idx(frame, state)];
	}


	private int idx(int frame, int state) {
		return index.nStates * (frame-currentPage.frame0) + state;
	}


	private void inflatePage(PageIndex.Entry page) throws IOException {
		System.out.print("Inflating page " + page.number + " @ offset " + page.offset + "... ");
		assert page != currentPage;
		currentPage = page;

		int unpackedPageLength = page.frameCount * index.nStates;
		if (pageBuf == null || pageBuf.length < unpackedPageLength) {
			System.out.print("Reallocating... ");
			pageBuf = new byte[unpackedPageLength];
		}

		InputStream is;

		is = inputStreamFactory.make();
		long skipped = is.skip(page.offset);
		assert skipped == page.offset;
		assert page.offset + page.compressedChunkLength <= index.getCompressedBytes();

		is = new InflaterInputStream(is);

		int rdtot = 0;
		while (rdtot < unpackedPageLength) {
			rdtot += is.read(pageBuf, rdtot, unpackedPageLength - rdtot);
		}
		is.close();
		assert rdtot == unpackedPageLength;

		// Inverse filter
		int f = page.frame0+1;
		final int fn = page.frame0 + page.frameCount;
		for (; f < fn; f++) {
			for (int s = 0; s < index.nStates; s++) {
				byte p = pageBuf[idx(f-1, s)];
				byte c = pageBuf[idx(f, s)];
				pageBuf[idx(f, s)] = (byte)(p + c);
			}
		}

		System.out.println("OK");
	}

}
