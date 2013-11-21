package facade;

import java.io.*;
import java.util.*;

import plugins.applis.SimpleAligneur.Aligneur;
import plugins.speechreco.aligners.sphiinx4.Alignment;
import plugins.speechreco.aligners.sphiinx4.S4AlignOrder;
import plugins.speechreco.aligners.sphiinx4.S4ForceAlignBlocViterbi;
import plugins.text.ListeElement;
import plugins.text.TexteEditor;
import plugins.text.elements.*;
import utils.ProgressDialog;

import javax.sound.sampled.*;

public class JTransAPI {
	/**
	 * Align words between anchors using linear interpolation (a.k.a.
	 * "equialign") instead of proper Sphinx alignment (batchAlign).
	 * Setting this flag to `true` yields very fast albeit inaccurate results.
	 */
	private static final boolean USE_LINEAR_ALIGNMENT = false;

	/**
	 * Target audio format. Any input audio files that do not match this format
	 * will be converted to it before being processed.
	 */
	private static final AudioFormat SUITABLE_AUDIO_FORMAT =
			new AudioFormat(16000, 16, 1, true, false);
	
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

	public static float frame2sec(int fr) {
		return (float)frame2millisec(fr)/1000f;
	}

	public static long frame2millisec(int fr) {
		// window = 25ms, donc milieu = 12ms
		return fr*10+12;
	}

	public static int millisec2frame(long ms) {
		return (int)((ms-12)/10);
	}

	public static int second2frame(float sec) {
		int fr = (int)(sec*100f);
		return fr;
	}

