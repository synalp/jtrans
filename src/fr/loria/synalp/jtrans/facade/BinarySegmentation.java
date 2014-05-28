package fr.loria.synalp.jtrans.facade;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Sorted sequence of time segments.
 */
public class BinarySegmentation {

	protected List<Segment> sequence;


	public static class Segment {
		protected float off;
		protected float len;

		public Segment(float off, float len) {
			this.off = off;
			this.len = len;
		}

		public Segment(Segment other) {
			this(other.off, other.len);
		}

		public boolean contains(float sec) {
			return !isAhead(sec) && !isBehind(sec);
		}

		public boolean isAhead(float sec) {
			return sec < getStart();
		}

		public boolean isBehind(float sec) {
			return sec >= getEnd();
		}

		public float getStart() {
			return off;
		}

		public float getEnd() {
			return off+len;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}

			if (!(obj instanceof Segment)) {
				return false;
			}

			Segment s = (Segment)obj;
			return off == s.off && len == s.len;
		}

		@Override
		public String toString() {
			return "off=" + off + ", len=" + len;
		}
	}


	public BinarySegmentation() {
		sequence = new ArrayList<>();
	}


	public BinarySegmentation(BinarySegmentation other) {
		sequence = new ArrayList<>(other.sequence.size());
		for (Segment seg: other.sequence) {
			sequence.add(new Segment(seg));
		}
	}


	public void union(float off, float len) {
		float a1 = off;
		float a2 = off+len;

		ListIterator<Segment> itr = sequence.listIterator();
		int ins = -1;

		while (itr.hasNext()) {
			Segment seg = itr.next();
			float b1 = seg.getStart();
			float b2 = seg.getEnd();

			if (a1 > b2) {
				continue;
			} else if (a2 < b1) {
				ins = itr.previousIndex();
				break;
			} else if (a2 <= b2) {
				if (a1 < b1) {
					seg.off = a1;
					seg.len = b2 - a1;
				}
				return;
			} else {
				assert a2 > b2;
				if (b1 < a1) {
					a1 = b1;
				}
				itr.remove();
			}
		}

		sequence.add(
				ins>=0? ins: sequence.size(),
				new Segment(a1, a2-a1));
	}


	public void union(BinarySegmentation other) {
		for (Segment seg: other.sequence) {
			union(seg.off, seg.len);
		}
	}


	/**
	 * Negates all segments.
	 * @param extent Process any negative space (beyond the last segment) until
	 *               this extent. This value must equal or exceed that given by
	 *               {@link #extent()}.
	 */
	public void negate(float extent) {
		assert extent >= extent();

		float negSpaceStart = 0;

		boolean removeFirst = !sequence.isEmpty() && sequence.get(0).off == 0;

		for (Segment seg: sequence) {
			float nextNegSpaceStart = seg.getEnd();

			float negSpaceLen = seg.getStart() - negSpaceStart;
			assert negSpaceLen >= 0;

			seg.off = negSpaceStart;
			seg.len = negSpaceLen;

			negSpaceStart = nextNegSpaceStart;
		}

		if (removeFirst) {
			sequence.remove(0);
		}

		float padding = extent - negSpaceStart;
		assert padding >= 0;

		if (padding > 0) {
			union(negSpaceStart, padding);
		}
	}


	public void intersect(BinarySegmentation other) {
		float extent = Math.max(extent(), other.extent());

		// a && b == !(!a || !b)
		this.negate(extent);
		other.negate(extent);
		this.union(other);
		this.negate(extent);

		other.negate(extent); // restore other
	}


	public int size() {
		return sequence.size();
	}


	public Segment get(int idx) {
		return sequence.get(idx);
	}


	/**
	 * Returns the time at which the last segment ends.
	 */
	public float extent() {
		return sequence.isEmpty()
				? 0
				: sequence.get(sequence.size()-1).getEnd();
	}


	@Override
	public boolean equals(Object o) {
		return this == o ||
				(o instanceof BinarySegmentation &&
				sequence.equals(((BinarySegmentation)o).sequence));
	}

}
