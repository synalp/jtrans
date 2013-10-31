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
	/**
	 * Align words between anchors using linear interpolation (a.k.a.
	 * "equialign") instead of proper Sphinx alignment (batchAlign).
	 * Setting this flag to `true` yields very fast albeit inaccurate results.
	 */
	private static final boolean USE_LINEAR_ALIGNMENT = false;
	
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

	public static final Object cachedObject(String objectName, Cache.Factory factory) {
		return Cache.cachedObject(aligneur.wavname, edit.getText().toString(), objectName, factory);
	}

	private static S4AlignOrder createS4AlignOrder(int motdeb, int trdeb, int motfin, int trfin) {
		S4AlignOrder order = new S4AlignOrder(motdeb, trdeb, motfin, trfin);
		try {
			s4blocViterbi.input2process.put(order);
			synchronized(order) {
				order.wait();
				// TODO ce thread ne sort jamais d'ici si sphinx plante
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return order;
	}

	/**
	 * Align words between startWord and endWord using Sphinx.
	 * Slow, but accurate.
	 *
	 * The resulting S4AlignOrder objects may be cached to save time.
	 */
	public static void batchAlign(final int startWord, final int startFrame, final int endWord, final int endFrame) {
		System.out.println("batch align "+startWord+"-"+endWord+" "+startFrame+":"+endFrame);
		if (s4blocViterbi==null) {
			String[] amots = new String[mots.size()];
			for (int i=0;i<mots.size();i++) {
				amots[i] = mots.get(i).getWordString();
			}
			s4blocViterbi = S4ForceAlignBlocViterbi.getS4Aligner(aligneur.wavname);
			s4blocViterbi.setMots(amots);
		}

		S4AlignOrder order = (S4AlignOrder)cachedObject(
				String.format("%05d_%05d_%05d_%05d.order", startWord, startFrame, endWord, endFrame),
				new Cache.Factory() {
					public Object make() { return createS4AlignOrder(startWord, startFrame, endWord, endFrame); }
				});

		if (order.alignWords!=null) {
			order.alignWords.adjustOffset(startFrame);
			order.alignPhones.adjustOffset(startFrame);
			order.alignStates.adjustOffset(startFrame);
			System.out.println("================================= ALIGN FOUND");
			System.out.println(order.alignWords.toString());
			String[] wordsThatShouldBeAligned = new String[1+endWord-startWord];
			for (int i=startWord, j=0;i<=endWord;i++,j++) {
				wordsThatShouldBeAligned[j]=mots.get(i).getWordString();
			}
			System.out.println("wordsthatshouldbealigned "+Arrays.toString(wordsThatShouldBeAligned));
			int[] locmots2segidx = order.alignWords.matchWithText(wordsThatShouldBeAligned);
			int nsegsbefore = alignementWords.merge(order.alignWords);
			int[] mots2segidx = Arrays.copyOf(locmots2segidx,locmots2segidx.length);
			for (int i=0;i<locmots2segidx.length;i++) {
				if (locmots2segidx[i]>=0)
					mots2segidx[i]+=nsegsbefore;
			}
			System.out.println("mots2segs "+locmots2segidx.length+" "+Arrays.toString(mots2segidx));
			if (edit!=null) {
				for (int i=startWord, j=0;i<=endWord;i++,j++) {
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
	 * Align words between startWord and endWord using linear interpolation.
	 * Very fast, but inaccurate.
	 */
	public static void linearAlign(int startWord, int startFrame, int endWord, int endFrame) {
		float frameDelta = ((float)(endFrame-startFrame))/((float)(endWord-startWord+1));
		float currEndFrame = startFrame + frameDelta;

		for (int i = startWord; i <= endWord; i++) {
			int newseg = alignementWords.addRecognizedSegment(
					mots.get(i).getWordString(), startFrame, (int)currEndFrame, null, null);

			alignementWords.setSegmentSourceEqui(newseg);
			mots.get(i).posInAlign = newseg;

			startFrame = (int)currEndFrame;
			currEndFrame += frameDelta;
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
		assert endFrame >= 0;
		// TODO: detruit les segments recouvrants

		int lastAlignedWord = getLastMotPrecAligned(word);
		int startWord;

		System.out.println("setalign "+word+" "+startFrame+" "+endFrame+" "+lastAlignedWord);

		// Find first word to align (startWord) and adjust startFrame if needed.
		if (lastAlignedWord >= 0) {
			startWord = lastAlignedWord + 1;
			if (startFrame < 0) {
				// Start aligning at the end frame of the last aligned word.
				int lastAlignedWordSeg = mots.get(lastAlignedWord).posInAlign;
				startFrame = alignementWords.getSegmentEndFrame(lastAlignedWordSeg);
			}
		} else {
			// Nothing is aligned yet; start aligning from the beginning.
			startWord = 0;
			startFrame = 0;
		}

		assert startWord <= word :
				String.format("start#%d <= word#%d ?? word:'%s'", startWord, word, elts.getMot(word).getWordString());

		if (startWord < word) {
			// There are unaligned words before `word`; align them.
			if (USE_LINEAR_ALIGNMENT) {
				linearAlign(startWord, startFrame, word, endFrame);
			} else {
				batchAlign(startWord, startFrame, word, endFrame);
			}
		} else {
			// Only one word to align; create a new manual segment.
			int newseg = alignementWords.addRecognizedSegment(
					elts.getMot(word).getWordString(), startFrame, endFrame, null, null);
			alignementWords.setSegmentSourceManu(newseg);
			elts.getMot(word).posInAlign = newseg;
		}

		// TODO: phonetiser et aligner auto les phonemes !!

		// Update GUI
		if (edit != null) {
			edit.colorizeAlignedWords(startWord, word);
			edit.repaint();
		}
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

		// Align words between the previous anchor and the current one. All
		// TRS files specify an anchor at time 0, so don't align on the
		// first anchor.
		TRSLoader.Anchor prevAnchor = trs.anchors.get(0);
		for (int i = 1; i < trs.anchors.size(); i++) {
			TRSLoader.Anchor anchor = trs.anchors.get(i);

			int character = anchor.character;
			int word = edit.getListeElement().getIndiceMotAtTextPosi(character);

			while (word < 0 && character > 0) {
				word = edit.getListeElement().getIndiceMotAtTextPosi(--character);
			}

			setAlignWord(word, prevAnchor.seconds, anchor.seconds);
			prevAnchor = anchor;
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
