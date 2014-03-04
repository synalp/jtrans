/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

package fr.loria.synalp.jtrans.speechreco.grammaire;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import fr.loria.synalp.jtrans.phonetiseur.PhonetiseurFacade;
import fr.loria.synalp.jtrans.speechreco.phonetiseurs.Morphalou;
import fr.loria.synalp.jtrans.speechreco.phonetiseurs.PronunciationsLexicon;
import fr.loria.synalp.jtrans.speechreco.phonetiseurs.SimplePhonetiseur;

public class Grammatiseur implements Serializable {

	public static boolean fastLoading=false;
	
	public final static String REP_PHONETISEUR = "fichiersPhonetiseur/";
	public final static String FICHIER_G_P_PROB = "graphemes_phonemes_matriceProba";
	public final static String RP_ARFF = "entetesArff/";
	public final static String REP_MODELS = "models/";
	private static final long serialVersionUID = 1L;
	PronunciationsLexicon dictionnaire;
	HashMap<String, String> numbersMap = new HashMap<String, String>();
	public HashMap<String, Integer> unk = new HashMap<String, Integer>();
	SimplePhonetiseur simplePhonetiseur = new SimplePhonetiseur();
	PhonetiseurFacade phonetiseur=null;
	Nombres parsernb = new Nombres();

	public static Grammatiseur grammatiseur = null;

	public static Grammatiseur getGrammatiseur() {
		if (grammatiseur == null) {
			grammatiseur = new Grammatiseur();
			if (!fastLoading)
				grammatiseur.initPhonetiseur();
		}
		return grammatiseur;
	}

	private Grammatiseur() {
		dictionnaire = new Morphalou();
		String rule = dictionnaire.getRule("le");
		if (rule==null||rule.length()==0)
			JOptionPane.showMessageDialog(null, "WARNING: error loading dictionary ! I'll use the phonetiser instead...");
		init();
	}

	public void initPhonetiseur() {
		System.out.println("init phonetiseur "+REP_PHONETISEUR + REP_MODELS);
		phonetiseur = new PhonetiseurFacade(REP_PHONETISEUR + FICHIER_G_P_PROB, REP_PHONETISEUR + RP_ARFF, REP_PHONETISEUR + REP_MODELS);
		try {
			phonetiseur.chargerClassifieurs();
		} catch (Exception ex) {
			System.out.println("Warning : le phonetiseur ne s'est pas charge correctement...");
			//            ex.printStackTrace();
			phonetiseur=null;
		}
	}

	public void reset() {
		unk = new HashMap<String, Integer>();
	}

	private String getRule4Nombre(String normeds) {
		StringBuilder res = new StringBuilder();
		String[] ss = normeds.split(" ");
		// ce sont les symboles utilises dans les rules en sortie de Nombres qu'il ne faut pas phonetiser mais recopier tels quels
		final String[] rulessymb = {"[","]","(",")","|"};
		final List<String> rsorted = Arrays.asList(rulessymb);
		for (String x : ss) {
			if (rsorted.contains(x)) res.append(x+" ");
			else res.append(dictionnaire.getRule(x));
		}
		return res.toString();
	}
	
