package plugins.text.elements;

public class Element_Bruit extends Element {
	public final String noise;

	public Element_Bruit(String noise) {
		this.noise = noise;
	}

	public int getType() {
		return 2;
	}

	public String toString() {
		return noise;
	}
}
