/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package speechreco.acousticModels.HMM;

import java.io.Serializable;

/**
 * This is an abstract class usually instanciated into a GMMDiag.
 * 
 * It ocntains a label (=an HMM name (String) and a state number) !
 * 
 * @author cerisara
 *
 */
public abstract class HMMState implements Serializable {
	
	public abstract HMMState clone();
	public abstract boolean isEmitting();
	public abstract void computeLogLikes(float[] data);
	public abstract float getLogLike();
	
	// nom de l'etat (utilise lorsqu'il y a tying)
	public String nom = null;

	public String toString() {
		return lab.toString();
	}
	
	public void setLab(Lab l) {
		lab = l;
	}
	public Lab getLab() {
		return lab;
	}
	public Lab lab = null;
}
