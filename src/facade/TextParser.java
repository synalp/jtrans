package facade;

import plugins.text.ListeElement;
import plugins.text.elements.*;
import plugins.text.regexp.TypeElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for raw transcription text.
 */
public class TextParser {

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
	 * Return sorted list of segments containing non-text items.
	 */
	public static List<Segment> findNonTextSegments(String normedText, List<TypeElement> listeTypes) {
		ArrayList<Segment> nonText = new ArrayList<Segment>();
		for (int type = 0; type < listeTypes.size(); type++) {
			for (Pattern pat: listeTypes.get(type).getPatterns()) {
				Matcher mat = pat.matcher(normedText);
				while (mat.find()) {
					nonText.add(new Segment(mat.start(), mat.end(), type));
				}
			}
		}
		Collections.sort(nonText);
		return nonText;
	}

	public static ListeElement parseString(String normedText, List<Segment> nonText) {
		ListeElement listeElts = new ListeElement();

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
				switch (seg.type) {
					case 0: // LOCUTEUR
						int num=0;
						String loc = normedText.substring(deb,fin);
						Matcher p = Pattern.compile("\\d").matcher(loc);
						if (p.find()) {
							int posnum = p.start();
							try {
								num=Integer.parseInt(loc.substring(posnum).trim());
								loc=loc.substring(0,posnum).trim();
							} catch (NumberFormatException e) {
								// e.printStackTrace();
							}
						}
						listeElts.addLocuteurElement(loc, num);
						break;
					case 1: // COMMENT
						listeElts.add(Element_Commentaire.fromSubstring(normedText, deb, fin));
						break;
					case 2: // BRUIT
						listeElts.add(Element_Mot.fromSubstring(normedText, deb, fin, true));
						break;
					case 3 : //Debut chevauchement
						listeElts.add(new Element_DebutChevauchement());
						break;
					case 4 : //Fin de chevauchement
						listeElts.add(new Element_FinChevauchement());
						break;
					case 5 : // ponctuation
						listeElts.add(new Element_Ponctuation(normedText.substring(deb, fin).charAt(0)));
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

			if(index > debutMot){
				listeElts.add(Element_Mot.fromSubstring(
						text, debutMot + precfin, index + precfin, false));
			}
		}
	}
}
