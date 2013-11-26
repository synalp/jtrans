/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package speechreco.frontEnd;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import plugins.buffer.JTemporalSig;

/**
 * This class computes a stream of cepstrum from a raw signal stream
 * 
 * @author cerisara
 *
 */
public class MFCC implements FrontEnd {
	Deltas dd;
	TimeWindow w;
	
	public MFCC(JTemporalSig sig) {
		w = new TimeWindow(sig);
		Hamming ham = new Hamming(w);
		FFT fft = new FFT(ham);
		MelFreq mel = new MelFreq(fft,w.sampPeriod());
		DCT dct = new DCT(mel);
		dd = new Deltas(dct);
	}
	
	public int getNcoefs() {return dd.getNcoefs();}
	
	public float[] getOneVector() {
		float[] rep = dd.getOneVector();
		return rep;
	}
	
	/**
	 * compare les prefixes dans toute la liste pour extraire le prefixe maximal commun a tous les elements de la liste:
	 * c'est ce prefixe qu'il faudra enlever pour sauver les MFCC, car la suite des paths est une information
	 * importante permettant de discriminer entre les elements.
	 * 
	 * @param list
	 */
	static String checkConstantPrefix(String list) throws IOException {
		BufferedReader f = new BufferedReader(new FileReader(list));
		String pref = null;
		String s = f.readLine();
		if (s==null) return null;
		int i=s.lastIndexOf('/');
		if (i>=0)
			pref = s.substring(0,i);
		else return null;
		
		for (;;) {
			s = f.readLine();
			if (s==null) break;
			i=s.lastIndexOf('/');
			if (i>=0) {
				String p = s.substring(0,i);
				while (!p.startsWith(pref)) {
					// il faut diminuer la taille du prefixe !
					i=p.lastIndexOf('/');
					if (i<0) return null;
					p=p.substring(0,i);
					i=pref.lastIndexOf('/');
					if (i<0) return null;
					pref=pref.substring(0,i);
				}
			}
		}
		f.close();
		return pref;
	}
}
