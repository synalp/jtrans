package tools.audio;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import plugins.applis.SimpleAligneur.Aligneur;

/**
 * on peut declarer des ActionListener qui recoive du player des events
 * lorsque le play start et end.
 * 
 * @author cerisara
 *
 */
public class PlayerGUI extends JPanel {
	private Player player=new Player();
	private JToggleButton playstop = new JToggleButton("play");
	private JButton rewind = new JButton("rewind");
	/**
	 * cursec indique, en secondes, le temps de début du play, et le temps de fin du play uniquement.
	 * Il est donc mis à jour seulement 2 fois par play: au début et à la fin.
	 */
	private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
	// date absolu de début du play 
	private long timePlayStartedMs = -1;
	private long timeIntervalPlayed = -1;
	private float relativeStartingSec = 0;
	
	private Aligneur aligneur=null;
	
	public PlayerGUI(Aligneur main) {
		super();
		aligneur=main;
		initgui();
		initactions();
	}
	
	public float getRelativeStartingSec() {
		return relativeStartingSec;
	}
	
	public void startPlaying() {
		{
			// on appelle les listeners avant de jouer car ceux-ci peuvent avoir
			// qqchose a faire juste avant de commencer !
			ActionEvent ev = new ActionEvent(this, 0, "playstart");
			for (ActionListener al : listeners) al.actionPerformed(ev);
//			player.play(aligneur.getWavSourceFromStart());
			player.play(aligneur.getAudioStreamFromSec(aligneur.getCurPosInSec()));
			timePlayStartedMs=System.currentTimeMillis();
			timeIntervalPlayed=0;
		}
		playstop.setText("stop");
		playstop.setSelected(true);
		repaint();
	}
	
	public void stopPlaying() {
		player.stopPlaying();
		if (timePlayStartedMs>=0)
			timeIntervalPlayed=System.currentTimeMillis()-timePlayStartedMs;
		timePlayStartedMs=-1;
		ActionEvent ev = new ActionEvent(this, 0, "playstop");
		for (ActionListener al : listeners) al.actionPerformed(ev);
		playstop.setText("play");
		playstop.setSelected(false);
		repaint();
	}
	
	public void addActionListener(ActionListener listener) {
		listeners.add(listener);
	}
	
	public boolean isPlaying() {return player.isPlaying();}
	
	/**
	 * Playback must have been stopped for this method to work !
	 * 
	 * @return the length of the play in ms, between its start and its end
	 */
	public long getTimePlayed() {return timeIntervalPlayed;}
	
	public long getTimePlayStarted() {return timePlayStartedMs;}
	
	public void setStartSec(float sec) {
		relativeStartingSec=sec;
	}
	
	private void initgui() {
		setLayout(new FlowLayout());
		add(playstop);
		add(rewind);
	}

	private void initactions() {
		rewind.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				relativeStartingSec=0;
				ActionEvent ev = new ActionEvent(this, 0, "rewind");
				for (ActionListener al : listeners) al.actionPerformed(ev);
			}
		});
		playstop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (playstop.isSelected()) {
					startPlaying();
				} else {
					stopPlaying();
				}
			}
		});
	}
	
	public static void main(String args[]) {
		PlayerGUI m = new PlayerGUI(null);
		JFrame jf = new JFrame();
		jf.getContentPane().add(m);
		jf.setSize(100, 80);
		jf.setVisible(true);
	}
}
