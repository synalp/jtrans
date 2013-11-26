package utils;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Entree est une representation d'une entree d'un dictionnaire:
 * nous avons choisit par defaut le format de BDLex.
 * Pour lire les entrees d'autres dictionnaires, il faut donc convertir dans le format de BDLex !
 * 
 * L'acces aux informations contenues dans Entree par des programmes externes doit se faire
 * a travers l'interface DicoEntry
 * 
 * @author xtof
 *
 */
public class Entree implements Serializable, Comparable<Entree>, DicoEntry {

    private static final long serialVersionUID = -5569009786502786053L;
    String phonesBase = "";
    String phonesOption = "";
    Entree autrePossible = null;
    private ArrayList<String> extension;

    class Syntax implements Serializable, Comparable<Syntax> {

        private static final long serialVersionUID = -1825572948836274656L;
        DicoEntry.POStag postag = DicoEntry.POStag.UNK;
        DicoEntry.Genre genre = DicoEntry.Genre.UNK;
        DicoEntry.Nombre nombre = DicoEntry.Nombre.UNK, nombre2 = DicoEntry.Nombre.UNK;
        DicoEntry.Personne pers = DicoEntry.Personne.UNK;
        DicoEntry.Temps temps = DicoEntry.Temps.UNK;
        DicoEntry.Mode mode = DicoEntry.Mode.UNK;
        String lemme = null, formeFlechie=null;

        public int compareTo(Syntax s) {
            if (postag == s.postag && genre == s.genre && nombre == s.nombre && nombre2 == s.nombre2 && pers == s.pers && temps == s.temps &&
                    mode == s.mode) {
                if (lemme == null) {
                    if (s.lemme == null) {
                        return 0;
                    } else {
                        return 1;
                    }
                } else if (lemme.equals(s.lemme)) {
                    return 0;
                }
            }
            return 1;
        }
        public String toString() {
        	return lemme+" "+postag+" "+genre+" "+nombre+" "+pers+" "+temps+" "+mode;
        }
    }
    // les autres syntaxes possibles sont contenues dans "autrepossible"
    Syntax syntax = new Syntax();

    public Genre getGenre() {return syntax.genre;}
	public Nombre getNombre() {return syntax.nombre;}
	public Personne getPersonne() {return syntax.pers;}
	public Temps getTemps() {return syntax.temps;}
	public Mode getMode() {return syntax.mode;}
    
    /**
     * return
     * 	0  si this est inclus dans e (this ne doit pas avoir de liste suivante)
     *  1  si ils sont diff�rents
     */
    public int compareTo(Entree e) {
        if (e == null) {
            return 1;
        }
        if (autrePossible != null) {
            return 1;
        }
        if (phonesBase.equals(e.phonesBase) && phonesOption.equals(e.phonesOption) && syntax.compareTo(e.syntax) == 0) {
            return 0;
        } else {
            return compareTo(e.autrePossible);
        }
    }

    /**
     * ecrase le 1er champ syntaxique !
     * @param tag
     */
    public void setPOStag(DicoEntry.POStag tag) {
        syntax.postag = tag;
    }

    public POStag getPOStag() {
        return syntax.postag;
    }

    public void setGenre(DicoEntry.Genre g) {
        syntax.genre = g;
    }

    public void setNombre(DicoEntry.Nombre n) {
        syntax.nombre = n;
    }

    /**
     * pour certains d�terminants, voir par exemple "mes" dans BDLex
     * @param n
     */
    public void setNombre2(DicoEntry.Nombre n) {
        syntax.nombre2 = n;
    }

    public void setPersonne(DicoEntry.Personne p) {
        syntax.pers = p;
    }

    public void setTemps(DicoEntry.Temps t) {
        syntax.temps = t;
    }

    public void setMode(DicoEntry.Mode m) {
        syntax.mode = m;
    }

    public String getLemme() {
    	return syntax.lemme;
    }
    public String getForme() {
    	return syntax.formeFlechie;
    }
    public void setLemme(String l) {
        syntax.lemme = "" + l;
    }
    public void setForme(String forme) {syntax.formeFlechie=forme;}

    /**
     * return false ssi this est inclus dans e
     * @param e est toujours le plus complet (l'ancien) !
     */
    public boolean combinerAvec(Entree e) {
        if (compareTo(e) != 0) {
            autrePossible = e;
            return true;
        } else {
            return false;
        }
    }

    /**
     * retourne la regle contenant toutes les prononciations _d'une seule_ Entree, sans les liaisons optionnelles
     */
    public String getRuleOneEntryNoLiaisons() {
        String s = phonesBase;
        s = PronunciationsLexicon.convertPhones(s);
        return s;
    }

    public String getRuleOneEntryNoLiaisonsWithSwa() {
        String s = phonesBase;
        s = PronunciationsLexicon.convertPhones(s);
        return s.replaceAll(" \\[ swa \\]", " swa");
    }

    /**
     * retourne la regle contenant toutes les prononciations _d'une seule_ Entree, avec les liaisons optionnelles
     * les autres entrees liees avec "autresPossible" ne sont pas prises en compte ici:
     * pour les inclure aussi dans la regle, il faut alors utiliser la methode getRuleAll()
     *
     * @return
     */
    public String getRuleOneEntry() {
        String s = getRuleOneEntryNoLiaisons();

        // fins optionnelles
        String f = phonesOption;
        if (f.length() > 0 && f.charAt(0) != '-' && f.charAt(0) != '+') {
            if (f.charAt(0) == '(') {
                if (f.charAt(1) == '~') {
                    s = s.trim();
                    int i = s.lastIndexOf(' ');
                    String opt1 = s.substring(i + 1); // liaison sans nasalite
                    s = s.substring(0, i); // on supprime le dernier phoneme
                    String opt2 = null; // pas de liaison: nasal
                    if (opt1.equals("eh") || opt1.equals("i")) {
                        opt2 = "in";
                    } else if (opt1.equals("oh")) {
                        opt2 = "on";
                    }
                    s += " ( " + opt1 + " n | " + opt2 + " )";
                } else {
                    // 2 consonnes optionnelles
                    s += " [ " + f.charAt(1) + " " + f.charAt(2) + " ]";
                }
            } else if (f.charAt(0) == '@') {
                if (f.length() > 1) {
                    // consonne de laison en plus
                    s += " [ swa [ " + f.charAt(1) + " ] ]";
                } else {
                    s += " [ swa ]";
                }
            } else { // 1 consonne latente
                s += " [ " + f.charAt(0) + " ]";
            }
        }
        return s;
    }

    /**
     * retourne la regle contenant toutes les prononciations de toutes les entrees du dico correspondant
     * a cette forme fl�chie, avec les liaisons optionnelles
     */
    public String getRuleAll() {
        if (autrePossible == null) {
            return getRuleOneEntry();
        } else {
            String s = " ( ";
            Entree e = this;
            for (;;) {
                s += e.getRuleOneEntry();
                e = e.autrePossible;
                if (e == null) {
                    break;
                } else {
                    s += " | ";
                }
            }
            s += " ) ";
            return s;
        }
    }

    public String toString() {
    	String s = phonesBase+" "+syntax;
    	return s;
    }
	@Override
	public ArrayList<String> getExtension() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public ArrayList<String> getExtensionNoLiaisons() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public ArrayList<String> getExtensionNoLiaisonsWithSwa() {
		// TODO Auto-generated method stub
		return null;
	}
}