	public String getGrammar(String phrase) {
		StringBuilder res = new StringBuilder();

		// d'abord les nombres
		List<String> phraseSansNombres = parsernb.getRule(phrase);
		
		for (int i=0;i<phraseSansNombres.size();i++) {
			String s = phraseSansNombres.get(i);
			if (s==null) {
				// le suivant est un "nombre" normalisé; il ne reste plus qu'à le phonétiser avec le dico
				s=phraseSansNombres.get(++i);
				res.append(getRule4Nombre(s));
			} else {
				// ce segment n'est pas un "nombre"
				String ruleseg="";
				String[] ss = s.split("\\s");
				for (String mot : ss) {
					if (mot.length() == 0) continue;
					String rule = dictionnaire.getRule(mot);
					// cas particulier très fréquent et mal codé dans Morphalou (??!!)
					if (mot.toLowerCase().equals("c'")) rule = "s";
					
					if (rule.length() == 0) {
						System.out.println("MOT HORS DICO "+mot);
						
						{
							// cas général
							
							// on essaye s'il existe en minuscules:
							String motminuscules = mot.toLowerCase();
							if (!motminuscules.equals(mot)) {
								rule = dictionnaire.getRule(motminuscules);
								if (rule.length() == 0) {
									// il n'est pas non plus en minuscule dans le dico

									// on regarde les cas particuliers
									if (mot.equals("BB") || mot.equals("bb")) {
										// test si c'est un bruit
										// en principe, tout est passe en minuscules ?
										rule = "bb ";
									} else if (mot.equals("HH") || mot.equals("hh")) {
										rule = "hh ";
									} else if (mot.equals("XX") || mot.equals("xx")) {
										rule = "xx ";
									} else {
										// on le phonétise
										rule = getRule4UnknownWords(mot);
									}
								}
							} else {
								// on le phonétise
								rule = getRule4UnknownWords(mot);
							}
						}
					}

					// traitement des acronymes
					String r2 = getRule4Acronymes(mot);
					if (r2!=null) {
						r2=r2.trim();
						if (r2.length()>0)
							rule = "( "+rule+" | "+r2+" ) ";
					}
					rule = rule.replaceAll(" *$", "");
					if (rule.length()>0)
						ruleseg += rule+" ";
				}
				res.append(ruleseg);
			} // n'est pas un nombre
		}

		return res.toString();
	}

	private String getRule4Acronymes(String mot) {
		for (int i=0;i<mot.length();i++)
			if (Character.isLowerCase(mot.charAt(i))) return null;
		System.out.println("acronyme possible "+mot);
		// c'est peut-etre un acronyme !
		String rule = "";
		int debnb=-1;
		for (int i=0;i<mot.length();i++) {
			if (Character.isLetter(mot.charAt(i))) {
				if (debnb>=0) {
					rule += getRule4Nombre(mot.substring(debnb,i));
					debnb=-1;
				}
				rule += dictionnaire.getRule(""+Character.toLowerCase(mot.charAt(i)))+" ";
			} else if (Character.isDigit(mot.charAt(i))) {
				if (debnb<0) debnb=0;
			}
		}
		if (debnb>=0) {
			rule += getRule4Nombre(mot.substring(debnb,mot.length()));
		}
		return rule;
	}

