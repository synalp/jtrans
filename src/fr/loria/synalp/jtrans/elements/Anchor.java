package fr.loria.synalp.jtrans.elements;

import fr.loria.synalp.jtrans.utils.TimeConverter;

public class Anchor
		implements Element, Comparable<Anchor>
{

	public float seconds;
	private final int timelessOrder;

	// TODO this setting should be saved to disk
	public static boolean showMinutes = true;

	// Private to avoid confusion
	private Anchor(float seconds, int timelessOrder) {
		this.seconds = seconds;
		this.timelessOrder = timelessOrder;
	}


	public static Anchor timedAnchor(float seconds) {
		return new Anchor(seconds, -1);
	}


	public static Anchor orderedTimelessAnchor(int order) {
		return new Anchor(-1f, order);
	}


	public String toString() {
		if (!hasTime()) {
			return "TL#" + timelessOrder;
		} else if (showMinutes) {
			return String.format("%d'%02d\"%03d",
					(int)(seconds/60f),
					(int)(seconds%60f),
					Math.round(seconds%1f * 1000f));
		} else {
			return String.format("%d.%03d",
					(int)seconds,
					Math.round(seconds%1f * 1000f));
		}
	}


	public int getFrame() {
		if (!hasTime()) {
			throw new IllegalArgumentException("anchor has no time");
		}
		return TimeConverter.second2frame(seconds);
	}


	public boolean hasTime() {
		return seconds >= 0;
	}


	public int compareTo(Anchor a) {
		if (a == null) {
			return 1;
		}

		if (hasTime() && a.hasTime()) {
			return Float.compare(seconds, a.seconds);
		}

		if (!hasTime() && !a.hasTime()) {
			return Integer.compare(timelessOrder, a.timelessOrder);
		}

		return hasTime()? 1: -1;
	}


	public boolean equals(Object a) {
		return a instanceof Anchor &&
				compareTo((Anchor)a) == 0;
	}

}
