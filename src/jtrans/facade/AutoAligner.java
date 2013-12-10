package jtrans.facade;

import java.io.File;
import java.util.*;

import jtrans.elements.*;
import jtrans.gui.JTransGUI;
import jtrans.speechreco.s4.Alignment;
import jtrans.speechreco.s4.S4AlignOrder;
import jtrans.speechreco.s4.S4ForceAlignBlocViterbi;
import jtrans.utils.TimeConverter;


/**
 * Handles automatic alignment
 */
public class AutoAligner {
	private JTransGUI aligneur;
	private Project project;
	private Track track;
	private List<Word> mots;
	private S4ForceAlignBlocViterbi s4blocViterbi;


	/**
	 * Align words between anchors using linear interpolation (a.k.a.
	 * "equialign") instead of proper Sphinx alignment (batchAlign).
	 * Setting this flag to `true` yields very fast albeit inaccurate results.
	 */
	private static final boolean USE_LINEAR_ALIGNMENT = false;


	public AutoAligner(Project project, Track track, JTransGUI aligneur) {
		this.project = project;
		this.track = track;
		this.aligneur = aligneur;
		track.refreshIndex();
		mots = track.elts.getMots();
	}

	private S4AlignOrder createS4AlignOrder(int motdeb, int trdeb, int motfin, int trfin) {
		if (s4blocViterbi == null)
			s4blocViterbi = aligneur.getS4aligner();

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

		if (!order.isEmpty())
			order.adjustOffset();

		return order;
	}

	/**
	 * Align words between startWord and endWord using Sphinx.
	 * Slow, but accurate.
	 *
	 * The resulting S4AlignOrder objects may be cached to save time.
	 *
	 * It is not merged into the main alignment (use mergeOrder() for that).
	 */
	private S4AlignOrder partialBatchAlign(final int startWord, final int startFrame, final int endWord, final int endFrame) {
		StringBuilder phrase = new StringBuilder();
		String prefix = "";
		for (int i = startWord; i <= endWord; i++) {
			phrase.append(prefix).append(mots.get(i));
			prefix = " ";
		}

		return (S4AlignOrder)Cache.cachedObject(
				"order",
				"ser",
				new Cache.ObjectFactory() {
					public Object make() {
						return createS4AlignOrder(startWord, startFrame, endWord, endFrame);
					}
				},
				new File(project.wavname), // make it a file to take into account its modification date
				phrase.toString(),
				startFrame,
				endFrame);
	}

	/**
	 * Merge an S4AlignOrder into the main alignment.
	 */
	private void mergeOrder(S4AlignOrder order, int startWord, int endWord) {
		if (order.alignWords != null) {
			String[] alignedWords = new String[1 + endWord - startWord];
			for (int i = 0; i < 1+endWord-startWord; i++)
				alignedWords[i] = mots.get(i + startWord).getWordString();
			int[] wordSegments = order.alignWords.matchWithText(alignedWords);

			// Merge word segments into the main word alignment
			int firstSegment = track.words.merge(order.alignWords);

			// Adjust posInAlign for Word elements
			for (int i = 0; i < wordSegments.length; i++) {
				int idx = wordSegments[i];

				// Offset if we have a valid segment index
				if (idx >= 0)
					idx += firstSegment;

				mots.get(i + startWord).posInAlign = idx;
			}
			track.refreshIndex();

			// Merge phoneme segments into the main phoneme alignment
			track.phons.merge(order.alignPhones);
		} else {
			System.out.println("================================= ALIGN FOUND null");
			// TODO
		}
	}

	/**
	 * Align words between startWord and endWord using linear interpolation in
	 * the main alignment.
	 * Very fast, but inaccurate.
	 */
	private void linearAlign(int startWord, int startFrame, int endWord, int endFrame) {
		float frameDelta = ((float)(endFrame-startFrame))/((float)(endWord-startWord+1));
		float currEndFrame = startFrame + frameDelta;

		assert frameDelta >= 1f:
				"can't align on fractions of frames! (frameDelta=" + frameDelta + ")";

		for (int i = startWord; i <= endWord; i++) {
			int newseg = track.words.addRecognizedSegment(
					mots.get(i).getWordString(), startFrame, (int)currEndFrame, null, null);

			track.words.setSegmentSourceEqui(newseg);
			mots.get(i).posInAlign = newseg;

			startFrame = (int)currEndFrame;
			currEndFrame += frameDelta;
		}
	}

