package fr.loria.synalp.jtrans.speechreco;

import edu.cmu.sphinx.result.Result;

/**
 * "ecoute" les resultats de la reco en live
 * 
 * @author cerisara
 *
 */
public interface RecoListener {
	public void recoEnCours(Result tmpres);
	public void recoFinie(Result finalres, String res);
}
