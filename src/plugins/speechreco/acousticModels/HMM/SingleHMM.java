/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.acousticModels.HMM;

import java.io.Serializable;
import java.util.Vector;

public class SingleHMM implements Serializable {
	private HMMState[] states;
	private String nomHMM;
	public float [][] trans;
	LogMath logMath = new LogMath();
	
	public SingleHMM(int nbStates) {
		nomHMM="";
		states = new HMMState[nbStates];
	}
	
	public SingleHMM clone() {
		SingleHMM hmm = new SingleHMM(getNstates());
		for (int i=0;i<getNstates();i++) {
			if (getState(i)==null) hmm.setState(i,null);
			else hmm.setState(i, getState(i).clone());
		}
		hmm.setNom("clone");
		hmm.trans = new float[trans.length][trans[0].length];
		for (int i=0;i<trans.length;i++)
			System.arraycopy(trans[i], 0, hmm.trans[i], 0, trans[i].length);
		return hmm;
	}
	
	public void setNom(String s) {
		nomHMM=""+s;
	}
	public String getNom() {
		return nomHMM;
	}
	
	public void setState(int idx, HMMState st) {
		states[idx]=st;
	}
	public boolean isEmitting(int idx) {
		if (states[idx]!=null)
			return states[idx].isEmitting();
		else
			return false;
	}
	public void setTrans(int i, int j, float tr) {
		trans[i][j]=tr;
	}
	public void setTrans(float [][] tr) {
		trans = tr;
	}
	public float getTrans(int i, int j) {
		return trans[i][j];
	}
	/**
	 * may return null if the state is non-emitting
	 * @param idx
	 * @return
	 */
	public HMMState getState(int idx) {
		return states[idx];
	}
	public int getNstates() {
		return states.length;
	}
	public int getNemittingStates() {
		int n=0;
		for (int i=0;i<states.length;i++) {
			if (states[i].isEmitting()) n++;
		}
		return n;
	}
}