	/**
	 * @deprecated
	 * @param phrase
	 * @return
	 */
	public String oldgetGrammar(String phrase) {

		// on suppose que le texte a deja ete pre-processe / normalise par ailleurs !
		// il ne doit rester plus que les mots + infos locuteur

		String ruleres = "sil ";
		String[] ss = phrase.split("\\s");
		// TODO: chiffres separes par des espaces !! Pour le moment, on les prononce separemment
		for (int i = 0; i < ss.length; i++) {
			String mot = ss[i];
			if (mot.length() == 0) {
				continue;
			}
			/*
			 * dictionnaire = BDLex ou Morphalou, plus un dico perso !
			 */
			String rule = dictionnaire.getRule(mot);
			// cas particulier très fréquent et mal codé dans Morphalou (??!!)
			if (mot.toLowerCase().equals("c'")) rule = "s";
			if (rule.length() == 0) {
				// test si c'est un nombre ou pas !
				if (Character.isDigit(mot.charAt(0))) {
					rule = getRule4Numbers(mot);
					System.out.println("dbug grammatiseur detection nombre "+mot+" "+rule);
				} else if (mot.length() > 2 && mot.charAt(0) == '-' && Character.isDigit(mot.charAt(1))) {
					rule = "m w in " + getRule4Real(mot);
					//					TODO: faire les "ième" et les chiffres romains ==> dans dico perso !
				} else if (mot.startsWith("ï¿½%")) {
					// test si c'est une info-locuteur: on la passe telle quelle
					ruleres += mot + " ";
					continue;
				} else if (mot.equals("BB") || mot.equals("bb")) {
					// test si c'est un bruit
					// en principe, tout est passe en minuscules ?
					rule = "bb ";
				} else if (mot.equals("HH") || mot.equals("hh")) {
					rule = "hh ";
				} else if (mot.equals("XX") || mot.equals("xx")) {
					rule = "xx ";
				} else if (mot.equals("%")) {
					rule = "p u R s an ";
				} else {
					// on essaye s'il existe en minuscules:
					String motminuscules = mot.toLowerCase();
					if (!motminuscules.equals(mot)) {
						rule = dictionnaire.getRule(motminuscules);
					}
					// on regarde s'il termine par une apostrophe. Si oui, peut-etre que le mot recree existe
					if (rule == null || rule.length() == 0) {
						if (mot.endsWith("'") && i < ss.length - 1) {
							// ceci permet de traiter les cas comme "aujourd' hui"
							// on mets alors systematiquement en minuscule
							mot = mot.toLowerCase() + ss[i + 1].toLowerCase();
							rule = dictionnaire.getRule(mot);
							if (rule.length() > 0) {
								// on saute le mot suivant
								i++;
							}
						}
					} else {
						System.out.println("rule comes from dico");
					}
				}
				if (rule == null || rule.length() == 0) {
					rule = getRule4UnknownWords(mot);
					System.out.println("rule comes from some phonetiseur");
				}
			} else {
				System.out.println("rule comes from dico");
			}
			ruleres += rule + " #" + mot + "# [ sil ] ";
			//			s+=rule + " [ sil ] ";
		}
		ruleres = ruleres.trim();
		// supprime les double espaces
		ruleres = ruleres.replaceAll("  +", " ");
		return ruleres;
	}

	String getRule4Real(String mot) {
		// on prononce toujours les chiffres reels en entier !
		int i = mot.indexOf(',');
		if (i < 0) {
			i = mot.indexOf('.');
		}
		if (i >= 0) {
			// nombre a virgule !
			String entier = getRule4IntEnEntier(mot.substring(0, i));
			if (entier == null) {
				return null;
			}
			String decimal = getRule4IntEnEntier(mot.substring(i + 1));
			if (decimal == null) {
				return null;
			}
			String s2 = dictionnaire.getRule("virgule");
			if (s2 == null) {
				return null;
			}
			return entier + " " + s2 + " " + decimal;
		} else {
			return getRule4IntEnEntier(mot);
		}
	}

	/**
	 * on transforme le nombre en mots, puis on appelle bdlex
	 */
	String getRule4Numbers(String mot) {
		int i = mot.indexOf(',');
		if (i < 0) {
			i = mot.indexOf('.');
		}
		if (i >= 0) {
			return getRule4Real(mot);
		}
		// cela peut etre un nombre, ou un Num de tel
		return getRule4Integers(mot);
	}

