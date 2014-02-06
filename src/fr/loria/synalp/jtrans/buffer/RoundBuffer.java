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

package fr.loria.synalp.jtrans.buffer;

import fr.loria.synalp.jtrans.utils.ProgressDisplay;

/** Le RoundBuffer est le buffer circulaire des donnees
 * en provenance du TemporalSig. */
public class RoundBuffer implements JTemporalSig {

	ProgressDisplay plog;
	
	//------------- Private Fields --------------
	/** Buffer de stockage des donn�es */
	private short[] buffer;

	/** Pointeur sur la prochaine position � laquelle ins�rer une donn�e */
	private int pointeur;

	private boolean isFilling;

	JTemporalSig source=null;
	
	/*
	 * Indique la taille maximum lue depuis le fichier jusqu'a present..;
	 */
	public Integer tailleMaxLueSoFar = 0;

	public boolean endOfFileReached = false;
	
	public long samplePosition;
	final short[] sigout = new short[1];

	//-------------- Constructors ---------------
	public RoundBuffer(ProgressDisplay pl, int bufferSize){
		plog = pl;
		this.pointeur = 0;
		samplePosition = 0;
		this.buffer = new short[bufferSize];
	}//RoundBuffer(int bufferSize)	

	//--------------- Methodes -----------------
	public void rewind() {
		if (source!=null) {
			source.rewind();
			clear();
		}
	}
	public short[] getSamples() {
		sigout[0]=getSample(samplePosition++);
		return sigout;
	}
	
	public void setSize(int size){
		buffer = new short[size];
		clear();
	}//setSize


	public int getSize(){
		return this.buffer.length;
	}//getSize()


	public void clear(){
		samplePosition = 0;
		pointeur = 0;
		tailleMaxLueSoFar=0;
		endOfFileReached = false;
	}//clear

	public float getFrameRate() {return source.getFrameRate();}

	public void setSource(JTemporalSig sig) {
		source=sig;
		rewind();
	}

	/** M�thode utilis�e pour remplir le buffer � partir du signal ouvert
	 * jusqu'� ce qu'il soit plein */
	public void fill(float ratio){
		if (source==null) return;
		System.err.println("refill "+ratio);
		plog.setIndeterminateProgress("Filling in buffer...");
		// TODO: utiliser un thread
		isFilling = true;
		short[] tab;
		int read = 0;
		int max2read = (int)(ratio*(float)buffer.length);
		while (read < max2read) {
			tab = source.getSamples();
			if (tab==null) {
				endOfFileReached = true;
				break;
			}
			int i;
			for (i=0;i<tab.length;i++) {
				addValue(tab[i]);
				tailleMaxLueSoFar++; read++;
			}
		}
		plog.setProgressDone();
		isFilling = false;
	}//fill

	public void addValue(short value){
		buffer[this.pointeur] = value;
		pointeur++;
		if (pointeur>=buffer.length) {
			pointeur %= buffer.length;
		}
	}

	/** M�thode permettant de r�cup�rer le sample demand�.
	 * @param sample position par rapport au d�but du fichier
	 * @return le sample demand� */
	public short getSample(long sample){	
		if(sample < 0) return 0;

		if (sample<tailleMaxLueSoFar-buffer.length) {
			if (isFilling) return 0;
			// recharger le debut du buffer
			source.rewind();
			clear();
			fill(0.3f);
		} else if (sample>=tailleMaxLueSoFar) {
			if (isFilling || endOfFileReached) {
				if (sample>=tailleMaxLueSoFar) return 0;
			} else {
				// charger la suite du buffer !
				fill(0.3f);
			}
		}
		// commme il n'y a pas de discontinuite lors du remplissage du buffer circulaire, on a toujours
		// le sample 0 en position 0 initialement
		int posInBuf = (int)(sample%buffer.length);
		return buffer[posInBuf];
	}//getValueAtPosiFromStart


	//------- toString de travail ------
	public String toString(){
		String res = "[ ";
		for(int value:buffer)
			res += value + " ";

		res += "]\n[ ";

		for(int i = 0; i < buffer.length; ++i){
			if(i == pointeur)
				res += "# ";
			else 
				res += "  ";
		}

		return res+"]";
	}//toString


}//class RoundBuffer