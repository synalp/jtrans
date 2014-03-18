package fr.loria.synalp.jtrans.viterbi;

import fr.loria.synalp.jtrans.utils.BufferUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.InflaterInputStream;

/**
 * Compressed swap reader for Viterbi backtracking.
 *
 * A single instance may be reused for several different graphs to avoid wasting
 * time re-allocating buffers.
 *
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


	public void init(PageIndex index, InputStreamFactory factory) {
		this.index = index;
		inputStreamFactory = factory;
		currentPage = null;
	}


	public void init(PageIndex index, final File file) {
		init(index, new InputStreamFactory() {
			@Override
			public InputStream make() throws IOException {
				return new FileInputStream(file);
			}
		});
	}


	public void init(PageIndex index, final byte[] buf) {
		init(index, new InputStreamFactory() {
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
		/*
		System.out.println("Inflating page " + page.number + " @ offset "
				+ page.offset + "... ");
		*/

		assert page != currentPage;
		currentPage = page;

		int unpackedPageLength = page.frameCount * index.nStates;
		pageBuf = BufferUtils.grow(pageBuf, unpackedPageLength);

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
				pageBuf[idx(f, s)] += pageBuf[idx(f-1, s)];
				assert pageBuf[idx(f, s)] >= 0 &&
						pageBuf[idx(f, s)] < StateGraph.MAX_TRANSITIONS;
			}
		}
	}

}
