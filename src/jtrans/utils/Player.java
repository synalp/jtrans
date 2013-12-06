package jtrans.utils;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JOptionPane;

public class Player {
	private int mixidx=-1;
	private SourceDataLine line=null;
	private AudioFormat format=null;
	private AudioInputStream data;
	private boolean stop=false;
	private boolean isplaying=false;

	public void setMixer(int m) {mixidx=m;}

	public void stopPlaying() {
		stop=true;
	}

	public boolean isPlaying() {
		return isplaying;
	}

	private void openLine(int mixidx) {
		System.out.println("openline "+mixidx+" "+line+" "+format);
		if (mixidx<0) {
			try {
				line = AudioSystem.getSourceDataLine(format);
				line.open(format);
			} catch (LineUnavailableException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Veuillez fermer tous les logiciels qui pourraient \n�tre en train d'utiliser la ligne audio");
			}
		} else {
			if (isPlaying()) stopPlaying();
			Mixer.Info[] mixersinfo = AudioSystem.getMixerInfo();
			Mixer mix = AudioSystem.getMixer(mixersinfo[mixidx]);
			DataLine.Info lineinfo = new DataLine.Info(SourceDataLine.class,format);
			try {
				line = (SourceDataLine)mix.getLine(lineinfo);
				System.out.println("line = "+line);
				line.open();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void play(AudioInputStream ais) {
		data=ais;
		format = ais.getFormat();
		openLine(mixidx);
		stop = false;
		line.start();
		Thread remplisseurThread = new Remplisseur();
		remplisseurThread.setPriority(Thread.MAX_PRIORITY);
		remplisseurThread.setName("Thread Remplisseur");
		remplisseurThread.start();
		isplaying=true;
		line.start();
	}

	private class Remplisseur extends Thread {
		public Remplisseur() {
			super("PlayerRemplisseurThread");
		}
		public void run(){
			//Remplissage du buffer
			byte[] frame = new byte[200];

			//on continue à la remplir
			try {
				while (!stop) {
					int nread;
					nread = data.read(frame);
					if (nread<0) break;
					line.write(frame, 0, nread);
				}
				if (!stop)
					while (line.available()>0) //on laisse le temps à la line de se vider
						Thread.sleep(100);
				line.flush();
				line.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			isplaying=false;
		}
	}

}
