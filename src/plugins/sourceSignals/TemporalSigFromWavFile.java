/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.sourceSignals;

import java.io.File;

import javax.sound.sampled.AudioFormat;

import plugins.buffer.JTemporalSig;

public class TemporalSigFromWavFile implements JTemporalSig {
	File openedFile;
	WavFile openedWaveFile;
	
	public float getFrameRate() {
		return openedWaveFile.getFrameRate();
	}
	
	public void openWavFile(File file) throws Exception {
		openedFile = file;
		openedWaveFile = new WavFile(file.getAbsolutePath());
	}//openFile

	public void close() {
		openedWaveFile.close();
	}
	
	public void rewind() {
		openedWaveFile.close();
		try {
			openedWaveFile = new WavFile(openedFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public short[] getSamples() {
		short[] s = openedWaveFile.getShortSamples();
		return s;
	}
	
	public AudioFormat getAudioFormat() {
		return openedWaveFile.getFormat();
	}
	
}
