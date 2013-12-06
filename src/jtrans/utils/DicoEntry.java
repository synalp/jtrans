package jtrans.utils;

import java.util.ArrayList;

/**
 * Represente une entree d'un dictionnaire
 * 
 * @author xtof
 *
 */
public interface DicoEntry {

	public static enum POStag { UNK,
		adv, conj, det, adjnomfem, /* adjectif ou nom feminin */
		adjnommasc, /* adjectif ou nom masculin */
		adjnom, /* adjectif ou nom masculin ou feminin */
		adj, interj, partpass, nom, prep, pron, verb
	}
	public static enum Genre {UNK, 
		masc, fem, invar, neutre
	}
	public static enum Nombre {UNK,
		sing, plur, invar, neutre
	}
	public static enum Personne {UNK,
		prem, deux, trois
	}
	public static enum Temps {UNK,
		pr, imparf, pass, futur
	}
	public static enum Mode {UNK,
		indicatif, subjonctif, conditionnel, imp, infinitif, participe
	}

	/**
	 * retourne l'ensemble des prononciations possibles (liaisons inclues)
	 */
	public ArrayList<String> getExtension();
	
	/**
	 * retourne l'ensemble des prononciations possibles (sans liaisons)
	 */
	public ArrayList<String> getExtensionNoLiaisons();

    /**
	 * retourne l'unique prononciation possible, c'est a dire celle du dico (sans liaisons)
	 */
	public ArrayList<String> getExtensionNoLiaisonsWithSwa();

	public POStag getPOStag();

	public Genre getGenre();
	public Nombre getNombre();
	public Personne getPersonne();
	public Temps getTemps();
	public Mode getMode();
	public String getLemme();
	public String getForme();

	public void setGenre(Genre x);
	public void setNombre(Nombre x);
	public void setPersonne(Personne x);
	public void setTemps(Temps x);
	public void setMode(Mode x);
}
