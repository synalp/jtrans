package jtrans.facade;

import jtrans.elements.*;
import jtrans.markup.JTRLoader;
import jtrans.speechreco.s4.Alignment;
import jtrans.utils.TimeConverter;

import javax.sound.sampled.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Used for easy serialization (for now).
 * Eventually this class should become more useful.
 * TODO: centralize project methods here
 */
public class Project {

	/**
	 * Target audio format. Any input audio files that do not match this format
	 * will be converted to it before being processed.
	 */
	private static final AudioFormat SUITABLE_AUDIO_FORMAT =
			new AudioFormat(16000, 16, 1, true, false);


	public List<Track> tracks = new ArrayList<Track>();
	public List<ElementType> types = new ArrayList<ElementType>(Arrays.asList(DEFAULT_TYPES));

	/** Audio file in a suitable format for processing */
	public String wavname;
	public transient File convertedAudioFile = null;
	public transient long audioSourceTotalFrames = -1;


	public void clearAlignment() {
		for (Track track : tracks)
			track.clearAlignment();
		refreshIndex();
	}


	public static final ElementType DEFAULT_TYPES[] = {
			new ElementType("Generic Comment", Color.YELLOW,
					"\\{[^\\}]*\\}",
					"\\[[^\\]]*\\]",
					"\\+"),

			new ElementType("Speaker", Color.GREEN,
					"(^|\\n)(\\s)*\\w\\d+\\s"),

			new ElementType("Noise", Color.CYAN,
					"\\*+"),

			new ElementType("Overlap", Color.PINK,
					"<",
					">"),

			new ElementType("Punctuation", Color.ORANGE,
					"\\?",
					"\\:",
					"\\;",
					"\\,",
					"\\.",
					"\\!"),
	};


	public void refreshIndex() {
		for (Track track : tracks)
			track.refreshIndex();
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
		AudioFormat af;

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

				AudioInputStream originalStream;
				try {
					originalStream = AudioSystem.getAudioInputStream(original);
				} catch (UnsupportedAudioFileException ex) {
					ex.printStackTrace();
					throw new Error("Unsupported audio file; should've been caught above!");
				}

				AudioSystem.write(
						AudioSystem.getAudioInputStream(SUITABLE_AUDIO_FORMAT, originalStream),
						AudioFileFormat.Type.WAVE,
						f);
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
			if (withWords)
				praatTier(w, id++, t.speakerName + " words", t.words);
			if (withPhons)
				praatTier(w, id++, t.speakerName + " phons", t.phons);
		}

		w.close();
	}


	/**
	 * Generates a Praat tier for an alignment.
	 * @param w Append text to this writer
	 * @param id Tier ID (Praat tier numbering starts at 1 and is contiguous!)
	 * @param name Tier name
	 */
	private void praatTier(Writer w, int id, String name, Alignment al)
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
				.append(Integer.toString(al.getNbSegments()));
		for (int j = 0; j < al.getNbSegments(); j++) {
			w.append("\n\t\tintervals [").append(Integer.toString(j+1)).append("]:")
					.append("\n\t\t\txmin = ")
					.append(Float.toString(TimeConverter.frame2sec(al.getSegmentDebFrame(j))))
					.append("\n\t\t\txmax = ")
					.append(Float.toString(TimeConverter.frame2sec(al.getSegmentEndFrame(j))))
					.append("\n\t\t\ttext = \"")
					.append(al.getSegmentLabel(j)).append('"'); // TODO escape strings
		}
	}
}
