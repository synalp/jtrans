/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.phonetiseurs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Cette classe charge les prononciations g�n�r�es par Vincent Colotte pour la base Morphalou,
 * en ajoutant des liaisons optionnelles avec des r�gles.
 * 
 * @author cerisara
 *
 */
public class Morphalou extends PronunciationsLexicon {
	private static final long serialVersionUID = 1L;
	final static String corpdir[] = {
		"ressources/ListeFinal_Morph_Dico_f.txt",
		"D:/xtof/corpus/Morphalou/ListeFinal_Morph_Dico_f.txt",
		"C:/xtof/corpus/Morphalou/ListeFinal_Morph_Dico_f.txt",
		"/tmp/ListeFinal_Morph_Dico_f.txt",
		"/media/OS/xtof/corpus/ListeFinal_Morph_Dico_f.txt",
		"/local/dable2/parole/Ressources/Morphalou/ListeFinal_Morph_Dico_f.txt",
		"/home/xtof/corpus/Morphalou/ListeFinal_Morph_Dico_f.txt"
	};

	public Morphalou() {
		try {
			loadCorpus();
		} catch (Exception e) {
			System.err.println("ERROR loading Morphalou - on continue sans dictionnaire !");
		}
	}
	
	public Iterator getMots() {
		return dico.keySet().iterator();
	}
	
	void loadCorpus() {
		dico = new HashMap<String,Entree>();
		Enumeration<String> f = getEntries(corpdir, false);
		System.err.println("loading main corpus...");
		loadFile(f);
		System.err.println("main corpus loaded !");
		f = getEntries(dicoperso, false);
		if (f==null) System.err.println("WARNING: morphalou dico perso not found !");
		else loadFileBDLEX(f, false);
		System.err.println("all corpus loaded !");
	}
	
	String addLiaison(String pron, String phone) {
		int i=pron.lastIndexOf(' ');
		if (i<0) i=0; else i++;
		String lastPhone=pron.substring(i);
		if (!lastPhone.equals(phone)) {
			// on ajoute la liaison possible que si le dernier phone n'est pas le meme phoneme !
			return pron+" [ "+phone+" ]";
		}
		else return pron;
	}
	
	public String getRule(Entree e) {
		String s = e.phonesBase;
		s = convertPhones(s);
		s=s.trim();
		
		// il n'y a pas de fins optionnelles dans Morphalou: il faut les generer en
		// fonction des terminaisons !
//		s=s.replaceAll("swa", "[ swa ]");
		char lastchar = curmot.charAt(curmot.length()-1);
		switch (lastchar) {
		case 't':
			s=addLiaison(s, "t"); break;
		case 'n':
			s=addLiaison(s, "n"); break;
		case 's':
			s=addLiaison(s, "z"); break;
		case 'z':
			s=addLiaison(s, "z"); break;
		case 'r':
			s=addLiaison(s, "R"); break;
		case 'd':
			s=addLiaison(s, "t"); break;
// TODO: f -> v "neuf an"
			// TODO: "bon an" oh > O
		}
		// il faut un espace final !
		if (s.charAt(s.length()-1)!=' ') s+=" ";
		return s;
	}
	
	void loadFile(Enumeration<String> bf) {
		while (bf.hasMoreElements()) {
			String s = bf.nextElement();
			/*
				if (ii%1000==0) {
					System.err.print("\r word "+s);
					System.err.flush();
				}
			 */
			int i = s.indexOf("___");
			String phonemes = s.substring(i+4);
			phonemes = phonemes.replaceAll(" ", "");
			String mot = s.substring(0,i);
			// pour le moment, je convertis le mot en minuscules !
			mot=mot.toLowerCase();
			String[] ss = mot.split(" ");
			mot = ss[1];
			// si plusieurs mots, on transforme cela en "segment", mais ca ne sert probablement a rien,
			// car lors des requetes, les mots sont isoles un par un...
			for (i=2;i<ss.length-2;i++)
				mot += "_"+ss[i];
			Entree e = new Entree();
			e.phonesBase=phonemes;
			if (dico.containsKey(mot)) {
				if (!e.combinerAvec(dico.get(mot)))
					e=dico.get(mot);
			}
			dico.put(mot, e);
		}
	}
	/**
	 * pour le dico perso !
	 */
	void loadFileBDLEX(Enumeration<String> bf, boolean withConvAccent) {
		String nom="";
		while (bf.hasMoreElements()) {
			String s = bf.nextElement();
			if (s==null) break;
			Entree e = new Entree();
			int i1 = s.indexOf(';');
			nom = s.substring(0,i1);
			// pour le moment, je convertis le mot en minuscules !
			boolean aa = false;
			nom=nom.toLowerCase();
			if (withConvAccent)
				nom = convertAccents(nom);
			s=s.substring(i1+1);
			i1 = s.indexOf(';');
			e.phonesBase=s.substring(0,i1);
			s=s.substring(i1+1);
			i1 = s.indexOf(';');
			e.phonesOption=s.substring(0,i1);
			if (dico.containsKey(nom)) {
				// System.out.println("multiple "+nom);
				if (!e.combinerAvec(dico.get(nom)))
					e=dico.get(nom);
			}
			dico.put(nom,e);
		}
		System.err.println("dernier nom lu "+nom);
	}

	public static void main(String args[]) {
		Morphalou m = new Morphalou();
		try {
			BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
			for (;;) {
				String s = bf.readLine();
				if (s.length()>0) {
					if (s.charAt(0)=='=') {
						switch (s.charAt(1)) {
						case 'q': return;
						}
					} else
						System.out.println(m.getRule(s));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
