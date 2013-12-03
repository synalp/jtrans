package markup;

import facade.Project;
import plugins.text.ListeElement;
import plugins.text.elements.*;
import plugins.text.regexp.TypeElement;
import utils.FileUtils;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Parser for raw transcription text.
 */
public class RawTextLoader implements MarkupLoader {
	/**
	 * Substitute junk characters with ones that JTrans can handle.
	 */
	public static String normalizeText(String text) {
		return text
				.replace('\u2019', '\'')            // smart quotes
				.replace("\r\n", "\n")              // Windows CRLF
				.replace('\r', '\n')                // remaining non-Unix linebreaks
				.replace('\u00a0', ' ')             // non-breaking spaces
				.replaceAll("[\"=/]", " ")          // junk punctuation marks
				.replaceAll("\'(\\S)", "\' $1")     // add space after apostrophes glued to a word
		;
	}

	/**
	 * Creates elements from a string according to the regular expressions
	 * defined in typeList.
	 */
	public static ListeElement parseString(String normedText, List<TypeElement> typeList) {
		class NonTextSegment implements Comparable<NonTextSegment> {
			public int start, end, type;

			public NonTextSegment(int start, int end, int type) {
				this.start = start;
				this.end = end;
				this.type = type;
			}

			public int compareTo(NonTextSegment other) {
				if (start > other.start) return 1;
				if (start < other.start) return -1;
				return 0;
			}
		}

		ListeElement listeElts = new ListeElement();
		ArrayList<NonTextSegment> nonText = new ArrayList<NonTextSegment>();

		for (int type = 0; type < typeList.size(); type++) {
			for (Pattern pat: typeList.get(type).getPatterns()) {
				Matcher mat = pat.matcher(normedText);
				while (mat.find())
					nonText.add(new NonTextSegment(mat.start(), mat.end(), type));
			}
		}
		Collections.sort(nonText);

		// Turn the non-text segments into Elements
		int prevEnd = 0;
		for (NonTextSegment seg: nonText) {
			int start = seg.start;
			int end = seg.end;
			if (prevEnd > start) {
				//cas entrecroisé : {-----------[---}-------]
				//on deplace de façon à avoir : {--------------}[-------]
				if (end > prevEnd) start = prevEnd;

				//cas imbriqué : {------[---]----------}
				//on ne parse pas l'imbriqué
				else continue;
			}

			// Line right before
			if (start > prevEnd) {
				String line = normedText.substring(prevEnd, start);
				parserListeMot(line, prevEnd, listeElts, normedText);
			}

			// Create the actual element
			String sub = normedText.substring(start, end);
			switch (seg.type) {
				case 0: // Speaker
					// TODO: allow creating new speakers in raw text files (not TRS/TextGrid!)
					listeElts.add(new Element_Commentaire(sub));
					break;
				case 1: // Comment
					listeElts.add(new Element_Commentaire(sub));
					break;
				case 2: // Noise
					listeElts.add(new Element_Bruit(sub));
					break;
				case 3: // Overlap Start
					listeElts.add(new Element_DebutChevauchement());
					break;
				case 4: // Overlap End
					listeElts.add(new Element_FinChevauchement());
					break;
				case 5: // Punctuation
					listeElts.add(new Element_Ponctuation(sub.charAt(0)));
					break;
				default:
					System.err.println("RawTextLoader: WARNING: unknown element type " + seg.type);
			}

			prevEnd = end;
		}

		// Line after the last element
		if (normedText.length() > prevEnd) {
			String line = normedText.substring(prevEnd);
			parserListeMot(line, prevEnd, listeElts, normedText);
		}

		return listeElts;
	}


	private static void parserListeMot(String ligne, int precfin, ListeElement listeElts, String text) {
		int index = 0;
		int debutMot;
		//on parcourt toute la ligne
		while(index < ligne.length()){

			//on saute les espaces
			while(index < ligne.length() &&
					Character.isWhitespace(ligne.charAt(index))){
				index++;
			}

			debutMot =  index;
			//on avance jusqu'au prochain espace

			while((index < ligne.length()) && (!Character.isWhitespace(ligne.charAt(index)))){
				index++;
			}

			if (index > debutMot){
				listeElts.add(new Element_Mot(text.substring(debutMot + precfin, index + precfin)));
			}
		}
	}

	@Override
	public Project parse(File file) throws ParsingException, IOException {
		Project project = new Project();
		BufferedReader reader = FileUtils.openFileAutoCharset(file);

		// Add default speaker
		Locuteur_Info speaker = new Locuteur_Info((byte)0, "L1");
		project.speakers.add(speaker);
		project.elts.add(new Element_Locuteur(speaker));

		while (true) {
			String line = reader.readLine();
			if (line == null)
				break;
			line = normalizeText(line.trim());
			project.elts.addAll(parseString(line, project.types));
		}

		reader.close();

		return project;
	}

	@Override
	public String getFormat() {
		return "Raw Text";
	}
}
