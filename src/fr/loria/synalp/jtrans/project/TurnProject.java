package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.align.AutoAligner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Strictly turn-based project.
 */
public class TurnProject extends Project {

	public static boolean ALIGN_OVERLAPS = true;

	public List<Turn> turns = new ArrayList<>();


	@Override
	public Iterator<Phrase> phraseIterator(int speaker) {
		return new TurnPhraseIterator(speaker);
	}

	protected class TurnPhraseIterator implements Iterator<Phrase> {
		final int spkID;
		final Iterator<Turn> rowItr;

		protected TurnPhraseIterator(int spkID) {
			this.spkID = spkID;
			rowItr = turns.iterator();
		}

		@Override
		public boolean hasNext() {
			return rowItr.hasNext();
		}

		@Override
		public Phrase next() {
			Turn cTurn = rowItr.next();
			// TODO skip empty?
			return new Phrase(cTurn.start, cTurn.end, cTurn.elts.get(spkID));
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public class Turn {
		public Anchor start;
		public Anchor end;
		public List<List<Element>> elts;

		public Turn() {
			elts = new ArrayList<>();
			for (int i = 0; i < speakerCount(); i++) {
				elts.add(new ArrayList<Element>());
			}
		}

		public void add(int speaker, Element e) {
			elts.get(speaker).add(e);
		}

		public void addAll(int speaker, List<Element> list) {
			elts.get(speaker).addAll(list);
		}

		public List<Word> getWords(int spkID) {
			List<Word> words = new ArrayList<>();
			for (Element e: elts.get(spkID)) {
				if (e instanceof Word) {
					words.add((Word)e);
				}
			}
			return words;
		}

		public float[] getMinMax() {
			float earliest = Float.MAX_VALUE;
			float latest = Float.MIN_VALUE;

			for (int i = 0; i < speakerCount(); i++) {
				for (Word w: getWords(i)) {
					if (w.isAligned()) {
						Word.Segment seg = w.getSegment();
						earliest = Math.min(earliest, seg.getStartSecond());
						latest   = Math.max(latest  , seg.getEndSecond());
					}
				}
			}

			if (earliest == Float.MAX_VALUE || latest == Float.MIN_VALUE) {
				return null;
			} else {
				return new float[]{earliest, latest};
			}
		}

		/**
		 * Returns the ID of the most important speaker in this turn.
		 * <p/>
		 * When aligning without overlapping speech, at most one speaker's
		 * speech is kept in each turn. We have to arbitrarily select a single
		 * priority speaker and ignore the speech of all other speakers.
		 * <p/>
		 * This method returns the ID of the speaker who utters the most words
		 * in the turn. In case of a tie, returns the first tied speaker.
		 */
		public int prioritySpeaker() {
			int maxWordCount = 0;
			int maxSpeaker = -1;

			for (int spk = 0; spk < elts.size(); spk++) {
				int wordCount = getWords(spk).size();
				if (wordCount > maxWordCount) {
					maxWordCount = wordCount;
					maxSpeaker = spk;
				}
			}

			return maxSpeaker;
		}

		public void clearAlignment() {
			for (int i = 0; i < speakerCount(); i++) {
				for (Word w: getWords(i)) {
					w.clearAlignment();
				}
			}
		}

		public boolean isEmpty() {
			for (List<Element> elList: elts) {
				if (!elList.isEmpty()) {
					return false;
				}
			}
			return true;
		}
	}


	@Override
	public List<Word> getWords(int speaker) {
		List<Word> words = new ArrayList<>();
		for (Turn r: turns) {
			for (Element e: r.elts.get(speaker)) {
				if (e instanceof Word) {
					words.add((Word)e);
				}
			}
		}
		return words;
	}


	public Turn newTurn() {
		Turn r = new Turn();
		turns.add(r);
		return r;
	}


	public int newSpeaker(String name) {
		for (Turn r: turns) {
			assert r.elts.size() == speakerNames.size();
			r.elts.add(new ArrayList<Element>());
		}

		int id = speakerNames.size();
		speakerNames.add(name);

		return id;
	}


	/**
	 * Aligns a chain of turns lacking timing information.
	 * <p/>
	 * Time boundaries for the alignment are defined by the initial anchor in
	 * the first turn and the final anchor in the last turn of the chain.
	 * These two anchors may be null, in which case the chain will be aligned
	 * throughout the length of the entire audio file.
	 * <p/>
	 * Besides the two aforementioned initial and final anchors, <strong>all
	 * other anchors in the chain list must be null</strong>.
	 * <p/>
	 * The alignment is done in two passes. First, a linear word sequence
	 * without overlapping speech is aligned. Then, the overlapping speech is
	 * aligned with timing information inferred from the first pass.
	 * @param aligner
	 * @param turns chain of turns lacking timing information
	 * @param overlaps If false, skip the second pass (don't align overlapping
	 *                 speech)
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void alignTurnChain(
			AutoAligner aligner,
			List<Turn> turns,
			boolean overlaps)
			throws IOException, InterruptedException
	{
		// Clear
		for (Turn turn: turns) {
			turn.clearAlignment();
		}

		// Big interleaved sequence
		List<Word> words = new ArrayList<>();
		for (Turn turn: turns) {
			assert null == turn.start || turn == turns.get(0);
			assert null == turn.end   || turn == turns.get(turns.size()-1);

			int pSpk = turn.prioritySpeaker();
			if (pSpk >= 0) {
				words.addAll(turn.getWords(pSpk));
			}
		}

		align(aligner,
				turns.get(0).start,
				turns.get(turns.size()-1).end,
				words,
				true); // priority, concatenate

		if (overlaps) {
			for (Turn turn: turns) {
				int pSpk = turn.prioritySpeaker();
				float[] minMax = turn.getMinMax();
				if (null == minMax) {
					continue;
				}

				for (int i = 0; i < turn.elts.size(); i++) {
					if (i == pSpk) {
						continue;
					}
					align(aligner,
							new Anchor(minMax[0]),
							new Anchor(minMax[1]),
							turn.getWords(i),
							false); // non-priority, don't concatenate
				}
			}
		}
	}


	public void align(AutoAligner aligner)
			throws IOException, InterruptedException
	{
		align(aligner, ALIGN_OVERLAPS);
	}


	/**
	 * Aligns all turns.
	 * <p/>
	 * Contiguous turns that lack timing information are "chained" together
	 * and aligned together as if they were one single, long turn.
	 * <p/>
	 * Turns with complete timing information are aligned independently.
	 *
	 * @see TurnProject#alignTurnChain
	 */
	public void align(AutoAligner aligner, boolean overlaps)
			throws IOException, InterruptedException
	{
		clearAlignment();

		// Index of the first turn in the current chain of turns lacking
		// timing information
		int chainStart = -1;

		for (int t = 0; t < turns.size(); t++) {
			Turn turn = turns.get(t);

			if (chainStart >= 0) {
				assert null == turn.start;
				if (null != turn.end || t == turns.size()-1) {
					// Stop chaining
					alignTurnChain(aligner,
							turns.subList(chainStart, t + 1), overlaps);
					chainStart = -1;
				}
				// Otherwise, keep chaining
			}

			else if (null == turn.end) {
				// chainStart < 0, no valid timing information
				// Start a new chain
				chainStart = t;
			}

			else {
				// chainStart < 0, valid timing information
				// Independent turn (i.e. has complete timing information)
				// Don't start a chain

				// priority speaker ID
				int pSpk = overlaps? -1: turn.prioritySpeaker();

				for (int i = 0; i < speakerCount(); i++) {
					if (!overlaps && i != pSpk) {
						continue;
					}
					align(aligner, turn.start, turn.end, turn.getWords(i),
							i == pSpk); // only concatenate if priority
				}
			}
		}

		assert chainStart < 0;
	}


	public void clearAnchorTimes() {
		for (Turn turn: turns) {
			turn.start = null;
			turn.end = null;
		}
	}


	/**
	 * Infer anchor timing from timing extrema in each aligned turn.
	 * @return number of inferred anchors
	 */
	public int inferAnchors() {
		int inferred = 0;

		for (Turn t: turns) {
			float[] minMax = t.getMinMax();
			if (null != minMax) {
				t.start = new Anchor(minMax[0]);
				t.end   = new Anchor(minMax[1]);
				inferred++;
			}
		}

		return inferred;
	}

}
