/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.aligners.sphiinx4.tests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import plugins.speechreco.aligners.Alignement;
import plugins.speechreco.aligners.sphiinx4.S4AlignOrder;
import plugins.speechreco.aligners.sphiinx4.S4ForceAlignBlocViterbi;
import plugins.text.TexteEditor;
import plugins.text.elements.Element_Mot;

/**
 * probleme: l'alignement force ne trouve pas de chemin finissant, mais elle stagne
 * sur les premiers HMMs, malgre des beams tres larges !
 * 
 * @author cerisara
 *
 */
public class RecoTestBug1 {
	TexteEditor editor = new TexteEditor();
	Alignement alignement;
	
    void loadProject() {
        try {
            BufferedReader f = new BufferedReader(new FileReader("jtransalign.txt"));
            String s = f.readLine();
            s = f.readLine();
            int nmots = Integer.parseInt(s.substring(8));
            s = f.readLine();
            int nancres = Integer.parseInt(s.substring(9));
            alignement = new Alignement();
            alignement.allocateForWords(nmots);
            // mots
            for (int i = 0; i < nmots; i++) {
                s = f.readLine();
                int j = s.indexOf(' ');
                if (j < 0) {
                    // TODO: parfois le save enregistre un \n de trop ??
                    System.err.println("warning load " + i + " " + nmots + " " + s);
                    i--;
                    continue;
                }
                int deb = Integer.parseInt(s.substring(0, j));
                int k = s.indexOf(' ', j + 1);
                int fin = Integer.parseInt(s.substring(j + 1, k));
                if (fin >= 0) {
                    alignement.setAlignForWord(i, deb, fin);
                }
                alignement.wordLabels[i] = s.substring(k + 1);
            }
            System.out.println("dernier mot " + s);
            // ancres
            for (int i = 0; i < nancres; i++) {
                s = f.readLine();
                int j = Integer.parseInt(s);
                alignement.addManualAnchorv2(j);
            }

            //  texte
            StringBuilder ss = new StringBuilder();
            int n = Integer.parseInt(f.readLine());
            for (int i = 0; i < n; i++) {
                s = f.readLine();
                ss.append(s + "\n");
            }
            editor.setText(ss.toString());

            // regexps
            editor.parserRegexpFromBufferedReader(f);

            parse();
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void creeAlignement() {
        System.err.println("cree alignement");
        alignement = new Alignement();
        int nmots = editor.getListeElement().getMots().size();
        alignement.allocateForWords(nmots);
        // 2eme passe pour remplir
        for (int i = 0; i < nmots; i++) {
            alignement.wordLabels[i] = editor.getListeElement().getWordElement(i).getWordString();
        }
    }

    public void parse() {
    	editor.setEditable(false);
    	editor.reparse();
        Alignement oldalign = alignement;
        creeAlignement();
        if (oldalign != null) {
            // reporte les anciens alignements
            alignement.importAlign(oldalign);
        }
    }

    public RecoTestBug1() {
    	loadProject();
		List<Element_Mot> lmots = editor.getListeElement().getMots();
		String[] mots = new String[lmots.size()];
		lmots.toArray(mots);
		S4ForceAlignBlocViterbi aligner = S4ForceAlignBlocViterbi.getS4Aligner("C:\\xtof\\corpus\\culture.wav");
		aligner.setMots(mots);
		aligner.start();
		int firstWord = 94;
		int lastMot = 114;
		int firstFrame = 2741;
		int lastFrame = 3437;
		S4AlignOrder order = new S4AlignOrder(firstWord, firstFrame, lastMot, lastFrame);
		aligner.input2process.add(order);
		synchronized (order) {
			try {
				order.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
    }
    
	public static void main(String args[]) {
		RecoTestBug1 m = new RecoTestBug1();
	}
}
