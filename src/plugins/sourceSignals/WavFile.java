/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.sourceSignals;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * En fait, cette classe supporte les .wav, mais aussi les fichiers compress�s !
 * 
 * @author cerisara
 *
 */
public class WavFile {
	float[] obs;
	byte[] buf;
    ByteBuffer bf;
    AudioInputStream son;

    public AudioFormat getFormat() {
    		return son.getFormat();
    }
    
    public WavFile(String nom) throws Exception {
    	AudioInputStream sonFormatOrigine = AudioSystem.getAudioInputStream(new File(nom));
    	// conversion en 16kHz:
//  	son = AudioSystem.getAudioInputStream(new AudioFormat(16000,16,1,true,sonFormatOrigine.getFormat().isBigEndian()),sonFormatOrigine);
    	son = AudioSystem.getAudioInputStream(new AudioFormat(16000,16,1,true,false),sonFormatOrigine);

    	System.out.println("opened wav converted to: "+son.getFormat());
    	
    	bf = ByteBuffer.allocate(2);
		if (son.getFormat().isBigEndian())
			bf.order(ByteOrder.BIG_ENDIAN);
		else
			bf.order(ByteOrder.LITTLE_ENDIAN);
		obs=new float[1];
	}//constructeur

	
	
	public void close() {
		try {
			son.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}//close()
	
	short[] obsshort = new short[1];
	public short[] getShortSamples() {
		try {
			bf.clear();
			byte[] bb = bf.array();
			int nread = son.read(bb);
			if (nread>0) {
				bf.rewind();
				obsshort[0] = bf.getShort();
				return obsshort;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}//getShortSamples
	
	public float[] getSamples() {
		try {
		    bf.clear();
			byte[] bb = bf.array();
			int nread = son.read(bb);
			if (nread>0) {
				// conversion des shorts en float
				bf.rewind();
				obs[0] = (float)bf.getShort();
				return obs;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}//getSamples
	
	
	public float getFrameRate(){
		return son.getFormat().getFrameRate();
	}
	
	
	public float getFrameLength(){
		return son.getFrameLength();
		
	}
}//Class WavFile
