package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.align.AutoAligner;
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


	public static Class<? extends AutoAligner> ALIGNER = ViterbiAligner.class;


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

	public abstract List<Word> getWords(int speaker);

	public abstract Iterator<Phrase> phraseIterator(int speaker);


	public Set<Word> getAllWords() {
		Set<Word> set = new HashSet<>();

		for (int i = 0; i < speakerCount(); i++) {
			set.addAll(getWords(i));
		}

		return set;
	}


	public void anonymizeWord(String w) {
		w = w.toLowerCase();

		for (Word word: getAllWords()) {
			if (word.toString().toLowerCase().equals(w)) {
				word.setAnonymize(true);
			}
		}
	}


	public AnonymizingAudioInputStream getAnonymizingAudioInputStream()
			throws IOException, UnsupportedAudioFileException
	{
		assert convertedAudioFile != null: "no converted audio file!";

		BinarySegmentation sequence = new BinarySegmentation();

		for (Word word: getAllWords()) {
			if (!word.shouldBeAnonymized()) {
				continue;
			}

			if (!word.isAligned()) {
				System.err.println("WARNING: Can't anonymize unaligned word!!!");
			}

			Word.Segment seg = word.getSegment();
			sequence.union(seg.getStartSecond(), seg.getLengthSeconds());
		}

		return new AnonymizingAudioInputStream(
				AudioSystem.getAudioInputStream(convertedAudioFile),
				sequence);
	}


	public void clearAlignment() {
		for (Word w: getAllWords()) {
			w.clearAlignment();
		}
	}


	public AutoAligner getAligner(
			Class<? extends AutoAligner> alignerClass,
			ProgressDisplay progress,
			boolean computeLikelihoods)
			throws IOException, ReflectiveOperationException
	{
		if (progress != null) {
			progress.setIndeterminateProgress("Initializing aligner...");
		}

		AutoAligner aa = alignerClass
				.getConstructor(File.class, ProgressDisplay.class)
				.newInstance(convertedAudioFile, progress);

		if (computeLikelihoods) {
			aa.setComputeLikelihoods(true);
			aa.initTrainers(speakerCount());

			// Set word speakers. This is necessary for scoring a set of words
			// belonging to various speakers with speaker-dependent Gaussians.
			for (int i = 0; i < speakerCount(); i++) {
				for (Word w: getWords(i)) {
					w.setSpeaker(i);
				}
			}
		}

		return aa;
	}


	public AutoAligner getStandardAligner(
			ProgressDisplay progress,
			boolean computeLikelihoods)
			throws IOException, ReflectiveOperationException
	{
		return getAligner(ALIGNER, progress, computeLikelihoods);
	}


	protected static void align(
			AutoAligner aligner,
			Anchor start,
			Anchor end,
			List<Word> words,
			boolean concatenate)
			throws IOException, InterruptedException
	{
		if (words.isEmpty()) {
			return;
		}

		int frameCount = aligner.getFrameCount();

		int iFrame = start == null? 0: start.getFrame();
		int fFrame = end   == null? frameCount: end.getFrame()-1;  // see explanation below.

		/* Why did we subtract 1 frame from the final anchor's time?
		Assume that we have 2 contiguous phrases. The same anchor
		serves as the FINAL anchor in the first phrase, and as the INITIAL
		anchor in the second phrase.
		Conceptually, anchors are like points in time, but we work with frames.
		So, anchors technically cover an entire frame. Thus, to avoid that two
		contiguous phrases overlap on 1 frame (i.e. that of the anchor
		they have in common), we subtract 1 frame from the final anchor. */

		if (iFrame >= frameCount) {
			throw new IllegalArgumentException(String.format(
					"Initial frame (%d) beyond frame count (%d)! " +
							"(in phrase: %s)",
					iFrame, frameCount, words));
		}

		if (fFrame >= frameCount) {
			System.err.println("WARNING: shaving frames off final anchor! " +
					"fFrame = " + fFrame + ", frameCount = " + frameCount);
			fFrame = frameCount - 1;
		}

		if (iFrame - fFrame > 1) {
			// initial frame after final frame
			throw new IllegalArgumentException(String.format(
					"Initial frame (%d, %s) after final frame (%d, %s)! " +
							"(in phrase: %s)",
					iFrame, start, fFrame, end, words));
		} else if (iFrame > fFrame) {
			// iFrame may legally be ahead of fFrame by 1 at most if the anchors
			// are too close together (because we have removed 1 frame from the
			// final frame, see above)
			System.err.println(String.format("WARNING: skipping anchors too " +
							"close together: frame %d (initial) vs %d (final) " +
							"(in phrase: %s)",
					iFrame, fFrame, words));
			return;
		}

		StateGraph graph = new StateGraph(words);
		aligner.align(graph, iFrame, fFrame, concatenate);
	}


	/**
	 * Aligns each speaker independently.
	 */
	public abstract void align(AutoAligner aligner)
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

				Phrase as1 = itr1.next();
				Phrase as2 = itr2.next();

				diffs.add(as2.getInitialAnchor().getFrame() - as1.getFinalAnchor().getFrame());
				diffs.add(as2.getFinalAnchor().getFrame() - as1.getFinalAnchor().getFrame());
			}

			if (itr2.hasNext()) {
				throw new IllegalArgumentException("counterpart speaker "
						+ i + " has more phrases than me");
			}
		}

		return diffs;
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
