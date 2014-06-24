package fr.loria.synalp.jtrans.speechreco;

import java.util.Arrays;

public class RecoWord {
	// simple String contenant le mot tel qu'il est affiché
	public String word=null;
	// indice des trames, frameEnd est exclue !
	public int frameDeb=-1, frameEnd=-1;
	// contient la mesure de confiance = proba posterior du mot
	public float score=-Float.MAX_VALUE;
	
	// tableau contenant simplement le nom des phonemes
	public String[] phones=null;
	// une String par trame, qui contient nom_du_phoneme:numero_etat
	public String[] phstates;
	
	public String getPhone(int t) {
		String s = phstates[t];
		int bracket = s.indexOf('[');
		return bracket >= 0? s.substring(0, bracket): s;
	}
	public int getState(int t) {
		int i = phstates[t].indexOf(':');
		if (i<0) return -1;
		return Integer.parseInt(phstates[t].substring(i+1));
	}
	
	public String toString() {
		return word+"("+frameDeb+"-"+frameEnd+") "+Arrays.toString(phones);
	}

	@Override
	public int hashCode() {
		return word.hashCode();
	}
	@Override
	public boolean equals(Object o) {
		boolean eq = word.equals(((RecoWord)o).word);
		return eq;
	}
}
