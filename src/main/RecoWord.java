package main;

public class RecoWord {
	public RecoWord(String mot, double conf) {
		word=mot;
		this.conf=conf;
	}
	public String word;
	public double conf;
	
	public String toString() {
		return word+"_"+conf;
	}
}
