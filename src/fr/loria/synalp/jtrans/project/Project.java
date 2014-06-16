package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.align.Aligner;
import fr.loria.synalp.jtrans.align.ViterbiAligner;
import fr.loria.synalp.jtrans.utils.*;
import fr.loria.synalp.jtrans.graph.StateGraph;

import javax.sound.sampled.*;
import java.io.*;
import java.util.*;

/**
 * An audio file and alignment tracks.
 * This class is mainly useful for easy serialization.
 */
public abstract class Project {

	/**
	 * Target audio format. Any input audio files that do not match this format
	 * will be converted to it before being processed.
	 */
	private static final AudioFormat SUITABLE_AUDIO_FORMAT =
			new AudioFormat(16000, 16, 1, true, false);


	public static Class<? extends Aligner> ALIGNER = ViterbiAligner.class;


	public File audioFile;
	/** Audio file in a suitable format for processing */
	public transient File convertedAudioFile = null;
	public transient long audioSourceTotalFrames = -1;


	protected List<String> speakerNames = new ArrayList<>();


	public int speakerCount() {
		return speakerNames.size();
	}

	public String getSpeakerName(int speaker) {
		return speakerNames.get(speaker);
	}

	public abstract List<Token> getTokens(int speaker);

	public abstract Iterator<Phrase> phraseIterator(int speaker);

	public Set<Token> getAllTokens() {
		Set<Token> set = new HashSet<>();

		for (int i = 0; i < speakerCount(); i++) {
			set.addAll(getTokens(i));
		}

		return set;
	}

	public void anonymizeWord(String w) {
		w = w.toLowerCase();

		for (Token token: getAllTokens()) {
			if (token.toString().toLowerCase().equals(w)) {
				token.setAnonymize(true);
			}
		}
	}


	public AnonymizingAudioInputStream getAnonymizingAudioInputStream()
			throws IOException, UnsupportedAudioFileException
	{
		assert convertedAudioFile != null: "no converted audio file!";

		BinarySegmentation sequence = new BinarySegmentation();

		for (Token token: getAllTokens()) {
			if (!token.shouldBeAnonymized()) {
				continue;
			}

			if (!token.isAligned()) {
				System.err.println("WARNING: Can't anonymize unaligned word!!!");
			}

			Token.Segment seg = token.getSegment();
			sequence.union(seg.getStartSecond(), seg.getLengthSeconds());
		}

		return new AnonymizingAudioInputStream(
				AudioSystem.getAudioInputStream(convertedAudioFile),
				sequence);
	}


	public void clearAlignment() {
		for (Token token: getAllTokens()) {
			token.clearAlignment();
		}
	}


	public Aligner getAligner(
			Class<? extends Aligner> alignerClass,
			ProgressDisplay progress,
			boolean computeLikelihoods)
			throws IOException, ReflectiveOperationException
	{
		if (progress != null) {
			progress.setIndeterminateProgress("Initializing aligner...");
		}

		Aligner aa = alignerClass
				.getConstructor(File.class, ProgressDisplay.class)
				.newInstance(convertedAudioFile, progress);

		if (computeLikelihoods) {
			aa.setComputeLikelihoods(true);
			aa.initTrainers(speakerCount());

			// A valid speaker ID on tokens is necessary for scoring a set of words
			// belonging to various speakers with speaker-dependent Gaussians.
			for (Token token: getAllTokens()) {
				assert token.getSpeaker() >= 0;
			}
		}

		return aa;
	}


	public Aligner getStandardAligner(
			ProgressDisplay progress,
			boolean computeLikelihoods)
			throws IOException, ReflectiveOperationException
	{
		return getAligner(ALIGNER, progress, computeLikelihoods);
	}


	/**
	 * Aligns each speaker independently.
	 * @param reference Use {@link StateGraph} yielded by this aligner as the
	 *                  base graph for the actual alignment. If {@code null},
	 *                  a full StateGraph containing all possible pronunciations
	 *                  will be used.
	 */
	public abstract void align(Aligner aligner, Aligner reference)
			throws IOException, InterruptedException;


	/**
	 * Returns differences in anchor times from one project to another
	 * using this project as a reference. Both projects must be otherwise
	 * identical.
	 * @param p project identical to this, except for its anchor times
	 * @return list of integer frame deltas
	 * @throws IllegalArgumentException if the projects are not identical or
	 * an anchor in this project has no time
	 */
	public List<Integer> anchorFrameDiffs(Project p)
			throws IllegalArgumentException
	{
		List<Integer> diffs = new ArrayList<>();
		if (speakerCount() != p.speakerCount()) {
			throw new IllegalArgumentException(
					"projects have different track counts");
		}

		for (int i = 0; i < speakerCount(); i++) {
			Iterator<Phrase> itr1 = phraseIterator(i);
			Iterator<Phrase> itr2 = p.phraseIterator(i);

			while (itr1.hasNext()) {
				if (!itr2.hasNext()) {
					throw new IllegalArgumentException("counterpart speaker "
							+ i + " ran out of phrases");
				}

				Phrase p1 = itr1.next();
				Phrase p2 = itr2.next();

				diffs.add(p2.getInitialAnchor().getFrame() - p1.getFinalAnchor().getFrame());
				diffs.add(p2.getFinalAnchor().getFrame() - p1.getFinalAnchor().getFrame());
			}

			if (itr2.hasNext()) {
				throw new IllegalArgumentException("counterpart speaker "
						+ i + " has more phrases than me");
			}
		}

		return diffs;
	}


