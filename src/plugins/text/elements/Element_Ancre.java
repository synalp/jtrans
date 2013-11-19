package plugins.text.elements;

public class Element_Ancre extends Element {
	public final float seconds;

	public Element_Ancre(float seconds) {
		this.seconds = seconds;
	}

	public String toString() {
		return String.format("%d'%02d\"%03d",
				(int)(seconds/60f),
				(int)(seconds%60f),
				Math.round(seconds%1f * 1000f));
	}

	public int getType() {
		return 6;
	}
}
