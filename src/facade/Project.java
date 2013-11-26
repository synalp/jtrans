package facade;

import plugins.speechreco.aligners.sphiinx4.Alignment;
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