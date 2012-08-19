package plugins.speechreco.adaptation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.GaussianMixture;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.MixtureComponent;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Senone;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneHMM;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneHMMState;
import plugins.applis.SimpleAligneur.Aligneur;
import plugins.speechreco.aligners.sphiinx4.AlignementEtat;
import plugins.speechreco.aligners.sphiinx4.HMMModels;

public class BiaisAdapt
{
	private Aligneur aligneur;

	public BiaisAdapt(Aligneur aligneur)
	{
		super();
		this.aligneur = aligneur;
	}

	public Aligneur getAligneur()
	{
		return aligneur;
	}

	public void setAligneur(Aligneur aligneur)
	{
		this.aligneur = aligneur;
	}

	public void adaptMAP(AlignementEtat alignPhones) {
		// TODO: 1- equi-split phone segments into state segments
		// 2- compute the "weights" of each Gaussian in the state by Mahalanobis
		// 3- adapt based on the weight

		String ss = JOptionPane.showInputDialog("Number of iterations ?");
		if (ss==null) return;
		int niters = Integer.parseInt(ss);

		HashMap<String,List<GaussianMixture>> ph2gauss = new HashMap<String, List<GaussianMixture>>();
		AcousticModel a=HMMModels.getAcousticModels();
		Iterator<HMM> hmmit = a.getHMMIterator();
		while (hmmit.hasNext()) {
			HMM hmm = hmmit.next();
			SenoneHMM shmm = (SenoneHMM) hmm;			
			int nstates = shmm.getTransitionMatrix().length;
			ArrayList<GaussianMixture> gau = new ArrayList<GaussianMixture>();
			ph2gauss.put(shmm.getBaseUnit().getName(), gau);
			for (int st=0;st<nstates;st++) {
				HMMState hmmst = shmm.getState(st);
				SenoneHMMState shmmst=(SenoneHMMState) hmmst;
				Senone s=shmmst.getSenone();
				if(s!=null) { 
					GaussianMixture g=(GaussianMixture) s;
					gau.add(g);
				}
			}
		}

		ArrayList<float[]> mfccs=new ArrayList<float[]>();
		aligneur.getS4aligner().mfccs.gotoFrame(0);
		while(aligneur.getS4aligner().mfccs.noMoreFramesAvailable==false) {
			Data d = aligneur.getS4aligner().mfccs.getData();
			if(!(d instanceof DataEndSignal) && !(d instanceof DataStartSignal)) {
				mfccs.add(((FloatData)d).getValues());
			}
		}

		for (int iter=0;iter<niters;iter++) {
			// calcule loglike
			float loglike = 0;
			for (int i=0;i<mfccs.size();i++) {
				float[] x = mfccs.get(i);
				int s = alignPhones.getSegmentAtFrame(i);
				if (s<0) continue;
				String ph=alignPhones.getSegmentLabel(s);
				List<GaussianMixture> gau = ph2gauss.get(ph);
				if (gau==null) System.out.println("ERROR NULL "+ph);
				else {
					MixtureComponent bestg = null; float bestsc = -Float.MAX_VALUE;
					for (GaussianMixture gm : gau) {
						for (MixtureComponent gauss : gm.getMixtureComponents()) {
							float sc = gauss.getScore(x);
							if (bestg==null||bestsc<sc) {
								bestsc=sc; bestg=gauss;
							}
						}
					}
					loglike+=bestsc;
				}
			}
			System.out.println("iter "+iter+" loglike "+loglike);
			
			// adapt
			for (int i=0;i<mfccs.size();i++) {
				float[] x = mfccs.get(i);
				int s = alignPhones.getSegmentAtFrame(i);
				if (s<0) continue;
				String ph=alignPhones.getSegmentLabel(s);
				List<GaussianMixture> gau = ph2gauss.get(ph);
				if (gau==null) {
					JOptionPane.showMessageDialog(null, "ERROR MAP adapt: "+ph+" "+ph2gauss.keySet());
				} else {
					MixtureComponent bestg = null; float bestsc = -Float.MAX_VALUE;
					for (GaussianMixture gm : gau) {
						for (MixtureComponent gauss : gm.getMixtureComponents()) {
							float sc = gauss.getScore(x);
							if (bestg==null||bestsc<sc) {
								bestsc=sc; bestg=gauss;
							}
						}
					}
					float[] m = bestg.getMean();
					for (int j=0;j<x.length;j++) {
						m[j] = 0.9f*m[j]+0.1f*x[j];
					}
					// TODO: augmenter weight de bestg !
				}
			}
		}
	}


	public float[] calculateBiais() {
		//récupération des means et mfccs
		float[] biais;
		ArrayList<float[]> means=new ArrayList<float[]>();
		AcousticModel a=HMMModels.getAcousticModels();
		Iterator<HMM> hmmit = a.getHMMIterator();
		while (hmmit.hasNext()) {
			HMM hmm = hmmit.next();
			SenoneHMM shmm = (SenoneHMM) hmm;			
			int nstates = shmm.getTransitionMatrix().length;
			for (int st=0;st<nstates;st++) {
				HMMState hmmst = shmm.getState(st);
				SenoneHMMState shmmst=(SenoneHMMState) hmmst;
				Senone s=shmmst.getSenone();
				if(s!=null) { 
					GaussianMixture g=(GaussianMixture) s;
					MixtureComponent[] mc=g.getMixtureComponents();
					System.out.println("debug adapt senone "+s+" "+shmm+" ngauss "+mc.length);
					int k=mc.length;
					for(int j=0;j<k;j++) {
						means.add(mc[j].getMean());
					}
				}
			}
		}
		aligneur.getS4aligner().mfccs.gotoFrame(0);
		ArrayList<float[]> mfccs=new ArrayList<float[]>();
		while(aligneur.getS4aligner().mfccs.noMoreFramesAvailable==false) {
			Data d = aligneur.getS4aligner().mfccs.getData();
			if(!(d instanceof DataEndSignal) && !(d instanceof DataStartSignal)) {
				mfccs.add(((FloatData) aligneur.getS4aligner().mfccs.getData()).getValues());
			}
		}
		//calcul du biais
		int taille=means.get(1).length;
		float[] mfcc=new float[taille];
		float[] mean=new float[taille];
		for(int i=0;i<mfccs.size();i++) {
			for(int j=0;j<taille;j++) {
				mfcc[j]=mfcc[j]+mfccs.get(i)[j]/(mfccs.size());
			}
		}
		for(int i=0;i<means.size();i++) {
			for(int j=0;j<taille;j++) {
				mean[j]=mean[j]+means.get(i)[j]/(means.size());
			}
		}
		biais=new float[taille];
		for(int i=0;i<taille;i++) {
			biais[i]=mean[i]-mfcc[i];
		}
		//ajout du biais
		float tauxCorrection=1;
		for(int i=0;i<means.size();i++) {
			for(int j=0;j<taille;j++) {
				means.get(i)[j]+=tauxCorrection*biais[j];
			}
		}
		return biais;
	}

}
