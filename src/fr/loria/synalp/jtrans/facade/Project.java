package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.elements.Anchor;
import fr.loria.synalp.jtrans.elements.Element;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.gui.JTransGUI;
import fr.loria.synalp.jtrans.markup.JTRLoader;
import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.utils.TimeConverter;

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


	public List<Track> tracks = new ArrayList<Track>();

	/** Audio file in a suitable format for processing */
	public String wavname;
	public transient File convertedAudioFile = null;
	public transient long audioSourceTotalFrames = -1;


	public void clearAlignment() {
		for (Track track : tracks)
			track.clearAlignment();
	}


	public void align(ProgressDisplay progress)
			throws IOException, InterruptedException
	{
		for (Track track: tracks) {
			// TODO - I don't think the MFCC buffer provides random access,
			// that's why I'm recreating a new aligner at every iteration
			AutoAligner aligner = new AutoAligner(
					convertedAudioFile, (int)audioSourceTotalFrames, progress);

			track.clearAlignment();

			AnchorSandwichIterator iter = track.sandwichIterator();
			while (iter.hasNext()) {
				AnchorSandwich sandwich = iter.next();

				if (sandwich.isEmpty()) {
					continue;
				}

				aligner.align(
						sandwich.getWords(),
						sandwich.getInitialAnchor().getFrame(),
						sandwich.getFinalAnchor().getFrame());
			}
		}
	}


	public void alignInterleaved(ProgressDisplay progress)
			throws IOException, InterruptedException
	{
		LinearBridge lb = new LinearBridge(tracks);

		for (Track track: tracks) {
			track.clearAlignment();
		}

		while (lb.hasNext()) {
			AutoAligner aligner = new AutoAligner(
					convertedAudioFile, (int)audioSourceTotalFrames, progress);
			AnchorSandwich interleaved = lb.nextInterleavedElementSequence();
			Anchor ia = interleaved.getInitialAnchor();
			Anchor fa = interleaved.getFinalAnchor();
			List<Word> seq = new ArrayList<Word>();

			for (Element el: interleaved) {
				if (el instanceof Word) {
					seq.add((Word)el);
				}
			}

			aligner.align(
					seq,
					ia == null || !ia.hasTime()? 0: ia.getFrame(),
					fa == null || !fa.hasTime()? -1: fa.getFrame());
		}
	}


	//==========================================================================
	// LOAD/SAVE/EXPORT
	//==========================================================================

	/**
	 * Sets the audio file for this project, and converts it to a suitable
	 * format if needed.
	 */
	public void setAudio(String path) {
		wavname = path;

		if (path != null) {
			convertedAudioFile = suitableAudioFile(new File(wavname));

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
		} else
			convertedAudioFile = null;
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

		if (af.matches(SUITABLE_AUDIO_FORMAT)) {
			System.out.println("suitableAudioFile: no conversion needed!");
			return original;
		}

		System.out.println("suitableAudioFile: need conversion, trying to get one from the cache");

		Cache.FileFactory factory = new Cache.FileFactory() {
			public void write(File f) throws IOException {
				System.out.println("suitableAudioFile: no cache found... creating one");

				AudioInputStream stream;
				try {
					stream = AudioSystem.getAudioInputStream(original);
				} catch (UnsupportedAudioFileException ex) {
					ex.printStackTrace();
					throw new Error("Unsupported audio file; should've been caught above!");
				}

				System.out.println("suitableAudioFile: source format " + af);

				// Workaround for sound library bug - Formats with an unknown
				// sample size (such as OGG and MP3) cannot be converted to the
				// suitable format in one pass. First, the stream must be
				// converted to a fixed sample size (e.g. 16-bit) while keeping
				// the sample rate and channel count intact. Only *then* can we
				// up/downsample and/or downmix the stream.

				if (af.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED) {
					System.out.println("suitableAudioFile: 16-bit conversion first");
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

				System.out.println("suitableAudioFile: written!");
			}
		};

		return Cache.cachedFile("converted", "wav", factory, original);
	}


	public void saveJson(File file) throws IOException {
		FileWriter w = new FileWriter(file);
		JTRLoader.newGson().toJson(this, w);
		w.close();
	}

/* TODO PARALLEL TRACKS
	public void saveRawText(File file) throws IOException {
		PrintWriter w = FileUtils.writeFileUTF(file.getAbsolutePath());
		String prefix = "";
		for (Element el: elts) {
			if (el instanceof SpeakerTurn) {
				prefix = "\n";
			} else if (el instanceof Word) {
				w.print(prefix);
				w.print(((Word) el).getWordString());
				prefix = " ";
			}
		}
		w.close();
	}
*/

	public void savePraat(File f, boolean withWords, boolean withPhons)
			throws IOException
	{
		FileWriter w = new FileWriter(f);

		w.append("File type = \"ooTextFile\"")
				.append("\nObject class = \"TextGrid\"")
				.append("\n")
				.append("\nxmin = 0")
				.append("\nxmax = ")
				.append(Float.toString(TimeConverter.frame2sec((int)audioSourceTotalFrames)))
				.append("\ntiers? <exists>")
				.append("\nsize = ")
				.append(Integer.toString(tracks.size() * ((withWords?1:0) + (withPhons?1:0))))
				.append("\nitem []:");

		int id = 1;
		for (Track t: tracks) {
			StringBuilder wordSB = new StringBuilder();
			StringBuilder phoneSB = new StringBuilder();

			List<Word> words = t.getWords();

			int wordCount = 0;
			int phoneCount = 0;

			for (Word word: words) {
				if (!word.isAligned()) {
					continue;
				}

				praatInterval(
						wordSB,
						wordCount+1,
						word.getSegment().getStartFrame(),
						word.getSegment().getEndFrame(),
						word.toString());
				wordCount++;

				for (Word.Phone phone: word.getPhones()) {
					praatInterval(
							phoneSB,
							phoneCount+1,
							phone.getSegment().getStartFrame(),
							phone.getSegment().getEndFrame(),
							phone.toString());
					phoneCount++;
				}
			}

			if (withWords) {
				praatTierHeader(w, id++, t.speakerName + " words", wordCount);
				w.write(wordSB.toString());
			}

			if (withPhons) {
				praatTierHeader(w, id++, t.speakerName + " phons", phoneCount);
				w.write(phoneSB.toString());
			}
		}

		w.close();
	}

	/**
	 * Appends a Praat tier header.
	 * @param w Append text to this writer
	 * @param id Tier ID (Praat tier numbering starts at 1 and is contiguous!)
	 * @param name Tier name
	 * @param intervalCount Number of intervals in the tier
	 */
	private void praatTierHeader(
			Appendable w, int id, String name, int intervalCount)
			throws IOException
	{
		assert id > 0;
		w.append("\n\titem [").append(Integer.toString(id)).append("]:")
				.append("\n\t\tclass = \"IntervalTier\"")
				.append("\n\t\tname = \"").append(name).append('"') // TODO escape strings
				.append("\n\t\txmin = 0")
				.append("\n\t\txmax = ")
				.append(Float.toString(TimeConverter.frame2sec((int)audioSourceTotalFrames)))
				.append("\n\t\tintervals: size = ")
				.append(Integer.toString(intervalCount));
	}


	/**
	 * Appends a Praat interval.
	 * @param w Append text to this writer
	 * @param id Interval ID (Interval numbering starts at 1 and is contiguous!)
	 */
	private static void praatInterval(
			Appendable w, int id, int xminFrame, int xmaxFrame, String content)
			throws IOException
	{
		w.append("\n\t\tintervals [").append(Integer.toString(id)).append("]:")
				.append("\n\t\t\txmin = ")
				.append(Float.toString(TimeConverter.frame2sec(xminFrame)))
				.append("\n\t\t\txmax = ")
				.append(Float.toString(TimeConverter.frame2sec(xmaxFrame)))
				.append("\n\t\t\ttext = \"")
				.append(content).append('"'); // TODO escape strings
	}

}