	String getRule4Integers(String mot) {
		// prononciations les plus courantes:
		// en entier, chiffre par chiffre, par groupe de 2 chiffres
		// autres prononciations:
		// par groupes de 3, 4, 5, 6 chiffres
		if (mot.length() == 4) {
			// en entier ou par groupe de 2 (ex: 3615)
			String s2 = getRule4IntEnEntier(mot);
			if (s2 == null) {
				return null;
			}
			String s = " ( " + s2 + " | ";
			s2 = getRule4IntBy2(mot);
			if (s2 == null) {
				return null;
			}
			s += s2 + " | ";
			// ou sous la forme d'une date
			s2 = getRule4IntEnEntier(mot.substring(0, 2));
			if (s2 == null) {
				return null;
			}
			s += s2;
			s2 = dictionnaire.getRule("cent");
			if (s2 == null) {
				return null;
			}
			s += s2;
			s2 = getRule4IntEnEntier(mot.substring(2, 4));
			if (s2 == null) {
				return null;
			}
			s += s2;
			s += " ) ";
			return s;
		} else if (mot.length() <= 3) {
			// on utilise generalement la prononciation entiere
			return getRule4IntEnEntier(mot);
		} else if (mot.length() == 5) {
			// on utilise generalement la prononciation entiere
			return getRule4IntEnEntier(mot);
		} else if (mot.length() == 6) {
			// On pourrait le dire par groupe de 3 (ex: 118 218), mais dans ce cas, on l'ecrit separe !
			// on support par 2 ou en entier
			String s2 = getRule4IntEnEntier(mot);
			if (s2 == null) {
				return null;
			}
			String s = " ( " + s2 + " | ";
			s2 = getRule4IntBy2(mot);
			if (s2 == null) {
				return null;
			}
			s += s2 + " ) ";
			return s;
		} else if (mot.length() == 8 || mot.charAt(0) == '0') {
			// numero de telephone ?
			// en tout cas, lettre par lettre et par couple de chiffres
			String s2 = getRule4IntBy1(mot);
			if (s2 == null) {
				return null;
			}
			String s = " ( " + s2 + " | ";
			s2 = getRule4IntEnEntier(mot);
			if (s2 == null) {
				return null;
			}
			s += s2 + " | ";
			s2 = getRule4IntBy2(mot);
			if (s2 == null) {
				return null;
			}
			s += s2 + " ) ";
			return s;
		} else {
			// pour le moment, j'autorise en entier ou chiffre par chiffre
			String s2 = getRule4IntBy1(mot);
			if (s2 == null) {
				return null;
			}
			String s = " ( " + s2 + " | ";
			s2 = getRule4IntEnEntier(mot);
			if (s2 == null) {
				return null;
			}
			s += s2 + " ) ";
			return s;
		}
	}

	String getRule4IntBy1(String mot) {
		String s = "";
		for (int i = 0; i < mot.length(); i++) {
			String s2 = dictionnaire.getRule(numbersMap.get(mot.substring(i, i + 1)));
			if (s2 == null) {
				return null;
			}
			s += s2 + " ";
		}
		return s;
	}

	String getRule4IntBy2(String mot) {
		String s = "";
		for (int i = 0; i < mot.length(); i += 2) {
			String tmp = mot.substring(i, i + 2);
			String s2 = dictionnaire.getRule(numbersMap.get(tmp));
			if (s2 == null) {
				return null;
			}
			s += s2 + " ";
		}
		return s;
	}

