package facade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import speechreco.aligners.sphiinx4.Alignment;
import speechreco.aligners.sphiinx4.S4AlignOrder;
import plugins.text.ListeElement;
import plugins.text.elements.*;
import plugins.text.regexp.TypeElement;
import utils.FileUtils;
import utils.InterfaceAdapter;
import utils.TimeConverter;

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
	public List<Locuteur_Info> speakers = new ArrayList<Locuteur_Info>();
	public ListeElement elts = new ListeElement();
	public String wavname;
	public Alignment words = new Alignment();
	public Alignment phons = new Alignment();
	public List<TypeElement> types = new ArrayList<TypeElement>(Arrays.asList(DEFAULT_TYPES));
	public List<S4AlignOrder> overlaps = new ArrayList<S4AlignOrder>();
	public List<Byte> overlapSpeakers = new ArrayList<Byte>();


	public void clearAlignment() {
		words = new Alignment();
		phons = new Alignment();
		overlaps = new ArrayList<S4AlignOrder>();
		overlapSpeakers = new ArrayList<Byte>();
		for (Element_Mot word: elts.getMots())
			word.posInAlign = -1;
		refreshIndex();
	}


	public void clearAlignmentInterval(int startFrame, int endFrame) {
		for (Element_Mot word: elts.getMots()) {
			int seg = word.posInAlign;
			if (word.posInAlign < 0)
				continue;
			if (words.getSegmentDebFrame(seg) >= startFrame &&
					words.getSegmentEndFrame(seg) <= endFrame)
				word.posInAlign = -1;
		}

		words.clearInterval(startFrame, endFrame);
		phons.clearInterval(startFrame, endFrame);
		// TODO: unalign phonemes, clear affected overlaps...

		refreshIndex();
	}


	/**
	 * Clears alignment around an anchor.
	 * @return an array of two integers representing the indices of the first
	 * and last elements whose alignment was cleared
	 */
	public ListeElement.Neighborhood<Element_Ancre> clearAlignmentAround(Element_Ancre anchor) {
		ListeElement.Neighborhood<Element_Ancre> range =
				elts.getNeighbors(anchor, Element_Ancre.class);

		for (int i = range.prevIdx + 1; i <= range.nextIdx - 1; i++) {
			Element el = elts.get(i);
			if (!(el instanceof Element_Mot))
				continue;
			Element_Mot word = (Element_Mot)el;
			word.posInAlign = -1;
		}

		clearAlignmentInterval(
				range.prev!=null? range.prev.getFrame(): 0,
				range.next!=null? range.next.getFrame(): Integer.MAX_VALUE);

		return range;
	}


	public static final TypeElement DEFAULT_TYPES[] = {
			new TypeElement("Speaker", Color.GREEN,
					"(^|\\n)(\\s)*\\w\\d+\\s"),

			new TypeElement("Comment", Color.YELLOW,
					"\\{[^\\}]*\\}",
					"\\[[^\\]]*\\]",
					"\\+"),

			new TypeElement("Noise", Color.CYAN,
					"\\*+"),

			new TypeElement("Overlap Start", Color.PINK,
					"<"),

			new TypeElement("Overlap End", Color.PINK,
					">"),

			new TypeElement("Punctuation", Color.ORANGE,
					"\\?",
					"\\:",
					"\\;",
					"\\,",
					"\\.",
					"\\!"),

			new TypeElement("Anchor", new Color(0xddffaa)),
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
			if (buf.length() > 0)
				buf.append(el instanceof Element_Locuteur ? '\n': ' ');

			int pos = buf.length();
			String str;
			if (el instanceof Element_Locuteur)
				str = speakers.get(((Element_Locuteur)el).getLocuteurID()).getName();
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
			if (el instanceof Element_Locuteur) {
				prefix = "\n";
			} else if (el instanceof Element_Mot) {
				w.print(prefix);
				w.print(((Element_Mot) el).getWordString());
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