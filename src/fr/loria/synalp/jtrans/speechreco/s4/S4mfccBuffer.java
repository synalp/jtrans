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

package fr.loria.synalp.jtrans.speechreco.s4;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor;
import edu.cmu.sphinx.frontend.feature.LiveCMN;
import edu.cmu.sphinx.frontend.filter.Dither;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * 
 * si on veut avoir beaucoup de flexibilite en aller-retour, il vaut mieux ne pas avoir de round-buffer
 * mais plutot conserver tous les MFCCs en RAM...
 * 
 * @author xtof
 *
 */
public class S4mfccBuffer extends BaseDataProcessor {

	//------------- Private Fields --------------
	/** Buffer de stockage des donn�es */
	private ArrayList<Data> buffer = new ArrayList<Data>();

	BaseDataProcessor source=null;
	
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

	public boolean gotoFrame(int fr) {
		if (fr<0||(fr>=curFrame&&endOfFileReached)) return false;
		curFrame=fr;
		noMoreFramesAvailable=false;
		return true;
	}
	
	//-------------- Constructors ---------------
	public S4mfccBuffer() {
		curFrame = 0;
	}

	public int getSize(){
		return this.buffer.size();
	}//getSize()


	public void clear(){
		curFrame = 0;
		endOfFileReached = false;
		buffer.clear();
	}//clear

	public void setSource(BaseDataProcessor sig) {
		source=sig;
	}

	private boolean loadNextFrame() {
		Data d = source.getData();
		if (d==null) return false;
		if (d instanceof DataEndSignal) return false;
		buffer.add(d);
		return true;
	}
	
	@Override
	public Data getData() throws DataProcessingException {
		while (curFrame>=buffer.size()) {
			if (!loadNextFrame()) {
				noMoreFramesAvailable=true;
				return new DataEndSignal(0);
//				return null;
			}
		}
		if (firstCall) {
			firstCall=false;
			return new DataStartSignal(100);
		}
		return buffer.get(curFrame++);
	}


	/**
	 * Returns all MFCC data until the end of the buffer.
	 */
	public List<FloatData> getAllData() {
		List<FloatData> data = new ArrayList<>();

		// Get data
		for (;;) {
			Data d = getData();
			if (d instanceof DataEndSignal) {
				break;
			}

			try {
				data.add(FloatData.toFloatData(d));
			} catch (IllegalArgumentException ex) {
				// not a FloatData/DoubleData
			}
		}

		System.out.println("Got " + data.size() + " frames");
		return data;
	}


	/**
	 * Returns all MFCC data in an audio file.
	 */
	public static List<FloatData> getAllData(File audio, boolean reco)
			throws IOException, UnsupportedAudioFileException
	{
		return getAllData(AudioSystem.getAudioInputStream(audio), reco);
	}


	/**
	 * Returns all MFCC data in an audio file.
	 */
	public static List<FloatData> getAllData(AudioInputStream audio, boolean withMFCC) {
		AudioFileDataSource afds = new AudioFileDataSource(3200, null);
		afds.setInputStream(audio, null);
		S4mfccBuffer mfcc = new S4mfccBuffer();
		mfcc.setSource(getFrontEnd(withMFCC, afds));
		return mfcc.getAllData();
	}


	/**
	 * Converts a list of FloatData instances to a 2D float array.
	 */
	public static float[][] to2DArray(List<FloatData> dataList) {
		float[][] data = new float[dataList.size()][];

		for (int i = 0; i < data.length; i++) {
			data[i] = dataList.get(i).getValues();
			assert 39 == data[i].length;
		}

		return data;
	}


	private static FrontEnd getFrontEnd(boolean withMFCC, DataProcessor... sourceList) {
		ArrayList<DataProcessor> frontEndList = new ArrayList<>();
		for (DataProcessor source: sourceList) {
			if (null != source) {
				frontEndList.add(source);
			}
		}

		frontEndList.add(new Dither(2,false,Double.MAX_VALUE,-Double.MAX_VALUE));
		frontEndList.add(new DataBlocker(50));
		frontEndList.add(new Preemphasizer(0.97));
		frontEndList.add(new RaisedCosineWindower(0.46f,25.625f,10f));
		frontEndList.add(new DiscreteFourierTransform(512, false));

		if (withMFCC) {
			frontEndList.add(new MelFrequencyFilterBank(133.33334, 6855.4976, 40));
			frontEndList.add(new DiscreteCosineTransform(40, 13));
			frontEndList.add(new LiveCMN(12, 100, 160));
			frontEndList.add(new DeltasFeatureExtractor(3));
		}

		return new FrontEnd(frontEndList);
	}

	public static FrontEnd getFrontEnd(DataProcessor... sourceList) {
		return getFrontEnd(true, sourceList);
	}

}
