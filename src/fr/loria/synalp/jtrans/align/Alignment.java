package fr.loria.synalp.jtrans.align;

import edu.cmu.sphinx.linguist.acoustic.HMMState;
import fr.loria.synalp.jtrans.project.Word;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Timeline of HMM states.
 */
public class Alignment implements Iterable<Alignment.Segment> {

	public static class Segment {
		int length;
		public final HMMState state;
		public final Word word;

		public Segment(HMMState state, Word word, int length) {
			this.length = length;
			this.state = state;
			this.word = word;
		}

		public Segment(HMMState state, Word word) {
			this(state, word, 1);
		}

		public Segment(Segment other) {
			length = other.length;
			state = other.state;
			word = other.word;
		}

		public String getUnit() {
			return state.getHMM().getBaseUnit().getName();
		}

		public boolean isPadding() {
			return null == state || null == word;
		}
	}


	List<Segment> segments;
	List<Word> uniqueWords;
	int frames;
	final int frameOffset;


	/**
	 * Constructs an empty alignment.
	 * @param frameOffset the first segment will be appended at this frame
	 *                    number
	 */
	public Alignment(int frameOffset) {
		segments = new ArrayList<>();
		uniqueWords = new ArrayList<>();
		frames = 0;
		this.frameOffset = frameOffset;
	}


	/**
	 * Deep-copies segments, shallow-copies unique words.
	 */
	public Alignment(Alignment other) {
		segments = new ArrayList<>(other.segments.size());
		for (Segment seg: other.segments) {
			segments.add(new Segment(seg));
		}

		uniqueWords = new ArrayList<>(other.uniqueWords);
		frames = other.frames;
		frameOffset = other.frameOffset;

		assert verify();
	}


	/**
	 * Pads the end of this alignment ith a padding segment so that
	 * {@code length + offset} reaches {@code tillFrame}.
	 * @throws IllegalStateException
	 * @return padding segment length
	 */
	public int pad(int tillFrame) {
		int curLastFrame = frameOffset + frames;

		if (tillFrame == curLastFrame) {
			return 0;
		}

		if (tillFrame < curLastFrame) {
			throw new IllegalStateException(String.format(
					"current frame position (%d) " +
					"exceeds requested padding frame number (%d)",
					curLastFrame, tillFrame));
		}

		int delta = tillFrame - curLastFrame;
		segments.add(new Segment(null, null, delta));
		frames += delta;

		assert verify();

		return delta;
	}


	/**
	 * Appends another timeline to the end of this timeline.
	 * <p/>Use {@link #pad} before invoking this method for the concatenation
	 * to be chronologically correct.
	 */
	public void concatenate(Alignment other) {
		for (Segment seg: other.segments) {
			segments.add(new Segment(seg));
		}

		frames += other.frames;
		uniqueWords.addAll(other.uniqueWords);

		assert verify();
	}


	/**
	 * Extends this timeline by a single frame at the end.
	 * <p/>Extends the last segment by 1 frame if the same state/word combo is
	 * prolonged, otherwise creates a new segment.
	 * @param word unique reference!
	 */
	public void newFrame(HMMState state, Word word) {
		Segment tail = segments.isEmpty()?
				null: segments.get(segments.size()-1);

		if (null != tail && tail.state == state && tail.word == word) {
			tail.length++;
		} else {
			segments.add(new Segment(state, word));
		}

		frames++;
		newWord(word);
		assert verify();
	}


	/**
	 * Appends a new segment to the end of this timeline.
	 * @param state Must be different from the state in the last segment.
	 *              To extend the last segment with the same state, use
	 *              {@link #newFrame(HMMState, Word)} instead
	 * @param word
	 * @param length length of the new segment, in frames
	 */
	public void newSegment(HMMState state, Word word, int length) {
		assert segments.isEmpty() ||
				state != segments.get(segments.size()-1).state
				: "same state across two segments";

		segments.add(new Segment(state, word, length));
		frames += length;
		newWord(word);
		assert verify();
	}


