/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.confidenceMeasure;

import plugins.applis.SimpleAligneur.Aligneur;
import plugins.speechreco.acousticModels.HMM.stateModels.GMMDiag;

public class AcousticCM {
	private Aligneur aligneur;
	private GMMDiag goodmod = null;
	private GMMDiag badmod = null;
	
	private float[][] accus = null;
	private int nbon=0, nbad=0;
	private boolean needupdate=false;
	
	public AcousticCM(Aligneur aligneur) {
		this.aligneur=aligneur;
	}
	
	int ns=4;
	public float getCM(int frdeb, int frfin) {
		frdeb = frfin-160*ns;
		if (frdeb<0) frdeb=0;
		if (needupdate) update();
		if (goodmod==null||badmod==null) return 0.5f;
		aligneur.mfccbuf.gotoFrame(frdeb);
		float[] x;
		float cumulconf = 0f;
		int nconf=0;
		for (int i=frdeb;i<frfin;i++) {
			x = aligneur.mfccbuf.getOneVector();
			if (x==null) break;
			goodmod.computeLogLikes(x);
			float goodLoglike = goodmod.getLogLike();
			badmod.computeLogLikes(x);
			float badLoglike = badmod.getLogLike();
			float conf = goodLoglike-badLoglike;
			cumulconf+=conf;
			nconf++;
		}
		if (nconf==0) return 0.5f;
		float conf = cumulconf/(float)nconf;
		return conf;
	}
	
	public void signalUserInfirmed(int frdeb, int frfin) {
		frdeb = frfin-160*ns;
		if (frdeb<0) frdeb=0;
		aligneur.mfccbuf.gotoFrame(frdeb);
		for (int i=frdeb;i<frfin;i++) {
			float[] x = aligneur.mfccbuf.getOneVector();
			if (x==null) break;
			if (accus==null) accus = new float[4][x.length];
			for (int j=0;j<x.length;j++) {
				accus[1][j]+=x[j];
				accus[3][j]+=x[j]*x[j];
			}
			nbad++;
		}
		needupdate=true;
	}
	public void signalUserConfirmed(int frdeb, int frfin) {
		if (frdeb<0) frdeb=0;
		frdeb = frfin-160*ns;
		aligneur.mfccbuf.gotoFrame(frdeb);
		for (int i=frdeb;i<frfin;i++) {
			float[] x = aligneur.mfccbuf.getOneVector();
			if (x==null) break;
			if (accus==null) accus = new float[4][x.length];
			for (int j=0;j<x.length;j++) {
				accus[0][j]+=x[j];
				accus[2][j]+=x[j]*x[j];
			}
			nbon++;
		}
		needupdate=true;
	}
	
	private void update() {
		if (goodmod==null) {
			goodmod = new GMMDiag(1,accus[0].length);
			badmod = new GMMDiag(1,accus[0].length);
		}
		for (int i=0;i<accus[0].length;i++) {
			if (nbon>0) {
				goodmod.setMean(0, i, accus[0][i]/(float)nbon);
				goodmod.setVar(0, i, accus[2][i]/(float)nbon-goodmod.getMean(0, i)*goodmod.getMean(0, i));
			}
			
			if (nbad>0) {
				badmod.setMean(0, i, accus[1][i]/(float)nbad);
				badmod.setVar(0, i, accus[3][i]/(float)nbad-badmod.getMean(0, i)*badmod.getMean(0, i));
			}
		}
		if (nbon>0)
			goodmod.precomputeDistance();
		if (nbad>0)
			badmod.precomputeDistance();
	}
}
