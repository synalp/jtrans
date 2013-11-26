/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package speechreco.acousticModels.HMM;

import java.io.Serializable;

/**
 * represents a label, i.e. a model name + a state number
 * 
 * @author cerisara
 *
 */
public class Lab implements Serializable {
	private String nomHMM;
	// numState contient le numero de l'etat tel que defini dans le fichier HTK
	private int numState=-1;
	private int deb=-1, fin=-1;
	
	public Lab() {
	}
	public Lab(String s) {
		setName(s);
	}
	public Lab(String s, int n) {
		setName(s); setStateIdx(n);
	}
	// copy-constructor
	public Lab(Lab ref) {
		setDeb(ref.getDeb());
		setFin(ref.getFin());
		setName(ref.getName());
		setStateIdx(ref.getState());
	}
	
	public String getName() {
		return nomHMM;
	}
	public int getState() {
		return numState;
	}
	public int getDeb() {return deb;}
	public int getFin() {return fin;}
	public void setName(String s) {
		nomHMM = ""+s;
	}
	public void setStateIdx(int i) {
		numState = i;
	}
	public void setDeb(int i) {
		deb=i;
	}
	public void setFin(int i) {
		fin=i;
	}
	public boolean isEqual(Lab l) {
		if (l.getState()!=-1 && getState()!=-1) {
			return l.getName().equals(getName()) && l.getState()==getState();
		} else {
			return l.getName().equals(getName());
		}
	}
	
	public String toString() {
		String r = "";
		if (deb>=0&&fin>=deb)
			r+=deb+" "+fin+" ";
		r+=nomHMM;
		if (numState>=0)
			r+="["+numState+"]";
		return r;
	}
}
