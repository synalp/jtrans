package plugins.speechreco.adaptation;

import java.util.Arrays;
import java.util.HashMap;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.GaussianMixture;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.MixtureComponent;
import edu.cmu.sphinx.util.LogMath;
import weka.core.matrix.Matrix;

/**
 * Dans cette implémentation du forward-backward, je fais l'hypothèse
 * qu'on a un alignement etats/trames connu, et je fais l'approx de Viterbi:
 * tous les alpha(t)=0 sauf celui aligné avec t. La proba de transition d'un
 * alpha à l'autre = 1.
 * 
 * @author cerisara
 *
 */
public class DetMLLR {
	
	/**
	 * 
	 * @return la log-proba d'etre dans la Gauss i pour cette trame
	 */
	float[] calcOccProb(GaussianMixture alignedGMMs, Data obs) {
		float[] logb = alignedGMMs.calculateComponentScore(obs);
		float[] p = new float[logb.length];
		LogMath logMath = new LogMath();
		float logsum=logb[0];
		for (int i=1;i<p.length;i++) {
			logMath.addAsLinear(logsum, logb[i]);
		}
		for (int i=0;i<p.length;i++) {
			p[i]-=logsum;
		}
		return p;
	}
	
	void calcGlobalTransform(GaussianMixture[] alignedGMMs, Data[] obs) {
		int ncoefs = alignedGMMs[0].getMixtureComponents()[0].getMean().length;
		HashMap<MixtureComponent, Integer> gauss2idx = new HashMap<MixtureComponent, Integer>(); 
		for (int t=0;t<obs.length;t++) {
			for (MixtureComponent gauss : alignedGMMs[t].getMixtureComponents()) {
				if (!gauss2idx.containsKey(gauss)) {
					gauss2idx.put(gauss, gauss2idx.size());
				}
			}
		}
		float[] sumLPerGauss = new float[gauss2idx.size()];
		Arrays.fill(sumLPerGauss, 0);
		for (int t=0;t<obs.length;t++) {
			float[] loglikePerGauss = calcOccProb(alignedGMMs[t], obs[t]);
			for (int g=0;g<loglikePerGauss.length;g++) {
				MixtureComponent gauss = alignedGMMs[t].getMixtureComponents()[g];
				int gaussidx = gauss2idx.get(gauss);
				sumLPerGauss[gaussidx]+=loglikePerGauss[g];
			}
		}
		
		Matrix[] g = new Matrix[ncoefs];
		Matrix[] k = new Matrix[ncoefs];
		Arrays.fill(g, null);
		Arrays.fill(k, null);
		for (MixtureComponent gauss : gauss2idx.keySet()) {
			int gaussidx = gauss2idx.get(gauss);
			double[][] etax = new double[1][ncoefs+1];
			for (int i=0;i<etax.length-1;i++) {
				etax[0][i+1]=gauss.getMean()[i];
			}
			Matrix eta = new Matrix(etax);
			Matrix etatTransposed = eta.transpose();
			Matrix x1 = eta.times(etatTransposed);
			Matrix x2 = x1.times(sumLPerGauss[gaussidx]);
			
			for (int i=0;i<ncoefs;i++) {
				Matrix y = x2.times(1f/gauss.getVariance()[i]);
				if (g[i]==null) g[i]=y;
				else {
					g[i].plus(y);
				}
			}

			// calcul de k
			for (int t=0;t<obs.length;t++) {
				boolean isCurgaussAligned = false;
				MixtureComponent[] mixes = alignedGMMs[t].getMixtureComponents();
				int gaussInMix = -1;
				for (int i=0;i<mixes.length;i++) {
					if (mixes[i]==gauss) {
						isCurgaussAligned=true; gaussInMix=i; break;
					}
				}
				if (!isCurgaussAligned) continue;
				double[][] o = new double[1][];
				o[0]=((DoubleData)obs[t]).getValues();
				Matrix x = new Matrix(o);
				Matrix y = x.times(etatTransposed);
				float[] loglikePerGauss = calcOccProb(alignedGMMs[t], obs[t]);
				x = y.times(loglikePerGauss[gaussInMix]);
				for (int i=0;i<ncoefs;i++) {
					y = x.times(1f/gauss.getVariance()[i]);
					if (k[i]==null) k[i]=y;
					else k[i].plus(y);
				}
			}
		}
		
		// la matrice de transformation, le biais est dans la dim0:
		double[][] w = new double[ncoefs+1][ncoefs];
		for (int i=0;i<ncoefs+1;i++) {
			Matrix gi = g[i].inverse();
			Matrix wi = k[i].times(gi);
			assert wi.getColumnDimension()==1;
			w[i]=wi.getArray()[0];
		}
	}
	
	public static void main(String args[]) {
		double[][] xx = {{1,3},{5,2}};
		Matrix m = new Matrix(xx);
		Matrix mi = m.inverse();
		System.out.println(mi.toString());
	}
}
