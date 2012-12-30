package main;

import java.io.BufferedReader;
import java.io.FileReader;

import phonetiseur.FileUtils;
import plugins.phonetiseur.PhonetiseurFacade;
import plugins.speechreco.grammaire.Grammatiseur;


public class Phonetiseur {
	private PhonetiseurFacade ph = null;

	public String phonetise(String mot) {
//		if (ph==null) ph = new PhonetiseurFacade(cheminFichierMatriceProbaEtLexique, cheminRepArff, cheminRepModel);
		return null;
	}

	public static void main(String[] args) throws Exception {
		Grammatiseur gram = Grammatiseur.getGrammatiseur();
		BufferedReader f= new FileUtils().openFileUTF(args[0]);
		for (int i=0;;i++) {
			String s=f.readLine();
			if (s==null) break;
			String r = gram.getGrammar(s);
			System.out.println("PHONUTT "+s+" .. .. "+r);
		}
		f.close();
	}
}
