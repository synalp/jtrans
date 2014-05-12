package fr.loria.synalp.jtrans.viterbi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class NodeTimeline {

	protected static class Segment {
		int length;
		public final int node;

		public Segment(int n) {
			length = 1;
			node = n;
		}
	}


	protected List<Segment> segments = new ArrayList<>();
	protected final int frames;


	public NodeTimeline(int[] timeline) {
		frames = timeline.length;

		Segment seg = null;

		for (int i = 0; i < timeline.length; i++) {
			if (seg == null || timeline[i] != seg.node) {
				seg = new Segment(timeline[i]);
				segments.add(seg);
			} else {
				seg.length++;
			}
		}
	}


	/**
	 * Converts this object to an array of StateGraph node indices (one node
	 * index per frame).
	 */
	public int[] toArray() {
		int[] timeline = new int[frames];
		int offset = 0;

		for (Segment seg: segments) {
			Arrays.fill(timeline, offset, offset + seg.length, seg.node);
			offset += seg.length;
		}

		return timeline;
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

}
