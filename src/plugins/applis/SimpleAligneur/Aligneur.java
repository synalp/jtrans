/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

package plugins.applis.SimpleAligneur;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.BevelBorder;

import com.google.gson.*;
import facade.JTransAPI;
import facade.Project;
import markup.*;
import main.SpeechReco;

import markup.TextGridLoader;
import plugins.buffer.RoundBuffer;
import plugins.buffer.RoundBufferFrontEnd;
import plugins.signalViewers.spectroPanel.SpectroControl;
import plugins.signalViewers.temporalSigPanel.TemporalSigPanel;
import plugins.signalViewers.temporalSigPanel.ToolBarTemporalSig;
import plugins.speechreco.acousticModels.HMM.LogMath;
import plugins.speechreco.adaptation.BiaisAdapt;
import plugins.speechreco.aligners.OldAlignment;
import plugins.speechreco.aligners.ForceAlignBlocViterbi;
import plugins.speechreco.aligners.sphiinx4.*;
import plugins.speechreco.aligners.sphiinx4.Alignment;
import plugins.text.ColoriageEvent;
import plugins.text.GriserWhilePlaying;
import plugins.text.ListeElement;
import plugins.text.TexteEditor;
import plugins.text.elements.*;
import plugins.utils.FileUtils;
import plugins.utils.InterfaceAdapter;
import plugins.utils.PrintLogger;
import speechreco.RecoWord;
import tools.audio.PlayerGUI;
import utils.ProgressDialog;

/**
 * Classe principale de JTrans
 * 
 * @author cerisara
 *
 */
public class Aligneur extends JPanel implements PrintLogger {

	public JFrame jf=null;

	/**
	 * position courante en millisecondes centralisee: mise a jour par les
	 * differents plugins qui peuvent la modifier;
	 */
	private float cursec = 0;
	public ControlBox ctrlbox;
	private SpectroControl sigpan=null;
	public boolean showPhones=false;

	private GriserWhilePlaying griserwhenplaying = new GriserWhilePlaying(null, null);
	private PlayerGUI playergui;

	public int mixidx=0;
	public boolean useS4aligner = true;

	public AutoAligner autoAligner = null;
	public TexteEditor edit;
	//	public Player player;
	public TemporalSigPanel sigPanel = null;
	public ToolBarTemporalSig toolbar = null;
	public KeysManager kmgr = null;
	PlayerListener playerController;

	/** Audio file in a suitable format for processing */
	public File convertedAudioFile = null;

	public RoundBuffer audiobuf = new RoundBuffer(this, 10000000);
	public RoundBufferFrontEnd mfccbuf;
	ForceAlignBlocViterbi blocViterbi = null;

	// position lue pour la derniere fois dans le flux audio
	long currentSample = 0;
	public Project project = new Project();
	int wordSelectedIdx = -1;
	public boolean caretSensible = false;
	JLabel infoLabel = new JLabel("Welcome to JTrans");

	/*
	 * pour l'alignement semi-automatique
	 */
	int wordBeforeBeginReco = 0, wordEndReco = 0;
	boolean userShouldConfirm = false;
	LogMath logmath = new LogMath();

	public float getCurPosInSec() {return cursec;}
	public void setCurPosInSec(float sec) {
		cursec=sec;
		int fr = JTransAPI.second2frame(cursec);
		int seg = project.words.getSegmentAtFrame(fr);
		// nouveau panel
		sigpan.setAudioInputStream(getCurPosInSec(), getAudioStreamFromSec(getCurPosInSec()));
		if (showPhones) {
			int segphidx = project.phons.getSegmentAtFrame(fr);
			sigpan.setAlign(project.phons);
			sigpan.setFirstSeg(segphidx);
		} else {
			sigpan.setFirstSeg(seg);
			sigpan.setAlign(project.words);
		}
		repaint();
		updateViewers();
	}

	public void print(String msg) {
		printInStatusBar(msg);
	}

	public void quit() {
		if (autoAligner!=null) autoAligner.terminateAll();
		if (edit!=null) edit.colorOrders.put(ColoriageEvent.endofthread);
		Thread.yield();
		if (playerController!=null) playerController.kill();
		Thread.yield();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("before dispose");
		jf.dispose();
		System.out.println("after dispose : exiting");
		System.exit(0);
	}

