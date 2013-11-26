/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package speechreco.aligners.sphiinx4;

import java.io.Serializable;

public class S4AlignOrder implements Serializable {
	final public static S4AlignOrder terminationOrder = new S4AlignOrder(-1, -1);
	
	boolean isBlocViterbi;
	int mot1, mot2=-1, tr1, tr2=-1;
	public Alignment alignWords, alignPhones, alignStates;
	// contient l'alignement des mots
	
	public S4AlignOrder(int firstWord, int firstFrame, int lastMot, int lastFrame) {
		isBlocViterbi = false;
		mot1=firstWord; mot2=lastMot;
		tr1=firstFrame; tr2=lastFrame;
	}
	
	public S4AlignOrder(int firstWord, int firstFrame) {
		isBlocViterbi = true;
		mot1=firstWord;
		tr1=firstFrame;
	}
	
	public boolean isBlocViterbi() {return isBlocViterbi;}
	
	public int getFirstMot() {return mot1;}
	public int getLastMot() {return mot2;}
	public int getFirstFrame() {return tr1;}
	public int getLastFrame() {return tr2;}

	public void adjustOffset() {
		alignWords.adjustOffset(tr1);
		alignPhones.adjustOffset(tr1);
		alignStates.adjustOffset(tr1);
	}

	public boolean isEmpty() {
		return alignWords == null;
	}
}
