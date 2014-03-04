package fr.loria.synalp.jtrans.viterbi;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PageIndex implements Serializable {

	public final int nStates;

	private List<Entry> index = new ArrayList<Entry>();
	private int totalFrameCount;
	private long totalCompressedLength;


	public class Entry implements Serializable {

		public final int number;
		public final int compressedChunkLength;
		public final int frameCount;
		public final int frame0;
		public final long offset;


		private Entry(int fc, int ccl) {
			number = index.size();
			compressedChunkLength = ccl;
			frameCount = fc;
			frame0 = totalFrameCount;
			totalFrameCount += frameCount;
			offset = totalCompressedLength;
			totalCompressedLength += compressedChunkLength;
		}


		public boolean within(int frame) {
			return frame >= frame0 && frame < frame0 + frameCount;
		}

	}


	public PageIndex(int nStates) {
		this.nStates = nStates;
	}


	public void putPage(int fc, int ccl) {
		index.add(new Entry(fc, ccl));
	}


	public Entry getPage(int frameNo) {
		int pageNo = 0;
		int tfc = 0;
		for	(; pageNo < index.size(); pageNo++) {
			Entry e = index.get(pageNo);
			if (frameNo < tfc + e.frameCount) {
				break;
			}
			tfc += e.frameCount;
		}
		return index.get(pageNo);
	}


	public int getPageCount() {
		return index.size();
	}


	public int getFrameCount() {
		return totalFrameCount;
	}


	public long getCompressedBytes() {
		return totalCompressedLength;
	}


	public void serialize(OutputStream out) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(this);
		oos.flush();
		oos.close();
	}


	public static PageIndex deserialize(InputStream in) throws IOException {
		ObjectInputStream ois = new ObjectInputStream(in);
		PageIndex pi;
		try {
			pi = (PageIndex)ois.readObject();
		} catch (ClassNotFoundException ex) {
			throw new IOException(ex);
		}
		ois.close();
		System.out.println("Deserialized - Page Count = " + pi.getPageCount());
		return pi;
	}

}
