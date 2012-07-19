/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.aligners.sphiinx4;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;

import main.LiveSpeechReco;
import plugins.applis.SimpleAligneur.Aligneur;
import plugins.text.TextEditorRecoListener;
import tools.audio.PlayerGUI;

public class ControlBox extends JPanel implements ActionListener {
	private Aligneur aligneur;
	private JButton alignButton;
	private tools.audio.PlayerGUI playergui;
	
	public PlayerGUI getPlayerGUI() {return playergui;}
	
	public ControlBox(Aligneur main) {
		aligneur = main;
		playergui = new PlayerGUI(main);
		
		alignButton = new JButton("AutoAlign");
		alignButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.useS4aligner=true;
				aligneur.s4fastAutoAlign();
			}
		});
		JButton asrButton = new JButton("AutoTranscript");
		asrButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.asr();
			}
		});
		JButton liveButton = new JButton("Live ASR");
		liveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LiveSpeechReco r = LiveSpeechReco.doReco();
				r.addResultListener(new TextEditorRecoListener(aligneur.edit));
			}
		});
		JButton stopit = new JButton("Stop It !");
		stopit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (aligneur.autoAligner!=null)
					aligneur.autoAligner.stopAutoAlign();
				LiveSpeechReco.stopall();
			}
		});
		
		Box b = Box.createHorizontalBox();
		add(b);
		b.add(Box.createGlue());
		b.add(playergui);
		b.add(Box.createGlue());
		b.add(alignButton);
		b.add(Box.createGlue());
		b.add(asrButton);
		b.add(Box.createGlue());
		b.add(liveButton);
		b.add(Box.createGlue());
		b.add(stopit);
		b.add(Box.createGlue());
		
		playergui.addActionListener(this);
		
		setMaximumSize(new Dimension(10000, alignButton.getPreferredSize().height+200));
	}

	// cette methode est appelee depuis le PlayerGUI
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("playstart")) {
			playergui.setStartSec(aligneur.getCurPosInSec());
			aligneur.newplaystarted();
		} else if (e.getActionCommand().equals("rewind")) {
			aligneur.setCurPosInSec(0);
		} else if (e.getActionCommand().equals("playstop")) {
			aligneur.newplaystopped();
			float prevStartingTime = playergui.getRelativeStartingSec();
			aligneur.setCurPosInSec(prevStartingTime+(float)playergui.getTimePlayed()/1000f);
		}
	}
}
 