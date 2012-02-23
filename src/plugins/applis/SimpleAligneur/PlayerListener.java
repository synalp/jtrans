/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.applis.SimpleAligneur;

import plugins.text.elements.Element_Mot;

/**
 * grise le mot en cours de lecture lorsque l'alignement est connu
 * 
 * @author cerisara
 *
 */
public class PlayerListener extends Thread {
	Aligneur aligneur;
	int deltat;

	int segGrayed=-1;

	private boolean stop=false;

	private boolean threadSuspended = true;

	public PlayerListener(Aligneur aligneur, int intervalleMilli) {
		super("playerlistener thread");
		this.aligneur=aligneur;
		deltat=intervalleMilli;
		start();
	}

	public void kill() {
		unpause();
		stop=true;
	}
	
	public static int millisec2frame(long ms) {
		return (int)(ms/10);
	}
	public static long frame2sample(int frame) {
		long f=frame;
		f*=160;
		return f;
	}
	public static int sample2frame(long sample) {
		long fr = sample/160;
		return (int)fr;
	}

	long untilsample=-1;
	public void playUntilFrame(int frame) {
		untilsample = frame2sample(frame);
	}

	public synchronized void pause() {
		threadSuspended=true;
	}
	public synchronized void unpause() {
		threadSuspended=false;
		notify();
	}

	public void run() {
		/*
		try {
			while (!stop) {
				synchronized(this) {
					if (threadSuspended) {
						// le play recommence
						while (threadSuspended) wait();
						segGrayed=-1;
					}
				}
				sleep(deltat);
				if (aligneur!=null && aligneur.player!=null && aligneur.alignement!=null) {
					long pos = aligneur.player.getLastSamplePlayed();
					if (untilsample>=0 && pos>=untilsample) {
						untilsample=-1;
						aligneur.stopPlaying();
					}
					
					int newSeg = aligneur.alignement.getSegmentAtFrame(sample2frame(pos));
					if (newSeg>=0&&newSeg!=segGrayed) {
						for (Element_Mot m : aligneur.edit.getListeElement().getMots()) {
							if (m.posInAlign==newSeg) {
								aligneur.edit.griseMot(m);
								segGrayed=newSeg;
								break;
							}
						}
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		*/
	}

}
