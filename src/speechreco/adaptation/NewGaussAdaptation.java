package speechreco.adaptation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import speechreco.aligners.sphiinx4.Alignment;
import speechreco.aligners.sphiinx4.S4mfccBuffer;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.GaussianMixture;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.MixtureComponent;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Senone;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneHMMState;

/**
 * Simple adaptation that add one Gaussian to all states of all models,
 * initially with a null weight, and then trains this Gaussian with incoming aligned data.
 * the weight of this Gaussian increases with the number of data used for training,
 * while the weights of the other SI Gaussians decrease accordingly.
 * 
 * ** modif: plutot que d'ajouter une nouvelle Gauss, on reutilise la gauss existante de poids le plus faible
 * Ceci correspond a de l'adaptation MAP !
 * 
 * @author xtof
 *
 */
public class NewGaussAdaptation {
	// associe chaque GMM a un index interne pour les tableaux suivants
	private HashMap<GaussianMixture, Integer> gmm2internidx = new HashMap<GaussianMixture, Integer>();
	
	// indique pour chaque GMM l'indice de la gauss apprise sur les test data
	private ArrayList<Integer> newgaussidx = new ArrayList<Integer>();
	
	// contient les accumulateurs X pour chaque GMM
	private ArrayList<float[]> accux = new ArrayList<float[]>();
	// contient les accumulateurs XX pour chaque GMM
	private ArrayList<float[]> accuxx = new ArrayList<float[]>();
	// contient les accumulateurs N pour chaque GMM
	private ArrayList<Integer> accun = new ArrayList<Integer>();

	// la nouvelle gauss est toujours la derniere !
	void initGMM(GaussianMixture gmm) {
		if (gmm2internidx.get(gmm)!=null) return;
		int internidx = newgaussidx.size();
		gmm2internidx.put(gmm, internidx);
		
		float[] ws = gmm.getComponentWeights();
		int gidx=0;
		for (int i=1;i<ws.length;i++) {
			if (ws[i]<ws[gidx]) gidx=i;
		}
		newgaussidx.add(gidx);
		float[] x = new float[gmm.dimension()];
		accux.add(x);
		Arrays.fill(x, 0);
		float[] xx = new float[gmm.dimension()];
		accuxx.add(xx);
		Arrays.fill(xx, 0);
		accun.add(0);
	}
	
	/**
	 * creates one new Gaussian per state
	 */
	public void init(AcousticModel mods) {
		Iterator<HMM> hmmsit = mods.getHMMIterator();
		while (hmmsit.hasNext()) {
			HMM hmm = hmmsit.next();
			try {
				for (int s=0;;s++) {
					HMMState etat = hmm.getState(s);
					if (etat.isEmitting()) {
						if (etat instanceof SenoneHMMState) {
							Senone sen = ((SenoneHMMState)etat).getSenone();
							if (sen instanceof GaussianMixture) {
								GaussianMixture gmm = (GaussianMixture)sen;
								initGMM(gmm);
							} else {
								System.err.println("WARNING: Senone unsupported ! "+sen.getClass().getName());
							}
						} else {
							System.err.println("WARNING: HMMstate unsupported ! "+etat.getClass().getName());
						}
					}
				}
			} catch (Exception e) {
				// plus d'etat !
			}
		}

	}

	private void trainGauss(GaussianMixture gmm) {
		int internidx = gmm2internidx.get(gmm);
		float nocc = accun.get(internidx);
		if (nocc>5) {
			int gaussid = newgaussidx.get(internidx);
			MixtureComponent gauss = gmm.getMixtureComponents()[gaussid];
			float[] mean = gauss.getMean();
			float[] accx = accux.get(internidx);
			for (int i=0;i<mean.length;i++) {
				mean[i]=accx[i]/nocc;
			}
			float[] var = gauss.getVariance();
			float[] accxx = accuxx.get(internidx);
			for (int i=0;i<var.length;i++) {
				var[i]=accxx[i]/nocc - mean[i]*mean[i];
			}
			// TODO increase the weight of the gauss !!
			
			gauss.transformStats();
			gauss.precomputeDistance();
		}
	}
	
	public void adapt(GaussianMixture[] alignedGMMs, Data[] obs) {
		assert alignedGMMs.length==obs.length;
		HashSet<GaussianMixture> changed = new HashSet<GaussianMixture>();
		for (int t=0;t<obs.length;t++) {
			if (alignedGMMs[t]==null) continue;
			if (!(obs[t] instanceof DoubleData)) continue;
			Integer internidx = gmm2internidx.get(alignedGMMs[t]);
			if (internidx==null) System.err.println("WARNING gauss unknown "+alignedGMMs[t]);
			else {
				double[] o = ((DoubleData)obs[t]).getValues();
				float[] x = accux.get(internidx);
				float[] xx = accuxx.get(internidx);
				for (int i=0;i<x.length;i++) {
					x[i]+=o[i];
					xx[i]+=o[i]*o[i];
				}
				accun.set(internidx, accun.get(internidx)+1);
				changed.add(alignedGMMs[t]);
			}
		}
		System.out.println("debug adapt "+obs.length+" "+changed.size());
		for (GaussianMixture gmm : changed) {
			trainGauss(gmm);
		}
	}
	
	private GaussianMixture getGMM(String phone, String etat) {
		return null;
	}
	
	/**
	 * 
	 * @param segdeb
	 * @param segfin inclus !
	 * @param mfccs
	 */
	public void adapt(Alignment alignPhones, Alignment alignStates, int segdeb, int segfin, S4mfccBuffer mfccs) {
		// on recupere la trame MFCC de debut
		int frdeb = alignStates.getSegmentDebFrame(segdeb);
		int frfin = alignStates.getSegmentEndFrame(segfin);
		mfccs.gotoFrame(frdeb);
		mfccs.firstCall=false;
		Data[] ds = new Data[frfin-frdeb];
		GaussianMixture[] gmms = new GaussianMixture[ds.length];
		for (int fr=frdeb;fr<frfin;fr++) {
			Data d = mfccs.getData();
			ds[fr-frdeb]=d;
			// ceci pourrait aller + vite en comparant fr avec les frdeb et frfin du segment courant
			int segp = alignPhones.getSegmentAtFrame(fr);
			String phoneName = alignPhones.getSegmentLabel(segp);
			int segs = alignStates.getSegmentAtFrame(fr);
			String stateName = alignStates.getSegmentLabel(segs);
			gmms[fr-frdeb] = getGMM(phoneName, stateName);
		}
		adapt(gmms, ds);
	}
}
