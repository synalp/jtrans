package facade;

import speechreco.aligners.sphiinx4.Alignment;
import speechreco.aligners.sphiinx4.S4AlignOrder;
import plugins.text.ListeElement;
import plugins.text.elements.*;
import plugins.text.regexp.TypeElement;

import java.awt.*;
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
	public int[] clearAlignmentAround(Element_Ancre anchor) {
		// anchor element indices
		int prev = 0;
		int curr = -1;
		int next = 0;

		for (int i = 0; i < elts.size(); i++) {
			Element el = elts.get(i);
			if (!(el instanceof Element_Ancre))
				continue;
			if (el == anchor) {
				curr = i;
			} else if (curr < 0) {
				prev = i;
			} else {
				next = i;
				break;
			}
		}

		for (int i = prev; i <= next; i++) {
			Element el = elts.get(i);
			if (!(el instanceof Element_Mot))
				continue;
			Element_Mot word = (Element_Mot)el;
			word.posInAlign = -1;
		}

		int start = ((Element_Ancre)elts.get(prev)).getFrame();
		int end   = ((Element_Ancre)elts.get(next)).getFrame();
		clearAlignmentInterval(start, end);
		return new int[]{prev, next};
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
}