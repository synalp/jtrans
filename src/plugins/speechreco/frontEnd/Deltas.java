/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.frontEnd;

import java.util.Arrays;
import java.util.List;

public class Deltas implements FrontEnd {
	
	private int cepstraBufferSize;
	private int cepstraBufferEdge;
	private int bufferPosition;
	private int currentPosition;
	private float[][] cepstraBuffer;
	/**
	 * nb of frames before (or after) the current frame used to compute deltas
	 */
	public int window = 3;
	private int jp1, jp2, jp3, jf1, jf2, jf3;
	private boolean firstFrame=true, lastFrame=false;
	private FrontEnd input;
	private List<float[]> outputQueue;
	private float[] curobs = null;
	
	public int getNcoefs() {
		return input.getNcoefs() * 3;
	}
	
	private int replicateLastCepstrum() {
		float[] last = null;
		if (bufferPosition > 0) {
			last = this.cepstraBuffer[bufferPosition - 1];
		} else if (bufferPosition == 0) {
			last = cepstraBuffer[cepstraBuffer.length - 1];
		} else {
			throw new Error("BufferPosition < 0");
		}
		for (int i = 0; i < window; i++) {
			addCepstrum(last);
		}
		return window;
	}
	public float[] getOneVector() {
		if (outputQueue.size() == 0) {
			float[] c = input.getOneVector();
			if (c==null) {
				if (!lastFrame) {
					int n = replicateLastCepstrum();
					computeFeatures(n);
					lastFrame=true;
				}
			} else if (firstFrame) {
				firstFrame = false;
				int n = processFirstCepstrum(c);
				computeFeatures(n);
			} else {
				addCepstrum(c);
				computeFeatures(1);
			}
		}
		if (outputQueue.size() > 0) {
			curobs = outputQueue.remove(0);
			return curobs;
		} else {
			return null;
		}
	}
	
	public Deltas(FrontEnd cepin) {
		input = cepin;
		cepstraBufferSize = 256;
		cepstraBuffer = new float[cepstraBufferSize][cepin.getNcoefs()];
		cepstraBufferEdge = cepstraBufferSize - (window * 2 + 2);
		outputQueue = new java.util.Vector<float[]>();
		reset();
	}
	private void reset() {
		firstFrame=true; lastFrame=false;
		bufferPosition = 0;
		currentPosition = 0;
	}
	
	private void addCepstrum(float[] cepstrum) {
		cepstraBuffer[bufferPosition++] = cepstrum;
		bufferPosition %= cepstraBufferSize;
	}
	private void computeFeature() {
		float[] feature = computeNextFeature();
		outputQueue.add(feature);
	}
	private float[] computeNextFeature() {
		float[] currentCepstrum = cepstraBuffer[currentPosition++];
		float[] mfc3f = cepstraBuffer[jf3++];
		float[] mfc2f = cepstraBuffer[jf2++];
		float[] mfc1f = cepstraBuffer[jf1++];
		float[] current = currentCepstrum;
		float[] mfc1p = cepstraBuffer[jp1++];
		float[] mfc2p = cepstraBuffer[jp2++];
		float[] mfc3p = cepstraBuffer[jp3++];
		float[] feature = new float[current.length * 3];
		// CEP; copy all the cepstrum data
		int j = 0;
		for (int k = 0; k < current.length; k++) {
			feature[j++] = (float) current[k];
		}
		// System.arraycopy(current, 0, feature, 0, j);
		// DCEP: mfc[2] - mfc[-2]
		for (int k = 0; k < mfc2f.length; k++) {
			feature[j++] = (float) (mfc2f[k] - mfc2p[k]);
		}
		// D2CEP: (mfc[3] - mfc[-1]) - (mfc[1] - mfc[-3])
		for (int k = 0; k < mfc3f.length; k++) {
			feature[j++] = (float) ((mfc3f[k] - mfc1p[k]) - (mfc1f[k] - mfc3p[k]));
		}
		if (jp3 > cepstraBufferEdge) {
			jf3 %= cepstraBufferSize;
			jf2 %= cepstraBufferSize;
			jf1 %= cepstraBufferSize;
			currentPosition %= cepstraBufferSize;
			jp1 %= cepstraBufferSize;
			jp2 %= cepstraBufferSize;
			jp3 %= cepstraBufferSize;
		}
		return feature;
	}
	
	private void computeFeatures(int totalFeatures) {
		if (totalFeatures == 1) {
			computeFeature();
		} else {
			// create the Features
			for (int i = 0; i < totalFeatures; i++) {
				computeFeature();
			}
		}
	}
	
	private int processFirstCepstrum(float[] cepstrum) {
		// At the start of an utterance, we replicate the first frame
		// into window+1 frames, and then read the next "window" number
		// of frames. This will allow us to compute the delta-
		// double-delta of the first frame.
		Arrays.fill(cepstraBuffer, 0, window + 1, cepstrum);
		bufferPosition = window + 1;
		bufferPosition %= cepstraBufferSize;
		currentPosition = window;
		currentPosition %= cepstraBufferSize;
		int numberFeatures = 1;
		for (int i = 0; i < window; i++) {
			float[] next = input.getOneVector();
			if (next==null) {
				replicateLastCepstrum();
				numberFeatures += i;
				break;
			}
			addCepstrum(next);
		}
		jp1 = currentPosition - 1;
		jp2 = currentPosition - 2;
		jp3 = currentPosition - 3;
		jf1 = currentPosition + 1;
		jf2 = currentPosition + 2;
		jf3 = currentPosition + 3;
		if (jp3 > cepstraBufferEdge) {
			jf3 %= cepstraBufferSize;
			jf2 %= cepstraBufferSize;
			jf1 %= cepstraBufferSize;
			jp1 %= cepstraBufferSize;
			jp2 %= cepstraBufferSize;
			jp3 %= cepstraBufferSize;
		}
		return numberFeatures;
	}
}