	/**
	 * Return an audio file in a suitable format for JTrans. If the original
	 * file isn't in the right format, convert it and cache it.
	 */
	public static File suitableAudioFile(final File original) {
		AudioFormat af;

		try {
			 af = AudioSystem.getAudioFileFormat(original).getFormat();
		} catch (UnsupportedAudioFileException ex) {
			ex.printStackTrace();
			return original;
		} catch (IOException ex) {
			ex.printStackTrace();
			return original;
		}

		if (af.matches(SUITABLE_AUDIO_FORMAT)) {
			System.out.println("suitableAudioFile: no conversion needed!");
			return original;
		}

		System.out.println("suitableAudioFile: need conversion, trying to get one from the cache");

		Cache.FileFactory factory = new Cache.FileFactory() {
			public void write(File f) throws IOException {
				System.out.println("suitableAudioFile: no cache found... creating one");

				AudioInputStream originalStream;
				try {
					 originalStream = AudioSystem.getAudioInputStream(original);
				} catch (UnsupportedAudioFileException ex) {
					ex.printStackTrace();
					throw new Error("Unsupported audio file; should've been caught above!");
				}

				AudioSystem.write(
						AudioSystem.getAudioInputStream(SUITABLE_AUDIO_FORMAT, originalStream),
						AudioFileFormat.Type.WAVE,
						f);
			}
		};

		return Cache.cachedFile("converted.wav", factory, original);
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
	public static S4AlignOrder partialBatchAlign(final int startWord, final int startFrame, final int endWord, final int endFrame) {
		System.out.println("batch align "+startWord+"-"+endWord+" "+startFrame+":"+endFrame);

		if (s4blocViterbi==null) {
			String[] amots = new String[mots.size()];
			for (int i=0;i<mots.size();i++) {
				amots[i] = mots.get(i).getWordString();
			}
			s4blocViterbi = S4ForceAlignBlocViterbi.getS4Aligner(aligneur.convertedAudioFile.getAbsolutePath());
			s4blocViterbi.setMots(amots);
		}

		return (S4AlignOrder)Cache.cachedObject(
				String.format("%05d_%05d_%05d_%05d.order", startWord, startFrame, endWord, endFrame),
				new Cache.ObjectFactory() {
					public Object make() {
						return createS4AlignOrder(startWord, startFrame, endWord, endFrame);
					}
				},
				aligneur.originalAudioFile,
				edit.getText());
	}

	/**
	 * Merge an S4AlignOrder into the main alignment.
	 */
	public static void mergeOrder(S4AlignOrder order, int startWord, int endWord) {
		if (order.alignWords != null) {
			System.out.println("================================= ALIGN FOUND");
			System.out.println(order.alignWords.toString());

			String[] alignedWords = new String[1 + endWord - startWord];
			for (int i = 0; i < 1+endWord-startWord; i++)
				alignedWords[i] = mots.get(i + startWord).getWordString();
			int[] wordSegments = order.alignWords.matchWithText(alignedWords);

			// Merge word segments into the main word alignment
			int firstSegment = alignementWords.merge(order.alignWords);

			// Adjust posInAlign for word elements (Element_Mot)
			for (int i = 0; i < wordSegments.length; i++) {
				int idx = wordSegments[i];

				// Offset if we have a valid segment index
				if (idx >= 0)
					idx += firstSegment;

				mots.get(i + startWord).posInAlign = idx;
			}
			elts.refreshIndex();

			// Merge phoneme segments into the main phoneme alignment
			alignementPhones.merge(order.alignPhones);
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
	public static void linearAlign(int startWord, int startFrame, int endWord, int endFrame) {
		float frameDelta = ((float)(endFrame-startFrame))/((float)(endWord-startWord+1));
		float currEndFrame = startFrame + frameDelta;

		assert frameDelta >= 1f:
				"can't align on fractions of frames! (frameDelta=" + frameDelta + ")";

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
	 * @param startWord number of the first word to align. If < 0, use last aligned word before `word`.
	 * @param word number of the last word to align
	 * @param startFrame can be < 0, in which case use the last aligned word.
	 * @param endFrame
	 */
	public static void setAlignWord(int startWord, int word, int startFrame, int endFrame) {
		assert endFrame >= 0;

		if (startWord < 0) {
			int lastAlignedWord = getLastMotPrecAligned(word);

			if (lastAlignedWord <= 0) {
				// Nothing is aligned yet; start aligning from the beginning.
				startWord = 0;
				startFrame = 0;
			} else {
				startWord = lastAlignedWord + 1;

				// Lagging behind the alignment - wait for a couple more words
				if (startWord > word)
					return;

				if (startFrame < 0) {
					// Start aligning at the end frame of the last aligned word.
					int lastAlignedWordSeg = mots.get(lastAlignedWord).posInAlign;
					startFrame = alignementWords.getSegmentEndFrame(lastAlignedWordSeg);
				}
			}
		}

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

	public static void setAlignWord(int startWord, int endWord, float startSecond, float endSecond) {
		int startFrame = second2frame(startSecond);
		int endFrame   = second2frame(endSecond);
		setAlignWord(startWord, endWord, startFrame, endFrame);
	}

	private static void setSilenceSegment(int curdebfr, int curendfr, Alignment al) {
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
		int curdebfr = second2frame(secdeb);
		int curendfr = second2frame(secfin);
		setSilenceSegment(curdebfr, curendfr, alignementWords);
		setSilenceSegment(curdebfr, curendfr, alignementPhones);
	}
	public static void clearAlignFromFrame(int fr) {
		// TODO
		throw new Error("clearAlignFromFrame: IMPLEMENT ME!");
	}
	
	// =========================
	// variables below are duplicate (point to) of variables in the mess of the rest of the code...
	private static ListeElement elts =  null;
	public static Alignment alignementWords = null;
	public static Alignment alignementPhones = null;
	public static ArrayList<S4AlignOrder> overlaps = new ArrayList<S4AlignOrder>();
	public static ArrayList<Byte> overlapSpeakers = new ArrayList<Byte>();
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
			if (mots.get(i).posInAlign>=0) return i;
		}
		return -1;
	}

	/**
	 * Align words automatically between anchors set manually.
	 * @param progress progress dialog to refresh
	 */
	public static void alignBetweenAnchors(ProgressDialog progress) {
		progress.setMessage("Aligning...");

		float alignFrom = 0;
		int startWord = 0;
		int word = -1;

		byte currentSpeaker = (byte)0xff;

		class Overlap {
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

		Overlap currentOverlap = null;

		for (int i = 0; i < elts.size(); i++) {
			Element e = elts.get(i);

			if (e instanceof Element_Mot) {
				word++;
			} else if (e instanceof Element_Ancre) {
				float alignTo = ((Element_Ancre) e).seconds;

				if (word >= 0 && word > startWord) {
					setAlignWord(startWord, word, alignFrom, alignTo);

					if (currentOverlap != null && currentOverlap.s2LastOverlappedWord >= 0) {
						// Find when the overlapped speech ends.
						int seg = mots.get(currentOverlap.s2LastOverlappedWord).posInAlign;
						currentOverlap.overlapEnd = frame2sec(
								alignementWords.getSegmentEndFrame(seg));

						if (currentOverlap.overlapEnd > currentOverlap.overlapStart) {
							System.out.println("Overlap: previous speaker starts speaking @"
									+ currentOverlap.s1StartsSpeaking
									+ ", gets overlapped @"
									+ currentOverlap.overlapStart
									+ ", stops speaking @"
									+ currentOverlap.overlapEnd);

							S4AlignOrder spk1Overlap = partialBatchAlign(
									currentOverlap.s1FirstWord,
									second2frame(currentOverlap.s1StartsSpeaking),
									currentOverlap.s1LastWord,
									second2frame(currentOverlap.overlapEnd));

							if (!spk1Overlap.isEmpty()) {
								overlaps.add(spk1Overlap);
								overlapSpeakers.add(currentOverlap.s1);
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
				for (; i < elts.size(); i++) {
					Element e2 = elts.get(i);
					if (e2 instanceof Element_Ancre) {
						alignTo = ((Element_Ancre) e2).seconds;
						break;
					} else if (e2 instanceof Element_Mot) {
						nextWord++;
					}
				}
				setAlignWord(startWord, word, alignFrom, alignTo);

				// TODO
				// TODO assert currentOverlap == null:
				// TODO		"an overlap was already ongoing!";

				currentOverlap = new Overlap();

				currentOverlap.s1 = currentSpeaker;

				currentOverlap.s1FirstWord = startWord;
				currentOverlap.s1LastNonOverlappedWord = word;
				currentOverlap.s1LastWord = nextWord;

				currentOverlap.s1StartsSpeaking = alignFrom;
				currentOverlap.overlapStart = alignTo;

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

			progress.setProgress((i+1) / (float)elts.size());
		}

		progress.setMessage("Finishing up...");
		progress.setIndeterminate(true);

		aligneur.caretSensible = true;

		// force la construction de l'index
		alignementWords.clearIndex();
		alignementWords.getSegmentAtFrame(0);
		alignementPhones.clearIndex();
		alignementPhones.getSegmentAtFrame(0);
		elts.refreshIndex();
	}
}