	public void saveRawText(File file) throws IOException {
		List<Element_Mot> mots = edit.getListeElement().getMots();
		try {
			PrintWriter fout = FileUtils.writeFileUTF(file.getAbsolutePath());
			for (Element_Mot m : mots)
				fout.println(m);
			fout.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Saves a copy of the converted audio file.
	 */
	public void saveWave(File outFile) throws IOException {
		byte[] buf = new byte[1024];
		int len;

		InputStream in = new FileInputStream(convertedAudioFile);
		OutputStream out = new FileOutputStream(outFile);

		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}

		in.close();
		out.close();
	}

	/**
	 * Sets the sound source file and converts it to a suitable format for
	 * JTrans if needed.
	 * @param path path to the sound file
	 */
	public void setAudioSource(String path) {
		project.wavname = path;

		if (path != null)
			convertedAudioFile = JTransAPI.suitableAudioFile(new File(project.wavname));
		else
			convertedAudioFile = null;
	}
	
	public AudioInputStream getAudioStreamFromSec(float sec) {
		if (convertedAudioFile == null)
			return null;

		AudioInputStream ais;

		try {
			ais = AudioSystem.getAudioInputStream(convertedAudioFile);
		} catch (UnsupportedAudioFileException ex) {
			ex.printStackTrace();
			ais = null;
		} catch (IOException ex) {
			ex.printStackTrace();
			ais = null;
		}

		if (ais == null) {
			JOptionPane.showMessageDialog(jf,
					"No audio stream from " + convertedAudioFile,
					"Error",
					JOptionPane.ERROR_MESSAGE);
			return null;
		}

		AudioFormat format = ais.getFormat();
		float frPerSec = format.getFrameRate();
		int byPerFr = format.getFrameSize();
		float fr2skip = frPerSec*sec;
		long by2skip = (long)(fr2skip*(float)byPerFr);

		try {
			ais.skip(by2skip);
			return ais;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void setText(String s) {
		edit.setText(s);
		repaint();
	}
	private void updateViewers() {
		// update spectro
		AudioInputStream aud = getAudioStreamFromSec(getCurPosInSec());
		if (aud!=null&&sigpan!=null) {
			sigpan.setAudioInputStream(getCurPosInSec(),aud);
			sigpan.refresh();
		}
	}

	private AutoAligner getAutoAligner() {
		// TODO: support for URL !!
		return AutoAligner.getAutoAligner(convertedAudioFile.getAbsolutePath(), null, edit, project.words, project.phons);
	}

	public void batch() {
		AutoAligner.batch=!AutoAligner.batch;
		s4fastAutoAlign();
	}

	public void s4fastAutoAlign() {
		if (!useS4aligner) return;
		autoAligner = getAutoAligner();
	}

	/**
	 * Returns a Gson object suitable for serializing and deserializing JTrans
	 * projects to/from JSON.
	 */
	private static Gson newGson() {
		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(Element.class, new InterfaceAdapter<Element>("$TYPE$"));
		gb.setPrettyPrinting();
		return gb.create();
	}

	/**
	 * Saves the project in JSON format.
	 */
	public void saveJsonProject(File file) throws IOException {
		FileWriter w = new FileWriter(file);
		newGson().toJson(project, w);
		w.close();
	}

	public void loadJsonProject(File file) throws IOException{
		FileReader r = new FileReader(file);
		project = newGson().fromJson(r, Project.class);
		r.close();

		caretSensible = true;
		setAudioSource(project.wavname);
		project.refreshIndex();

		setProject(project);
	}

	public void setEditionMode() {
		caretSensible=false;
		edit.setEditable(true);
		edit.getHighlighter().removeAllHighlights();
		System.out.println("seteditionmode");
	}

	private float lastSecClickedOnSpectro = 0;
	public void clicOnSpectro(float frf) {
		float prevsec = getCurPosInSec();
		float sec = OldAlignment.frame2second((int) frf);
		sec += prevsec;
		// on lit une seconde avant la pos
		setCurPosInSec(sec-1);
		lastSecClickedOnSpectro=sec;
		ctrlbox.getPlayerGUI().startPlaying();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ctrlbox.getPlayerGUI().stopPlaying();
		setCurPosInSec(prevsec);
	}

	public void gototime() {
		String tt = JOptionPane.showInputDialog("Time (in seconds) to go to:");
		if (tt==null) return;
		tt=tt.trim();
		if (tt.length()==0) return;
		Float sec = Float.parseFloat(tt);
		setCurPosInSec(sec);
	}

	public void printInStatusBar(String msg) {
		infoLabel.setText(msg);
		infoLabel.repaint();
	}

	public Aligneur(boolean withGUI) {
		initPanel();
		if (withGUI) createJFrame();
	}

	public Aligneur() {
		JTransAPI.aligneur=this;
		initPanel();
		createJFrame();
	}

	public JMenuBar createMenus() {
		JMenuBar menubar = (new Menus(this)).menus();
		return menubar;
	}

	private void createJFrame() {
		// Use OS X menu bar if possible
		if (System.getProperty("os.name").toLowerCase().contains("mac"))
			System.setProperty("apple.laf.useScreenMenuBar", "true");

		jf = new JFrame("jtrans 1.2");
		JMenuBar menubar = (new Menus(this)).menus();
		jf.setJMenuBar(menubar);
		jf.getContentPane().add(this);

		//		setDefaultCloseOperation(EXIT_ON_CLOSE);
		final Aligneur aligneur = this;
		jf.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				aligneur.quit();
			}
		});
		jf.pack();
		jf.setVisible(true);
	}

