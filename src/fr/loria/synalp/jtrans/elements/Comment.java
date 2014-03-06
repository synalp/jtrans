package fr.loria.synalp.jtrans.elements;

/**
 * Text element that must be skipped during the alignment.
 */
public class Comment implements Element {
	private String text;
	private Type type;

	public static enum Type {
		FREEFORM,
		NOISE,
		BEEP,
		PUNCTUATION,
		OVERLAP_MARK,
		SPEAKER_MARK,
	}

	public Comment(String text, Type type) {
		this.text = text;
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public String toString() {
		return "[" + text + "]";
	}
}
