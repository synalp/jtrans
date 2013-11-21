/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.aligners;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import utils.SuiteDeMots;

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

	private TreeSet<Integer> manualAnchors = new TreeSet<Integer>();

	public TreeSet<Integer> getAncres() {
		return manualAnchors;
	}
	/**
	 * retourne la liste des ancres precedentes qui ont ete supprimees 
	 * 
	 * @param wordidx
	 * @return
	 */
	public ArrayList<Integer> addManualAnchorv2(int wordidx) {
		ArrayList<Integer> deletedAnchors = new ArrayList<Integer>();
		int frdeb = wordsDeb[wordidx];
		int frfin = wordsFin[wordidx];
		// verifie les ancres avant et apres
		for (;;) {
			SortedSet<Integer> avant = manualAnchors.headSet(wordidx);
			if (avant.size()>0) {
				int lastavant = avant.last();
				if (wordsFin[lastavant]>=frdeb) {
					manualAnchors.remove(lastavant);
					deletedAnchors.add(lastavant);
				} else break;
			} else break;
		}
		for (;;) {
			SortedSet<Integer> apres = manualAnchors.tailSet(wordidx);
			if (apres.size()>0) {
				int firstapres = apres.first();
				if (wordsFin[firstapres]<=frfin) {
					manualAnchors.remove(firstapres);
					System.err.println("delete anchors "+firstapres);
					deletedAnchors.add(firstapres);
				} else break;
			} else break;
		}
		manualAnchors.add(wordidx);
		return deletedAnchors;
	}
	
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
	
	public void importAlign(OldAlignment old) {
		for (int i=0;i<getNbMots();i++) {
			wordsDeb[i]=wordsFin[i]=-1;
		}
		SuiteDeMots sold = new SuiteDeMots(old.wordLabels);
		for (int i=0;i<sold.getNmots();i++)
			sold.setTag(i, i);
		SuiteDeMots snew = new SuiteDeMots(wordLabels);
		snew.align(sold);
		lastAlignedWord=-1;
		for (int i=0;i<wordLabels.length;i++) {
			float[] j=snew.getLinkedTags(i);
			if (j!=null&&j.length>0) {
				wordsDeb[i]=old.wordsDeb[(int)j[0]];
				wordsFin[i]=old.wordsFin[(int)j[0]];
				if (wordsDeb[i]>=0) lastAlignedWord=i;
			}
		}
		// reporte les ancres
		manualAnchors=old.manualAnchors;
	}
	
	public int getNbMots() {
		return wordsDeb.length;
	}
	public int getLastWordAligned() {
		return lastAlignedWord;
	}
	public int getWordAtSample(long sample) {
		int fr = sample2frame(sample);
		int ww = Arrays.binarySearch(wordsDeb, 0, lastAlignedWord, fr);
		if (ww<0) ww=-ww-2;
		return ww;
//		for (int i=0;i<=lastAlignedWord;i++) {
//			if (fr>=wordsDeb[i]&&fr<=wordsFin[i]) {
//				return i;
//			}
//		}
//		return -1;
	}
	public String getWord(int motidx) {return wordLabels[motidx];}
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
	public long getSampleFin(int motidx) {
		int fr = getFrameFin(motidx);
		if (fr>=0) return frame2sample(fr);
		else return -1;
	}
	public void setAlignForWord(int motidx, int frdeb, int frfin) {
		wordsDeb[motidx] = frdeb;
		wordsFin[motidx] = frfin;
		if (motidx>lastAlignedWord&&frdeb>=0) lastAlignedWord=motidx;
	}
	public void setEndSample(int motidx, long samplefin) {
		wordsFin[motidx] = sample2frame(samplefin);
		if (motidx>lastAlignedWord) lastAlignedWord=motidx;
	}

	public int addAnchorAtLast() {
		manualAnchors.add(lastAlignedWord);
		return lastAlignedWord;
	}
	public void equiAlignBetweenWords(int mot1, int mot2, int frdeb, int frfin) {
		int nbmots = mot2 - mot1 +1;
		
		int framedeb = frdeb;
		int framesPerMot = (frfin +1 - frdeb) / nbmots;
		for (int i=mot1;i<=mot2;i++) {
			wordsDeb[i] = framedeb;
			framedeb += framesPerMot;
			wordsFin[i] = framedeb++;
		}
		wordsDeb[mot1] = frdeb;
		wordsFin[mot2] = frfin;
	}
	/**
	 * return le premier mot qui n'etait pas aligne et qui l'a ete ici
	 * reconstruit l'alignement des mots precedents jusqu'au dernier mot avec un alignement
	 * (car on interdit d'avoir des "trous" sans alignement dans le texte)
	 * ne touche pas aux mots suivants !
	 * mets-a-jour lastAlignedWord sur l'ancre
	 * 
	 * @param motidx
	 * @param endframe
	 * @return
	 */
	public int addAnchorAt(int motidx, int endframe) {
		if (motidx>=wordLabels.length) {
			return -1;
		}
		wordsFin[motidx]=endframe;
		manualAnchors.add(motidx);
		
		// alignement equi-* depuis le dernier mot aligne
		int lastMot = motidx - 1;
		for (; lastMot >= 0; lastMot--) {
			if (wordsFin[lastMot] >= 0)
				break;
		}
		int nbmots = motidx - lastMot;
		int framedeb = 0;
		if (lastMot >= 0)
			framedeb = wordsFin[lastMot] + 1;
		int firstMotRealigned = lastMot+1;
		int framesPerMot = (endframe - framedeb) / nbmots;
		for (lastMot++; lastMot < motidx; lastMot++) {
			wordsDeb[lastMot] = framedeb;
			framedeb += framesPerMot;
			wordsFin[lastMot] = framedeb++;
		}
		wordsDeb[motidx] = framedeb;
		lastAlignedWord = motidx;
		return firstMotRealigned;
	}
	
	String[] wordStates; // contient le nb de trames dans chaque etat
	
	public OldAlignment() {
		
	}
	public OldAlignment(int nbTrames) {
		labels = new String[nbTrames];
		tramesDeb = new int[nbTrames];
		tramesFin = new int[nbTrames];
	}
	public void allocateForWords(int nmots) {
		wordLabels = new String[nmots];
		wordsDeb = new int[nmots];
		wordsFin = new int[nmots];
		for (int i=0;i<nmots;i++) wordsDeb[i]=wordsFin[i]=-1;
	}
	
	public void appendMLF(String nom, String mfc) {
		int i=mfc.lastIndexOf('.');
		String lab = mfc.substring(0,i)+".lab";
		try {
			PrintWriter pf = new PrintWriter(new FileWriter(nom,true));
			pf.println("\""+lab+"\"");
			for (i=0;i<labels.length;i++) {
				float tdeb = frame2second(tramesDeb[i]);
				tdeb*=10000000f;
				float tfin = frame2second(tramesFin[i]);
				tfin*=10000000f;
				pf.println((int)tdeb+" "+(int)tfin+" "+labels[i].replace('[', '_').replace(']', ' '));
			}
			pf.println(".");
			pf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String[] getWordsLabels() {
		computeWordsLimits();
		return wordLabels;
	}
	public int[] getWordsDebs() {
		computeWordsLimits();
		return wordsDeb;
	}
	public int[] getWordsFins() {
		computeWordsLimits();
		return wordsFin;
	}
	public String[] getWordsStateAlign() {
		return wordStates;
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

	public static long frame2sample(int frame) {
		long f=frame;
		f*=160;
		f+=205; // moitie d'une window
		return f;
	}
	public static int sample2frame(long sample) {
		sample-=205;
		if (sample<0) return 0;
		return (int)(sample/160);
	}
	public static float frame2second(int trame) {
		return (float)trame/100f;
	}
	public static float sample2second(long sample) {
		return (float)sample/16000f;
	}
	public static int second2sample(float sec) {
		return (int)(sec*16000f);
	}
}