	String getRule4IntEnEntier(String mot) {
		String s = "";
		if (mot.length() >= 10) {
			if (mot.charAt(0) != '0') {
				String s2 = getRule4IntEnEntier(mot.substring(0, mot.length() - 9));
				if (s2 == null) {
					return null;
				}
				s += s2;
				s2 = dictionnaire.getRule("millard");
				if (s2 == null) {
					return null;
				}
				s += s2;
			}
			String s2 = getRule4IntEnEntier(mot.substring(mot.length() - 9));
			if (s2 == null) {
				return null;
			}
			s += s2;
		} else if (mot.length() >= 7) {
			if (mot.charAt(0) != '0') {
				String s2 = getRule4IntEnEntier(mot.substring(0, mot.length() - 6));
				if (s2 == null) {
					return null;
				}
				s += s2;
				s2 = dictionnaire.getRule("million");
				if (s2 == null) {
					return null;
				}
				s += s2;
			}
			String s2 = getRule4IntEnEntier(mot.substring(mot.length() - 6));
			if (s2 == null) {
				return null;
			}
			s += s2;
		} else if (mot.length() >= 4) {
			if (mot.length() == 4 && mot.charAt(0) == '1') {
				String s2 = dictionnaire.getRule("mille");
				if (s2 == null) {
					return null;
				}
				s += s2;
			} else if (mot.charAt(0) != '0') {
				String s2 = getRule4IntEnEntier(mot.substring(0, mot.length() - 3));
				if (s2 == null) {
					return null;
				}
				s += s2;
				s2 = dictionnaire.getRule("mille");
				if (s2 == null) {
					return null;
				}
				s += s2;
			}
			String s2 = getRule4IntEnEntier(mot.substring(mot.length() - 3));
			if (s2 == null) {
				return null;
			}
			s += s2;
		} else if (mot.length() == 3) {
			if (mot.charAt(0) == '1') {
				String s2 = dictionnaire.getRule("cent");
				if (s2 == null) {
					return null;
				}
				s += s2;
			} else if (mot.charAt(0) != '0') {
				String s2 = dictionnaire.getRule(numbersMap.get(mot.substring(0, 1)));
				if (s2 == null) {
					return null;
				}
				s += s2;
				s2 = dictionnaire.getRule("cent");
				if (s2 == null) {
					return null;
				}
				s += s2;
			}
			String ss = mot.substring(1, 3);
			if (!ss.equals("00")) {
				if (ss.charAt(0) == '0') {
					ss = "" + ss.charAt(1);
				}
				String s2 = dictionnaire.getRule(numbersMap.get(ss));
				if (s2 == null) {
					return null;
				}
				s += s2;
			}
		} else {
			String ss = mot;
			if (!ss.equals("00")) {
				//				if (ss.charAt(0)=='0') ss = ""+ss.charAt(1);
				String[] st = numbersMap.get(ss).split("_");
				for (String x : st) {
					String s2 = dictionnaire.getRule(x);
					if (s2 == null) {
						return null;
					}
					s += s2;
				}
			}
		}
		return s;
	}

