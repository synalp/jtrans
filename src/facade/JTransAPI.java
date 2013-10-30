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
	 * Align all words until `word`.
	 * 
	 * @param word number of the last word to align
	 * @param startFrame can be < 0, in which case use the last aligned word.
	 * @param endFrame
	 */
	public static void setAlignWord(int word, int startFrame, int endFrame) {
		final boolean linearInterpolation = true; // equialign
		// TODO: detruit les segments recouvrants

		int lastAlignedWord = getLastMotPrecAligned(word);

		System.out.println("setalign "+word+" "+startFrame+" "+endFrame+" "+lastAlignedWord);

		if (lastAlignedWord < word-1) {
			if (lastAlignedWord >= 0) {
				// il y a des mots non alignes avant
				int prevWordSeg = mots.get(lastAlignedWord).posInAlign;
				int prevWordEndFrame = alignementWords.getSegmentEndFrame(prevWordSeg);

				if (linearInterpolation) {
					// equi-aligne les mots precedents
					float frameDelta = ((float)(endFrame-prevWordEndFrame))/((float)(word+1-lastAlignedWord));
					startFrame = alignementWords.getSegmentEndFrame(prevWordSeg);
					float currEndFrame = alignementWords.getSegmentEndFrame(prevWordSeg)+frameDelta;

					for (int i = lastAlignedWord+1; i <= word; i++) {
						if (startFrame >= 0 && currEndFrame < startFrame)
							currEndFrame = startFrame;

						int newseg = alignementWords.addRecognizedSegment(
								mots.get(i).getWordString(), startFrame, (int)currEndFrame, null, null);

						alignementWords.setSegmentSourceEqui(newseg);
						mots.get(i).posInAlign = newseg;

						startFrame = (int)currEndFrame;
						currEndFrame += frameDelta;
					}
				} else {
					// auto-align les mots precedents
					if (startFrame >= 0)
						prevWordEndFrame = startFrame;

					batchAlign(lastAlignedWord+1, prevWordEndFrame, word, endFrame);
				}

				if (edit!=null) edit.colorizeAlignedWords(lastAlignedWord, word);
			} else {
				// aucun mot avant aligne
				batchAlign(0, 0, word, endFrame);
				if (edit!=null) edit.colorizeAlignedWords(0, word);
			}
		} else {
			// il y a un seul mot a aligner
			if (lastAlignedWord >= 0) {
				int lastAlignedWordSeg = mots.get(lastAlignedWord).posInAlign;
				endFrame = alignementWords.getSegmentEndFrame(lastAlignedWordSeg);
			}
				else endFrame = 0;

			if (startFrame < 0)
				startFrame = endFrame;

			int newseg = alignementWords.addRecognizedSegment(
					elts.getMot(word).getWordString(), startFrame, endFrame, null, null);
			alignementWords.setSegmentSourceManu(newseg);
			elts.getMot(word).posInAlign=newseg;

			if (edit!=null) edit.colorizeAlignedWords(word, word);
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
