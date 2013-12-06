package jtrans.elements;

import jtrans.utils.TimeConverter;

public class Anchor extends Element {
	public float seconds;

	// TODO this setting should be saved to disk
	public static boolean showMinutes = true;

	public Anchor(float seconds) {
		this.seconds = seconds;
	}

	public String toString() {
		if (showMinutes) {
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
		return TimeConverter.second2frame(seconds);
	}

	public int getType() {
		return 6;
	}
}