	public void saveUNK(String nom) {
		try {
			PrintWriter pf = new PrintWriter(new FileWriter(nom));
			Iterator<Entry<String, Integer>> it = unk.entrySet().iterator();
			while (it.hasNext()) {
				Entry e = (Entry<String, Integer>) it.next();
				pf.println(e.getKey() + " " + e.getValue());
			}
			pf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	String getRule4UnknownWords(String mot) {
		// on l'enregistre dans la liste des mots inconnus
		Integer ii = unk.get(mot);
		if (ii != null) {
			unk.put(mot, ii + 1);
		} else {
			unk.put(mot, 1);
		}

		// nouvelle approche: je phonetise les mots inconnus !
		if (true) {
			try {
				System.out.print("===== Phonetiseur ===> " + mot);
				if (phonetiseur==null) {
					String r = simplePhonetiseur.getRule(mot);
					System.out.println(" -> simplephonetiser -> " + r);
					return r;
				} else {
					String r = phonetiseur.phonetiser(mot, null);
					r=r.replaceAll("swa", "[ swa ]");
					System.out.println(" -> " + r);
					return r;
				}
			} catch (Exception ex) {
				System.out.println("erreur advanced phonetiseur ");
				ex.printStackTrace();
				String r = simplePhonetiseur.getRule(mot);
				System.out.println(" -> ERREUR !!! -> " + r);
				return r;
			}
		}

		int nphonesmax;
		if (mot.length() <= 5) {
			// c'est peut-etre un acronyme, on peut dire lettre par lettre !
			// Note: les acronymes connus sont dans un dico perso !
			// on autorise donc 2 phonemes par lettre
			nphonesmax = mot.length() + mot.length() / 2;
		} else {
			// le mot est trop long, on ne l'epellera pas: on autorise 1 phoneme/lettre
			nphonesmax = mot.length();
		}
		// inutile de mettre trop de phonemes: ca reconnaitra n'imp !
		if (nphonesmax > 10) {
			nphonesmax = 10;
		}
		/* SOLUTION 1: boucle de tous les phonemes... Mais c'est tres long avec les 3ph !!
		 * */
		String[] phones = dictionnaire.phones;
		String s = "";
		for (int l = 1; l <= nphonesmax; l++) {
			s += " ( ";
			for (int i = 0; i < phones.length; i++) {
				if (!phones[i].equals("sil")) {
					s += "QCQ" + phones[i] + " | ";
				}
			}
			int i = s.lastIndexOf('|');
			s = s.substring(0, i) + ") [ ";
		}
		int i = s.lastIndexOf('[');
		s = s.substring(0, i);
		for (int l = 1; l <= nphonesmax - 1; l++) {
			s += " ] ";
		}

		// SOLUTION 2: code specifique pour indiquer un phoneme quelconque
		/*
        String s = "";
        for (int l=1;l<=nphonesmax;l++) {
        s+=" QCQ [ ";
        }
        int i = s.lastIndexOf('[');
        s=s.substring(0,i);
        for (int l=1;l<=nphonesmax-1;l++) s+=" ] ";
		 */
		return s;
	}

	void init(HashMap<String, String> map, String[] a, String[] b) {
		for (int i = 0; i < a.length; i++) {
			map.put(a[i], b[i]);
		}
	}

	void init() {
		// Attention ! Ne mettre dans ces tables que des mots qui appartiennent a BDLex !
		{
			String[] a = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
			String[] b = {"zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf"};
			init(numbersMap, a, b);
		}
		{
			String[] a = {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09"};
			String[] b = {"zéro zéro", "zéro un", "zéro deux", "zéro trois", "zéro quatre", "zéro cinq", "zéro six", "zéro sept", "zéro huit", "zéro neuf"};
			init(numbersMap, a, b);
		}
		{
			String[] a = {"10", "11", "12", "13", "14", "15", "16", "17", "18", "19"};
			String[] b = {"dix", "onze", "douze", "treize", "quatorze", "quinze", "seize", "dix-sept", "dix-huit", "dix-neuf"};
			init(numbersMap, a, b);
		}
		{
			String[] a = {"20", "21", "22", "23", "24", "25", "26", "27", "28", "29"};
			String[] b = {"vingt", "vingt_et_un", "vingt-deux", "vingt-trois", "vingt-quatre", "vingt-cinq", "vingt-six", "vingt-sept", "vingt-huit", "vingt-neuf"};
			init(numbersMap, a, b);
		}
		{
			String[] a = {"30", "31", "32", "33", "34", "35", "36", "37", "38", "39"};
			String[] b = {"trente", "trente_et_un", "trente-deux", "trente-trois", "trente-quatre", "trente-cinq", "trente-six", "trente-sept", "trente-huit", "trente-neuf"};
			init(numbersMap, a, b);
		}
		{
			String[] a = {"40", "41", "42", "43", "44", "45", "46", "47", "48", "49"};
			String[] b = {"quarante", "quarante_et_un", "quarante-deux", "quarante-trois", "quarante-quatre", "quarante-cinq", "quarante-six", "quarante-sept", "quarante-huit", "quarante-neuf"};
			init(numbersMap, a, b);
		}
		{
			String[] a = {"50", "51", "52", "53", "54", "55", "56", "57", "58", "59"};
			String[] b = {"cinquante", "cinquante_et_un", "cinquante-deux", "cinquante-trois", "cinquante-quatre", "cinquante-cinq", "cinquante-six", "cinquante-sept", "cinquante-huit", "cinquante-neuf"};
			init(numbersMap, a, b);
		}
		{
			String[] a = {"60", "61", "62", "63", "64", "65", "66", "67", "68", "69"};
			String[] b = {"soixante", "soixante_et_un", "soixante-deux", "soixante-trois", "soixante-quatre", "soixante-cinq", "soixante-six", "soixante-sept", "soixante-huit", "soixante-neuf"};
			init(numbersMap, a, b);
		}
		{
			String[] a = {"70", "71", "72", "73", "74", "75", "76", "77", "78", "79"};
			String[] b = {"soixante-dix", "soixante_et_onze", "soixante-douze", "soixante-treize", "soixante-quatorze", "soixante-quinze", "soixante-seize", "soixante-dix-sept", "soixante-dix-huit", "soixante-dix-neuf"};
			init(numbersMap, a, b);
		}
		{
			String[] a = {"80", "81", "82", "83", "84", "85", "86", "87", "88", "89"};
			String[] b = {"quatre-vingt", "quatre-vingt-un", "quatre-vingt-deux", "quatre-vingt-trois", "quatre-vingt-quatre", "quatre-vingt-cinq", "quatre-vingt-six", "quatre-vingt-sept", "quatre-vingt-huit", "quatre-vingt-neuf"};
			init(numbersMap, a, b);
		}
		{
			String[] a = {"90", "91", "92", "93", "94", "95", "96", "97", "98", "99"};
			String[] b = {"quatre-vingt-dix", "quatre-vingt-onze", "quatre-vingt-douze", "quatre-vingt-treize", "quatre-vingt-quatorze", "quatre-vingt-quinze", "quatre-vingt-seize", "quatre-vingt-dix-sept", "quatre-vingt-dix-huit", "quatre-vingt-dix-neuf"};
			init(numbersMap, a, b);
		}
	}

	static boolean unittest() {
		Grammatiseur m = new Grammatiseur();
		final String[] totest = {"12","il est 13h30","la FNAC chute de -4.568% en 1934","l' ANPE -4.5% à 10°C"};

		for (String s : totest) {
			System.out.print("test "+s+" : ");
			String res = m.getGrammar(s);
			System.out.println(res);
		}
		return true;
	}

	public static void main(String args[]) throws Exception {

		if (unittest()) return;

		Grammatiseur m;

		String list = null, unk = null, txt = null, outlist = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-list")) {
				list = args[++i];
			} else if (args[i].equals("-unk")) {
				unk = args[++i];
			} else if (args[i].equals("-txt")) {
				txt = args[++i];
			} else if (args[i].equals("-outlist")) {
				outlist = args[++i];
			}
		}

		m = new Grammatiseur();

		String gram = null;
		if (list == null) {
			try {
				BufferedReader onetxt = new BufferedReader(new FileReader(txt));
				System.out.println("save gram in " + txt + ".gram");
				PrintWriter pf = new PrintWriter(new FileWriter(txt + ".gram"));
				for (;;) {
					String ss = onetxt.readLine();
					if (ss == null) {
						break;
					}

					ss = ss.toLowerCase();

					gram = m.getGrammar(ss);
					pf.println(gram);
				}
				pf.close();
				onetxt.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if (outlist == null) {
				try {
					BufferedReader bf = new BufferedReader(new FileReader(list));
					for (;;) {
						String s = bf.readLine();
						if (s == null) {
							break;
						}
						BufferedReader oneraw = new BufferedReader(new FileReader(s));
						System.out.println("save gram in " + s + ".gram");
						PrintWriter pf = new PrintWriter(new FileWriter(s + ".gram"));
						for (;;) {
							String ss = oneraw.readLine();
							if (ss == null) {
								break;
							}
							gram = m.getGrammar(ss);
							pf.println(gram);
						}
						pf.close();
						oneraw.close();
					}
					bf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				try {
					BufferedReader bf = new BufferedReader(new FileReader(list));
					BufferedReader flistout = new BufferedReader(new FileReader(outlist));
					for (;;) {
						String s = bf.readLine();
						if (s == null) {
							break;
						}
						String outnom = flistout.readLine();
						BufferedReader oneraw = new BufferedReader(new FileReader(s));
						System.out.println("save gram in " + outnom);
						PrintWriter pf = new PrintWriter(new FileWriter(outnom));
						for (;;) {
							String ss = oneraw.readLine();
							if (ss == null) {
								break;
							}
							gram = m.getGrammar(ss);
							pf.println(gram);
						}
						pf.close();
						oneraw.close();
					}
					bf.close();
					flistout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		if (unk != null) {
			// save UNK
			System.out.println("Saves unknown words in " + unk);
			m.saveUNK(unk);
		}
	}
}
