/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.aligners;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import plugins.speechreco.aligners.sphiinx4.Alignment;

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
	
	public Alignment fullalign=null;
	
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
	/**
	 * detruit alignements + ancres depuis motidx inclu
	 * mets-a-jout lastAlignedWord sur le mot precedent motidx
	 * 
	 * @param motidx
	 * @return la liste des ancres supprimees
	 */
	public ArrayList<Integer> clearAlignFrom(int motidx) {
		ArrayList<Integer> deletedanchors = new ArrayList<Integer>();
		System.err.println("clear align from "+motidx+" "+wordLabels[motidx]);
		for (int i=motidx;i<wordLabels.length;i++) {
			if (wordsDeb[i]<0) break;
			wordsDeb[i]=wordsFin[i]=-1;
		}
		try {
			for (;;) {
				Integer lastanchor = manualAnchors.last();
				if (lastanchor==null||lastanchor<motidx) break;
				System.err.println("remove anchor "+lastanchor);
				manualAnchors.remove(lastanchor);
				deletedanchors.add(lastanchor);
			}
		} catch (NoSuchElementException e) {
			System.err.println("pas d'ancres precedentes...");
		}
		lastAlignedWord=motidx-1;
		System.err.println("lastalgned1 "+lastAlignedWord);
		while (lastAlignedWord>=0&&wordsDeb[lastAlignedWord]<0) lastAlignedWord--;
		System.err.println("lastalgned2 "+lastAlignedWord);
		return deletedanchors;
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
	
	public void save(String nom) {
		try {
			PrintWriter pf = new PrintWriter(new FileWriter(nom));
			for (int i=0;i<labels.length;i++) {
				pf.println(i+" "+labels[i]+" "+tramesDeb[i]+" "+tramesFin[i]);
			}
			pf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	public void saveTRS(String nom) {
		try {
			PrintWriter pf = new PrintWriter(new FileWriter(nom));
			pf.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
			pf.println("<!DOCTYPE Trans SYSTEM \"trans-13.dtd\">");
			pf.println("<Trans scribe=\"YM\" audio_filename=\"\" version=\"1\" version_date=\"981211\" xml:lang=\"fr\" elapsed_time=\"\">");
			pf.println("<Speakers>");
			pf.println("<Speaker id=\"sp\" name=\"X\" type=\"male\"/>");
			pf.println("</Speakers>");
			pf.println("<Episode>");
			float t = frame2second(tramesFin[tramesFin.length-1]);
			// on n'utilise pas les turn !
			pf.println("<Section type=\"filler\" startTime=\"0.000\" endTime=\"1.000\">");
		    pf.println("<Turn speaker=\"sp\" startTime=\"0.000\" endTime=\"1.000\">");
			for (int i=0;i<labels.length;i++) {
				if (labels[i]==null) continue;
				if (i>0&&tramesDeb[i]!=tramesFin[i-1]) {
					t = frame2second(tramesDeb[i]);
					pf.println("<Sync time=\""+t+"\"/>");
				}
				pf.println(labels[i].replace('[', '_').replace(']', ' '));
				t = frame2second(tramesFin[i]);
				pf.println("<Sync time=\""+t+"\"/>");
			}
			pf.println("</Turn>");
			pf.println("</Section>");
			pf.println("</Episode>");
			pf.println("</Trans>");
			pf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static OldAlignment load(String nom) {
		try {
			BufferedReader bf = new BufferedReader(new FileReader(nom));
			int n=0;
			for (;;) {
				String s = bf.readLine();
				if (s==null) break;
				n++;
			}
			bf.close();
			OldAlignment align = new OldAlignment(n);
			bf = new BufferedReader(new FileReader(nom));
			for (int i=0;i<n;i++) {
				String s = bf.readLine();
				if (s==null) break;
				String[] ss = s.split(" ");
				align.labels[i] = ""+ss[1];
				align.tramesDeb[i] = Integer.parseInt(ss[2]);
				align.tramesFin[i] = Integer.parseInt(ss[3]);
			}
			bf.close();
			return align;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
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
	
	/**
	 * obsolete: cette fonction detruit l'alignement en phones. De plus, l'acces direct
	 * a l'attribut labels n'est pas bon: il faut donc utiliser plutot getWordLabels() !!
	 */
	public void keepOnlyWords() {
		int deb=tramesDeb[0];
		Vector<String> newlabels = new Vector<String>();
		Vector<Integer> newdebs = new Vector<Integer>();
		Vector<Integer> newfins = new Vector<Integer>();
		// debug
/*
		for (int i=0;i<labels.length;i++) {
			System.err.println("debug "+i+" "+labels[i]+" "+tramesDeb[i]+" "+tramesFin[i]);
		}
		*/
			for (int i=0;i<labels.length;i++) {
			// on recherche un silence:
			if (labels[i].equals("sil[2]")) {
				deb=tramesDeb[i];
			} else if (labels[i].equals("sil[4]")) {
				newlabels.add("sil");
				newdebs.add(deb);
				newfins.add(tramesFin[i]);
				deb=tramesFin[i];
			} else {
				// on recherche un mot:
				int pos = labels[i].indexOf('(');
				if (pos>=0) {
					newlabels.add(labels[i].substring(pos+1,labels[i].indexOf(')')));
					newdebs.add(deb);
					newfins.add(tramesFin[i]);
					deb=tramesFin[i];
				}
			}
		}
		labels = new String[newlabels.size()];
		tramesDeb = new int[labels.length];
		tramesFin = new int[labels.length];
		newlabels.toArray(labels);
		for (int i=0;i<labels.length;i++) {
			tramesDeb[i] = newdebs.get(i);
			tramesFin[i] = newfins.get(i);
		}
	}
	public void removeStateInfo() {
		String old="";
		int deb=-1,fin=-1;
		Vector<String> newlabels = new Vector<String>();
		Vector<Integer> newdebs = new Vector<Integer>();
		Vector<Integer> newfins = new Vector<Integer>();
		for (int i=0;i<labels.length;i++) {
			String[] ss = labels[i].split("\\[");
			if (!ss[0].equals(old)) {
				if (old.length()>0) {
					newlabels.add(old);
					newdebs.add(deb);
					newfins.add(fin);
				}
				old=ss[0];
				deb=tramesDeb[i];
			}
			fin=tramesFin[i];
		}
		labels = new String[newlabels.size()];
		tramesDeb = new int[labels.length];
		tramesFin = new int[labels.length];
		newlabels.toArray(labels);
		for (int i=0;i<labels.length;i++) {
			tramesDeb[i] = newdebs.get(i);
			tramesFin[i] = newfins.get(i);
		}
	}
	public void removeCDinfo() {
		for (int i=0;i<labels.length;i++) {
			String s = labels[i];
			int j = s.indexOf('-');
			if (j>=0) {
				s=s.substring(j+1);
			}
			j = s.indexOf('+');
			if (j>=0) {
				s=s.substring(0,j);
			}
			labels[i]=s;
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
