/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.aligners;

import java.io.BufferedReader;
import java.io.IOException;

import plugins.applis.SimpleAligneur.Aligneur;
import plugins.text.elements.Element_Mot;
import plugins.utils.FileUtils;

public class STMAlign {
	public static OldAlignment loadAlign(String stmfile, OldAlignment parsedTextAlign, Aligneur aligneur) {
		try {
			OldAlignment align = new OldAlignment();
			align.allocateForWords(parsedTextAlign.wordLabels.length);

			int posMotInEditor=0;
			int editorLigne=0;
			String editortxt = aligneur.edit.getText();
			Element_Mot mot = aligneur.edit.getListeElement().getFirstMot();
			int motidx=0;
			while (posMotInEditor<mot.start) {
				if (editortxt.charAt(posMotInEditor)=='\n') editorLigne++;
				posMotInEditor++;
			}
			
			BufferedReader f = FileUtils.openFileISO(stmfile);
			int ligneSTM=-1;
			// dans cette boucle, "mot" doit toujours pointer vers le 1er mot de la ligne suivante
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				if (s.startsWith(";;")) continue;
				String[] ss = s.split(" ");
				float tdeb=Float.parseFloat(ss[3]);
				float tfin=Float.parseFloat(ss[4]);
				int i=s.indexOf('>')+2;
				if (i>=s.length()) continue; // pas de texte associe a ce segment
				String txt = s.substring(i);
				if (txt.indexOf("ignore_time_segment_in_scoring")>=0) continue;
				// chaque ligne dans le textEditor doit correspondre a une ligne dans le fichier STM !!
				// je recupere les mots sur cette ligne
				ligneSTM++;
				if (editorLigne>ligneSTM) {
					// le mot suivant se trouve sur une ligne apres
					continue;
				}
				if (editorLigne<ligneSTM) System.err.println("ERROR ligne pass�e ! "+s);
				// instants de debut et fin de la ligne:
				int frdeb = seconds2frames(tdeb);
				int frfin = seconds2frames(tfin);
				// on cherche la fin de ligne:
				int endOfLine = posMotInEditor+1;
				while (endOfLine<editortxt.length()) {
					if (editortxt.charAt(endOfLine)=='\n') break;
					endOfLine++;
				}
				// on cherche le dernier mot de la ligne:
				int dermotidx=motidx;
				Element_Mot dermot = mot;
//				while (dermot.nextEltInGram!=null) {
//					if (dermot.nextEltInGram.posDebInTextPanel>endOfLine) break;
//					dermotidx++;
//					dermot=dermot.nextEltInGram;
//				}
				
				align.setAlignForWord(dermotidx, frdeb, frfin);
				align.equiAlignBetweenWords(motidx, dermotidx, frdeb, frfin);
				
//				mot=dermot.nextEltInGram;
				if (mot!=null) {
					motidx=dermotidx+1;
					while (posMotInEditor<mot.start) {
						if (editortxt.charAt(posMotInEditor)=='\n') editorLigne++;
						posMotInEditor++;
					}
				} else motidx=-1;
				// si mot est null, on doit alors etre sur la derniere ligne !
			}
			f.close();
			return align;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static String importText(String stmfile) {
		try {
			BufferedReader f = FileUtils.openFileISO(stmfile);
			StringBuilder sb = new StringBuilder();
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				if (s.startsWith(";;")) continue;
				String[] ss = s.split(" ");
				int i=s.indexOf('>')+2;
				if (i>=s.length()) continue; // pas de texte associe a ce segment
				String txt = s.substring(i);
				if (txt.indexOf("ignore_time_segment_in_scoring")>=0) continue;
				ss = txt.trim().split(" ");
				for (i=0;i<ss.length;i++) {
					sb.append(ss[i]+" ");
				}
				sb.append('\n');
			}
			f.close();
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static int seconds2frames(float sec) {
		int f = (int)(sec*100f);
		return f;
	}
}