	private void initPanel() {
		setLayout(new BorderLayout());

		edit = new TexteEditor(project);
		JTransAPI.edit=edit;
		ctrlbox = new ControlBox(this);
		playergui = ctrlbox.getPlayerGUI();
		infoLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));

		sigpan = new SpectroControl(this);
		AudioInputStream aud = getAudioStreamFromSec(getCurPosInSec());
		if (aud!=null) sigpan.setAudioInputStream(getCurPosInSec(),aud);

		// Add everything to the panel

		add(ctrlbox, BorderLayout.NORTH);

		add(new JScrollPane(edit) {{
			setPreferredSize(new Dimension(edit.getWidth(), 300));
		}}, BorderLayout.CENTER);

		add(new JPanel(new BorderLayout()) {{
			add(sigpan, BorderLayout.NORTH);
			add(infoLabel, BorderLayout.SOUTH);
		}}, BorderLayout.SOUTH);
	}

	void rewindAudio() {
		audiobuf.rewind();
	}
	void goHome() {
		//		if (player.isPlaying()) return;
		currentSample=0;
		wordSelectedIdx=0;
		if (sigPanel!=null) {
			sigPanel.setProgressBar(0);
		}
		Element_Mot firstmot = edit.getListeElement().getMot(0);
		edit.griseMot(firstmot);
	}

	/**
	 * sorte de "reinit" pour rendre la main au user
	 */
	void stopAll() {
		stopPlaying();
		stopAlign();
		caretSensible = true;
		// attention ! ne pas modifier usersShouldConfirm !!
	}

	void stopAlign() {
		if (blocViterbi != null) {
			blocViterbi.pauseReco = true;
		}
	}

	/**
	 * cette fonction est toujours appel�e lorsque l'audio a fini de jouer, soit
	 * parce qu'on a appuye sur ESC, soit parce qu'on est arriv� � la fin
	 */
	void stopPlaying() {
		ctrlbox.getPlayerGUI().stopPlaying();
		/*
		if (player.isPlaying()) {
			currentSample = player.getLastSamplePlayed();
			player.stopPlaying();
			if (sigPanel != null) {
				sigPanel.stopPlaying();
			}
		}
		if (playerController != null) {
			playerController.pause();
		}
		 */
	}

	void goToLastAlignedWord() {
		/*
        caretSensible = false;
        if (player != null && player.isPlaying()) {
            System.err.println("dont goto last aligned word because playing");
            return;
        }
        int lastAlignedWord = project.words.getLastWordAligned();
        if (lastAlignedWord < 0) {
            wordSelectedIdx = 0;
        } else {
            wordSelectedIdx = lastAlignedWord;
        }
        griseSelectedWord();
        caretSensible = true;
		 */
	}

	public void inputControls() {
		kmgr = new KeysManager(this);
		new MouseManager(this);
	}

	public void newplaystarted() {
		if (project.words!=null) {
			System.out.println("newplaystarted "+project.words.getNbSegments());
			griserwhenplaying = new GriserWhilePlaying(playergui, edit);
			griserwhenplaying.setAlignement(project.words);
		} else
			System.out.println("newplaystarted no align");
	}
	public void newplaystopped() {
		griserwhenplaying.killit();
	}

	void restartPlaying() {
		/*
		if (player.isPlaying()) return;
		if (sigPanel != null) {
			// on demarre depuis la barre de selection du sigPanel
			long sfin = sigPanel.getEndSelection();
			if (sfin >= 0) {
				long sdeb = sigPanel.getStartSelection();
				if (sfin - sdeb < 1000) {
					restartPlaying(0);
					return;
				}
				//				 positionne l'audio buffer
				audiobuf.samplePosition = sdeb;
				if (sigPanel != null) {
					sigPanel.replayFrom(sdeb);
				}
				//				 relance le thread qui grise les mots
				if (project.words != null) {
					playerController.unpause();
				}
				//				 lance la lecture
				player.play(mixidx,new plugins.player.PlayerListener() {

					public void playerHasFinished() {
						stopPlaying();
					}
				}, sdeb, sfin);
			} else {
				// on demarre depuis le dernier currentSample
				restartPlaying(0);
			}
		} else {
			restartPlaying(0);
		}
		 */
	}

	/**
	 * 
	 * @param nsec : nb de secondes de recul
	 */
	void restartPlaying(int nsec) {
		System.out.println("restart playing");
		caretSensible = true;
		if (sigPanel != null) {
			currentSample = sigPanel.getProgressBar();
			nsec = 0;
			//        } else if (project.words != null) {
			//            int lastAlignedWord = project.words.getLastWordAligned();
			//            if (wordSelectedIdx < 0 || project.words.getFrameFin(wordSelectedIdx) < 0) {
			//                if (lastAlignedWord < 0) {
			//                    currentSample = 0;
			//                } else {
			//                    currentSample = OldAlignment.frame2sample(project.words.getFrameFin(lastAlignedWord));
			//                }
			//            } else {
			//                currentSample = OldAlignment.frame2sample(project.words.getFrameFin(wordSelectedIdx));
			//            }
		} else {
			//            currentSample = 0;
		}

		// recule de nsec secondes
		currentSample -= 16000 * nsec;
		if (currentSample < 0) {
			currentSample = 0;
		}
		// positionne l'audio buffer
		audiobuf.samplePosition = currentSample;
		if (sigPanel != null) {
			sigPanel.replayFrom(currentSample);
		}
		// relance le thread qui grise les mots
		if (project.words != null) {
			playerController.unpause();
		}
		// lance la lecture
		/*
		if (player==null) return;
		player.play(mixidx,new plugins.player.PlayerListener() {
			public void playerHasFinished() {
				stopPlaying();
			}
		}, currentSample);
		 */
	}

	public void startPlayingFrom(float nsec) {
		/*
		currentSample = OldAlignment.second2sample(nsec);
		caretSensible = true;

		if (currentSample < 0) {
			currentSample = 0;
		}
		// positionne l'audio buffer
		audiobuf.samplePosition = currentSample;
		System.err.println("play from " + OldAlignment.sample2second(currentSample));
		// relance le thread qui grise les mots
		if (project.words != null) {
			playerController.unpause();
		}
		// lance la lecture
		player.play(mixidx, new plugins.player.PlayerListener() {

			public void playerHasFinished() {
				stopPlaying();
			}
		}, currentSample);
		 */
	}

	public void clearAlign() {
		clearAlignFrom(0);
		// pour supprimer les SIL en debut de fichier
		project.words.clear();
		project.phons.clear();
	}
	void clearAlignFrom(int mot) {
		if (autoAligner!=null) autoAligner.stopAutoAlign();
		// cherche le prochain mot qui est aligné
		int seg4mot=-1;
		List<Element_Mot> mots = edit.getListeElement().getMots();
		for (;mot<mots.size();mot++) {
			seg4mot = mots.get(mot).posInAlign;
			if (seg4mot>=0&&!project.words.getSegmentLabel(seg4mot).equals("SIL")) break;
		}
		if (seg4mot<0) return; // rien n'est aligne
		// supprimer les alignements
		int fr = project.words.getSegmentDebFrame(seg4mot);
		project.words.cutAfterFrame(fr);
		project.phons.cutAfterFrame(fr);
		project.words.clearIndex();
		project.phons.clearIndex();
		
		// supprimer la correspondance mots/segments
		for (int i=mot;i<edit.getListeElement().getMots().size();i++) {
			edit.getListeElement().getMot(i).posInAlign=-1;
		}
		// decolorier les mots qui ne sont plus alignes:
		if (edit!=null) {
			edit.getHighlighter().removeAllHighlights();
			if (mot>0) edit.colorizeWords(0, mot - 1);
		}
		repaint();
	}

	public void toggleShowPhones() {
		showPhones=!showPhones;
	}

	// aligne tous les mots 
	void doForceAnchor(float sec, int mot) {
		int mot0 = getLastMotAligned();
		System.out.println("forceanchor "+mot0);
		if (mot0<0) {
			// TODO: sec indique l'offset qu'il faut considerer dans le fichier WAV
			// donc, il faut ajouter un silence au debut du fichier jusqu'a cet offset
			if (JTransAPI.isBruit(0)) {
				// TODO: verifier que le mot suivant est bien un mot, et non un bruit ou silence
				JTransAPI.setAlignWord(-1, 0, 0f, sec);
			} else {
				// ajouter un silence devant
				// TODO: est-ce que le prochain alignement auto, lorsqu'il verra qu'il n'y a aucun mot
				// de deja aligne, prendra en compte le premier silence qui est aligne ?
				
				JTransAPI.setSilenceSegment(0f, sec);
			}
			return;
		}
		// TODO: refaire la suite avec JTransAPI
		JTransAPI.setAlignWord(-1, mot, -1, sec);
	}
	
	void clicAtCaretPosition(int caretPos, int button) {
		int mot = edit.getListeElement().getIndiceMotAtTextPosi(caretPos);
		if (mot<0) {
			System.out.println("no mot at position "+caretPos);
			return;
		}
		// simplification:
		// shift+clic = del align from there + replay from here
		// ctrl +clic = equi-align up to here + auto-align from here
		// shift + ctrl + clic = manual def of limits for one word
		// ctrl = toggle play/pause
		// le realign pourra se fait en batch apres, en comparant les word-align et les phone-aligns pour detecter les segments non-alignes
		if (kmgr.isShiftOn) {
			ctrlbox.getPlayerGUI().stopPlaying();
			Thread.yield();
			try {
				Thread.sleep(60);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (kmgr.isControlOn) {
				System.out.println("last aligned word "+getLastMotAligned());
				System.out.println("manual definition of word limits");
				float deb=-1, end=-1;
				String s = JOptionPane.showInputDialog("word "+mot+" "+edit.getListeElement().getMot(mot).getWordString()+" start (in sec):").trim();
				if (s.length()>0) deb = Float.parseFloat(s);
				s = JOptionPane.showInputDialog("word "+mot+" end (in sec):").trim();
				if (s.length()>0) end = Float.parseFloat(s);
				
				int segmentDuMot = edit.getListeElement().getMot(mot).posInAlign;
				if (segmentDuMot>=0) {
					// mot deja aligne
					if (deb>=0) project.words.setSegmentDebFrame(segmentDuMot, JTransAPI.second2frame(deb));
					if (end>=0) project.words.setSegmentEndFrame(segmentDuMot, JTransAPI.second2frame(end));
				} else {
					// mot non encore aligne
					if (mot>0) {
						// ce n'est pas le 1er mot: j'aligne auto les precedents jusqu'au debut
						// TODO
					} else {
						// premier mot
						project.words.clear();
						int curdebfr = 0;
						if (deb>0) {
							curdebfr = JTransAPI.second2frame(deb);
							if (curdebfr>0) {
								project.words.addRecognizedSegment("SIL", 0, curdebfr, null, null);
							}
						}
						if (end>=0) {
							int newseg = project.words.addRecognizedSegment(edit.getListeElement().getMot(0).getWordString(),
									curdebfr, JTransAPI.second2frame(end), null, null);
							edit.getListeElement().getMot(0).posInAlign=newseg;
						} else {
							// TODO
						}
						System.out.println("debug segs  \n"+project.words.toString());
					}
				}
				System.out.println("last aligned word "+getLastMotAligned());
				if (edit!=null) edit.colorizeWords(0, mot);
				repaint();
			} else { // juste un SHIFT-clic
				// position pour le play
				wordSelectedIdx=mot;
				Element_Mot emot = edit.getListeElement().getMot(mot);
				int segidx = emot.posInAlign;
				if (segidx>=0) {
					int frame = project.words.getSegmentDebFrame(segidx);
					cursec = OldAlignment.frame2second(frame);
					long currentSample = OldAlignment.frame2sample(frame);
					if (currentSample<0) currentSample=0;
					edit.griseMot(emot);
					// vieux panel
					if (sigPanel!=null) {
						sigPanel.setProgressBar(currentSample);
					}
					// nouveau panel
					sigpan.setAudioInputStream(getCurPosInSec(), getAudioStreamFromSec(getCurPosInSec()));
					if (showPhones) {
						int segphidx = project.phons.getSegmentAtFrame(frame);
						sigpan.setAlign(project.phons);
						sigpan.setFirstSeg(segphidx);
					} else {
						sigpan.setFirstSeg(segidx);
						sigpan.setAlign(project.words);
					}
					repaint();
				} else {
					System.err.println("warning: pas de segment associé au mot "+emot.getWordString());
				}
				Thread.yield();
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				clearAlignFrom(mot);
				ctrlbox.getPlayerGUI().startPlaying();
			}
		} else if (kmgr.isControlOn) {
			lastSecClickedOnSpectro = playergui.getRelativeStartingSec()+(float)playergui.getTimePlayed()/1000f;
			System.out.println("Ctrl-clic without playing: set at last selected frame "+lastSecClickedOnSpectro+" "+playergui.getRelativeStartingSec()+" "+playergui.getTimePlayed());
			System.out.println("set word "+mot+" "+edit.getListeElement().getMot(mot).getWordString());
			float sec0 = getCurPosInSec();
			setCurPosInSec(lastSecClickedOnSpectro);
			int segmentDuMot = edit.getListeElement().getMot(mot).posInAlign;
			if (segmentDuMot<0) {
				// il n'est pas aligné, que fais-je ???
				System.out.println("ctrl-clic when playing: create anchor");
				doForceAnchor(lastSecClickedOnSpectro,mot);
			} else {
				System.out.println("set end of segment "+segmentDuMot+" "+project.words.getSegmentLabel(segmentDuMot));
				System.out.println("set at frame "+JTransAPI.second2frame(lastSecClickedOnSpectro));
				project.words.setSegmentEndFrame(segmentDuMot, JTransAPI.second2frame(lastSecClickedOnSpectro));
			}
			if (edit!=null) edit.colorizeWords(0, mot);
			setCurPosInSec(sec0);
			if (edit!=null) edit.getListeElement().refreshIndex();
			repaint();
			useS4aligner=true;
			s4fastAutoAlign();
		} else {
			System.out.println("clic sans control: repositionne mot "+edit.getListeElement().getMot(mot).getWordString());

			// nouveau player:
			boolean replay = ctrlbox.getPlayerGUI().isPlaying();
			ctrlbox.getPlayerGUI().stopPlaying();
			Thread.yield();
			try {
				Thread.sleep(60);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// position pour le play
			wordSelectedIdx=mot;
			Element_Mot emot = edit.getListeElement().getMot(mot);
			int segidx = emot.posInAlign;
			if (segidx>=0) {
				int frame = project.words.getSegmentDebFrame(segidx);
				cursec = OldAlignment.frame2second(frame);
				long currentSample = OldAlignment.frame2sample(frame);
				if (currentSample<0) currentSample=0;
				edit.griseMot(emot);
				// vieux panel
				if (sigPanel!=null) {
					sigPanel.setProgressBar(currentSample);
				}
				// nouveau panel
				sigpan.setAudioInputStream(getCurPosInSec(), getAudioStreamFromSec(getCurPosInSec()));
				if (showPhones) {
					int segphidx = project.phons.getSegmentAtFrame(frame);
					sigpan.setAlign(project.phons);
					sigpan.setFirstSeg(segphidx);
				} else {
					sigpan.setFirstSeg(segidx);
					sigpan.setAlign(project.words);
				}
				repaint();
			} else {
				System.err.println("warning: pas de segment associé au mot "+emot.getWordString());
				MouseManager.clicMotMenu(this,mot);
				replay=false;
			}
			Thread.yield();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (replay) ctrlbox.getPlayerGUI().startPlaying();
		}
	}

	public int getLastMotAligned() {
		List<Element_Mot> mots = edit.getListeElement().getMots();
		for (int i=0;i<mots.size();i++) {
			if (mots.get(i).posInAlign<0) return i-1;
		}
		return mots.size()-1;
	}

	/**
	 * lastFrame est la derniere trame du mot clicke; on suppose qu'on ne connait pas le debut
	 * de ce mot, on realigne donc jusqu'a ce mot inclu
	 */
	void realignBeforeAnchor(int motClicked, int lastFrame) {
		int firstMot = motClicked;
		clearAlignFrom(firstMot); // arrete aussi l'autoaligner
		firstMot = getLastMotAligned()+1;
		int firstFrame = 0;
		if (firstMot>0) {
			int segidx = edit.getListeElement().getMots().get(firstMot-1).posInAlign;
			firstFrame = project.words.getSegmentEndFrame(segidx);
		}

		// ici, je fais un Viterbi pour realigner les mots avant
		S4AlignOrder order = new S4AlignOrder(firstMot, firstFrame, motClicked, lastFrame);
		try {
			S4ForceAlignBlocViterbi s4aligner = getS4aligner();
			s4aligner.input2process.put(order);
			synchronized (order) {
				order.wait();
			}

			if (order.alignWords!=null) {
				order.alignWords.adjustOffset(firstFrame);
				order.alignPhones.adjustOffset(firstFrame);
				order.alignStates.adjustOffset(firstFrame);

				int[] mots2segidx = new int[mots.length];
				String[] wordsThatShouldBeAligned = Arrays.copyOfRange(mots, firstMot, mots.length);
				System.out.println("wordsthatshouldbealigned "+Arrays.toString(wordsThatShouldBeAligned));
				int[] locmots2segidx = order.alignWords.matchWithText(wordsThatShouldBeAligned);
				int nsegsbefore = project.words.merge(order.alignWords);
				for (int i=0;i<locmots2segidx.length;i++) {
					if (locmots2segidx[i]>=0)
						mots2segidx[firstMot+i]=locmots2segidx[i]+nsegsbefore;
					else
						mots2segidx[firstMot+i]=-1;
				}
				System.out.println("mots2segs "+locmots2segidx.length+" "+Arrays.toString(mots2segidx));
				int lastMotAligned=-1;
				if (edit!=null) {
					lastMotAligned=edit.getListeElement().importAlign(mots2segidx,firstMot);
					System.err.println("last mot aligned "+lastMotAligned);
					edit.getListeElement().refreshIndex();
				}
				project.phons.merge(order.alignPhones);
				if (lastMotAligned>=0) {
					if (firstMot==0)
						edit.colorizeWords(firstMot, lastMotAligned - 1);
					else
						edit.colorizeWords(firstMot - 1, lastMotAligned - 1);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		/*
        // pour le moment, j'equi-align les mots precedents tant que le vieil align > ancre ou au moins jusqu'a 5 mots en arriere
        // pour cela, je supprime ici leur alignement, qui sera recree dans addAnchor
        Set<Integer> anchors = alignement.getAncres();
        int firstMot;
        for (firstMot=motClicked-1;firstMot>=0;firstMot--) {
        	// on remote 5s avant le clic ou jusqu'a trouver un mot aligne
        	if (anchors.contains(firstMot)||(alignement.getWordEndFrame(firstMot)>=0&&alignement.getWordEndFrame(firstMot)<lastFrame-500)) break;
        }
        firstMot++;
        System.out.print("REALIGN from fr="+alignement.getWordEndFrame(firstMot)+" to fr="+lastFrame);
        for (int j=firstMot;j<=motClicked;j++) {
        	System.out.print(j+" ");
        }
        System.out.println();
        int firstFrame = 0;
        if (firstMot>0) firstFrame = alignement.getWordEndFrame(firstMot-1)+1;

        // ici, je fais un Viterbi pour realigner les mots avant
        S4AlignOrder order = new S4AlignOrder(firstMot, firstFrame, motClicked, lastFrame);
        try {
        	S4ForceAlignBlocViterbi s4aligner = getS4aligner();
        	s4aligner.input2process.put(order);
	        synchronized (order) {
				order.wait();
			}
			List<Element_Mot>  lmots = edit.getListeElement().getMots();
			String[] mots = new String[lmots.size()];
			for (int i=0;i<lmots.size();i++) {
				mots[i] = lmots.get(i).getWordString();
			}
			order.fullalign.checkWithText(Arrays.asList(mots),firstMot);
			alignement.merge(order.fullalign,firstMot);
	        // align auto les mots suivants
			// non ! car on veut parfois faire un batch align ensuite, si F2
	        // s4fastAutoAlign();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

        for (int i=firstMot;i<=motClicked;i++)
        	edit.inkInColor(edit.getListeElement().getMot(i), edit.getListeElement().getMot(i), AutoAligner.alignedColor);
        playerController.pause();
        // non: pas dans le cas F2
        // alignement.addManualAnchorv2(motClicked);
        // edit.souligne(edit.getListeElement().getMot(motClicked));
		 */
	}

	void insertManualAnchor(final int motClicked, float seconds) {
		ctrlbox.getPlayerGUI().stopPlaying();
		long absms;
		if (seconds<0) {
			long relms = ctrlbox.getPlayerGUI().getTimePlayed();
			System.out.println("insertanchor cursec = "+getCurPosInSec());
			absms = (long)(getCurPosInSec()*1000f)+relms;
		} else {
			absms = (long)(1000f*seconds);
		}
		final int fr = JTransAPI.millisec2frame(absms);
		System.out.println("insert anchor "+absms+" "+fr);

		// c'est le AWT event thread qui appelle cette fonction: il ne faut pas le bloquer !
		new Thread(new Runnable() {
			@Override
			public void run() {
				realignBeforeAnchor(motClicked,fr);
				//		        alignement.addManualAnchorv2(motClicked);
				// edit.souligne(edit.getListeElement().getMot(motClicked));
				// aligne auto les mots suivants
				// s4fastAutoAlign();
				// relance le player
				//		        startPlayingFrom(OldAlignment.frame2second(lastFrame)-2);
				repaint();
			}
		}).start();

		/*
		final int lastFrame = OldAlignment.sample2frame(player.getLastSamplePlayed());
		player.stopPlaying();

		// c'est le AWT event thread qui appelle cette fonction: il ne faut pas le bloquer !
		new Thread(new Runnable() {
			@Override
			public void run() {
				realignBeforeAnchor(motClicked,lastFrame);
				//		        alignement.addManualAnchorv2(motClicked);
				edit.souligne(edit.getListeElement().getMot(motClicked));
				// aligne auto les mots suivants
				s4fastAutoAlign();
				// relance le player
				//		        startPlayingFrom(OldAlignment.frame2second(lastFrame)-2);
			}
		}).start();
		 */
	}

	public void playOutsideAlign() {
	}

	public void setNwordsForward(int nw) {
		ForceAlignBlocViterbi.Nwords = nw;
	}

	private void getRecoResult(main.SpeechReco asr) {
		List<String> lmots = getRecoResultOld(asr);
		//    	alignement.merge(asr.fullalign,0);
		//   	alignement.checkWithText(lmots, 0);

		//    	System.out.println("debuglmots "+lmots.size()+" "+alignement.wordsIdx.size()+" "+alignement.wordsEnd.size());
		edit.colorizeWords(0, lmots.size() - 1);
		repaint();
	}

	private List<String> getRecoResultOld(main.SpeechReco asr) {
		StringBuilder sb = new StringBuilder();
		ArrayList<String> lmots = new ArrayList<String>();

		project = new Project();
		for (RecoWord word : asr.resRecoPublic) {
			String[] phones = new String[word.frameEnd-word.frameDeb];
			int[] states = new int[word.frameEnd-word.frameDeb];
			for (int t=0;t<word.frameEnd-word.frameDeb;t++) {
				phones[t] = word.getPhone(t);
				states[t] = word.getState(t);
				// TODO : ajouter les GMM qui ont ete perdues dans RecoWord
			}
			project.words.addRecognizedSegment(word.word,word.frameDeb,word.frameEnd,phones,states);
			if (word.word.charAt(0)=='<') continue;
			int posdebinpanel = sb.length();
			sb.append(word.word+" ");
			lmots.add(word.word);
			//    		project.words.wordsIdx.add(project.words.words.size()-1);
			int posfininpanel = sb.length();
			Element_Mot ew = new Element_Mot(word.word, false);
			ew.start = posdebinpanel;
			ew.end = posfininpanel;
			project.elts.add(ew);
		}
		setProject(project);

		//    	rec=rec.replaceAll("<[^>]+>", "");
		//    	rec=rec.replaceAll("  +", " ");
		//    	rec=rec.trim();
		//    	System.out.println("TEXT FROM RECO: "+rec);
		edit.setText(sb.toString());
		edit.lastSelectedWord = edit.lastSelectedWord2 = null;
		edit.setCaretPosition(0);
		edit.textChanged = false;
		edit.setIgnoreRepaint(false);
		edit.repaint();
		return lmots;
	}

	// fonction activee par la touche X qui sert a afficher/debugger 
	public void debug() {
		//    	System.out.println(project.words.alignedGMMs);
		//   	System.out.println(project.words.alignedGMMs.size());
		//   	System.out.println(project.words.phones.size());
		//  	System.out.println(project.words.states.size());
		// 	SpeechReco asr = SpeechReco.getSpeechReco();
		//   	asr.getGMMs(project.words);
	}

	public void asr() {
		ProgressDialog waiting = new ProgressDialog(jf, new Runnable() {
			@Override
			public void run() {
				final SpeechReco asr = SpeechReco.getSpeechReco();
				asr.recoListener=new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						getRecoResult(asr);
					}
				};
				asr.doReco(convertedAudioFile.getAbsolutePath(), "");
				getRecoResult(asr);
			}
		}, "please wait: transcribing...");
		waiting.setVisible(true);
	}

	String[] mots;
	public S4ForceAlignBlocViterbi getS4aligner() {
		// TODO: support for URL !!
		S4ForceAlignBlocViterbi s4aligner = S4ForceAlignBlocViterbi.getS4Aligner(convertedAudioFile.getAbsolutePath());
		List<Element_Mot>  lmots = edit.getListeElement().getMots();
		mots = new String[lmots.size()];
		for (int i=0;i<lmots.size();i++) {
			mots[i] = lmots.get(i).getWordString();
		}
		s4aligner.setMots(mots);
		return s4aligner;
	}

	/**
	 * Une zone du signal doit etre selectionnee.
	 * 1 ou plusieurs mots dans le texte doivent l'etre aussi.
	 * Aligne alors ce/ces mot/mots avec la partie du signal
	 */
	public void alignManual() {
		final long sdeb = sigPanel.getSelectionStart();
		final long send = sigPanel.getSelectionEnd();
		if (sdeb<0||send<0||send<sdeb+3) return;

		int caret1 = edit.getSelectionStart();
		int caret2 = edit.getSelectionEnd();
		final int mot1 = edit.getListeElement().getIndiceMotAtTextPosi(caret1);
		final int mot2 = edit.getListeElement().getIndiceMotAtTextPosi(caret2);
		final Element_Mot elmot1 = edit.getListeElement().getMot(mot1);
		final Element_Mot elmot2 = edit.getListeElement().getMot(mot2);

		new Thread(new Runnable() {
			@Override
			public void run() {
				edit.griseMotsRed(elmot1, elmot2);
				clearAlignFrom(mot1);
				System.out.println("REALIGN BEFORE");
				if (mot1-1>=0)
					realignBeforeAnchor(mot1-1, OldAlignment.sample2frame(sdeb)-1);
				S4AlignOrder order = new S4AlignOrder(mot1, OldAlignment.sample2frame(sdeb), mot2, OldAlignment.sample2frame(send));
				try {
					S4ForceAlignBlocViterbi s4aligner = getS4aligner();
					System.out.println("ALIGN SELECTED SEGMENT "+mots[mot1]+" ... "+mots[mot2]);

					s4aligner.input2process.put(order);
					synchronized(order) {
						order.wait();
					}
					System.out.println("================================= BATCH ALIGN FOUND");
					System.out.println(order.alignWords.toString());
					//					order.fullalign.checkWithText(Arrays.asList(mots),mot1);
					//					project.words.merge(order.fullalign,mot1);

					stopAlign();

					// quand on selectionne une zone du signal, on suppose qu'il n'y a pas de silence aux bouts !
					//					project.words.setEndFrameForWord(mot2, OldAlignment.sample2frame(send));

					edit.griseMotsRed(elmot1, elmot2);
					edit.setSelectedTextColor(null);
					//	    	        project.words.addManualAnchorv2(mot1);
					//	    	        project.words.addManualAnchorv2(mot2);
					edit.colorizeWords(mot1, mot2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * positionne l'audio et le texte à un endroit donné du texte
	 */
	public void gotoSourcePos(long posInText) {
		clicAtCaretPosition((int)posInText,MouseEvent.BUTTON1);
	}

	public void biasAdapt() {
		BiaisAdapt b=new BiaisAdapt(this);
		b.calculateBiais();
	}
	
	public static void main(String args[]) {
		Aligneur m = new Aligneur();
		MarkupLoader loader = null;
		String markupFileName = null;
		String audioFileName = null;

		m.inputControls();
		for (String arg: args) {
			String lcarg = arg.toLowerCase();

			if (lcarg.endsWith(".jtr")) {
				m.friendlyLoadProject(new File(arg));
			}

			else if (lcarg.endsWith(".wav")) {
				audioFileName = arg;
			}

			else if (lcarg.endsWith(".trs")) {
				loader = new TRSLoader();
				markupFileName = arg;
			}

			else if (lcarg.endsWith(".textgrid")) {
				loader = new TextGridLoader();
				markupFileName = arg;
			}

			else if (lcarg.endsWith(".txt")) {
				loader = new RawTextLoader();
				markupFileName = arg;
			}

			else {
				System.err.println("args error: dont know what to do with " + arg);
				return;
			}
		}

		boolean loaded = false;
		if (loader != null && markupFileName != null)
			loaded = m.friendlyLoadMarkup(loader, new File(markupFileName));

		m.setAudioSource(audioFileName);

		if (loaded && audioFileName != null)
			m.alignWithProgress();
	}

	/**
	 * Loads text markup file, refreshes indexes and updates the UI.
	 * Displays a message dialog if an error occurs.
	 * @param loader loader for the adequate markup format
	 * @return true if the file was loaded with no errors
	 */
	public boolean friendlyLoadMarkup(MarkupLoader loader, File markupFile) {
		printInStatusBar("Parsing markup");

		try {
			setProject(loader.parse(markupFile));
		} catch (Exception ex) {
			printInStatusBar("Couldn't parse markup");
			ex.printStackTrace();

			String message = "Couldn't parse \"" + markupFile.getName() +
					"\"\nas a \"" + loader.getFormat() + "\" file.\n\n";

			if (ex instanceof ParsingException)
				message += "This file is either erroneous or it contains " +
						"tokens that JTrans can't handle yet.";
			else if (ex instanceof IOException)
				message += "An I/O error occurred.";
			else
				message += "An unknown exception was thrown.\n" +
						"This isn't supposed to happen.\n" +
						"Please file a bug report.";

			JOptionPane.showMessageDialog(jf, message + "\n\n" + ex,
					"Parsing failed", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		jf.setTitle(markupFile.getName());
		printInStatusBar("Ready");
		return true;
	}


	/**
	 * Loads JTrans project file, refreshes indexes and updates the UI.
	 * Displays a message dialog if an error occurs.
	 * @return true if the file was loaded with no errors
	 */
	public boolean friendlyLoadProject(File file) {
		try {
			printInStatusBar("Loading project");
			loadJsonProject(file);
		} catch (IOException ex) {
			printInStatusBar("Couldn't load project");
			ex.printStackTrace();

			JOptionPane.showMessageDialog(jf, "Couldn't open project \""
					+ file.getName() + "\"\nbecause an I/O error occured.\n\n" + ex,
					"Couldn't open project", JOptionPane.ERROR_MESSAGE);
			return false;
		} catch (JsonParseException ex) {
			ex.printStackTrace();

			JOptionPane.showMessageDialog(jf, "Couldn't open project \""
					+ file.getName() + "\"\nbecause it is not a valid JSON file.\n\n"
					+ ex.getLocalizedMessage(),
					"Couldn't open project", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		jf.setTitle(file.getName());
		printInStatusBar("Ready");
		return true;
	}



	public void alignWithProgress() {
		final ProgressDialog progress = new ProgressDialog(jf, null, "Aligning...");
		progress.setRunnable(new Runnable() {
			public void run() { JTransAPI.alignBetweenAnchors(progress); }});
		progress.setVisible(true);
	}


	//==========================================================================
	// PRAAT OUTPUT
	//==========================================================================

	/**
	 * Breaks down a linear alignment into parallel alignments by speaker.
	 * Does not handle overlaps.
	 */
	private static Alignment[] breakDownBySpeaker(int speakers, Alignment speakerTurns, Alignment linearAlignment) {
		Alignment[] spk = new Alignment[speakers];
		for (int i = 0; i < speakers; i++)
			spk[i] = new Alignment();

		int[] seg2seg = linearAlignment.mapSegmentTimings(speakerTurns);

		for (int i = 0; i < linearAlignment.getNbSegments(); i++) {
			int spkCode = Integer.parseInt(speakerTurns.getSegmentLabel(seg2seg[i]));
			if (spkCode < 0)
				continue;
			spk[spkCode].addRecognizedSegment(
					linearAlignment.getSegmentLabel(i),
					linearAlignment.getSegmentDebFrame(i),
					linearAlignment.getSegmentEndFrame(i),
					null,
					null);
		}

		return spk;
	}

	/**
	 * Generates a Praat tier for an alignment.
	 * @param w Append text to this writer
	 * @param id Tier ID (Praat tier numbering starts at 1 and is contiguous!)
	 * @param name Tier name
	 * @param finalFrame Final frame in the entire file
	 */
	private static void praatTier(Writer w, int id, String name, int finalFrame, Alignment al) throws IOException {
		assert id > 0;
		w.append("\n\titem [").append(Integer.toString(id)).append("]:")
				.append("\n\t\tclass = \"IntervalTier\"")
				.append("\n\t\tname = \"").append(name).append('"') // TODO escape strings
				.append("\n\t\txmin = 0")
				.append("\n\t\txmax = ").append(Float.toString(JTransAPI.frame2sec(finalFrame)))
				.append("\n\t\tintervals: size = ")
				.append("" + al.getNbSegments());
		for (int j = 0; j < al.getNbSegments(); j++) {
			w.append("\n\t\tintervals [").append(Integer.toString(j+1)).append("]:")
					.append("\n\t\t\txmin = ")
					.append(Float.toString(JTransAPI.frame2sec(al.getSegmentDebFrame(j))))
					.append("\n\t\t\txmax = ")
					.append(Float.toString(JTransAPI.frame2sec(al.getSegmentEndFrame(j))))
					.append("\n\t\t\ttext = \"")
					.append(al.getSegmentLabel(j)).append('"'); // TODO escape strings
		}
	}


	public void savePraat(File f, boolean withWords, boolean withPhons) throws IOException {
		ListeElement    lst          = project.elts;
		int             speakers     = project.speakers.size();
		int             tiers        = speakers * ((withWords?1:0) + (withPhons?1:0));
		int             finalFrame   = project.words.getSegmentEndFrame(project.words.getNbSegments() - 1);
		Alignment       speakerTurns = lst.getLinearSpeakerTimes(project.words);
		Alignment[]     spkWords     = null;
		Alignment[]     spkPhons     = null;
		FileWriter      fw           = new FileWriter(f);

		fw.append("File type = \"ooTextFile\"")
				.append("\nObject class = \"TextGrid\"")
				.append("\n")
				.append("\nxmin = 0")
				.append("\nxmax = ").append("" + JTransAPI.frame2sec(finalFrame))
				.append("\ntiers? <exists>")
				.append("\nsize = ").append("" + tiers)
				.append("\nitem []:");

		// Linear, non-overlapping tiers
		if (withWords) spkWords = breakDownBySpeaker(speakers, speakerTurns, project.words);
		if (withPhons) spkPhons = breakDownBySpeaker(speakers, speakerTurns, project.phons);

		// Account for overlaps
		for (int i = 0; i < JTransAPI.overlaps.size(); i++) {
			int speakerID = JTransAPI.overlapSpeakers.get(i);
			S4AlignOrder order = JTransAPI.overlaps.get(i);
			if (withWords)
				spkWords[speakerID].overwrite(order.alignWords);
			if (withPhons)
				spkPhons[speakerID].overwrite(order.alignPhones);
		}

		// Now that we have the final segment count, generate Praat tiers
		int id = 1;
		for (int i = 0; i < speakers; i++) {
			String name = project.speakers.get(i).getName();
			if (withWords)
				praatTier(fw, id++, name + " words", finalFrame, spkWords[i]);
			if (withPhons)
				praatTier(fw, id++, name + " phons", finalFrame, spkPhons[i]);
		}

		fw.close();
	}


	public void setProject(Project project) {
		this.project = project;
		JTransAPI.setProject(project);
		edit.setProject(project);
	}
}
