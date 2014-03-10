package fr.loria.synalp.jtrans.facade;

import java.io.*;
import java.util.*;

import fr.loria.synalp.jtrans.elements.*;
import fr.loria.synalp.jtrans.gui.trackview.MultiTrackTable;
import fr.loria.synalp.jtrans.speechreco.s4.Alignment;
import fr.loria.synalp.jtrans.speechreco.s4.S4AlignOrder;
import fr.loria.synalp.jtrans.speechreco.s4.S4ForceAlignBlocViterbi;
import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.utils.TimeConverter;
import fr.loria.synalp.jtrans.viterbi.StateGraph;
import fr.loria.synalp.jtrans.viterbi.SwapDeflater;
import fr.loria.synalp.jtrans.viterbi.SwapInflater;

import javax.swing.*;


/**
 * Handles automatic alignment
 */
public class AutoAligner {
	private Project project;
	private ProgressDisplay progress;
	private Track track;
	private List<Word> mots;
	private MultiTrackTable view;
	private S4ForceAlignBlocViterbi s4blocViterbi;
	private SwapDeflater swapWriter;
	private SwapInflater swapReader;

	/**
	 * Alignment algorithm types.
	 */
	public static enum Algorithm {
		/**
		 * Aligns words between anchors using linear interpolation.
		 * Very fast, but very inaccurate. For testing only.
		 */
		LINEAR_INTERPOLATION,

		/**
		 * Aligns words between anchors with Sphinx4 and the Viterbi algorithm.
		 * May fail to align every single word.
		 * The implementation is old and somewhat messy.
		 * @see fr.loria.synalp.jtrans.speechreco.s4.S4ForceAlignBlocViterbi
		 */
		FORCE_ALIGN_BLOC_VITERBI,

		/**
		 * Aligns words between anchors with Sphinx4, the Viterbi algorithm
		 * and a full backtrack stack.
		 * Every word is guaranteed to be aligned, albeit not always precisely.
		 * May use a lot of memory and/or swap space on disk.
		 * @see fr.loria.synalp.jtrans.viterbi.StateGraph
		 */
		FULL_BACKTRACK_VITERBI,
	}


	/** The algorithm to use when aligning. */
	public static Algorithm algorithm = Algorithm.FULL_BACKTRACK_VITERBI;


	/**
	 * Maximum number of bytes for a Viterbi backtrack stack to reside in
	 * memory. Above this threshold, the stack will be swapped to disk.
	 * Only applies to FULL_BACKTRACK_VITERBI.
	 */
	public static final int SWAP_THRESHOLD_BYTES = 1024*1024*16;


	/**
	 * @param view UI component to update. May be null for headless mode.
	 */
	public AutoAligner(Project project,
					   Track track,
					   ProgressDisplay progress,
					   MultiTrackTable view)
	{
		this.project = project;
		this.track = track;
		this.progress = progress;
		this.view = view;
		track.refreshIndex();
		mots = track.getWords();
	}


	private S4ForceAlignBlocViterbi getS4aligner(Track track)
	{
		// Create an array of word strings
		List<Word> wordElements = track.getWords();
		String[] wordStrings = new String[wordElements.size()];
		for (int i = 0; i < wordElements.size(); i++)
			wordStrings[i] = wordElements.get(i).toString();
		// Create the aligner
		S4ForceAlignBlocViterbi s4aligner = S4ForceAlignBlocViterbi.getS4Aligner(
				project.convertedAudioFile.getAbsolutePath(), progress);
		s4aligner.setMots(wordStrings);
		return s4aligner;
	}


