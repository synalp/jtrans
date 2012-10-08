/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.utils.mike;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class AudioBuffer extends AudioInputStream {
	AudioFormat audioformat;
	
	AudioInputStream input;
	byte[] bybuf;
	
	// nb total de bytes lus so far
	int totnread=0;
	
	// indice du dernier byte lu
	int endpos=0;
	
	// indice du cyte courant (il peut "preceder" endpos si on est revenu en arriere)
	int curpos=0;
	
	public AudioBuffer(AudioInputStream in) {
		super(in,in.getFormat(),in.getFrameLength());
		input = in;
		audioformat = in.getFormat();
		bybuf = new byte[(int)audioformat.getSampleRate()*2*60]; // 1min
	}
	
	public int available() {
		return audioformat.getSampleSizeInBits()/16;
	}
	
	public long skip(long n) throws IOException {
		long nskipped=0;
		byte[] tmp = new byte[2];
		for (;;) {
			int x= read(tmp);
			if (x>=0) nskipped += x;
			else break;
		}
		return nskipped;
	}
	
	byte[] onesample = new byte[2];
	public int read(byte[] buf, int off, int len) throws IOException {
		int mynread=0;
		for (int bufidx=0;bufidx<len;) {
			if (curpos>=endpos) {
				// il faut lire de nouveaux samples
				int nread = input.read(onesample);
				if (nread<0) {
					if (mynread>0) return mynread;
					else return -1;
				}
				mynread+=nread;
				totnread+=nread;
				if (endpos>=bybuf.length) endpos=0;
				// je suppose qu'on lit les samples par paire de bytes
				bybuf[endpos++]=onesample[0];
				bybuf[endpos++]=onesample[1];
			}
			if (curpos>=bybuf.length) curpos=0;
			buf[bufidx++]=bybuf[curpos++];
			buf[bufidx++]=bybuf[curpos++];
		}
		return mynread;
	}
	public int read(byte[] buf) throws IOException {
		return read(buf,0,buf.length);
	}
	
	public int read() throws IOException {
		throw new IOException();
	}
	
	// ============= marks
	
	public boolean markSupported() {
		return false;
	}
	
	public void mark(int readlimit) {
		
	}

	public void reset() throws IOException {
		
	}
	
	// ================ specific
	
	public double getPower() {
		ByteBuffer bf = ByteBuffer.wrap(bybuf);
		int deb = curpos-(int)audioformat.getSampleRate()/100;
		double p = 0;
		if (deb<0) {
			for (int i=bybuf.length+deb;i<bybuf.length;i+=2) {
				short s = bf.getShort(i);
				p+=s*s;
			}
			for (int i=0;i<curpos;i+=2) {
				short s = bf.getShort(i);
				p+=s*s;
			}
		} else {
			for (int i=deb;i<curpos;i+=2) {
				short s = bf.getShort(i);
				p+=s*s;
			}
		}
		return p;
	}
	
	public static void main(String args[]) throws Exception {
		Microphone mic = Microphone.getMicrophone();
		AudioBuffer m =new AudioBuffer(mic.getAudioStream());
		
		AudioSystem.write(m, AudioFileFormat.Type.WAVE, new File("C:/oo.wav"));
	}
}
