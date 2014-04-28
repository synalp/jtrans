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

		private Turn() {
			elts = new ArrayList<>();
			for (int i = 0; i < speakerNames.size(); i++) {
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
	 * Aligns all words in all tracks of this project with timeless anchors.
	 */
	public void alignWithoutTimes(AutoAligner aligner)
			throws IOException, InterruptedException
	{
		// Clear existing alignment
		clearAlignment();

		// Clear anchor times
		for (Turn turn: turns) {
			turn.start = null;
			turn.end = null;
		}

		// Big interleaved sequence
		List<Word> words = new ArrayList<>();
		for (Turn turn: turns) {
			int pSpk = turn.prioritySpeaker();
			if (pSpk >= 0) {
				words.addAll(turn.getWords(pSpk));
			}
		}

		align(aligner, null, null, words);

		deduceAnchors();

		// Align yet-unaligned overlaps
		if (ALIGN_OVERLAPS) {
			for (Turn turn: turns) {
				int pSpk = turn.prioritySpeaker();
				float[] minMax = turn.getMinMax();
				if (null == minMax) {
					continue;
				}

				for (int i = 0; i < speakerCount(); i++) {
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


	public void align(AutoAligner aligner, boolean overlaps)
			throws IOException, InterruptedException
	{
		clearAlignment();

		for (Turn t : turns) {
			int pSpk = overlaps? -1: t.prioritySpeaker();

			for (int i = 0; i < speakerCount(); i++) {
				if (!overlaps && i == pSpk) {
					continue;
				}

				align(aligner, t.start, t.end, t.getWords(i));
			}
		}
	}


	public void deduceAnchors() {
		for (Turn t: turns) {
			float[] minMax = t.getMinMax();
			if (null != minMax) {
				t.start = new Anchor(minMax[0]);
				t.end   = new Anchor(minMax[1]);
			}
		}
	}

}
