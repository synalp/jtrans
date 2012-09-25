package plugins.sourceSignals;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.frontend.util.VUMeterMonitor;
import edu.cmu.sphinx.frontend.util.WavWriter;

public class Mike2wav {
	JDialog mainwindow = null;
	Microphone mike=null;
	
	boolean save=false;
	
	public String waveout = "wavout.wav";
	
	BaseDataProcessor last = null;
	WavWriter wavwriter = null;
	VUMeterMonitor vum = null;
	ArrayBlockingQueue<Boolean> stopAll = new ArrayBlockingQueue<Boolean>(1);
	
	private void stopit() {
		BaseDataProcessor ender = new BaseDataProcessor() {
			@Override
			public Data getData() throws DataProcessingException {
				return new DataEndSignal(0);
			}
		};
		wavwriter.setPredecessor(ender);
		wavwriter.getData();
		save=false;
		wavwriter.setPredecessor(null);
		last=vum;
		System.out.println("saving ended");
		
		try {
			stopAll.put(true);
			Thread.yield();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		mike.stopRecording();
		vum.getVuMeterDialog().dispose();
//		JOptionPane.showMessageDialog(null, "wav saved in "+waveout);
		if (listener!=null) listener.actionPerformed(new ActionEvent(this, 0, "wavesaved "+waveout));
	}
	
	// singleton
	private Mike2wav(int mixidx) {
		mxidx=mixidx;

		Mixer.Info[] mixinfos = AudioSystem.getMixerInfo();
		System.out.println("mixinfos "+mixinfos.length);
		for (int m=0;m<mixinfos.length;m++) {
			
			Line.Info[] linf = AudioSystem.getMixer(mixinfos[m]).getTargetLineInfo();
			System.out.println("mixer "+m+" "+mixinfos[m]+" nlines="+linf.length);
			for (int i=0;i<linf.length;i++) {
				if (linf[i] instanceof DataLine.Info) {
					DataLine.Info di = (DataLine.Info)linf[i];
					System.out.println("linf "+Arrays.toString(di.getFormats()));
				}
			}
		}
		
		wavwriter = new WavWriter(waveout, true, 16, true, false);
		BaseDataProcessor starter = new BaseDataProcessor() {
			@Override
			public Data getData() throws DataProcessingException {
				return new DataStartSignal(16000);
			}
		};
		wavwriter.setPredecessor(starter);
		wavwriter.getData();
		
		mike = new Microphone(16000, 16, 1, true, true, true, 10, false, "average", 0, ""+mxidx, 6400);
		DataBlocker datablocker = new DataBlocker(100);
		datablocker.setPredecessor(mike);
		vum = new VUMeterMonitor();
		vum.setPredecessor(datablocker);
		
		mainwindow = vum.getVuMeterDialog();
		mainwindow.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar()=='s') {
					if (save) {
						stopit();
					} else {
						save=true;
						wavwriter.setPredecessor(vum);
						last=wavwriter;
					}
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {
			}
			@Override
			public void keyPressed(KeyEvent e) {
			}
		});
		
		mike.initialize();
		mike.startRecording();
		Thread aspirateur = new Thread(new Runnable() {
			@Override
			public void run() {
				for (;;) {
					if (!stopAll.isEmpty()) break;
					if (last!=null) last.getData();
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				System.out.println("end of aspirateur");
			}
		});
		aspirateur.start();
		last=vum;
	}
	private static Mike2wav unik = null;
	private int mxidx=0;
	public static Mike2wav getMike2wav(int mixidx) {
		if (unik==null) unik = new Mike2wav(mixidx);
		return unik;
	}
	
	/**
	 * Mike2wav prévient lorsqu'un nouveau fichier .wav a été enregistré
	 * @param al
	 */
	public void addActionListener(ActionListener al) {
		listener=al;
	}
	private ActionListener listener=null;
	
	public void help() {
		JOptionPane.showMessageDialog(null, "Press 's' to start recording and 's' again to stop");
	}
	
	public static void main(String args[]) {
		Mike2wav m = Mike2wav.getMike2wav(1);
	}
}
