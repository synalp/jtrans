/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.frontEnd;

/**
 * apply Hamming window
 * 
 * @author cerisara
 *
 */
public class Hamming implements FrontEnd {
	private float [] cosineWindow;
	private final float alpha = 0.46f;
	TimeWindow w;
	private float [] res;
	
	public Hamming(TimeWindow win) {
		w=win;
		res = new float[win.getNcoefs()];
		createWindow(win.getNcoefs());
	}
	public int getNcoefs() {
		return res.length;
	}
	public float[] getOneVector() {
		float[] obs = w.getOneVector();
		if (obs==null) return null;
		applyHamming(obs);
		return res;
	}
    /**
	 * Creates the Hamming Window.
	 */
	private void createWindow(int ncoefs) {
		cosineWindow = new float[ncoefs];
		if (cosineWindow.length > 1) {
			float oneMinusAlpha = (1 - alpha);
			for (int i = 0; i < cosineWindow.length; i++) {
				cosineWindow[i] = oneMinusAlpha - alpha * (float)Math.cos(2f * Math.PI * (float)i / ((double) cosineWindow.length - 1.0));
			}
		}
	}
	public void applyHamming(float[] o) {
		// il suffit de multiplier sample par sample
		for (int i = 0; i < o.length; i++) {
			res[i] = o[i]*cosineWindow[i];
		}
	}
	
}
