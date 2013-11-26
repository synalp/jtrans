package plugins.signalViewers.spectroPanel;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import plugins.applis.SimpleAligneur.Aligneur;
import plugins.speechreco.aligners.sphiinx4.Alignment;
import utils.TimeConverter;

/**
 * "wrap" un SpectroPanel dans un autre Panel, et
 * ajoute des controles
 * 
 * @author xtof
 *
 */
public class SpectroControl extends JPanel {
	SpectroPanel spectro = new SpectroPanel();
	TimelineWords words = new TimelineWords();
	JButton refresh = new JButton("refresh");
	JSlider offset = new JSlider(0,200,80);
	JSlider zoom = new JSlider(0,200,80);
	JLabel posdeb = new JLabel("0 sec");
	JButton p1s = new JButton("+1sec");
	JButton m1s = new JButton("-1sec");
	Aligneur aligneur;

	public SpectroControl(Aligneur main) {
		aligneur=main;
		initgui();
		spectro.aligneur=aligneur;
	}

	public void setAlign(Alignment al) {
		words.setAlign(al);
		words.repaint();
	}

	public void setFirstSeg(int seg) {
		words.setFirstSeg(seg);
	}
	public void setFirstFrame(int fr) {
		words.setFirstFrame(fr);
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
		Box b0 = Box.createVerticalBox();
		add(b0);
		Box ba = Box.createHorizontalBox();
		b0.add(ba);
		ba.add(spectro);
		ba.add(Box.createHorizontalGlue());
		Box bb = Box.createHorizontalBox();
		b0.add(bb);
		bb.add(words);
		bb.add(Box.createHorizontalGlue());
		b0.add(Box.createVerticalGlue());
		Box b1 = Box.createHorizontalBox();
		b0.add(b1);
		b0.add(Box.createVerticalGlue());

		b1.add(Box.createHorizontalGlue());
		b1.add(posdeb);
		b1.add(Box.createHorizontalGlue());
		b1.add(p1s);
		b1.add(Box.createHorizontalGlue());
		b1.add(m1s);
		b1.add(Box.createHorizontalGlue());
		b1.add(zoom);
		b1.add(Box.createHorizontalGlue());
		b1.add(refresh);
		b1.add(Box.createHorizontalGlue());
		b1.add(offset);
		b1.add(Box.createHorizontalGlue());

		p1s.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				float curs = Float.parseFloat(posdeb.getText());
				curs+=1;
				aligneur.setCurPosInSec(curs);
			}
		});
		m1s.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				float curs = Float.parseFloat(posdeb.getText());
				curs-=1;
				aligneur.setCurPosInSec(curs);
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

	private void gottosec(float s) {
		int fr = TimeConverter.second2frame(s);
		setFirstFrame(fr);
	}

	public void refresh() {
		// on a un X-pixel = 1 frame = 0.01s, et avec le zoom: 1 frame = zoom xpixel
		// pour afficher toute la largeur, il faut donc calculer width/zoom frames
		// on commence tjs a la frame 0, car l'audioinputstream est decale pour commencer au bon endroit
		//		int nfr = (int)((float)getWidth()*0.6f/spectro.getZoom());
		spectro.computeSpectrogram(0, 500);
		int w = (int)((float)getWidth()*0.9f);
		int h = 150;
		spectro.setSize(new Dimension(w, h));
		spectro.setPreferredSize(new Dimension(w, h));
		int h2=50;
		words.setSize(new Dimension(w, h2));
		words.setPreferredSize(new Dimension(w, h2));
		validate();
	}

	public void setAudioInputStream(float debsec, AudioInputStream a) {
		try {
			posdeb.setText(""+debsec);
			spectro.setAudioInputStream(a);
			refresh();
			repaint();
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "pas de sphinx4.jarfdfdf  ? "+e);
		}
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
