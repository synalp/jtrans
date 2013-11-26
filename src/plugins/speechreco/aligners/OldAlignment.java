/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.aligners;

import java.util.ArrayList;

/**
 * @deprecated utiliser plutot Alignment
 * @author xtof
 *
 */
public class OldAlignment {
	public String[] labels;
	public int[] tramesDeb;
	public int[] tramesFin;
	public float loglike;
	
	// on ne peut pas stocker le loglike des mots, car cette info disparait
	// avec les tokens intermediaires !
	
	// les 3 champs suivants ne sont calcules que a la demande
	// ces 3 champs sont utilises par PlayerListener
	public String[] wordLabels=null;
	private int[] wordsDeb, wordsFin;
	
	// dernier mot align�
	private int lastAlignedWord=-1;

	public String toString() {
		String s=loglike+" ";
		for (int i=0;i<labels.length;i++)
			s+=labels[i]+"("+tramesDeb[i]+"-"+tramesFin[i]+") ";
		return s;
	}
	
	public void print() {
		for (int i=0;i<=lastAlignedWord;i++) {
			System.out.println("align: "+i+" "+wordLabels[i]+" "+wordsDeb[i]+" "+wordsFin[i]);
		}
	}

	public int getFrameDeb(int motidx) {
		if (motidx>=wordsDeb.length) {
//			System.err.println("WARNING getFrameDeb: trop de mots ? "+motidx);
			return -1;
		}
		return wordsDeb[motidx];
	}
	public int getFrameFin(int motidx) {
		if (motidx>=wordsFin.length) {
//			System.err.println("WARNING getFrameFin: trop de mots ? "+motidx);
			return -1;
		}
		return wordsFin[motidx];
	}

	public void setAlignForWord(int motidx, int frdeb, int frfin) {
		wordsDeb[motidx] = frdeb;
		wordsFin[motidx] = frfin;
		if (motidx>lastAlignedWord&&frdeb>=0) lastAlignedWord=motidx;
	}
	
	String[] wordStates; // contient le nb de trames dans chaque etat

	public OldAlignment(int nbTrames) {
		labels = new String[nbTrames];
		tramesDeb = new int[nbTrames];
		tramesFin = new int[nbTrames];
	}
	
	public String[] getWordsLabels() {
		computeWordsLimits();
		return wordLabels;
	}

	void computeWordsLimits() {
		if (wordLabels!=null) return;
		int deb=tramesDeb[0];
		ArrayList<String> newlabels = new ArrayList<String>();
		ArrayList<Integer> newdebs = new ArrayList<Integer>();
		ArrayList<Integer> newfins = new ArrayList<Integer>();
		// contient le nb de trames dans chaque etat
		ArrayList<String> stateFrames = new ArrayList<String>();
		String states = "";
		for (int i=0;i<labels.length;i++) {
			int j=labels[i].indexOf('[');
			if (j<0) System.err.println("ERRERRRRRRRRRRRRRRRR "+i+" "+labels[i]);
			String curPhone = labels[i].substring(0,labels[i].indexOf('['));
			// chaque entree correspond a un nouvel etat
			int nFramesInOneState=tramesFin[i]+1-tramesDeb[i];
			states+=curPhone+" "+nFramesInOneState+" ";
			// on recherche un silence:
			if (labels[i].equals("sil[2]")) {
				deb=tramesDeb[i];
			} else if (labels[i].equals("sil[4]")) {
				newlabels.add("sil");
				newdebs.add(deb);
				newfins.add(tramesFin[i]);
				deb=tramesFin[i];
				stateFrames.add(states);
				states="";
			} else {
				// on recherche un mot: ils apparaissent avec le dernier etat du dernier phoneme
				int pos = labels[i].indexOf('(');
				if (pos>=0) {
					stateFrames.add(states);
					states="";
					newlabels.add(labels[i].substring(pos+1,labels[i].indexOf(')')));
					newdebs.add(deb);
					newfins.add(tramesFin[i]);
					deb=tramesFin[i];
				}
			}
		}
		wordLabels = new String[newlabels.size()];
		wordsDeb = new int[labels.length];
		wordsFin = new int[labels.length];
		wordStates = new String[stateFrames.size()];
		newlabels.toArray(wordLabels);
		stateFrames.toArray(wordStates);
		for (int i=0;i<wordLabels.length;i++) {
			wordsDeb[i] = newdebs.get(i);
			wordsFin[i] = newfins.get(i);
		}
	}
}
