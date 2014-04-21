package fr.loria.synalp.jtrans.facade;

import javax.sound.sampled.AudioInputStream;
import java.io.IOException;

/**
 * Audio filter that replaces "anonymous" time segments with silence.
 */
public class AnonymizingAudioInputStream extends AudioInputStream {

	protected final float sampleRate;
	protected final int frameSize;

	protected final BinarySegmentation sequence;
	protected int seqIdx;
	protected long seqIdxFrame = -1;


	public AnonymizingAudioInputStream(
			AudioInputStream source,
			BinarySegmentation sequence)
	{
		super(source, source.getFormat(), source.getFrameLength());
		this.sequence = sequence;
		sampleRate = getFormat().getSampleRate();
		frameSize = getFormat().getFrameSize();
	}


	protected boolean shouldAnonymize(long frame) {
		float sec = frame / sampleRate;

		// rewind
		if (frame < seqIdxFrame) {
			seqIdx = 0;
			System.out.println("rewind sequence");
		}

		seqIdxFrame = frame;

		while (seqIdx < sequence.size() && sequence.get(seqIdx).isBehind(sec)) {
			seqIdx++;
		}

		return seqIdx < sequence.size() && sequence.get(seqIdx).contains(sec);
	}


	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		long oldFramePos = framePos;
		int bytesRead = super.read(b, off, len);

		int bIdx = off;
		for (long i = oldFramePos; i < framePos; i++) {
			if (shouldAnonymize(i)) {
				for (int j = 0; j < frameSize; j++) {
					b[bIdx++] = 0;
				}
			} else {
				bIdx += frameSize;
			}
		}

		return bytesRead;
	}

}
