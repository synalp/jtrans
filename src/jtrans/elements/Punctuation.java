package jtrans.elements;

public class Punctuation extends Element {
	public final char mark;

	public Punctuation(char mark) {
		this.mark = mark;
	}

	public int getType() {
		return 5;
	}

	public String toString() {
		return Character.toString(mark);
	}
}
