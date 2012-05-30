package plugins.speechreco.adaptation;

import java.util.ArrayList;
import java.util.Iterator;

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
	
	
	public float[] calculateBiais() {
		//récupération des means et mfccs
		float[] biais;
		ArrayList<float[]> mfccs=new ArrayList<float[]>();
		ArrayList<float[]> means=new ArrayList<float[]>();
		while(aligneur.getS4aligner().mfccs.noMoreFramesAvailable==false) {
			if(!(aligneur.getS4aligner().mfccs.getData() instanceof DataEndSignal) && !(aligneur.getS4aligner().mfccs.getData() instanceof DataStartSignal)) {
				mfccs.add(((FloatData) aligneur.getS4aligner().mfccs.getData()).getValues());
			}
		}
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
					int k=mc.length;
					for(int j=0;j<k;j++) {
						means.add(mc[j].getMean());
					}
				}
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