	/**
	 * Appends a word to the list of unique words if the word isn't already the
	 * last word in the list.
	 */
	private void newWord(Word word) {
		Word lastUWord = uniqueWords.isEmpty()?
				null: uniqueWords.get(uniqueWords.size()-1);
		if (null != word && lastUWord != word) {
			uniqueWords.add(word);
		}
	}


	public int getFrameOffset() {
		return frameOffset;
	}


	public int getLength() {
		return frames;
	}


	public int getSegmentCount() {
		return segments.size();
	}


	public int getUniqueWordCount() {
		return uniqueWords.size();
	}


	public List<Word> getUniqueWords() {
		return uniqueWords;
	}


	public HMMState getStateAtFrame(int frame) {
		return getSegmentAtFrame(frame).state;
	}


	public Segment getSegmentAtFrame(int frame) {
		// TODO: start lookup at index (-> speed up)

		int total = frameOffset;
		for (Segment seg: segments) {
			if (frame >= total && frame < total+seg.length) {
				return seg;
			}
			total += seg.length;
		}
		return null;
	}


	public Segment getSegment(int idx) {
		return segments.get(idx);
	}


	/**
	 * Commits word and phone alignments to Word objects.
	 */
	public void commitToWords() {
		for (Word w: uniqueWords) {
			if (w != null) {
				w.clearAlignment();
			}
		}

		Word pWord = null;        // previous word
		Word.Phone phone = null;  // current phone
		String pUnit = null;
		int segStart = frameOffset; // absolute frame number

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


	/**
	 * Modifies the position of a transition between two segments.
	 * @param lhsIdx index of the segment on the lefthand side of the transition
	 * @param lhsNewLength new length to give to the LHS segment
	 */
	public void modifyTransition(int lhsIdx, int lhsNewLength) {
		assert lhsIdx < segments.size()-1;
		assert lhsNewLength > 0;
		assert lhsNewLength <= getMaxLengthExpandingRightward(lhsIdx);

		Segment a = segments.get(lhsIdx);
		Segment b = segments.get(lhsIdx+1);

		int delta = lhsNewLength - a.length;
		a.length += delta;
		b.length -= delta;

		assert verify();
	}


	/**
	 * Returns the maximum length for a segment expanding toward the right,
	 * without erasing/overlapping any segments on its righthand side.
	 * Important: the segment may still grow on its lefthand side!
	 */
	public int getMaxLengthExpandingRightward(int segIdx) {
		assert segIdx < segments.size()-1;
		Segment a = segments.get(segIdx);
		Segment b = segments.get(segIdx+1);
		return a.length + (b.length - 1);
	}


	public int transitionCount() {
		return segments.size();
	}


	public boolean verify() {
		int frameSum = 0;
		for (Segment seg: segments) {
			assert seg.length >= 1;
			frameSum += seg.length;
		}
		assert frameSum == frames;
		return true;
	}


	/**
	 * Randomly changes the length of two adjacent segments.
	 * @param maxDist The new lengths cannot stray further than this distance
	 *                from the initial lengths. Use a negative number to bypass
	 *                this limitation.
	 */
	public void wiggle(Random random, int maxDist) {
		int lhsSeg = -1;
		int maxLength = -1;

		while (maxLength <= 1) {
			lhsSeg = random.nextInt(transitionCount()-1);
			maxLength = getMaxLengthExpandingRightward(lhsSeg);
		}

		int newLength;

		if (maxDist < 0) {
			newLength = 1 + random.nextInt(maxLength);
		} else {
			int currLength = segments.get(lhsSeg).length;

			int min = Math.max(currLength - maxDist, 1);
			int max = Math.min(currLength + maxDist, maxLength); // inclusive

			newLength = currLength;
			while (currLength == newLength) {
				newLength = min + random.nextInt(1 + max - min);
			}

			assert maxDist >= Math.abs(newLength - currLength);
		}

		modifyTransition(lhsSeg, newLength);
	}


	@Override
	public Iterator<Segment> iterator() {
		return segments.iterator();
	}

}
