package fr.loria.synalp.jtrans.anchorless;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.InflaterInputStream;

public class SwapInflater {

	final int nStates;
	final File file;
	ByteBuffer pageBuf;
	PageIndex.Entry currentPage;
	PageIndex index;


	public SwapInflater(int nStates, File file, PageIndex index) {
		this.file = file;
		this.nStates = nStates;
		this.index = index;
	}


	public int get(int frame, int state) throws IOException {
		if (currentPage == null || !currentPage.within(frame)) {
			inflatePage(index.getPage(frame));
		}
		return pageBuf.getInt(idx(frame, state));
	}


	private int idx(int frame, int state) {
		return 4 * ((frame-currentPage.frame0)*nStates + state);
	}


	private void inflatePage(PageIndex.Entry page) throws IOException {
		System.out.print("Inflating page " + page.number + " @ offset " + page.offset + "... ");
		assert page != currentPage;
		currentPage = page;

		int unpackedPageLength = page.frameCount * nStates * 4;
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

		for (int relF = 1; relF < page.frameCount; relF++) {
			int absF = relF + page.frame0;
			for (int s = 0; s < nStates; s++) {
				int p = pageBuf.getInt(idx(absF - 1, s));
				int c = pageBuf.getInt(idx(absF, s));
				pageBuf.putInt(idx(absF, s), p+c);
			}
		}

		System.out.println("OK");
	}

}