	public void printWordFrameDiffs(Project p) {
		if (speakerCount() != p.speakerCount()) {
			throw new IllegalArgumentException(
					"projects have different track counts");
		}

		for (int i = 0; i < speakerCount(); i++) {
			Iterator<Phrase> itr1 = phraseIterator(i);
			Iterator<Phrase> itr2 = p.phraseIterator(i);

			while (itr1.hasNext()) {
				if (!itr2.hasNext()) {
					throw new IllegalArgumentException("counterpart speaker "
							+ i + " ran out of phrases");
				}

				Phrase ph1 = itr1.next();
				Phrase ph2 = itr2.next();

				if (ph1.size() != ph2.size()) {
					throw new IllegalArgumentException(
							"phrases have different lengths in speaker #" + i);
				}

				for (int w = 0; w < ph1.size(); w++) {
					Token w1 = ph1.get(w);
					Token w2 = ph2.get(w);

					if (!w1.isAligned()) {
						continue;
					}

					if (!w2.isAligned()) {
						System.err.println("WARNING: counterpart word unaligned: " + w2);
						continue;
					}

					int f1 = w1.getFirstNonSilenceFrame();
					int f2 = w2.getFirstNonSilenceFrame();

					int l1 = w1.getLastNonSilenceFrame();
					int l2 = w2.getLastNonSilenceFrame();

					if (f1 == -1) {
						System.err.println("WARNING: fully silent word in reference: " + w1);
						continue;
					}

					if (f2 == -1) {
						System.err.println("WARNING: fully silent word in counterpart: " + w1);
						continue;
					}

					if (!w1.toString().equals(w2.toString())) {
						throw new IllegalArgumentException("word is different in counterpart ("
								+ w1 + " vs " + w2 + ")");
					}

					assert f1 != -1 : w1;
					assert f2 != -1 : w2;
					assert l1 != -1 : w1;
					assert l2 != -1 : w2;

					// grep-friendly keyword,
					// track number,
					// word-anchor-counter,
					// start sec 1,
					// start sec 2,
					// start diff,
					// end diff,
					// length diff,
					// word
					System.out.printf("worddiff %d %3d %6.2f %6.2f %4d %4d %4d %s\n",
							i,
							w,
							TimeConverter.frame2second(f1),
							TimeConverter.frame2second(f2),
							f2 - f1,
							l2 - l1,
							l2 - f2 - (l1 - f1),
							w1);

				}
			}
		}
	}


	//==========================================================================
	// LOAD/SAVE/EXPORT
	//==========================================================================

	/**
	 * Sets the audio file for this project, and converts it to a suitable
	 * format if needed.
	 */
	public void setAudio(File audioFile) {
		this.audioFile = audioFile;

		if (audioFile != null) {
			convertedAudioFile = suitableAudioFile(audioFile);

			try {
				AudioInputStream audioInputStream =
						AudioSystem.getAudioInputStream(convertedAudioFile);
				AudioFormat format = audioInputStream.getFormat();
				long frames = audioInputStream.getFrameLength();
				double durationInSeconds = (frames+0.0) / format.getFrameRate();
				audioSourceTotalFrames = TimeConverter.second2frame((float)durationInSeconds);
			} catch (IOException ex) {
				audioSourceTotalFrames = -1;
			} catch (UnsupportedAudioFileException ex) {
				audioSourceTotalFrames = -1;
			}
		} else {
			convertedAudioFile = null;
			audioSourceTotalFrames = -1;
		}

		System.out.println("audioSourceTotalFrames: " + audioSourceTotalFrames);
	}


	/**
	 * Return an audio file in a suitable format for JTrans. If the original
	 * file isn't in the right format, convert it and cache it.
	 */
	public static File suitableAudioFile(final File original) {
		final AudioFormat af;

		try {
			af = AudioSystem.getAudioFileFormat(original).getFormat();
		} catch (UnsupportedAudioFileException ex) {
			ex.printStackTrace();
			return original;
		} catch (IOException ex) {
			ex.printStackTrace();
			return original;
		}

		System.out.println("suitableAudioFile: source format " + af);

		if (af.matches(SUITABLE_AUDIO_FORMAT)) {
			System.out.println("suitableAudioFile: no conversion needed!");
			return original;
		}

		System.err.println("WARNING: for best results, provide your own " +
				"audio file in this format: " + SUITABLE_AUDIO_FORMAT);

		Cache.FileFactory factory = new Cache.FileFactory() {
			public void write(File f) throws IOException {
				AudioInputStream stream;
				try {
					stream = AudioSystem.getAudioInputStream(original);
				} catch (UnsupportedAudioFileException ex) {
					ex.printStackTrace();
					throw new IllegalStateException("Unsupported audio file; " +
							"should've been caught above!");
				}

				// Workaround for sound library bug - Formats with an unknown
				// sample size (such as OGG and MP3) cannot be converted to the
				// suitable format in one pass. First, the stream must be
				// converted to a fixed sample size (e.g. 16-bit) while keeping
				// the sample rate and channel count intact. Only *then* can we
				// up/downsample and/or downmix the stream.

				if (af.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED) {
					stream = AudioSystem.getAudioInputStream(
						new AudioFormat(
								af.getSampleRate(),
								SUITABLE_AUDIO_FORMAT.getSampleSizeInBits(),
								af.getChannels(),
								true,
								false),
						stream);
				}

				AudioSystem.write(
						AudioSystem.getAudioInputStream(SUITABLE_AUDIO_FORMAT, stream),
						AudioFileFormat.Type.WAVE,
						f);

				System.out.println("suitableAudioFile: written! " + f);
			}
		};

		return Cache.cachedFile("converted", "wav", factory, original);
	}

}
