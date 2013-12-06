package jtrans.facade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jtrans.elements.*;
import jtrans.speechreco.s4.Alignment;
import jtrans.speechreco.s4.S4AlignOrder;
import jtrans.utils.FileUtils;
import jtrans.utils.InterfaceAdapter;
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
	public List<Speaker> speakers = new ArrayList<Speaker>();
	public ElementList elts = new ElementList();
	public String wavname;
	public Alignment words = new Alignment();
	public Alignment phons = new Alignment();
	public List<ElementType> types = new ArrayList<ElementType>(Arrays.asList(DEFAULT_TYPES));
	public List<S4AlignOrder> overlaps = new ArrayList<S4AlignOrder>();
	public List<Byte> overlapSpeakers = new ArrayList<Byte>();

	// TODO this setting should be saved to disk
	public static boolean linebreakBeforeAnchors = false;


	public void clearAlignment() {
		words = new Alignment();
		phons = new Alignment();
		overlaps = new ArrayList<S4AlignOrder>();
		overlapSpeakers = new ArrayList<Byte>();
		for (Word word: elts.getMots())
			word.posInAlign = -1;
		refreshIndex();
	}


	/**
	 * Clears alignment around an anchor.
	 * @return the anchor's neighborhood, i.e. the range of elements
	 * that got unaligned
	 */
	public ElementList.Neighborhood<Anchor> clearAlignmentAround(Anchor anchor) {
		ElementList.Neighborhood<Anchor> range =
				elts.getNeighbors(anchor, Anchor.class);

		int from = range.prev!=null? range.prevIdx: 0;
		int to   = range.next!=null? range.nextIdx: elts.size()-1;

		// Unalign the affected words
		for (int i = from; i <= to; i++) {
			Element el = elts.get(i);
			if (el instanceof Word)
				((Word) el).posInAlign = -1;
		}

		// Remove segments
		int beforeRemoval = words.getNbSegments();
		words.clearInterval(
				range.prev != null ? range.prev.getFrame() : 0,
				range.next != null ? range.next.getFrame() : Integer.MAX_VALUE);
		int removed = beforeRemoval - words.getNbSegments();

		// Adjust elements following the removal
		for (int i = to+1; i < elts.size(); i++) {
			Element el = elts.get(i);
			if (!(el instanceof Word))
				continue;
			((Word) el).posInAlign -= removed;
		}

		// TODO: unalign phonemes, clear affected overlaps...
		refreshIndex();

		return range;
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
		words.buildIndex();
		phons.buildIndex();
		elts.refreshIndex();
	}

	/**
	 * Renders the element list as a long string and sets element positions
	 * accordingly.
	 * @return the rendered string
	 */
	public String render() {
		StringBuilder buf = new StringBuilder();

		for (Element el: elts) {
			if (buf.length() > 0) {
				if (el instanceof SpeakerTurn ||
						(linebreakBeforeAnchors && el instanceof Anchor))
					buf.append('\n');
				else
					buf.append(' ');
			}

			int pos = buf.length();
			String str;
			if (el instanceof SpeakerTurn)
				str = speakers.get(((SpeakerTurn)el).getLocuteurID()).getName();
			else
				str = el.toString();
			buf.append(str);

			el.start = pos;
			el.end = pos + str.length();
		}

		return buf.toString();
	}


	//==========================================================================
	// LOAD/SAVE/EXPORT
	//==========================================================================

	/**
	 * Returns a Gson object suitable for serializing and deserializing JTrans
	 * projects to/from JSON.
	 */
	private static Gson newGson() {
		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(Element.class, new InterfaceAdapter<Element>("$TYPE$"));
		gb.setPrettyPrinting();
		return gb.create();
	}


	public static Project fromJson(File file) throws IOException {
		FileReader r = new FileReader(file);
		Project project = newGson().fromJson(r, Project.class);
		r.close();
		project.refreshIndex();
		return project;
	}


	public void saveJson(File file) throws IOException {
		FileWriter w = new FileWriter(file);
		newGson().toJson(this, w);
		w.close();
	}


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
}