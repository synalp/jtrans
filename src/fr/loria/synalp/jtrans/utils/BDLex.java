package fr.loria.synalp.jtrans.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BDLex extends PronunciationsLexicon implements Serializable {
//	String corpdir = "/local/parole1/Mhatlex/BDLEX-V2.1/BDLex/";
//	String corpdir = "C:/xtof/res/BDLex/BDLEX-V2.1/BDLex/";

//    String corpdir = "C:/cygwin/home/cerisara/corpus/BDLEX-V2.1/BDLex/";
    String corpdir = "/home/xtof/parole1/Mhatlex/BDLEX-V2.1/BDLex/";
//	String dicoperso = "/users/parole/cerisara/cvsreps2/ESTER2/dicoLORIA";
//	String dicoperso = "C:/xtof/ESTER2/dicoLORIA";
//  String dicoperso = "C:/cygwin/home/cerisara/cvsreps/ESTER2/dicoLORIA";
	String dicoperso = "/home/xtof/nishome/cvsreps2/ESTER2/dicoLORIA";
	
	String[] possiblePaths = {
			"/home/xtof/corpus/Mhatlex/BDLEX-V2.1/BDLex/",
			"/home/xtof/cvs/ESTER2",
			"/home/xtof/parole1/Mhatlex/BDLEX-V2.1/BDLex/",
			"/home/xtof/nishome/cvsreps2/ESTER2/",
			"D:/xtof/libs/Mhatlex/BDLEX-V2.1/BDLex/",
			"C:/xtof/corpus/Mhatlex/BDLEX-V2.1/BDLex/",
			"D:/xtof/cvs/ESTER2/"};
	
	public File findFile(String name) {
		for (String s : possiblePaths) {
			File f = new File(s+"/"+name);
			if (f.exists()) return f;
		}
		return null;
	}
	
    /**
     * Les phonemes utilises en sortie sont les suivants:
     */
    final public static String[] phones = {"a", "an", "b", "bb", "d", "e", "E", "eh", "eu", "euf", "f", "g", "H", "hh", "i", "in", "j", "J", "k", "l", "m", "n", "o", "O", "oh", "on", "p", "R", "s", "S", "sil", "swa", "t", "u", "v", "w", "xx", "y", "z", "Z"};

    public static BDLex loadBin(String binFile) {
        BDLex x;
        try {
            ObjectInputStream f = new ObjectInputStream(new FileInputStream(binFile));
            x = (BDLex) f.readObject();
            f.close();
        } catch (Exception e) {
            System.err.println("impossible de charger la version binaire... je charge la version texte !");
            x = new BDLex();
            System.err.println("j'ecrase l'ancienne version binaire...");
            x.saveBin(binFile);
        }
        return x;
    }

    /**
     * il ne faut jamais charger 2 fois BDLex, car il prend trop de memoire !
     * @return
     */
    public static BDLex getBDLex() {
    	if (singletonBDLex==null)
    		singletonBDLex=new BDLex();
    	return singletonBDLex;
    }
    
    private static BDLex singletonBDLex = null;
    
    private BDLex() {
        System.err.println("BDLex de JHTK");
        loadCorpus();
    }

    public BDLex(String[] dicos, boolean withConvAccent) {
        System.err.println("BDLex de JHTK");
        dico = new HashMap<String, Entree>();
        for (int i = 0; i < dicos.length; i++) {
            loadFile(dicos[i], withConvAccent);
        }
    }

    public BDLex(String corpdir, String dicoperso) {
        this.corpdir = corpdir;
        this.dicoperso = dicoperso;
        System.err.println("BDLex de JHTK");
        loadCorpus();
    }

    public void saveBin(String nom) {
        try {
            ObjectOutputStream f = new ObjectOutputStream(new FileOutputStream(nom));
            f.writeObject(this);
            f.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * je teste d'abord en chargeant tout le corpus en memoire... on verra si ca tient facilement !
     */
    void loadCorpus() {
        dico = new HashMap<String, Entree>();
        for (char l = 'a'; l <= 'z'; l++) {
            loadFile(l + ".B.flx", true);
        }
        loadFile("apostrophe.B.flx", true);
        loadFile("clitic.B.flx", true);
        System.out.println("BDLex read");
        if (dicoperso.length() > 0) {
            loadFile("dicoLORIA", false);
        }
        System.out.println("dicoperso read");
    }

    void loadFile(String fichnom, boolean withConvAccent) {
        try {
        	File f = findFile(fichnom);
        	if (f==null) {
        		System.out.println("WARNING: pas de fichier trouve: "+fichnom);
        		return;
        	}
        	// TODO: attention a l'encoding: pas de pb pour BDLex, mais des problemes pour dicoPerso !
            BufferedReader bf = new BufferedReader(new FileReader(f));
            String forme = "";
            for (;;) {
                String s = bf.readLine();
                if (s == null) {
                    break;
                }
                if (!s.equals("")) {
                    Entree e = new Entree();

                    // forme fl�chie
                    int i1 = s.indexOf(';');
                    forme = s.substring(0, i1);
                    // pour le moment, je convertis le mot en minuscules !
                    forme = forme.toLowerCase();
                    if (withConvAccent) {
                        forme = convertAccents(forme);
                    }

                    // prononciation de base
                    s = s.substring(i1 + 1);
                    i1 = s.indexOf(';');
                    e.phonesBase = s.substring(0, i1);

                    // prononciations alternatives
                    s = s.substring(i1 + 1);
                    i1 = s.indexOf(';');
                    e.phonesOption = s.substring(0, i1);

                    // POS-tag
                    s = s.substring(i1 + 1);
                    i1 = s.indexOf(';');
                    char cs = s.charAt(0);
                    switch (cs) {
                        case 'A':
                            e.setPOStag(DicoEntry.POStag.adj);
                            break;
                        case 'c':
                            e.setPOStag(DicoEntry.POStag.conj);
                            break;
                        case 'd':
                            e.setPOStag(DicoEntry.POStag.det);
                            break;
                        case 'F':
                            e.setPOStag(DicoEntry.POStag.adjnomfem);
                            break;
                        case 'G':
                            e.setPOStag(DicoEntry.POStag.adjnom);
                            break;
                        case 'J':
                            e.setPOStag(DicoEntry.POStag.adj);
                            break;
                        case 'i':
                            e.setPOStag(DicoEntry.POStag.interj);
                            break;
                        case 'K':
                            e.setPOStag(DicoEntry.POStag.partpass);
                            break;
                        case 'M':
                            e.setPOStag(DicoEntry.POStag.adjnommasc);
                            break;
                        case 'N':
                            e.setPOStag(DicoEntry.POStag.nom);
                            break;
                        case 'p':
                            e.setPOStag(DicoEntry.POStag.prep);
                            break;
                        case 'P':
                            e.setPOStag(DicoEntry.POStag.pron);
                            break;
                        case 'V':
                            e.setPOStag(DicoEntry.POStag.verb);
                            break;
                        case ';':
                            break;
                        default:
                            System.err.println("ERREUR, CODE POSTAG inconnu ! " + s);
                    }

                    // genre, nombre, personne
                    s = s.substring(i1 + 1);
                    i1 = s.indexOf(';');
                    String c5 = s.substring(0, i1);
                    s = s.substring(i1 + 1);
                    i1 = s.indexOf(';');
                    String c6 = s.substring(0, i1);
                    if (cs == 'N' || cs == 'J' || cs == 'F' || cs == 'G' || cs == 'M' || cs == 'K') {
                        switch (c5.charAt(0)) { // genre
                            case 'M':
                                e.setGenre(DicoEntry.Genre.masc);
                                break;
                            case 'F':
                                e.setGenre(DicoEntry.Genre.fem);
                                break;
                            case 'i':
                                e.setGenre(DicoEntry.Genre.invar);
                                break;
                            case '0':
                                e.setGenre(DicoEntry.Genre.masc);
                                break;
                            default:
                                System.err.println("ERREUR, CODE GENRE inconnu ! " + c5);
                        }
                        switch (c5.charAt(1)) { // nombre
                            case 'S':
                                e.setNombre(DicoEntry.Nombre.sing);
                                break;
                            case 'P':
                                e.setNombre(DicoEntry.Nombre.plur);
                                break;
                            case 'j':
                                e.setNombre(DicoEntry.Nombre.invar);
                                break;
                            case '0':
                                e.setNombre(DicoEntry.Nombre.neutre);
                                break;
                            default:
                                System.err.println("ERREUR, CODE NOMBRE  inconnu ! " + c5);
                        }
                    // pas de champ 6
                    } else if (cs == 'V') { //personne+nombre ... temps+mode
                    	if (c5.length() > 0) {
                    		switch (c5.charAt(0)) { // personne
                    		case '1':
                    			e.setPersonne(DicoEntry.Personne.prem);
                    			break;
                    		case '2':
                    			e.setPersonne(DicoEntry.Personne.deux);
                    			break;
                    		case '3':
                    			e.setPersonne(DicoEntry.Personne.trois);
                    			break;
                    		default:
                    			System.err.println("ERREUR, CODE PERS inconnu ! " + c5);
                    		}
                    		switch (c5.charAt(1)) { // nombre
                    		case 'S':
                    			e.setNombre(DicoEntry.Nombre.sing);
                    			break;
                    		case 'P':
                    			e.setNombre(DicoEntry.Nombre.plur);
                    			break;
                    		case 'j':
                    			e.setNombre(DicoEntry.Nombre.invar);
                    			break;
                    		case '0':
                    			e.setNombre(DicoEntry.Nombre.neutre);
                    			break;
                    		default:
                    			System.err.println("ERREUR, CODE NOMBRE  inconnu ! " + c5);
                    		}
                    	}
                    	if (c6.length() > 0) {
                    		if (c6.equals("inf")) {
                    			e.setMode(DicoEntry.Mode.infinitif);
                    		} else {
                        		switch (c6.charAt(0)) { // temps
                        		case 'p':
                        			e.setTemps(DicoEntry.Temps.pr);
                        			break;
                        		case 'i':
                        			e.setTemps(DicoEntry.Temps.imparf);
                        			break;
                        		case 'a':
                        			e.setTemps(DicoEntry.Temps.pass);
                        			break;
                        		case 'f':
                        			e.setTemps(DicoEntry.Temps.futur);
                        			break;
                        		default:
                        			System.err.println("ERREUR, CODE TEMPS inconnu ! " + c6);
                        		}
                        		switch (c6.charAt(1)) { // mode
                        		case 'i':
                        			e.setMode(DicoEntry.Mode.indicatif);
                        			break;
                        		case 's':
                        			e.setMode(DicoEntry.Mode.subjonctif);
                        			break;
                        		case 'c':
                        			e.setMode(DicoEntry.Mode.conditionnel);
                        			break;
                        		case 'I':
                        			e.setMode(DicoEntry.Mode.imp);
                        			break;
                        		case 'P':
                        			e.setMode(DicoEntry.Mode.participe);
                        			break;
                        		default:
                        			System.err.println("ERREUR, CODE MODE inconnu ! " + c6);
                        		}
                    		}
                    	}
                    } else if (cs == 'd') { // genre+nombre (+ perso+nombre)
                        switch (c5.charAt(0)) { // genre
                            case 'M':
                                e.setGenre(DicoEntry.Genre.masc);
                                break;
                            case 'F':
                                e.setGenre(DicoEntry.Genre.fem);
                                break;
                            case 'i':
                                e.setGenre(DicoEntry.Genre.invar);
                                break;
                            case '0':
                                e.setGenre(DicoEntry.Genre.masc);
                                break;
                            default:
                                System.err.println("ERREUR, CODE GENRE inconnu ! " + c5);
                        }
                        switch (c5.charAt(1)) { // nombre
                            case 'S':
                                e.setNombre(DicoEntry.Nombre.sing);
                                break;
                            case 'P':
                                e.setNombre(DicoEntry.Nombre.plur);
                                break;
                            case 'j':
                                e.setNombre(DicoEntry.Nombre.invar);
                                break;
                            case '0':
                                e.setNombre(DicoEntry.Nombre.neutre);
                                break;
                            default:
                                System.err.println("ERREUR, CODE NOMBRE  inconnu ! " + c5);
                        }
                        if (c5.length() > 2) {
                            switch (c5.charAt(2)) { // personne
                                case '1':
                                    e.setPersonne(DicoEntry.Personne.prem);
                                    break;
                                case '2':
                                    e.setPersonne(DicoEntry.Personne.deux);
                                    break;
                                case '3':
                                    e.setPersonne(DicoEntry.Personne.trois);
                                    break;
                                default:
                                    System.err.println("ERREUR, CODE PERS inconnu ! " + c5);
                            }
                            switch (c5.charAt(3)) { // nombre
                                case 'S':
                                    e.setNombre2(DicoEntry.Nombre.sing);
                                    break;
                                case 'P':
                                    e.setNombre2(DicoEntry.Nombre.plur);
                                    break;
                                case 'j':
                                    e.setNombre2(DicoEntry.Nombre.invar);
                                    break;
                                case '0':
                                    e.setNombre2(DicoEntry.Nombre.neutre);
                                    break;
                                default:
                                    System.err.println("ERREUR, CODE NOMBRE  inconnu ! " + c5);
                            }
                        }
                    // TODO: sous-cat�gorie du d�terminant dans le champ c6 !!
                    } else if (cs == 'P') {
                        switch (c5.charAt(0)) { // genre
                            case 'M':
                                e.setGenre(DicoEntry.Genre.masc);
                                break;
                            case 'F':
                                e.setGenre(DicoEntry.Genre.fem);
                                break;
                            case 'i':
                                e.setGenre(DicoEntry.Genre.invar);
                                break;
                            case '0':
                                e.setGenre(DicoEntry.Genre.masc);
                                break;
                            default:
                                System.err.println("ERREUR, CODE GENRE inconnu ! " + c5);
                        }
                        switch (c5.charAt(1)) { // nombre
                            case 'S':
                                e.setNombre(DicoEntry.Nombre.sing);
                                break;
                            case 'P':
                                e.setNombre(DicoEntry.Nombre.plur);
                                break;
                            case 'j':
                                e.setNombre(DicoEntry.Nombre.invar);
                                break;
                            case '0':
                                e.setNombre(DicoEntry.Nombre.neutre);
                                break;
                            default:
                                System.err.println("ERREUR, CODE NOMBRE  inconnu ! " + c5);
                        }
                        if (c5.length() > 2) {
                            switch (c5.charAt(2)) { // personne
                                case '1':
                                    e.setPersonne(DicoEntry.Personne.prem);
                                    break;
                                case '2':
                                    e.setPersonne(DicoEntry.Personne.deux);
                                    break;
                                case '3':
                                    e.setPersonne(DicoEntry.Personne.trois);
                                    break;
                                default:
                                    System.err.println("ERREUR, CODE PERS inconnu ! " + c5);
                            }
                        }
                    // TODO: sous-cat�gorie du pronom dans le champ c6 !!
                    } else if (cs == 'c' || cs == 'A') {
                        // TODO: sous-cat�gorie dans le champ c6 !!
                    } else if (cs == 'p' || cs == 'i') {
                        // TODO: sous-cat�gorie dans le champ c6 !!
                    }

                    // lemme et forme
                    e.setForme(forme);
                    s = s.substring(i1 + 1);
                    i1 = s.indexOf(';');
                    String lemme = s.substring(0, i1);
                    if (lemme.length() > 0) {
                        if (lemme.equals("=")) {
                            e.setLemme(forme);
                        } else {
                            e.setLemme(lemme);
                        }
                    } else
                    	e.setLemme(forme);

                    // on ajoute l'entr�e au dictionnaire
                    if (dico.containsKey(forme)) {
                        if (!e.combinerAvec(dico.get(forme))) {
                            e = dico.get(forme);
                        }
                    }
                    dico.put(forme, e);
                }
            }
            bf.close();
            System.err.println("dernier nom lu " + forme);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * retourne toujours un ArrayList, mais qui peut etre vide si le mot n'est pas dans le vocab,
     * s'il n'a pas de lemmes,
     *
     * @param mot
     * @return
     */
    public ArrayList<String> getLemme(String mot) {
        ArrayList<String> res = new ArrayList<String>();
        Entree e = dico.get(mot);
        while (e != null) {
            res.add(e.syntax.lemme);
            e = e.autrePossible;
        }
        return res;
    }

    public static void main(String args[]) {
        BDLex m = new BDLex();
    	if (args.length>0 && args[0].equals("-all")) {
    		String mot = args[1];
    		List<DicoEntry> en = m.getDicoEntries(mot);
    		for (DicoEntry e : en) {
    			System.out.println("entry: "+e);
    		}
    	}
        try {
            BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
            for (;;) {
                String s = bf.readLine();
                if (s.length() > 0) {
                    if (s.charAt(0) == '=') {
                        switch (s.charAt(1)) {
                            case 'q':
                                return;
                        }
                    } else {
                        System.out.println(m.getRule(s));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
