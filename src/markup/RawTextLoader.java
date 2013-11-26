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
		ListeElement listeElts = new ListeElement();
		ArrayList<Segment> nonText = new ArrayList<Segment>();

		for (int type = 0; type < typeList.size(); type++) {
			for (Pattern pat: typeList.get(type).getPatterns()) {
				Matcher mat = pat.matcher(normedText);
				while (mat.find())
					nonText.add(new Segment(mat.start(), mat.end(), type));
			}
		}
		Collections.sort(nonText);

		// on transforme les elements obtenus par ce parsing en elements pour jtrans
		int precfin = 0;
		boolean parserCettePartie;
		for (Segment seg: nonText){
			parserCettePartie = true;
			int deb = seg.deb;
			int fin = seg.fin;
			if (precfin > deb) {
				//cas entrecroisé : {-----------[---}-------]
				//on deplace de façon à avoir : {--------------}[-------]
				if (fin > precfin) deb = precfin;

					//cas imbriqué : {------[---]----------}
					//on ne parse pas l'imbriqu�
				else parserCettePartie = false;
			}//if (precfin > deb)

			if(parserCettePartie){

				// ligne de texte située avant
				if (deb-precfin>0) {
					String ligne = normedText.substring(precfin,deb);
					parserListeMot(ligne, precfin, listeElts, normedText);
				}//if (deb-precfin>0)

				//l'élement en lui même
				String sub = normedText.substring(deb, fin);
				switch (seg.type) {
					case 0: // LOCUTEUR
						// TODO: allow creating new speakers in raw text files (not TRS/TextGrid!)
						listeElts.add(new Element_Commentaire(sub));
						break;
					case 1: // COMMENT
						listeElts.add(new Element_Commentaire(sub));
						break;
					case 2: // BRUIT
						listeElts.add(new Element_Bruit(sub));
						break;
					case 3 : //Debut chevauchement
						listeElts.add(new Element_DebutChevauchement());
						break;
					case 4 : //Fin de chevauchement
						listeElts.add(new Element_FinChevauchement());
						break;
					case 5 : // ponctuation
						listeElts.add(new Element_Ponctuation(sub.charAt(0)));
						break;
					default : System.err.println("HOUSTON, ON A UN PROBLEME ! TYPE PARSE INCONNU");
				}
				precfin = fin;
			}
		}

		//ligne de texte située après le dernier élément
		if (normedText.length()-precfin>0) {
			String ligne = normedText.substring(precfin);
			parserListeMot(ligne, precfin, listeElts, normedText);
		}
		return listeElts;
	}//reparse


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
