package jtrans.elements;

public class Noise extends Element {
	public final String noise;

	public Noise(String noise) {
		this.noise = noise;
	}

	public int getType() {
		return 2;
	}

	public String toString() {
		return noise;
	}
}
