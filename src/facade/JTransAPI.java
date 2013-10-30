package facade;

import java.util.*;

import plugins.applis.SimpleAligneur.Aligneur;
import plugins.signalViewers.spectroPanel.SpectroControl;
import plugins.speechreco.aligners.sphiinx4.AlignementEtat;
import plugins.speechreco.aligners.sphiinx4.S4AlignOrder;
import plugins.speechreco.aligners.sphiinx4.S4ForceAlignBlocViterbi;
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
	
	public static void batchAlign(int motdeb, int trdeb, int motfin, int trfin) {
		System.out.println("batch align "+motdeb+"-"+motfin+" "+trdeb+":"+trfin);
		if (s4blocViterbi==null) {
			String[] amots = new String[mots.size()];
			for (int i=0;i<mots.size();i++) {
				amots[i] = mots.get(i).getWordString();
			}
			s4blocViterbi = S4ForceAlignBlocViterbi.getS4Aligner(aligneur.wavname);
			s4blocViterbi.setMots(amots);
		}
		
		S4AlignOrder order = new S4AlignOrder(motdeb, trdeb, motfin, trfin);
		try {
			s4blocViterbi.input2process.put(order);
			synchronized(order) {
				order.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (order.alignWords!=null) {
			order.alignWords.adjustOffset(trdeb);
			order.alignPhones.adjustOffset(trdeb);
			order.alignStates.adjustOffset(trdeb);
			System.out.println("================================= ALIGN FOUND");
			System.out.println(order.alignWords.toString());
			String[] wordsThatShouldBeAligned = new String[1+motfin-motdeb];
			for (int i=motdeb, j=0;i<=motfin;i++,j++) {
				wordsThatShouldBeAligned[j]=mots.get(i).getWordString();
			}
			System.out.println("wordsthatshouldbealigned "+Arrays.toString(wordsThatShouldBeAligned));
			int[] locmots2segidx = order.alignWords.matchWithText(wordsThatShouldBeAligned);
			int nsegsbefore = aligneur.alignement.merge(order.alignWords);
			int[] mots2segidx = Arrays.copyOf(locmots2segidx,locmots2segidx.length);
			for (int i=0;i<locmots2segidx.length;i++) {
				if (locmots2segidx[i]>=0)
					mots2segidx[i]+=nsegsbefore;
			}
			System.out.println("mots2segs "+locmots2segidx.length+" "+Arrays.toString(mots2segidx));
			if (edit!=null) {
				for (int i=motdeb, j=0;i<=motfin;i++,j++) {
					System.out.println("posinalign "+i+" "+mots2segidx[j]);
					mots.get(i).posInAlign=mots2segidx[j];
				}
				edit.getListeElement().refreshIndex();
			}
			alignementPhones.merge(order.alignPhones);
		} else {
			System.out.println("================================= ALIGN FOUND null");
			// TODO
		}
	}
	
	/**
	 * 
	 * This function automatically align all the words until mot
	 * 
	 * @param mot
	 * @param frdeb can be <0, in which case use the last aligned word.
	 * @param frfin
	 */
	public static void setAlignWord(int mot, int frdeb, int frfin) {
		final boolean equialign=false;
		// TODO: detruit les segments recouvrants

		float curendfr=-1;
		int mot0 = getLastMotPrecAligned(mot);
		System.out.println("setalign "+mot+" "+frdeb+" "+frfin+" "+mot0);
		if (mot0<mot-1) {
			// il y a des mots non alignes avant
			if (mot0>=0) {
				int mot0seg = mots.get(mot0).posInAlign;
				int mot0endfr = alignementWords.getSegmentEndFrame(mot0seg);
				if (equialign) {
					// equi-aligne les mots precedents
					float frdelta = ((float)(frfin-mot0endfr))/(float)(mot+1-mot0);
					int prevseg = mot0seg;
					float curdebfr = alignementWords.getSegmentEndFrame(prevseg);
					curendfr = alignementWords.getSegmentEndFrame(prevseg)+frdelta;
					for (int i=mot0+1;i<mot;i++) {
						if (frdeb>=0&&curendfr>frdeb) curendfr=frdeb;
						int nnewseg = alignementWords.addRecognizedSegment(mots.get(i).getWordString(), (int)curdebfr, (int)curendfr, null, null);
						mots.get(i).posInAlign=nnewseg;
						prevseg=nnewseg;
						curdebfr = curendfr;
						curendfr+=frdelta;
					}
				} else {
					// auto-align les mots precedents
					if (frdeb>=0) mot0endfr=frdeb;
					batchAlign(mot0+1, mot0endfr, mot, frfin);
				}
				if (edit!=null) edit.colorizeAlignedWords(mot0, mot);
			} else {
				// aucun mot avant aligne
				batchAlign(0, 0, mot, frfin);
				if (edit!=null) edit.colorizeAlignedWords(0, mot);
			}
		} else {
			// il y a un seul mot a aligner
			if (mot0>=0) {
				int mot0seg = mots.get(mot0).posInAlign;
				curendfr = alignementWords.getSegmentEndFrame(mot0seg);
			} else curendfr=0;
			if (!equialign) {
				if (frdeb<0) frdeb=(int)curendfr;
				int newseg = alignementWords.addRecognizedSegment(elts.getMot(mot).getWordString(), frdeb, frfin, null, null);
				alignementWords.setSegmentSourceManu(newseg);
				elts.getMot(mot).posInAlign=newseg;
			}
			if (edit!=null) edit.colorizeAlignedWords(mot, mot);
		}
		
		if (equialign) {
			// aligne le dernier mot
			if (frdeb<0) frdeb=(int)curendfr;
			int newseg = alignementWords.addRecognizedSegment(elts.getMot(mot).getWordString(), frdeb, frfin, null, null);
			alignementWords.setSegmentSourceManu(newseg);
			elts.getMot(mot).posInAlign=newseg;
		}
		
		// TODO: phonetiser et aligner auto les phonemes !!
		
		if (edit!=null) edit.repaint();
	}
	public static void setAlignWord(int mot, float secdeb, float secfin) {
		int curdebfr = SpectroControl.second2frame(secdeb);
		int curendfr = SpectroControl.second2frame(secfin);
		setAlignWord(mot, curdebfr, curendfr);
	}
	private static void setSilenceSegment(int curdebfr, int curendfr, AlignementEtat al) {
		// detruit tous les segments existants deja a cet endroit
		ArrayList<Integer> todel = new ArrayList<Integer>();
		clearAlignFromFrame(curdebfr);
		for (int i=0;i<al.getNbSegments();i++) {
			int d=al.getSegmentDebFrame(i);
			if (d>=curendfr) break;
			int f=al.getSegmentEndFrame(i);
			if (f<curdebfr) continue;
			// il y a intersection
			if (d>=curdebfr&&f<=curendfr) {
				// ancient segment inclu dans nouveau
				todel.add(i);
			} else {
				// TODO: faire les autres cas d'intersection
			}
		}
		for (int i=todel.size()-1;i>=0;i--) al.delSegment(todel.get(i));
		int newseg=al.addRecognizedSegment("SIL", curdebfr, curendfr, null, null);
		al.setSegmentSourceManu(newseg);
	}
	public static void setSilenceSegment(float secdeb, float secfin) {
		int curdebfr = SpectroControl.second2frame(secdeb);
		int curendfr = SpectroControl.second2frame(secfin);
		setSilenceSegment(curdebfr, curendfr, alignementWords);
		setSilenceSegment(curdebfr, curendfr, alignementPhones);
	}
	public static void clearAlignFromFrame(int fr) {
		// TODO
	}
	
	// =========================
	// variables below are duplicate (point to) of variables in the mess of the rest of the code...
	private static ListeElement elts =  null;
	public static AlignementEtat alignementWords = null;
	public static AlignementEtat alignementPhones = null;
	public static TexteEditor edit = null;
	public static Aligneur aligneur = null;
	public static S4ForceAlignBlocViterbi s4blocViterbi = null;
	
	public static void setElts(ListeElement e) {
		elts=e;
		mots = elts.getMots();
	}
	
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
//System.out.println("ZZZZZZZZZ "+i+" "+mots.get(i).posInAlign);
			if (mots.get(i).posInAlign>=0) return i;
		}
		return -1;
	}

	public static void loadTRS(String trsfile) {
		TRSLoader trs = null;
		try {
			trs = new TRSLoader(trsfile);
		} catch (Exception e) {
			System.err.println("TRS loader failed!");
			e.printStackTrace();
			// TODO handle failure gracefully -IJ
		}

		TexteEditor zonetexte = TexteEditor.getTextEditor();
		zonetexte.setText(trs.text);
		zonetexte.setEditable(false);

		// il ne faut pas que reparse modifie le texte !!!
		zonetexte.reparse(false);
		System.out.println("apres parsing: nelts=" + elts.size() + " ancres=" + trs.anchors);

		// Now that the listElts is known, maps the time pointers to the elts in the liste
		TRSLoader.Anchor prevAnchor = null;
		for (TRSLoader.Anchor anchor: trs.anchors) {
			System.out.println("Anchor: " + anchor.character + " " + anchor.seconds);

			int character = anchor.character;

			int mot = edit.getListeElement().getIndiceMotAtTextPosi(character);
			System.out.println(elts.getIndiceMotAtTextPosi(character));

			while (mot < 0 && character > 0) {
				mot = edit.getListeElement().getIndiceMotAtTextPosi(--character);
			}

			if (null == prevAnchor) {
				setAlignWord(mot, -1, anchor.seconds);
				prevAnchor = anchor;
			} else
				setAlignWord(mot, prevAnchor.seconds, anchor.seconds);
		}

		aligneur.caretSensible = true;

		// force la construction de l'index
		alignementWords.clearIndex();
		alignementWords.getSegmentAtFrame(0);
		System.out.println("align index built");
		alignementPhones.clearIndex();
		alignementPhones.getSegmentAtFrame(0);
		edit.getListeElement().refreshIndex();
	}
}
