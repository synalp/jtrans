package facade;

import java.io.File;
import java.util.*;

import plugins.applis.SimpleAligneur.Aligneur;
import speechreco.aligners.sphiinx4.Alignment;
import speechreco.aligners.sphiinx4.S4AlignOrder;
import speechreco.aligners.sphiinx4.S4ForceAlignBlocViterbi;
import plugins.text.elements.*;
import utils.TimeConverter;
import utils.ProgressDialog;


/**
 * Handles automatic alignment
 */
public class AutoAligner {
	private Aligneur aligneur;
	private Project project;
	private List<Element_Mot> mots;
	private S4ForceAlignBlocViterbi s4blocViterbi;


	/**
	 * Align words between anchors using linear interpolation (a.k.a.
	 * "equialign") instead of proper Sphinx alignment (batchAlign).
	 * Setting this flag to `true` yields very fast albeit inaccurate results.
	 */
	private static final boolean USE_LINEAR_ALIGNMENT = false;


	public AutoAligner(Project project, Aligneur aligneur) {
		this.project = project;
		this.aligneur = aligneur;
		project.refreshIndex();
		mots = project.elts.getMots();
	}


	private class Overlap {
		/**
		 * ID of speaker #1 (the one who gets interrupted)
		 */
		byte s1;

		// speaker 1 word indices
		int s1FirstWord = -1;
		int s1LastNonOverlappedWord = -1;
		int s1LastWord = -1;

		// speaker 2 word indices
		int s2FirstWord = -1;
		int s2LastOverlappedWord = -1;

		// seconds
		/**
		 * When speaker #1 starts speaking alone.
		 */
		float s1StartsSpeaking = -1;

		/**
		 * When speaker #2 starts speaking (while speaker #1 is still
		 * speaking). Start of overlapped speech.
		 */
		float overlapStart = -1;

		/**
		 * When speaker #1 stops speaking (while speaker #2 is still
		 * speaking). End of overlapped speech.
		 */
		float overlapEnd = -1;
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
			int firstSegment = project.words.merge(order.alignWords);

			// Adjust posInAlign for word elements (Element_Mot)
			for (int i = 0; i < wordSegments.length; i++) {
				int idx = wordSegments[i];

				// Offset if we have a valid segment index
				if (idx >= 0)
					idx += firstSegment;

				mots.get(i + startWord).posInAlign = idx;
			}
			project.refreshIndex();

			// Merge phoneme segments into the main phoneme alignment
			project.phons.merge(order.alignPhones);
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
			int newseg = project.words.addRecognizedSegment(
					mots.get(i).getWordString(), startFrame, (int)currEndFrame, null, null);

			project.words.setSegmentSourceEqui(newseg);
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
					startFrame = project.words.getSegmentEndFrame(lastAlignedWordSeg);
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
			int newseg = project.words.addRecognizedSegment(
					project.elts.getMot(word).getWordString(), startFrame, endFrame, null, null);
			project.words.setSegmentSourceManu(newseg);
			project.elts.getMot(word).posInAlign = newseg;
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
		setSilenceSegment(curdebfr, curendfr, project.words);
		setSilenceSegment(curdebfr, curendfr, project.phons);
	}
	public void clearAlignFromFrame(int fr) {
		// TODO
		throw new Error("clearAlignFromFrame: IMPLEMENT ME!");
	}



