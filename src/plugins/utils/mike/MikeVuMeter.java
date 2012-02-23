/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.utils.mike;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class MikeVuMeter extends JPanel {
	AudioBuffer audio;
	boolean stopit = false;

	public MikeVuMeter(AudioBuffer aubuf) {
		audio = aubuf;

		setLayout(new FlowLayout());

		JButton quit = new JButton("quit");
		quit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopit=true;
			}
		});
		add(quit);

		final JProgressBar bar = new JProgressBar();
		add(bar);

		new Thread(new Runnable() {
			@Override
			public void run() {
				byte[] buf = new byte[2];
				try {
					while (!stopit)
						audio.read(buf);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();

		final Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (!stopit) {
						Thread.sleep(10);
						double p = audio.getPower();
						p/=10E6;
						System.out.println("debug power "+p);
						bar.setValue((int)p);
						bar.repaint();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
	}

	public static void main(String args[]) {
		JFrame jf = new JFrame("mike vumeter");
		Microphone mic = Microphone.getMicrophone();
		AudioBuffer buf = new AudioBuffer(mic.getAudioStream());
		MikeVuMeter m = new MikeVuMeter(buf);
		jf.getContentPane().add(m);
		jf.setSize(500,300);
		jf.setVisible(true);
	}
}
