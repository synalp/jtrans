package facade;

import java.util.ArrayList;
import java.util.List;

import plugins.signalViewers.spectroPanel.SpectroControl;
import plugins.speechreco.aligners.sphiinx4.AlignementEtat;
import plugins.text.ListeElement;
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
		int newseg = alignementWords.addRecognizedSegment(elts.getMot(mot).getWordString(), frdeb, frfin, null, null);
		elts.getMot(mot).posInAlign=newseg;
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
			}
		}
		for (int i=todel.size()-1;i>=0;i--) alignementWords.delSegment(todel.get(i));
		alignementWords.addRecognizedSegment("SIL", curdebfr, curendfr, null, null);
	}
	public static void clearAlignFromFrame(int fr) {
		// TODO
	}
	
	// =========================
	// variables below are duplicate (point to) of variables in the mess of the rest of the code...
	public static ListeElement elts =  null;
	public static AlignementEtat alignementWords = null;
	
	// =========================
	private static List<Element_Mot> mots = null;
	
	// =========================
	private static void initmots() {
		if (elts!=null)
			if (mots==null) {
				mots=elts.getMots();
			}
	}
}
