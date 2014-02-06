package fr.loria.synalp.jtrans.speechreco;

public class RecoWord2 {
	public RecoWord2(String mot, double conf) {
		word=mot;
		this.conf=conf;
	}
	public String word;
	public double conf;
	
	public String toString() {
		return word+"_"+conf;
	}
}
