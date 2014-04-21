package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.elements.Anchor;
import fr.loria.synalp.jtrans.elements.Element;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.utils.TimeConverter;
import fr.loria.synalp.jtrans.viterbi.StateGraph;
import fr.loria.synalp.jtrans.viterbi.StatePool;

import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * An audio file and alignment tracks.
 * This class is mainly useful for easy serialization.
 */
public class Project {

	/**
	 * Target audio format. Any input audio files that do not match this format
	 * will be converted to it before being processed.
	 */
	private static final AudioFormat SUITABLE_AUDIO_FORMAT =
			new AudioFormat(16000, 16, 1, true, false);


	public static boolean ALIGN_OVERLAPS = true;


	public static Class<? extends AutoAligner> ALIGNER = ViterbiAligner.class;


	public List<Track> tracks = new ArrayList<Track>();

	public File audioFile;
	/** Audio file in a suitable format for processing */
	public transient File convertedAudioFile = null;
	public transient long audioSourceTotalFrames = -1;


	public void anonymizeWord(String w) {
		w = w.toLowerCase();

		for (Track track: tracks) {
			for (Word word: track.getWords()) {
				if (word.toString().toLowerCase().equals(w)) {
					word.setAnonymize(true);
				}
			}
		}
	}


	public AnonymizingAudioInputStream getAnonymizingAudioInputStream()
			throws IOException, UnsupportedAudioFileException
	{
		assert convertedAudioFile != null: "no converted audio file!";

		BinarySegmentation sequence = new BinarySegmentation();

		for (Track track: tracks) {
			for (Word word: track.getWords()) {
				if (!word.shouldBeAnonymized()) {
					continue;
				}

				if (!word.isAligned()) {
					System.err.println("WARNING: Can't anonymize unaligned word!!!");
				}

				Word.Segment seg = word.getSegment();
				sequence.union(seg.getStartSecond(), seg.getLengthSeconds());
			}
		}

		return new AnonymizingAudioInputStream(
				AudioSystem.getAudioInputStream(convertedAudioFile),
				sequence);
	}


	public void clearAlignment() {
		for (Track track : tracks)
			track.clearAlignment();
	}


	public void clearAllAnchorTimes() {
		LinearBridge lb = new LinearBridge(tracks);
		int order = 0;

		while (lb.hasNext()) {
			AnchorSandwich[] simultaneousSandwiches = lb.next();

			for (AnchorSandwich sandwich: simultaneousSandwiches) {
				if (sandwich != null && sandwich.getInitialAnchor() != null) {
					setOrderOnTimedAnchors(sandwich.getInitialAnchor(), order++);
					break;
				}
			}
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
			aa.setScorers(tracks.size());
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


	/**
	 * Aligns all words contained between two anchors (i.e. "anchor sandwich").
	 */
	private void align(AutoAligner aligner, AnchorSandwich sandwich)
			throws IOException, InterruptedException
	{
		if (sandwich.isEmpty()) {
			return;
		}

		int frameCount = aligner.getFrameCount();

		Anchor iAnchor = sandwich.getInitialAnchor();
		Anchor fAnchor = sandwich.getFinalAnchor();

		int iFrame = iAnchor == null || !iAnchor.hasTime()?
				0: iAnchor.getFrame();

		int fFrame = fAnchor == null || !fAnchor.hasTime()?
				frameCount: fAnchor.getFrame()-1;  // see explanation below.

		/* Why did we subtract 1 frame from the final anchor's time?
		Assume that we have 2 contiguous anchor sandwiches. The same anchor
		serves as the FINAL anchor in the first sandwich, and as the INITIAL
		anchor in the second sandwich.
		Conceptually, anchors are like points in time, but we work with frames.
		So, anchors technically cover an entire frame. Thus, to avoid that two
		contiguous AnchorSandwiches overlap on 1 frame (i.e. that of the anchor
		they have in common), we subtract 1 frame from the final anchor. */

		if (iFrame >= frameCount) {
			throw new IllegalArgumentException(String.format(
					"Initial frame (%d) beyond frame count (%d)! " +
					"(in anchor sandwich: %s)",
					iFrame, frameCount, sandwich));
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
					"(in anchor sandwich: %s)",
					iFrame, iAnchor, fFrame, fAnchor, sandwich));
		} else if (iFrame > fFrame) {
			// iFrame may legally be ahead of fFrame by 1 at most if the anchors
			// are too close together (because we have removed 1 frame from the
			// final frame, see above)
			System.err.println(String.format("WARNING: skipping anchors too " +
					"close together: frame %d (initial) vs %d (final) " +
					"(in anchor sandwich: %s)",
					iFrame, fFrame, sandwich));
			return;
		}

		StateGraph graph = new StateGraph(new StatePool(), sandwich.getWords());
		aligner.align(graph, iFrame, fFrame);
	}


	/**
	 * Aligns all words in all tracks of this project with timed anchors.
	 * @param clear If true, clear any previously existing alignment information
	 *              to start a new alignment from scratch. If false, don't touch
	 *              aligned words; only attempt to align unaligned words.
	 */
	public void align(AutoAligner aligner, boolean clear)
			throws IOException, InterruptedException
	{
		for (Track track: tracks) {
			if (clear) {
				track.clearAlignment();
			}

			AnchorSandwichIterator iter = track.sandwichIterator();

			while (iter.hasNext()) {
				AnchorSandwich sandwich = iter.next();

				if (clear || !sandwich.isFullyAligned()) {
					align(aligner, sandwich);
				}
			}
		}
	}