	/**
	 * Align all words until `word`.
	 *
	 * @param startWord number of the first word to align. If < 0, use last aligned word before `word`.
	 * @param word number of the last word to align
	 * @param startFrame can be < 0, in which case use the last aligned word.
	 * @param endFrame
	 */
	private void setAlignWord(int startWord, int word, int startFrame, int endFrame) {
		assert endFrame >= 0;

		if (startWord < 0) {
			int lastAlignedWord = getLastMotPrecAligned(word);

			if (lastAlignedWord <= 0) {
				// Nothing is aligned yet; start aligning from the beginning.
				startWord = 0;
				startFrame = 0;
			} else {
				startWord = lastAlignedWord + 1;

				if (startFrame < 0) {
					// Start aligning at the end frame of the last aligned word.
					int lastAlignedWordSeg = mots.get(lastAlignedWord).posInAlign;
					startFrame = track.words.getSegmentEndFrame(lastAlignedWordSeg);
				}
			}
		}

		// Lagging behind the alignment - wait for a couple more words
		if (startWord > word)
			return;

		if (startWord < word) {
			// There are unaligned words before `word`; align them.
			if (USE_LINEAR_ALIGNMENT) {
				linearAlign(startWord, startFrame, word, endFrame);
			} else {
				S4AlignOrder order = partialBatchAlign(startWord, startFrame, word, endFrame);
				mergeOrder(order, startWord, word);
			}
		} else {
			// Only one word to align; create a new manual segment.
			int newseg = track.words.addRecognizedSegment(
					track.elts.getMot(word).getWordString(), startFrame, endFrame, null, null);
			track.words.setSegmentSourceManu(newseg);
			track.elts.getMot(word).posInAlign = newseg;
		}

		// Update GUI
		aligneur.edit.colorizeWords(startWord, word);
	}

	private void setAlignWord(int startWord, int endWord, float startSecond, float endSecond) {
		int startFrame = TimeConverter.second2frame(startSecond);
		int endFrame   = TimeConverter.second2frame(endSecond);
		setAlignWord(startWord, endWord, startFrame, endFrame);
	}

	private void setSilenceSegment(int curdebfr, int curendfr, Alignment al) {
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

	private int getLastMotPrecAligned(int midx) {
		for (int i=midx;i>=0;i--) {
			if (mots.get(i).posInAlign>=0) return i;
		}
		return -1;
	}

	public void setSilenceSegment(float secdeb, float secfin) {
		int curdebfr = TimeConverter.second2frame(secdeb);
		int curendfr = TimeConverter.second2frame(secfin);
		setSilenceSegment(curdebfr, curendfr, track.words);
		setSilenceSegment(curdebfr, curendfr, track.phons);
	}
	public void clearAlignFromFrame(int fr) {
		// TODO
		throw new Error("clearAlignFromFrame: IMPLEMENT ME!");
	}



	/**
	 * Align words automatically between anchors set manually.
	 */
	public void alignBetweenAnchors() {
		aligneur.setIndeterminateProgress("Aligning...");

		track.clearAlignment();

		float alignFrom = 0;
		int startWord = 0;
		int word = -1;

		for (int i = 0; i < track.elts.size(); i++) {
			Element e = track.elts.get(i);

			if (e instanceof Word) {
				word++;
			} else if (e instanceof Anchor) {
				float alignTo = ((Anchor) e).seconds;

				if (word >= 0 && word >= startWord) {
					setAlignWord(startWord, word, alignFrom, alignTo);
				}

				alignFrom = alignTo;
				startWord = word + 1;
			}

			aligneur.setProgress(
					"Aligning " + (i + 1) + "/" + track.elts.size() + "...",
					(i + 1) / (float) track.elts.size());
		}

		track.refreshIndex();
	}


	/**
	 * Aligns words automatically. Does not account for anchors.
	 */
	public void alignRaw() {
		aligneur.setIndeterminateProgress("Aligning...");
		track.clearAlignment();

		int lastAlignedWord = 0;
		int previousLAW = -1;

		// Keep going until all words are aligned or the aligner gets stuck
		while (lastAlignedWord < mots.size() && lastAlignedWord > previousLAW) {
			setAlignWord(-1, mots.size()-1, -1, aligneur.audioSourceTotalFrames);

			previousLAW = lastAlignedWord;
			lastAlignedWord = getLastMotPrecAligned(mots.size()-1);

			aligneur.setProgress(
					"Aligning " + (lastAlignedWord + 1) + "/" + track.elts.size() + "...",
					(lastAlignedWord + 1) / (float) track.elts.size());
		}

		track.refreshIndex();
	}
}
