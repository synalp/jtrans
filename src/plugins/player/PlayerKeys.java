/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.player;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JLabel;

import plugins.buffer.RoundBuffer;
import plugins.sourceSignals.TemporalSigFromWavFile;
import utils.PrintLogger;

public class PlayerKeys extends JFrame implements PlayerListener {
	public Player player=null;
	
	public PlayerKeys() {
		super("player keys");
		initgui();
		setSize(500,200);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setVisible(true);
	}
	
	public void playerHasFinished() {
		
	}
	
	private void initgui() {
		JLabel l = new JLabel("F1: play   ESC: stop");
		getContentPane().add(l);
		
		final PlayerListener li = this;
		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
				case 112: // F1
					player.play(-1,li,0);
					break;
				case 27: // ESC
					player.stopPlaying();
					break;
				}
			}
		});
	}

	public static void main(String args[]) throws Exception 
	{
		TemporalSigFromWavFile wav = new TemporalSigFromWavFile();
		wav.openWavFile(new File(args[0]));
		RoundBuffer buf = new RoundBuffer(new PrintLogger() {
			public void print(String msg) {
				System.out.println(msg);
			}
		}, 10000000);
		buf.setSource(wav);
		plugins.player.Player player = new plugins.player.Player(buf,wav.getAudioFormat());
		PlayerKeys gui = new PlayerKeys();
		gui.player=player;
	}
}
