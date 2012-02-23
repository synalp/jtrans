/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.frontEnd;

import java.util.LinkedList;
import java.util.List;

import plugins.buffer.JTemporalSig;

/**
 * This module shall be put just after the Microphone.
 * It takes the (variable size) inputs from the mike and stores them
 * in a buffer so that its output is a fixed size nb of coefs suitable to
 * be analyzed by FFT
 * 
 * @author cerisara
 *
 */
public class TimeWindow implements FrontEnd {
	public final int winSize = 410;
	public final int winShift = 160;
	
	private JTemporalSig tsig;
    private DataList audioList;
    private float [] oneWin;
    // nb de short qu'il y a deja dans la fenetre courante
    private int curpos;
    private float[] obs;
    
    public int getNcoefs() {
    	return winSize;
    }
    
	public TimeWindow(JTemporalSig sig) {
		tsig=sig;
		audioList = new DataList();
		oneWin = new float[winSize];
		curpos=0;
	}
	
	// VAD
	public boolean isSpeech() {
		if (obs==null) return false;
		float nrj=0f;
		for (int i=0;i<obs.length;i++)
			nrj+=Math.abs(obs[i]);
		nrj = (float)Math.log(nrj);
		System.err.println("nrj= "+nrj+" "+obs.length);
		
		return true;
	}
	
	private int fillInObs(short[] x) {
		int pos = 0;
		// on cree une (ou plus) nouvelle obs pleine
		int nObsCreated = 0;
		for (;;) {
			// on complete l'obs courant
			int n2fill = x.length-pos;
			if (n2fill+curpos>winSize)
				n2fill = winSize-curpos;
			for (int i=0, j=curpos;i<n2fill;i++,j++) oneWin[j]=x[pos++];
			curpos += n2fill;
			if (curpos<winSize) {
				// on n'a pas assez de data: on cree simplement une obs incomplete
				return nObsCreated;
			}
			// on a trop de data, on cree une obs complete
			audioList.add(oneWin);
			nObsCreated++;
			float [] newWin = new float[winSize];
			// on remplit la debut de la nouvelle obs avec le shift de l'ancienne
			System.arraycopy(oneWin,winShift,newWin,0,winSize-winShift);
			oneWin=newWin;
			curpos = winSize-winShift;
		}
	}
	
	public float[] getOneVector() {
		while (audioList.size() == 0) {
			// le buffer est vide: il faut le remplir
			int nobs;
			short[] obsin;
			for (;;) {
				obsin = tsig.getSamples();
				if (obsin==null) {
					return null;
				}
				// on complete l'obs courante
				nobs = fillInObs(obsin);
//				nsamp = obsin.length + curpos;
				if (nobs > 0)
					// on a assez de data: on peut s'arreter de lire
					break;
			}
		}
		obs = audioList.remove();
		return obs;
	}
	
	public float sampPeriod() {
		return 10000000f/16000f;
	}
}

class DataList {
	/**
	 * Manages the data as a FIFO queue
	 */
	private List list;

	/**
	 * Creates a new data list
	 */
	public DataList() {
		list = new LinkedList();
	}

	/**
	 * Adds a data to the queue
	 *
	 * @param data the data to add
	 */
	public synchronized void add(float[] data) {
		list.add(data);
		notify();
	}

	/**
	 * Returns the current size of the queue
	 *
	 * @return the size of the queue
	 */
	public synchronized int size() {
		return list.size();
	}

	public synchronized void clear() {
		list.clear();
	}
	
	/**
	 * Removes the oldest item on the queue
	 *
	 * @return the oldest item
	 */
	public synchronized float[] remove() {
		try {
			while (list.size() == 0) {
				// System.out.println("Waiting...");
				wait();
			}
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		float[] data = (float[]) list.remove(0);
		if (data == null) {
			System.out.println("DataList is returning null.");
		}
		return data;
	}
}