	private S4AlignOrder createS4AlignOrder(int motdeb, int trdeb, int motfin, int trfin) {
		if (s4blocViterbi == null)
			s4blocViterbi = getS4aligner(track);

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
	 * It is not merged into the main alignment (use merge()).
	 */
	private S4AlignOrder partialBatchAlign(final int startWord, final int startFrame, final int endWord, final int endFrame) {
		return (S4AlignOrder)Cache.cachedObject(
				"order",
				"ser",
				new Cache.ObjectFactory() {
					public Object make() {
						return createS4AlignOrder(startWord, startFrame, endWord, endFrame);
					}
				},
				new File(project.wavname), // make it a file to take into account its modification date
				track.speakerName,
				getPhrase(startWord, endWord),
				startFrame,
				endFrame);
	}


	/**
	 * Align words between startWord and endWord using Sphinx and the revised
	 * Viterbi algorithm. Slow, and disk space hungry, but accurate.
	 *
	 * The result is not merged into the main alignment (use merge()).
	 */
	private Alignment[] partialBatchAlignNewViterbi(
			final int startWord,
			final int startFrame,
			final int endWord,
			final int endFrame)
			throws IOException, InterruptedException
	{
		//----------------------------------------------------------------------
		// Initialize heavy objects as needed

		// Only used for the MFCC buffer
		if (s4blocViterbi == null)
			s4blocViterbi = getS4aligner(track);

		if (swapWriter == null)
			swapWriter = SwapDeflater.getSensibleSwapDeflater(true);

		if (swapReader == null)
			swapReader = new SwapInflater();

		//----------------------------------------------------------------------
		// Initialize graph and swap streams

		String words = getPhrase(startWord, endWord);
		StateGraph graph = new StateGraph(words);

		boolean inRAM = (endFrame-startFrame+1)*graph.getStateCount()
				<= SWAP_THRESHOLD_BYTES;
		final OutputStream out;
		final SwapInflater.InputStreamFactory inFactory;

		if (inRAM) {
			System.out.println("Viterbi backpointers: keep in RAM");
			out = new ByteArrayOutputStream();
			inFactory = new SwapInflater.InputStreamFactory() {
				@Override
				public InputStream make() throws IOException {
					return new ByteArrayInputStream(
							((ByteArrayOutputStream)out).toByteArray());
				}
			};
		} else {
			System.out.println("Viterbi backpointers: swap to disk");
			final File swapFile = Cache.getCacheFile("backtrack", "swp",
					project.convertedAudioFile,
					track.speakerName,
					words,
					startFrame,
					endFrame);
			out = new FileOutputStream(swapFile);
			inFactory = new SwapInflater.InputStreamFactory() {
				@Override
				public InputStream make() throws IOException {
					return new FileInputStream(swapFile);
				}
			};
		}

		//----------------------------------------------------------------------
		// Run alignment

		swapWriter.init(graph.getStateCount(), out);
		graph.viterbi(s4blocViterbi.mfccs, swapWriter, startFrame, endFrame);

		swapReader.init(swapWriter.getIndex(), inFactory);
		return graph.getAlignments(swapReader, startFrame);
	}


	/**
	 * Merge a partial alignment into the main alignment.
	 */
	private void merge(
			Alignment wordAl,
			Alignment phonAl,
			int startWord,
			int endWord)
	{
		if (wordAl == null) {
			return;
		}

		String[] alignedWords = new String[1 + endWord - startWord];
		for (int i = 0; i < 1+endWord-startWord; i++)
			alignedWords[i] = mots.get(i + startWord).toString();
		int[] wordSegments = wordAl.matchWithText(alignedWords);

		// Merge word segments into the main word alignment
		int firstSegment = track.words.merge(wordAl);

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
		track.phons.merge(phonAl);
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
					mots.get(i).toString(), startFrame, (int)currEndFrame);

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
	private void setAlignWord(int startWord, final int word, int startFrame, int endFrame) {
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

		// There are unaligned words before `word`; align them.
		switch (algorithm) {
			case LINEAR_INTERPOLATION:
				linearAlign(startWord, startFrame, word, endFrame);
				break;

			case FORCE_ALIGN_BLOC_VITERBI:
			{
				S4AlignOrder order = partialBatchAlign(
						startWord, startFrame, word, endFrame);
				merge(order.alignWords, order.alignPhones, startWord, word);
				break;
			}

			case FULL_BACKTRACK_VITERBI:
			{
				Alignment[] alignments;
				try {
					alignments = partialBatchAlignNewViterbi(
							startWord, startFrame, word, endFrame);
				} catch (Exception ex) {
					throw new Error(ex);
				}
				merge(alignments[0], alignments[1], startWord, word);
				break;
			}

			default:
				throw new Error("Unknown algorithm: " + algorithm);
		}
	}


	private void setAlignWord(int startWord, int endWord, float startSecond, float endSecond) {
		int startFrame = TimeConverter.second2frame(startSecond);
		int endFrame   = TimeConverter.second2frame(endSecond);
		setAlignWord(startWord, endWord, startFrame, endFrame);
	}


	private int getLastMotPrecAligned(int midx) {
		for (int i=midx;i>=0;i--) {
			if (mots.get(i).posInAlign>=0) return i;
		}
		return -1;
	}


	/**
	 * Returns a string of all words between the given indices (inclusive),
	 * joined by the space character.
	 */
	public String getPhrase(int startWord, int endWord) {
		StringBuilder phrase = new StringBuilder();
		String prefix = "";
		for (int i = startWord; i <= endWord; i++) {
			phrase.append(prefix).append(mots.get(i));
			prefix = " ";
		}
		return phrase.toString();
	}


	/**
	 * Align words automatically between anchors set manually.
	 */
	public void alignBetweenAnchors() {
		progress.setIndeterminateProgress("Track \"" + track.speakerName
				+ "\": starting alignment...");

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

			progress.setProgress(String.format(
					"Track \"%s\": aligning element %d of %d...",
					track.speakerName, i+1, track.elts.size()),
					(i+1) / (float)track.elts.size());

			// Update GUI
			if (view != null) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						view.repaint();
					}
				});
			}
		}

		// Align any remaining words until the end of the audio file
		if (word >= 0 && word >= startWord) {
			setAlignWord(startWord, word, alignFrom,
					TimeConverter.frame2sec((int)project.audioSourceTotalFrames));
		}

		track.refreshIndex();
	}


	/**
	 * Aligns words automatically. Does not account for anchors.
	 */
	public void alignRaw() {
		progress.setIndeterminateProgress("Track \"" + track.speakerName
				+ "\": aligning...");

		track.clearAlignment();

		int lastAlignedWord = 0;
		int previousLAW = -1;

		// Keep going until all words are aligned or the aligner gets stuck
		while (lastAlignedWord < mots.size() && lastAlignedWord > previousLAW) {
			setAlignWord(-1, mots.size()-1, -1, project.audioSourceTotalFrames);

			previousLAW = lastAlignedWord;
			lastAlignedWord = getLastMotPrecAligned(mots.size()-1);

			progress.setProgress(String.format(
					"Track \"%s\": aligning element %d of %d...",
					track.speakerName, lastAlignedWord+1, track.elts.size()),
					(lastAlignedWord+1) / (float)track.elts.size());
		}

		track.refreshIndex();
	}
}
