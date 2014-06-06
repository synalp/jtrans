package fr.loria.synalp.jtrans.gui.signalViewers.spectroPanel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.loria.synalp.jtrans.gui.JTransGUI;
import fr.loria.synalp.jtrans.utils.TimeConverter;

/**
 * "wrap" un SpectroPanel dans un autre Panel, et
 * ajoute des controles
 * 
 * @author xtof
 *
 */
public class SpectroControl extends JPanel {
	float startSeconds = 0;
	SpectroPanel spectro = new SpectroPanel();
	TimelineWords words = new TimelineWords();
	JButton refresh = new JButton("refresh");
	JSlider offset = new JSlider(0,200,80);
	JSlider zoom = new JSlider(1,200,80);
	JLabel posdeb = new JLabel("0 sec");
	JButton p1s = new JButton("+1sec");
	JButton m1s = new JButton("-1sec");
	JTransGUI gui;
	private JComboBox<String> chooser;
	private int spkID;

	public SpectroControl(JTransGUI main) {
		gui = main;
		initgui();
		spectro.aligneur= gui;
	}

	public void setSpeaker(int spkID) {
		this.spkID = spkID;
		chooser.setSelectedIndex(spkID);
		refreshWords();
	}

	public void refreshWords() {
		words.setTokens(gui.project.getTokens(spkID));
		words.setFirstFrame(TimeConverter.second2frame(startSeconds));
		words.repaint();
	}

	public SpectroControl(){
		initgui();
	}

	//	public void setPreferredSize(Dimension d) {
	//		super.setPreferredSize(d);
	//		spectro.setPreferredSize(new Dimension(d.width, spectro.getPreferredSize().height));
	//	}
	//	
	private void initgui() {
		chooser = new JComboBox<>();
		for (int i = 0; i < gui.project.speakerCount(); i++) {
			chooser.addItem(gui.project.getSpeakerName(i));
		}

		chooser.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setSpeaker(chooser.getSelectedIndex());
			}
		});

		setLayout(new BorderLayout());
		add(new JPanel(new BorderLayout()) {{
			add(spectro, BorderLayout.CENTER);
			add(words, BorderLayout.PAGE_END);
		}}, BorderLayout.CENTER);


		Box b1 = Box.createHorizontalBox();
		b1.add(chooser);
		b1.add(Box.createHorizontalGlue());
		b1.add(posdeb);
		b1.add(p1s);
		b1.add(m1s);
		b1.add(Box.createHorizontalGlue());
		b1.add(zoom);
		b1.add(Box.createHorizontalGlue());
		b1.add(refresh);
		b1.add(Box.createHorizontalGlue());
		b1.add(offset);
		b1.add(Box.createHorizontalGlue());

		add(b1, BorderLayout.PAGE_END);

		p1s.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				float curs = Float.parseFloat(posdeb.getText());
				curs+=1;
				gui.setCurPosInSec(curs);
			}
		});
		m1s.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				float curs = Float.parseFloat(posdeb.getText());
				curs-=1;
				gui.setCurPosInSec(curs);
			}
		});
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});
		offset.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int v = offset.getValue();
				double d = (double)v/1.0;
				spectro.setOffsetF(d);
				refresh();
			}
		});
		zoom.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int v = zoom.getValue();
				float z = (float)v/30f;
				spectro.setZoom(z);
				words.setZoom(z);
				refresh();
			}
		});

		// set valeurs initiales
		{
			int v = offset.getValue();
			double d = (double)v/1.0;
			spectro.setOffsetF(d);
			v = zoom.getValue();
			float z = (float)v/30f;
			spectro.setZoom(z);
			words.setZoom(z);
			refresh();
		}
	}

	public void refresh() {
		// on a un X-pixel = 1 frame = 0.01s, et avec le zoom: 1 frame = zoom xpixel
		// pour afficher toute la largeur, il faut donc calculer width/zoom frames
		// on commence tjs a la frame 0, car l'audioinputstream est decale pour commencer au bon endroit
		//		int nfr = (int)((float)getWidth()*0.6f/spectro.getZoom());
		spectro.computeSpectrogram(0, 500);
		int w = (int)((float)getWidth()*0.9f);
		int h = 80;
		spectro.setSize(new Dimension(w, h));
		spectro.setPreferredSize(new Dimension(w, h));
		int h2 = 40;
		words.setSize(new Dimension(w, h2));
		words.setPreferredSize(new Dimension(w, h2));
		validate();
	}

	public void setAudioInputStream(float startSeconds, AudioInputStream a) {
		this.startSeconds = startSeconds;
		posdeb.setText(String.format("%.3f", startSeconds));
		spectro.setAudioInputStream(a);
		refresh();
		repaint();
	}

	public static void main(String[] args) throws Exception {
		SpectroControl m = new SpectroControl();
		File f = new File("C:/xx.wav");
		System.out.println("wav found "+f.exists());
		AudioInputStream ais = AudioSystem.getAudioInputStream(f);
		m.setAudioInputStream(0,ais);
		JFrame jf = new JFrame();
		jf.setSize(500,500);
		jf.getContentPane().add(m);
		jf.setVisible(true);
	}
}
