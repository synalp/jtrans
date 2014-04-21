package fr.loria.synalp.jtrans.facade;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Sorted sequence of time segments.
 */
public class BinarySegmentation {

	protected List<Segment> sequence = new ArrayList<>();


	public static class Segment {
		protected float off;
		protected float len;

		public Segment(float off, float len) {
			this.off = off;
			this.len = len;
		}

		public boolean contains(float sec) {
			return !isAhead(sec) && !isBehind(sec);
		}

		public boolean isAhead(float sec) {
			return sec < off;
		}

		public boolean isBehind(float sec) {
			return sec >= off+len;
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


	public void union(float off, float len) {
		float a1 = off;
		float a2 = off+len;

		ListIterator<Segment> itr = sequence.listIterator();
		int ins = -1;

		while (itr.hasNext()) {
			Segment seg = itr.next();
			float b1 = seg.off;
			float b2 = seg.off+seg.len;

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


	public int size() {
		return sequence.size();
	}


	public Segment get(int idx) {
		return sequence.get(idx);
	}

}
