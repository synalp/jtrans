/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package jtrans.speechreco.s4;

import java.util.HashMap;

import edu.cmu.sphinx.linguist.acoustic.tiedstate.MixtureComponent;

/**
 * pour chaque état aligné avec une trame, 
 * 1- calcule la meilleure Gaussienne alignée avec la trame
 * 2- calcule le biais d'adaptation pour cette Gaussienne = une fraction du delta
 * 3- une fois tout le signal vu, calcule le biais moyen par Gaussienne
 * 
 * @author cerisara
 *
 */
public class Adaptation {
	class Biais {
		float[] bias;
		int n=0;
	}
	HashMap<MixtureComponent, Biais> gauss2bias = new HashMap<MixtureComponent, Biais>();
	public float ratio = 0.1f;
	
	/*
	public void adapt(Alignment align, S4mfccBuffer mfccs) {
		for (int tr=align.getStartFrame();tr<align.getStartFrame()+align.getNbFrames();tr++) {
			mfccs.gotoFrame(tr);
			Data obs = mfccs.getData();
			if (obs==null) {
				System.err.println("ERROR adapt pas assez d'obs !");
				return;
			}
			if (obs instanceof DataStartSignal) obs = mfccs.getData();
			
			MixtureComponent bestgauss;
			float[] bias;
			if (obs instanceof DoubleData) {
				bestgauss = align.getBestGauss(tr, obs);
				float[] mean = bestgauss.getMean();
				bias = new float[mean.length];
				double[] x = ((DoubleData) obs).getValues();
				
				// debug
				System.out.println("DEBUG adapt "+tr+" "+align.getStartFrame()+" "+x[0]);

				assert mean.length==x.length;
				for (int i=0;i<x.length;i++) {
					bias[i] = ratio*((float)x[i]-mean[i]);
				}
			} else if (obs instanceof FloatData) {
				bestgauss = align.getBestGauss(tr, obs);
				float[] mean = bestgauss.getMean();
				bias = new float[mean.length];
				float[] x = ((FloatData) obs).getValues();

//				// debug
//				System.out.println("DEBUG adapt "+tr+" "+align.getFirstFrame()+" "+x[0]);

				assert mean.length==x.length;
				for (int i=0;i<x.length;i++) {
					bias[i] = ratio*(x[i]-mean[i]);
				}
			} else {
				System.err.println("ERROR adaptation data type "+obs.getClass().getName());
				continue;
			}
			
			// merging des biais
			Biais b = gauss2bias.get(bestgauss);
			if (b==null) {
				b = new Biais(); b.bias=bias;
				gauss2bias.put(bestgauss, b);
			}
			for (int i=0;i<bias.length;i++)
				b.bias[i]+=bias[i];
			b.n++;
		}
		
		// adaptation des gauss
		for (MixtureComponent gauss : gauss2bias.keySet()) {
			Biais biais = gauss2bias.get(gauss);
			float[] unnormed = biais.bias;
			float[] normed = new float[unnormed.length];
			for (int i=0;i<normed.length;i++)
				normed[i] = unnormed[i]/(float)biais.n;
		}
	}
	*/
}