	/**
	 * Aligns all words in all tracks of this project with timeless anchors.
	 */
	public void alignInterleaved(AutoAligner aligner)
			throws IOException, InterruptedException
	{
		// Clear existing alignment
		for (Track track: tracks) {
			track.clearAlignment();
		}

		// Align big interleaved sequences
		LinearBridge lb = new LinearBridge(tracks);
		while (lb.hasNext()) {
			align(aligner, lb.nextInterleavedElementSequence());
		}

		// Deduce times on timeless anchors
		deduceTimes();

		// Align yet-unaligned overlaps
		if (ALIGN_OVERLAPS) {
			align(aligner, false);
		}
	}


	/**
	 * Returns a linear state graph matching the state sequence followed in the
	 * aligned words. Overlapping speech is ignored as per the contract of
	 * LinearBridge.
	 */
	public StateGraph getLinearStateGraph() {
		// Find all aligned words in a linear sequence
		List<Word> sequence = new ArrayList<>();
		LinearBridge bridge = new LinearBridge(tracks);
		while (bridge.hasNext()) {
			sequence.addAll(
					bridge.nextInterleavedElementSequence().getAlignedWords());
		}

		assert !sequence.isEmpty():
				"text must be aligned before generating a linear state graph";

		String[][] rules    = new String[sequence.size()][];
		int        wi       = 0;

		// Fill state graph info
		for (Word w: sequence) {
			List<Word.Phone> phones = w.getPhones();
			String[] rule = new String[phones.size()];
			for (int i = 0; i < phones.size(); i++) {
				rule[i] = phones.get(i).toString();
			}

			rules[wi] = rule;
			wi++;
		}

		// Don't use extra optional interword silences. Aligned words already
		// contain the silence "phones" that come after them.
		StateGraph sg = new StateGraph(
				new StatePool(), rules, sequence, false);
		assert sg.isLinear();

		return sg;
	}


	/**
	 * Deduce times on timeless anchors
	 */
	public void deduceTimes() {
		for (Track track: tracks) {
			AnchorSandwichIterator iter = track.sandwichIterator();

			while (iter.hasNext()) {
				AnchorSandwich sandwich = iter.next();
				List<Word> words = sandwich.getWords();

				if (words.isEmpty()) {
					continue;
				}

				{
					int iF = words.get(0).getFirstNonSilenceFrame();
					Anchor ia = sandwich.getInitialAnchor();
					if (null != ia && !ia.hasTime() && iF >= 0) {
						setTimeOnTimelessAnchors(ia, iF);
					}
				}

				{
					int ff = words.get(words.size()-1).getLastNonSilenceFrame();
					Anchor fa = sandwich.getFinalAnchor();
					if (null != fa && !fa.hasTime() && ff >= 0) {
						setTimeOnTimelessAnchors(fa, ff);
					}
				}
			}
		}
	}


	/**
	 * Sets time for all timeless anchors equal to the reference anchor.
	 * @param reference reference timeless anchor. MUST be timeless for
	 *                  Anchor.equals() to work.
	 * @param frame time to set
	 */
	void setTimeOnTimelessAnchors(Anchor reference, int frame) {
		assert !reference.hasTime();

		for (Track track: tracks) {
			for (Element el: track.elts) {
				if (el instanceof Anchor) {
					Anchor a = (Anchor)el;
					if (a != reference && a.equals(reference)) {
						assert !a.hasTime();
						a.setFrame(frame);
					}
				}
			}
		}

		reference.setFrame(frame);
	}


	void setOrderOnTimedAnchors(Anchor reference, int order) {
		for (Track track: tracks) {
			for (Element el: track.elts) {
				if (el instanceof Anchor) {
					Anchor a = (Anchor)el;
					if (a != reference && a.equals(reference)) {
						a.setOrder(order);
					}
				}
			}
		}

		reference.setOrder(order);
	}


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
		List<Integer> diffs = new ArrayList<Integer>();
		if (tracks.size() != p.tracks.size()) {
			throw new IllegalArgumentException(
					"projects have different track counts");
		}

		for (int i = 0; i < tracks.size(); i++) {
			Track t1 = tracks.get(i);
			Track t2 = p.tracks.get(i);

			if (t1.elts.size() != t2.elts.size()) {
				throw new IllegalArgumentException(
						"projects have different lengths for track #" + i);
			}

			for (int j = 0; j < t1.elts.size(); j++) {
				Element e1 = t1.elts.get(j);
				Element e2 = t2.elts.get(j);

				if (e1 instanceof Anchor) {
					if (!(e2 instanceof Anchor)) {
						throw new IllegalArgumentException(
								"counterpart element #" + j + " not an anchor");
					}

					Anchor a1 = (Anchor)e1;
					Anchor a2 = (Anchor)e2;

					if (!a1.hasTime()) {
						throw new IllegalArgumentException("anchor " + a1 +
								"(reference project) has no time");
					}

					if (!a2.hasTime()) {
						System.err.println("Warning: " + a2 +
								" (counterpart) has no time; skipping...");
						continue;
					}

					diffs.add(a2.getFrame() - a1.getFrame());
				}

				else if (!e1.getClass().equals(e2.getClass())) {
					throw new IllegalArgumentException("counterpart element #"
							+ j + " instance of different class");
				}
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
