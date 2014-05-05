package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.utils.TimeConverter;

public class Anchor
		implements Comparable<Anchor>
{

	public float seconds;

	// TODO this setting should be saved to disk
	public static boolean showMinutes = true;

	public Anchor(float seconds) {
		this.seconds = seconds;
	}


	public String toString() {
		String s;

		if (showMinutes) {
			s = String.format("%d'%02d\"%03d",
					(int)(seconds/60f),
					(int)(seconds%60f),
					Math.round(seconds%1f * 1000f));
		} else {
			s = String.format("%d.%03d",
					(int)seconds,
					Math.round(seconds%1f * 1000f));
		}

		return s;
	}


	public int getFrame() {
		return TimeConverter.second2frame(seconds);
	}


	public void setSeconds(float seconds) {
		this.seconds = seconds;
	}


	public void setFrame(int frame) {
		seconds = TimeConverter.frame2sec(frame);
	}


	public int compareTo(Anchor a) {
		if (a == null) {
			return 1;
		}

		return Float.compare(seconds, a.seconds);
	}


	public boolean equals(Object a) {
		return a instanceof Anchor &&
				compareTo((Anchor)a) == 0;
	}

}
