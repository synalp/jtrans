package fr.loria.synalp.jtrans.elements;

/**
 * Text element that must be skipped during the alignment.
 */
public class Comment implements Element {
	private String text;
	private int type;

	public Comment(String text, int type) {
		this.text = text;
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public String toString() {
		return "[" + text + "]";
	}
}
