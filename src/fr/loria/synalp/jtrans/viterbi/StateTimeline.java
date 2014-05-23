package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.linguist.acoustic.HMMState;
import fr.loria.synalp.jtrans.elements.Word;

import java.util.ArrayList;
import java.util.List;

public class StateTimeline {

	// TODO: easier offset management


	protected static class Segment {
		int length;
		final HMMState state;
		final Word word;

		public Segment(HMMState state, Word word) {
			length = 1;
			this.state = state;
			this.word = word;
		}

		public String getUnit() {
			return state.getHMM().getBaseUnit().getName();
		}
	}


	List<Segment> segments;
	List<Word> uniqueWords;
	int frames;


	public StateTimeline() {
		segments = new ArrayList<>();
		uniqueWords = new ArrayList<>();
		frames = 0;
	}


	public StateTimeline(StateTimeline other) {
		segments = new ArrayList<>(other.segments);
		uniqueWords = new ArrayList<>(other.uniqueWords);
		frames = other.frames;
	}


	private Segment getLastSegment() {
		if (!segments.isEmpty()) {
			return segments.get(segments.size()-1);
		} else {
			return null;
		}
	}


	/**
	 *
	 * @param state
	 * @param word unique reference!
	 */
	public void newFrame(HMMState state, Word word) {
		Segment lastSegment = getLastSegment();
		if (null != lastSegment && lastSegment.state == state && lastSegment.word == word) {
			lastSegment.length++;
		} else {
			segments.add(new Segment(state, word));
		}

		if (uniqueWords.isEmpty() || uniqueWords.get(uniqueWords.size()-1) != word) {
			uniqueWords.add(word);
		}

		frames++;
	}


	public int getLength() {
		return frames;
	}


	public List<Word> getUniqueWords() {
		return uniqueWords;
	}


	public HMMState getStateAtFrame(int frame) {
		return getSegmentAtFrame(frame).state;
	}


	public Segment getSegmentAtFrame(int frame) {
		int total = 0;
		for (Segment seg: segments) {
			if (frame >= total && frame < total+seg.length) {
				return seg;
			}
			total += seg.length;
		}
		return null;
	}


	public void setWordAlignments(int offset) {
		for (Word w: uniqueWords) {
			if (w != null) {
				w.clearAlignment();
			}
		}

		Word pWord = null;        // previous word
		Word.Phone phone = null;  // current phone
		String pUnit = null;
		int segStart = offset; // absolute frame number

		for (Segment seg: segments) {
			int segEnd = segStart + seg.length - 1;
			assert segStart <= segEnd;

			Word word = seg.word;
			if (word != pWord) {
				word.setSegment(segStart, segEnd);
				pWord = word;
			} else if (null != word) {
				word.getSegment().setEndFrame(segEnd);
			}

			String unit = seg.getUnit();
			if (pUnit == null || !unit.equals(pUnit)) {
				if (null != word) {
					phone = new Word.Phone(
							unit, new Word.Segment(segStart, segEnd));
					word.addPhone(phone);
				}
				pUnit = unit;
			} else if (null != phone) {
				phone.getSegment().setEndFrame(segEnd);
			}

			segStart += seg.length;
		}
	}

}
