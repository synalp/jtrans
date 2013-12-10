package jtrans.facade;

import jtrans.elements.*;
import jtrans.markup.JTRLoader;
import jtrans.speechreco.s4.Alignment;
import jtrans.speechreco.s4.S4AlignOrder;
import jtrans.utils.FileUtils;
import jtrans.utils.TimeConverter;

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
	public List<Track> tracks = new ArrayList<Track>();
	public String wavname;
	public List<ElementType> types = new ArrayList<ElementType>(Arrays.asList(DEFAULT_TYPES));

	// TODO this setting should be saved to disk
	public static boolean linebreakBeforeAnchors = true;


	public void clearAlignment() {
		for (Track track : tracks)
			track.clearAlignment();
		refreshIndex();
	}


	public static final ElementType DEFAULT_TYPES[] = {
			new ElementType("Speaker", Color.GREEN,
					"(^|\\n)(\\s)*\\w\\d+\\s"),

			new ElementType("Comment", Color.YELLOW,
					"\\{[^\\}]*\\}",
					"\\[[^\\]]*\\]",
					"\\+"),

			new ElementType("Noise", Color.CYAN,
					"\\*+"),

			new ElementType("Overlap Start", Color.PINK,
					"<"),

			new ElementType("Overlap End", Color.PINK,
					">"),

			new ElementType("Punctuation", Color.ORANGE,
					"\\?",
					"\\:",
					"\\;",
					"\\,",
					"\\.",
					"\\!"),

			new ElementType("Anchor", new Color(0xddffaa)),
	};


	public void refreshIndex() {
		for (Track track : tracks)
			track.refreshIndex();
	}


	//==========================================================================
	// LOAD/SAVE/EXPORT
	//==========================================================================


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


	public void savePraat(File f, boolean withWords, boolean withPhons) throws IOException {
		int             tiers        = speakers.size() * ((withWords?1:0) + (withPhons?1:0));
		int             finalFrame   = words.getSegmentEndFrame(words.getNbSegments() - 1);
		Alignment       speakerTurns = elts.getLinearSpeakerTimes(words);
		Alignment[]     spkWords     = null;
		Alignment[]     spkPhons     = null;
		FileWriter      w            = new FileWriter(f);

		w.append("File type = \"ooTextFile\"")
				.append("\nObject class = \"TextGrid\"")
				.append("\n")
				.append("\nxmin = 0")
				.append("\nxmax = ").append("" + TimeConverter.frame2sec(finalFrame))
				.append("\ntiers? <exists>")
				.append("\nsize = ").append("" + tiers)
				.append("\nitem []:");

		// Linear, non-overlapping tiers
		if (withWords) spkWords = words.breakDownBySpeaker(speakers.size(), speakerTurns);
		if (withPhons) spkPhons = phons.breakDownBySpeaker(speakers.size(), speakerTurns);

		// Account for overlaps
		for (int i = 0; i < overlaps.size(); i++) {
			int speakerID = overlapSpeakers.get(i);
			S4AlignOrder order = overlaps.get(i);
			if (withWords)
				spkWords[speakerID].overwrite(order.alignWords);
			if (withPhons)
				spkPhons[speakerID].overwrite(order.alignPhones);
		}

		// Now that we have the final segment count, generate Praat tiers
		int id = 1;
		for (int i = 0; i < speakers.size(); i++) {
			String name = speakers.get(i).getName();
			if (withWords)
				praatTier(w, id++, name + " words", finalFrame, spkWords[i]);
			if (withPhons)
				praatTier(w, id++, name + " phons", finalFrame, spkPhons[i]);
		}

		w.close();
	}


	/**
	 * Generates a Praat tier for an alignment.
	 * @param w Append text to this writer
	 * @param id Tier ID (Praat tier numbering starts at 1 and is contiguous!)
	 * @param name Tier name
	 * @param finalFrame Final frame in the entire file
	 */
/* TODO
	private static void praatTier(Writer w, int id, String name, int finalFrame, Alignment al) throws IOException {
		assert id > 0;
		w.append("\n\titem [").append(Integer.toString(id)).append("]:")
				.append("\n\t\tclass = \"IntervalTier\"")
				.append("\n\t\tname = \"").append(name).append('"') // TODO escape strings
				.append("\n\t\txmin = 0")
				.append("\n\t\txmax = ").append(Float.toString(TimeConverter.frame2sec(finalFrame)))
				.append("\n\t\tintervals: size = ")
				.append("" + al.getNbSegments());
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
*/
}
