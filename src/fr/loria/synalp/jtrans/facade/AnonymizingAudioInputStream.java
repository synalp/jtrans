package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.facade.BinarySegmentation.Segment;

import javax.sound.sampled.AudioInputStream;
import java.io.IOException;

/**
 * Audio filter that replaces "anonymous" time segments with silence.
 */
public class AnonymizingAudioInputStream extends AudioInputStream {

	protected final float sampleRate;

	protected final BinarySegmentation sequence;
	protected int seqIdx;
	protected long seqIdxFrame = -1;

	/**
	 * Time to fade in/out of silence around anonymized words.
	 */
	public static float FADE_DURATION = .1f;


	public AnonymizingAudioInputStream(
			AudioInputStream source,
			BinarySegmentation sequence)
	{
		super(source, source.getFormat(), source.getFrameLength());
		this.sequence = sequence;
		sampleRate = getFormat().getSampleRate();

		if (	2 != getFormat().getFrameSize() ||
				1 != getFormat().getChannels() ||
				getFormat().isBigEndian())
		{
			throw new IllegalArgumentException("Can only anonymize 16-bit " +
					"signed, little-endian, mono audio streams! " +
					"(current format: " + getFormat() + ")");
		}
	}


	protected float getVolume(long frame) {
		float sec = frame / sampleRate;

		// rewind
		if (frame < seqIdxFrame) {
			seqIdx = 0;
			System.out.println("rewind sequence");
		}

		seqIdxFrame = frame;

		// Find sequence segment ahead of, or containing, our frame
		while (seqIdx < sequence.size() && sequence.get(seqIdx).isBehind(sec)) {
			seqIdx++;
		}

		// Anonymize/Fade in/Fade out

		int pSeqIdx = seqIdx-1;
		float minDist = Float.MAX_VALUE;

		// Fade out of silence
		if (pSeqIdx >= 0 && pSeqIdx < sequence.size()) {
			Segment pSeg = sequence.get(pSeqIdx);
			assert pSeg.isBehind(sec);
			minDist = Math.min(minDist, sec - pSeg.getEnd());
		}

		if (seqIdx < sequence.size()) {
			Segment seg = sequence.get(seqIdx);
			if (seg.contains(sec)) {
				// Anonymize
				return 0;
			} else if (seg.isAhead(sec)) {
				// Fade into silence
				minDist = Math.min(minDist, seg.getStart() - sec);
			}
		}

		if (minDist >= 0 && minDist < FADE_DURATION) {
			return minDist / FADE_DURATION;
		} else {
			return 1;
		}
	}


	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		long oldFramePos = framePos;
		int bytesRead = super.read(b, off, len);

		for (int f = (int)oldFramePos; f < framePos; f++) {
			float volume = getVolume(f);
			int j = off + (f-(int)oldFramePos) *2;

			// little endian!
			short sample = (short)((b[j+1]&0xff) << 8 | (b[j]&0xff));

			if (volume <= 0.001) {
				// Make extra sure to erase -- don't multiply by negative number
				sample = 0;
			} else if (volume != 1) {
				sample *= volume;
			}

			// little endian!
			b[j+1] = (byte)(sample >> 8);
			b[j] = (byte)(sample & 0xff);
		}

		return bytesRead;
	}

}
