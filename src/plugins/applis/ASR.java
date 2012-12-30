/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.applis;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import plugins.utils.mike.AudioBuffer;
import plugins.utils.mike.Microphone;
import plugins.utils.mike.MikeVuMeter;

public class ASR extends JPanel {
	AudioBuffer buf;
	
	public ASR() {
		super();
		setLayout(new FlowLayout());
		Microphone mic = Microphone.getMicrophone();
		buf = new AudioBuffer(mic.getAudioStream());
		MikeVuMeter m = new MikeVuMeter(buf);
		add(m);
		
		JButton push2talk = new JButton("push to talk");
		add(push2talk);
		push2talk.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				
			}
		});
	}
	
	public static void main(String args[]) {
		ASR m = new ASR();
		JFrame jf = new JFrame("ASR");
		jf.getContentPane().add(m);
		jf.setSize(500,500);
		jf.setVisible(true);
	}
}
