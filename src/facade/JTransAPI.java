package facade;

import java.util.ArrayList;
import java.util.List;

import plugins.signalViewers.spectroPanel.SpectroControl;
import plugins.speechreco.aligners.sphiinx4.AlignementEtat;
import plugins.text.ListeElement;
import plugins.text.TexteEditor;
import plugins.text.elements.Element_Mot;

public class JTransAPI {
	
	public static int getNbWords() {
		if (elts==null) return 0;
		initmots();
		return mots.size();
	}
	public static boolean isBruit(int mot) {
		initmots();
		Element_Mot m = elts.getMot(mot);
		return m.isBruit;
	}
	public static void setAlignWord(int mot, int frdeb, int frfin) {
		// TODO: detruit les segments recouvrants

		// equi-aligne les mots precedents
		float curendfr=-1;
		int mot0 = getLastMotPrecAligned(mot);
		if (mot0<mot-1) {
			// il y a des mots non alignes avant
			if (mot0>=0) {
				int mot0seg = mots.get(mot0).posInAlign;
				int mot0endfr = alignementWords.getSegmentEndFrame(mot0seg);
				float frdelta = ((float)(frfin-mot0endfr))/(float)(mot+1-mot0);
				int prevseg = mot0seg;
				float curdebfr = alignementWords.getSegmentEndFrame(prevseg);
				curendfr = alignementWords.getSegmentEndFrame(prevseg)+frdelta;
				for (int i=mot0+1;i<mot;i++) {
					if (frdeb>=0&&curendfr>frdeb) curendfr=frdeb;
System.out.println("LLLLLLLLLLLLLLLLLLLLLLLL "+curdebfr+" "+curendfr);
					int nnewseg = alignementWords.addRecognizedSegment(mots.get(i).getWordString(), (int)curdebfr, (int)curendfr, null, null);
					mots.get(i).posInAlign=nnewseg;
					prevseg=nnewseg;
					curdebfr = curendfr;
					curendfr+=frdelta;
				}
				if (edit!=null) edit.colorizeAlignedWords(mot0, mot);
			} else {
				// TODO: tous les mots avants ne sont pas alignes
			}
		} else
			if (edit!=null) edit.colorizeAlignedWords(mot, mot);
		
		// aligne le dernier mot
		if (frdeb<0) frdeb=(int)curendfr;
System.out.println("KKKKKKKKKKKKKKK "+frdeb+" "+frfin+" "+mot0);
		int newseg = alignementWords.addRecognizedSegment(elts.getMot(mot).getWordString(), frdeb, frfin, null, null);
		alignementWords.setSegmentSourceManu(newseg);
		elts.getMot(mot).posInAlign=newseg;


		if (edit!=null) edit.repaint();
	}
	public static void setAlignWord(int mot, float secdeb, float secfin) {
		int curdebfr = SpectroControl.second2frame(secdeb);
		int curendfr = SpectroControl.second2frame(secfin);
		setAlignWord(mot, curdebfr, curendfr);
	}
	public static void setSilenceSegment(float secdeb, float secfin) {
		int curdebfr = SpectroControl.second2frame(secdeb);
		int curendfr = SpectroControl.second2frame(secfin);
		// detruit tous les segments existants deja a cet endroit
		ArrayList<Integer> todel = new ArrayList<Integer>();
		clearAlignFromFrame(curdebfr);
		for (int i=0;i<alignementWords.getNbSegments();i++) {
			int d=alignementWords.getSegmentDebFrame(i);
			if (d>=curendfr) break;
			int f=alignementWords.getSegmentEndFrame(i);
			if (f<curdebfr) continue;
			// il y a intersection
			if (d>=curdebfr&&f<=curendfr) {
				// ancient segment inclu dans nouveau
				todel.add(i);
			} else {
				// TODO: faire les autres cas d'intersection
			}
		}
		for (int i=todel.size()-1;i>=0;i--) alignementWords.delSegment(todel.get(i));
		int newseg=alignementWords.addRecognizedSegment("SIL", curdebfr, curendfr, null, null);
		alignementWords.setSegmentSourceManu(newseg);
	}
	public static void clearAlignFromFrame(int fr) {
		// TODO
	}
	
	// =========================
	// variables below are duplicate (point to) of variables in the mess of the rest of the code...
	public static ListeElement elts =  null;
	public static AlignementEtat alignementWords = null;
	public static TexteEditor edit = null;
	
	// =========================
	private static List<Element_Mot> mots = null;
	
	// =========================
	private static void initmots() {
		if (elts!=null)
			if (mots==null) {
				mots=elts.getMots();
			}
	}
	private static int getLastMotPrecAligned(int midx) {
		initmots();
		for (int i=midx;i>=0;i--) {
System.out.println("ZZZZZZZZZ "+i+" "+mots.get(i).posInAlign);
			if (mots.get(i).posInAlign>=0) return i;
		}
		return -1;
	}
}