	/**
	 * Align words automatically between anchors set manually.
	 * @param progress progress dialog to refresh
	 */
	public void alignBetweenAnchors(ProgressDialog progress) {
		progress.setMessage("Aligning...");

		project.clearAlignment();

		float alignFrom = 0;
		int startWord = 0;
		int word = -1;

		byte currentSpeaker = (byte)0xff;

		Overlap currentOverlap = null;

		for (int i = 0; i < project.elts.size(); i++) {
			Element e = project.elts.get(i);

			if (e instanceof Element_Mot) {
				word++;
			} else if (e instanceof Element_Ancre) {
				float alignTo = ((Element_Ancre) e).seconds;

				if (word >= 0 && word >= startWord) {
					setAlignWord(startWord, word, alignFrom, alignTo);

					if (currentOverlap != null && currentOverlap.s2LastOverlappedWord >= 0) {
						// Find when the overlapped speech ends.
						int seg = mots.get(currentOverlap.s2LastOverlappedWord).posInAlign;
						currentOverlap.overlapEnd = TimeConverter.frame2sec(
								project.words.getSegmentEndFrame(seg));

						if (currentOverlap.overlapEnd > currentOverlap.overlapStart) {
							S4AlignOrder spk1Overlap = partialBatchAlign(
									currentOverlap.s1FirstWord,
									TimeConverter.second2frame(currentOverlap.s1StartsSpeaking),
									currentOverlap.s1LastWord,
									TimeConverter.second2frame(currentOverlap.overlapEnd));

							if (!spk1Overlap.isEmpty()) {
								project.overlaps.add(spk1Overlap);
								project.overlapSpeakers.add(currentOverlap.s1);
							}
						}

						currentOverlap = null;
					}
				}

				alignFrom = alignTo;
				startWord = word + 1;
			} else if (e instanceof Element_DebutChevauchement) {
				// Skip straight to next speaker, i.e. skip current speaker's
				// overlapped speech until next anchor
				float alignTo = -1;
				int nextWord = word;
				for (; i < project.elts.size(); i++) {
					Element e2 = project.elts.get(i);
					if (e2 instanceof Element_Ancre) {
						alignTo = ((Element_Ancre) e2).seconds;
						break;
					} else if (e2 instanceof Element_Mot) {
						nextWord++;
					}
				}
				setAlignWord(startWord, word, alignFrom, alignTo);

				// TODO assert currentOverlap == null:
				// TODO		"an overlap was already ongoing!";

				// Don't start an overlap if the first speaker's overlapped
				// section does not contain any word element.
				if (nextWord > word) {
					currentOverlap = new Overlap();

					currentOverlap.s1 = currentSpeaker;

					currentOverlap.s1FirstWord = startWord;
					currentOverlap.s1LastNonOverlappedWord = word;
					currentOverlap.s1LastWord = nextWord;

					currentOverlap.s1StartsSpeaking = alignFrom;
					currentOverlap.overlapStart = alignTo;
				}

				alignFrom = alignTo;
				word = nextWord;
				startWord = word + 1;
			} else if (e instanceof Element_FinChevauchement) {
				if (currentOverlap == null) {
					System.err.println("ERRONEOUS INPUT: no overlap is currently active!");
				} else {
					currentOverlap.s2FirstWord = startWord;
					currentOverlap.s2LastOverlappedWord = word;
				}
			} else if (e instanceof Element_Locuteur) {
				currentSpeaker = ((Element_Locuteur) e).getLocuteurID();
			}

			progress.setProgress((i+1) / (float)project.elts.size());
		}

		project.refreshIndex();
	}


	/**
	 * Aligns words automatically. Does not account for anchors.
	 * @param progress progress dialog to refresh
	 */
	public void alignRaw(ProgressDialog progress) {
		progress.setMessage("Aligning...");

		project.clearAlignment();

		int lastAlignedWord = 0;
		int previousLAW = -1;

		// Keep going until all words are aligned or the aligner gets stuck
		while (lastAlignedWord < mots.size() && lastAlignedWord > previousLAW) {
			setAlignWord(-1, mots.size()-1, -1, aligneur.audioSourceTotalFrames);

			previousLAW = lastAlignedWord;
			lastAlignedWord = getLastMotPrecAligned(mots.size()-1);

			progress.setProgress(lastAlignedWord / ((float) mots.size() - 1));
		}

		project.refreshIndex();
	}
}
