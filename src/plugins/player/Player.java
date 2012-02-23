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

package plugins.player;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JOptionPane;

import plugins.applis.SimpleAligneur.Aligneur;
import plugins.buffer.JTemporalSig;

/** 
 * Cette classe est le player en lui m�me : 
 * c'est cette classe qui se charge d'ouvrir la dataLine, 
 * de la remplir et de la lancer.
 */
public class Player {
	//-------- Private Fields -------
	private volatile boolean stop;
	private SourceDataLine line=null;
	private Clip clip=null;
	private JTemporalSig data;
	AudioFormat format;

	long firstSamplePlayed = 0;

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
			System.out.println("op1");
			if (isPlaying()) stopPlaying();
			System.out.println("op2");
			Mixer.Info[] mixersinfo = AudioSystem.getMixerInfo();
			Mixer mix = AudioSystem.getMixer(mixersinfo[mixidx]);
			DataLine.Info lineinfo = new DataLine.Info(SourceDataLine.class,format);
			System.out.println("op3 "+lineinfo);
			try {
				line = (SourceDataLine)mix.getLine(lineinfo);
				System.out.println("line = "+line);
				System.out.println("op4");
				line.open();
				System.out.println("op5");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	private void openLineClip(int mixidx, long firstSample, long lastSample) {
		if (lastSample-firstSample>1600000) {
			System.err.println("ERROR: selection too long to play");
			return;
		}
		if (isPlaying()) stopPlaying();
		try {
			if (mixidx<0)
				clip = AudioSystem.getClip(null);
			else {
				Mixer.Info[] mixersinfo = AudioSystem.getMixerInfo();
				clip = AudioSystem.getClip(mixersinfo[mixidx]);
			}

			int len = (int)(lastSample-firstSample);
			len<<=1;
			byte[] selectedbuf = new byte[len];
			for (int i=0;i<len;) {
				short[] s=data.getSamples();
				for (int j=0;j<s.length&&i<len;j++) {
					selectedbuf[i++] = (byte) (s[j] & 0xff);
					selectedbuf[i++] = (byte) (s[j] >> 8);
				}
			}
			clip.open(format,selectedbuf,0,len);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,"Veuillez fermer tous les logiciels qui pourraient \n�tre en train d'utiliser la ligne audio");
		}
	}
	
	//--------- Constructor -------
	public Player(JTemporalSig data, AudioFormat format){
		this.format=format;
		stop = true;
		this.data = data;
	}//Constructor

	public void setData(JTemporalSig data) {
		this.data = data;
	}

	public boolean isPlaying() {
		return (!stop) || clip!=null;
	}

	//----------- Methodes -----------
	public void play(int mixidx, PlayerListener endListener, long firstSample){
		openLine(mixidx);
		System.out.println("play0");
		stop = false;
		line.start();
		System.out.println("play1");
		Thread remplisseurThread = new Remplisseur(endListener);
		remplisseurThread.setPriority(Thread.MAX_PRIORITY);
		remplisseurThread.setName("Thread Remplisseur");
		remplisseurThread.start();
		line.start();
		firstSamplePlayed = firstSample;
	}//play

	PlayerListener playlist=null;
	public void play(int mixidx, final PlayerListener endListener, long firstSample, long lastSample){
		playlist=endListener;
System.err.println("play "+firstSample+" "+lastSample);
		long nSamples = lastSample-firstSample;
		if (nSamples<=0) return;
		openLineClip(mixidx, firstSample, lastSample);
		clip.addLineListener(new LineListener() {
			public void update(LineEvent e) {
				if (e.getType()==LineEvent.Type.STOP) {
					stopPlaying();
				}
			}
		});
		stop=false;
		clip.start();
		firstSamplePlayed = firstSample;
	}//play

	public void stopPlaying() {
		System.err.println("stop playing");
		stop = true;
		if(line != null) {
			//line.drain();
			line.stop();
			line.flush();
			line.drain();
			line.close();
			line=null;
		}
		if (clip!=null) {
			clip.stop();
			clip.flush();
			clip.close();
			clip=null;
		}
		if (playlist!=null)
			playlist.playerHasFinished();
	}//stopReading()

	public long getLastSamplePlayed() {
		if (line==null) {
			if (clip==null) return -1;
			return firstSamplePlayed + clip.getLongFramePosition();
		}
		return firstSamplePlayed + line.getLongFramePosition();
	}

	//----------------------------------------------
	//---------- Private Class ---------------------
	//----------------------------------------------


	/**
	 * Classe charg�e de remplir la line audio
	 * tant qu'il y a des donn�es � lire 
	 * et tant qu'il y a de la place dans le buffer de la ligne audio.
	 */
	private class Remplisseur extends Thread {
		PlayerListener listener = null;
		public Remplisseur(PlayerListener l) {
			super("PlayerRemplisseurThread");
			listener=l;
		}
		public void run(){
			//Remplissage du buffer
			short[] temptab;
			byte[] frame = new byte[2];

			//on continue � la remplir
			while ( (temptab = data.getSamples()) != null && !stop){
				frame[0] = (byte) (temptab[0] & 0xff);
				frame[1] = (byte) (temptab[0] >> 8);
				line.write(frame, 0, 2);
			}//while
			if(temptab == null){
				while (line.available()>0) //on laisse le temps à la line de se vider
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					listener.playerHasFinished();
			}
		}
	}
	private class RemplisseurSegment extends Thread {
		public long nSamples=-1;
		public RemplisseurSegment(long ns) {
			nSamples=ns;
		}
		public void run(){
			//Remplissage du buffer
			short[] temptab;
			byte[] frame = new byte[2];
			long nSamplesSent = 0;

			//on continue � la remplir
			while ( (temptab = data.getSamples()) != null && !stop && nSamplesSent<nSamples){
				frame[0] = (byte) (temptab[0] & 0xff);
				frame[1] = (byte) (temptab[0] >> 8);
				line.write(frame, 0, 2);
				nSamplesSent++;
			}//while
			System.err.println("sent "+nSamplesSent+" "+nSamples+" "+line.available());
		}
	}

}//class Player
