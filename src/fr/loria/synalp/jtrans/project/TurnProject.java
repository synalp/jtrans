package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.align.Aligner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Strictly turn-based project.
 */
public class TurnProject extends Project {

    /** virtual speaker for empty turns 
	keep in mind to export his track properly
     */
    public final static String hackNobody = "*Hack-Nobody*";

    public static boolean ALIGN_OVERLAPS = true;

    public List<Turn> turns = new ArrayList<>();

    /* the original XML Document */
    public org.w3c.dom.Document document;

	@Override
	public Iterator<Phrase> phraseIterator(int speaker) {
		return new TurnPhraseIterator(speaker);
	}

	@Override
	public List<Token> getTokens(int speaker) {
		List<Token> tokens = new ArrayList<>();
		for (Turn turn: turns) {
			tokens.addAll(turn.spkTokens.get(speaker));
		}
		return tokens;
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
			return new Phrase(cTurn.start, cTurn.end, cTurn.spkTokens.get(spkID));
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public class Turn {
		public Anchor start;
	    public Anchor end;
		/** Tokens per speaker */
		public List<List<Token>> spkTokens;

		public Turn() {
			spkTokens = new ArrayList<>();
			for (int i = 0; i < speakerCount(); i++) {
				spkTokens.add(new ArrayList<Token>());
			}
		}

		public void add(int speaker, Token token) {
			token.setSpeaker(speaker);
			spkTokens.get(speaker).add(token);
		}

		public void addAll(int speaker, List<Token> list) {
			for (Token token: list) {
				token.setSpeaker(speaker);
			}
			spkTokens.get(speaker).addAll(list);
		}

		public float[] getMinMax() {
			float earliest = Float.MAX_VALUE;
			float latest = Float.MIN_VALUE;

			for (int i = 0; i < speakerCount(); i++) {			 
				for (Token w: spkTokens.get(i)) {
					if (w.isAligned()) {
						Token.Segment seg = w.getSegment();
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

			for (int spk = 0; spk < spkTokens.size(); spk++) {
				int wordCount = 0;
				for (Token token: spkTokens.get(spk)) {
					if (token.isAlignable()) {
						wordCount++;
					}
				}

				if (wordCount > maxWordCount) {
					maxWordCount = wordCount;
					maxSpeaker = spk;
				}
			}

			return maxSpeaker;
		}

		public boolean isEmpty() {
			for (List<Token> tokens: spkTokens) {
				if (!tokens.isEmpty()) {
					return false;
				}
			}
			return true;
		}

		public boolean isPartiallyAligned() {
			for (List<Token> tokens: spkTokens) {
				for (Token token: tokens) {
					if (token.isAligned()) {
						return true;
					}
				}
			}
			return false;
		}
	}


	public Turn newTurn() {
		Turn t = new Turn();
		turns.add(t);
		return t;
	}


	public int newSpeaker(String name) {
		for (Turn t: turns) {
			assert t.spkTokens.size() == speakerNames.size();
			t.spkTokens.add(new ArrayList<Token>());
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
	protected static void alignTurnChain(
			Aligner aligner,
			List<Turn> turns,
			boolean overlaps,
			Aligner reference)
			throws IOException, InterruptedException
	{
		// Big interleaved sequence
		List<Token> words = new ArrayList<>();
		for (Turn turn: turns) {
			assert !turn.isPartiallyAligned();
			assert null == turn.start || turn == turns.get(0);
			assert null == turn.end   || turn == turns.get(turns.size()-1);

			int pSpk = turn.prioritySpeaker();
			if (pSpk >= 0) {
				words.addAll(turn.spkTokens.get(pSpk));
			}
		}

		aligner.align(
				turns.get(0).start,
				turns.get(turns.size()-1).end,
				words,
				reference);

		if (overlaps) {
			for (Turn turn: turns) {
				int pSpk = turn.prioritySpeaker();
				float[] minMax = turn.getMinMax();
				if (null == minMax) {
					continue;
				}

				for (int i = 0; i < turn.spkTokens.size(); i++) {
					if (i == pSpk) {
						continue;
					}
					aligner.align(
							new Anchor(minMax[0]),
							new Anchor(minMax[1]),
							turn.spkTokens.get(i),
							reference);
				}
			}
		}
	}


	@Override
	public void align(Aligner aligner, Aligner reference)
			throws IOException, InterruptedException
	{
		align(aligner, ALIGN_OVERLAPS, reference);
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
	public void align(Aligner aligner, boolean overlaps, Aligner reference)
			throws IOException, InterruptedException
	{
		clearAlignment();

		if (turns.isEmpty()) {
			return;
		}

		// Index of the first turn in the current chain of turns lacking
		// timing information
		int chainStart = -1;

		// Don't process last turn!
		for (int t = 0; t < turns.size()-1; t++) {
			Turn turn = turns.get(t);

			if (chainStart >= 0) {
				assert null == turn.start;
				if (null != turn.end) {
					// Stop chaining
					alignTurnChain(aligner,
							turns.subList(chainStart, t + 1),
							overlaps,
							reference);
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
					aligner.align(
							turn.start, turn.end,
							turn.spkTokens.get(i),
							reference);
				}
			}
		}

		// Last turn
		if (chainStart < 0) {
			chainStart = turns.size()-1;
		}
		alignTurnChain(aligner,
				turns.subList(chainStart, turns.size()),
				overlaps,
				reference);
		chainStart = -1;
	}


	/**
	 * Clears all anchors except for the first and the last.
	 * <p/>
	 * The first and the last anchors are kept to ease the anchorless alignment
	 * process with partial transcriptions that do not cover the entire audio
	 * source.
	 */
	public void clearAnchorTimes() {
		if (turns.size() <= 1) {
			return;
		}

		turns.get(0).end = null;
		turns.get(turns.size()-1).start = null;

		for (int i = 1; i < turns.size()-1; i++) {
			Turn turn = turns.get(i);
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
