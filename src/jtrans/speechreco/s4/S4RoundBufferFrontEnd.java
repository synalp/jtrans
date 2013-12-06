/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

/*
Copyright Christophe Cerisara, Josselin Pierre (1er septembre 2008)

cerisara@loria.fr

Ce logiciel est un programme informatique servant � aligner un
corpus de parole avec sa transcription textuelle.

Ce logiciel est r�gi par la licence CeCILL-C soumise au droit fran�ais et
respectant les principes de diffusion des logiciels libres. Vous pouvez
utiliser, modifier et/ou redistribuer ce programme sous les conditions
de la licence CeCILL-C telle que diffus�e par le CEA, le CNRS et l'INRIA 
sur le site "http://www.cecill.info".

En contrepartie de l'accessibilit� au code source et des droits de copie,
de modification et de redistribution accord�s par cette licence, il n'est
offert aux utilisateurs qu'une garantie limit�e.  Pour les m�mes raisons,
seule une responsabilit� restreinte p�se sur l'auteur du programme,  le
titulaire des droits patrimoniaux et les conc�dants successifs.

A cet �gard  l'attention de l'utilisateur est attir�e sur les risques
associ�s au chargement,  � l'utilisation,  � la modification et/ou au
d�veloppement et � la reproduction du logiciel par l'utilisateur �tant 
donn� sa sp�cificit� de logiciel libre, qui peut le rendre complexe � 
manipuler et qui le r�serve donc � des d�veloppeurs et des professionnels
avertis poss�dant  des  connaissances  informatiques approfondies.  Les
utilisateurs sont donc invit�s � charger  et  tester  l'ad�quation  du
logiciel � leurs besoins dans des conditions permettant d'assurer la
s�curit� de leurs syst�mes et ou de leurs donn�es et, plus g�n�ralement, 
� l'utiliser et l'exploiter dans les m�mes conditions de s�curit�. 

Le fait que vous puissiez acc�der � cet en-t�te signifie que vous avez 
pris connaissance de la licence CeCILL-C, et que vous en avez accept� les
termes.
 */

package jtrans.speechreco.s4;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;

import jtrans.utils.PrintLogger;

/** Le RoundBuffer est le buffer circulaire des donnees
 * en provenance du TemporalSig. */
public class S4RoundBufferFrontEnd extends BaseDataProcessor {

	//------------- Private Fields --------------
	/** Buffer de stockage des donn�es */
	private Data[] buffer;

	/** Pointeur sur la prochaine position � laquelle ins�rer une donn�e */
	private int pointeur;

	private boolean isFilling;

	BaseDataProcessor source=null;
	
	/**
	 * Indique la taille maximum lue depuis le fichier jusqu'a present..;
	 */
	public Integer tailleMaxLueSoFar = 0;

	/**
	 * Indique si le buffer ne peut plus recuperer des data depuis le fichier
	 */
	public boolean endOfFileReached = false;
	/**
	 * Indique si le buffer ne peut plus fournir de nouvelles data
	 */
	public boolean noMoreFramesAvailable = false;
	/**
	 * Au 1er appel, il faut retourner un DataStartSignal
	 */
	public boolean firstCall = true;
	
	private int curFrame=0;
	final short[] sigout = new short[1];

	PrintLogger plog=null;
	
	public boolean gotoFrame(int fr) {
		if (fr<0||(fr>=curFrame&&endOfFileReached)) return false;
		curFrame=fr;
		noMoreFramesAvailable=false;
		return true;
	}
	
	//-------------- Constructors ---------------
	public S4RoundBufferFrontEnd(PrintLogger pl, int bufferSize){
		plog=pl;
		this.pointeur = 0;
		curFrame = 0;
		this.buffer = new Data[bufferSize];
	}//RoundBuffer(int bufferSize)	

	public int getSize(){
		return this.buffer.length;
	}//getSize()


	public void clear(){
		curFrame = 0;
		pointeur = 0;
		tailleMaxLueSoFar=0;
		endOfFileReached = false;
	}//clear

	public void setSource(BaseDataProcessor sig) {
		source=sig;
	}

	/** M�thode utilis�e pour remplir le buffer � partir du signal ouvert
	 * jusqu'� ce qu'il soit plein */
	public void fill(float ratio){
		System.err.println("refill "+ratio);
		if (plog!=null)
			plog.print("please wait, filling in buffer...");
		// TODO: utiliser un thread
		isFilling = true;
		
		Data tab;
		int read = 0;
		int max2read = (int)(ratio*(float)buffer.length);
		while (read < max2read) {
			tab = source.getData();
			if (tab==null) {
				endOfFileReached = true;
				break;
			}
			buffer[pointeur++]=tab;
			if (pointeur>=buffer.length) {
				pointeur %= buffer.length;
			}
			tailleMaxLueSoFar++; read++;
		}
		isFilling = false;
		if (plog!=null)
			plog.print("mfcc buffer filled !");
		System.err.println("filled "+tailleMaxLueSoFar);
	}//fill

	@Override
	public Data getData() throws DataProcessingException {
		if (curFrame<tailleMaxLueSoFar-buffer.length) {
			System.out.println("mfcc rewind asked !");
			// TODO
			// on ne peut pas rewind() avec CE mfffbuf, mais il faut que l'appelant gere le rewind() !
			return null;
		} else if (curFrame>=tailleMaxLueSoFar) {
			if (isFilling) {
				// TODO: wait until filled
				// pour le moment, tout est synchrone, donc ca peut attendre...
				return null;
			} else if (endOfFileReached) {
				noMoreFramesAvailable=true;
				return null;
			} else {
				// charger la suite du buffer !
				while (curFrame>=tailleMaxLueSoFar)
					fill(0.3f);
			}
		}
		// commme il n'y a pas de discontinuite lors du remplissage du buffer circulaire, on a toujours
		// le sample 0 en position 0 initialement
		int posInBuf = (int)(curFrame%buffer.length);
		if (firstCall) {
			firstCall=false;
			return new DataStartSignal(100);
		}
		curFrame++;
		return buffer[posInBuf];
	}

}//class RoundBuffer