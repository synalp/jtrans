package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.elements.Element;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.facade.AutoAligner;

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
				words);

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
							turn.getWords(i));
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
	 * Any turns with incomplete timing information are chained together.
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
				// Start a chain
				chainStart = t;
			}

			else {
				// Independent turn (timing information)
				int pSpk = overlaps? -1: turn.prioritySpeaker();

				for (int i = 0; i < speakerCount(); i++) {
					if (!overlaps && i != pSpk) {
						continue;
					}
					align(aligner, turn.start, turn.end, turn.getWords(i));
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
