/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

package plugins.applis.SimpleAligneur;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.BevelBorder;

import com.google.gson.JsonParseException;
import facade.AutoAligner;
import facade.Cache;
import facade.Project;
import markup.*;
import main.SpeechReco;

import markup.TextGridLoader;
import plugins.buffer.RoundBuffer;
import plugins.buffer.RoundBufferFrontEnd;
import plugins.signalViewers.spectroPanel.SpectroControl;
import plugins.signalViewers.temporalSigPanel.TemporalSigPanel;
import plugins.signalViewers.temporalSigPanel.ToolBarTemporalSig;
import plugins.text.ListeElement;
import speechreco.adaptation.BiaisAdapt;
import speechreco.aligners.sphiinx4.*;
import plugins.text.ColoriageEvent;
import plugins.text.GriserWhilePlaying;
import plugins.text.TexteEditor;
import plugins.text.elements.*;
import utils.*;
import utils.PrintLogger;
import speechreco.RecoWord;
import tools.audio.PlayerGUI;

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

	public TexteEditor edit;
	//	public Player player;
	public TemporalSigPanel sigPanel = null;
	public ToolBarTemporalSig toolbar = null;
	public KeysManager kmgr = null;
	PlayerListener playerController;

	/** Audio file in a suitable format for processing */
	public File convertedAudioFile = null;
	public long audioSourceTotalFrames = -1;

	public RoundBuffer audiobuf = new RoundBuffer(this, 10000000);
	public RoundBufferFrontEnd mfccbuf;

	// position lue pour la derniere fois dans le flux audio
	long currentSample = 0;
	public Project project = new Project();
	int wordSelectedIdx = -1;
	public boolean caretSensible = true;
	JLabel infoLabel = new JLabel("Welcome to JTrans");

	public float getCurPosInSec() {return cursec;}
	public void setCurPosInSec(float sec) {
		cursec=sec;
		int fr = TimeConverter.second2frame(cursec);
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
	 * Target audio format. Any input audio files that do not match this format
	 * will be converted to it before being processed.
	 */
	private static final AudioFormat SUITABLE_AUDIO_FORMAT =
			new AudioFormat(16000, 16, 1, true, false);


	/**
	 * Return an audio file in a suitable format for JTrans. If the original
	 * file isn't in the right format, convert it and cache it.
	 */
	public static File suitableAudioFile(final File original) {
		AudioFormat af;

		try {
			af = AudioSystem.getAudioFileFormat(original).getFormat();
		} catch (UnsupportedAudioFileException ex) {
			ex.printStackTrace();
			return original;
		} catch (IOException ex) {
			ex.printStackTrace();
			return original;
		}

		if (af.matches(SUITABLE_AUDIO_FORMAT)) {
			System.out.println("suitableAudioFile: no conversion needed!");
			return original;
		}

		System.out.println("suitableAudioFile: need conversion, trying to get one from the cache");

		Cache.FileFactory factory = new Cache.FileFactory() {
			public void write(File f) throws IOException {
				System.out.println("suitableAudioFile: no cache found... creating one");

				AudioInputStream originalStream;
				try {
					originalStream = AudioSystem.getAudioInputStream(original);
				} catch (UnsupportedAudioFileException ex) {
					ex.printStackTrace();
					throw new Error("Unsupported audio file; should've been caught above!");
				}

				AudioSystem.write(
						AudioSystem.getAudioInputStream(SUITABLE_AUDIO_FORMAT, originalStream),
						AudioFileFormat.Type.WAVE,
						f);
			}
		};

		return Cache.cachedFile("converted", "wav", factory, original);
	}

	/**
	 * Sets the sound source file and converts it to a suitable format for
	 * JTrans if needed.
	 * @param path path to the sound file
	 */
	public void setAudioSource(String path) {
		project.wavname = path;

		if (path != null) {
			convertedAudioFile = suitableAudioFile(new File(project.wavname));

			try {
				AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(convertedAudioFile);
				AudioFormat format = audioInputStream.getFormat();
				long frames = audioInputStream.getFrameLength();
				double durationInSeconds = (frames+0.0) / format.getFrameRate();
				audioSourceTotalFrames = TimeConverter.second2frame((float)durationInSeconds);
			} catch (IOException ex) {
				audioSourceTotalFrames = -1;
			} catch (UnsupportedAudioFileException ex) {
				audioSourceTotalFrames = -1;
			}
		} else
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

	private float lastSecClickedOnSpectro = 0;
	public void clicOnSpectro(float frf) {
		float prevsec = getCurPosInSec();
		float sec = TimeConverter.frame2sec((int) frf);
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
		initPanel();
		createJFrame();
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

	void goHome() {
		//		if (player.isPlaying()) return;
		currentSample=0;
		wordSelectedIdx=0;
		if (sigPanel!=null) {
			sigPanel.setProgressBar(0);
		}
		Element_Mot firstmot = project.elts.getMot(0);
		edit.griseMot(firstmot);
	}

	/**
	 * sorte de "reinit" pour rendre la main au user
	 */
	void stopAll() {
		stopPlaying();
//		stopAlign();
		caretSensible = true;
		// attention ! ne pas modifier usersShouldConfirm !!
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
		// cherche le prochain mot qui est aligné
		int seg4mot=-1;
		List<Element_Mot> mots = project.elts.getMots();
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
		for (int i=mot;i<project.elts.getMots().size();i++) {
			project.elts.getMot(i).posInAlign=-1;
		}
		// decolorier les mots qui ne sont plus alignes:
		if (edit!=null) {
			edit.getHighlighter().removeAllHighlights();
			if (mot>0) edit.colorizeWords(0, mot - 1);
		}
		repaint();
	}

	void anchorClicked(Element_Ancre anchor) {
		String newPosString = JOptionPane.showInputDialog(jf,
				"Enter new anchor position in seconds:",
				Float.toString(anchor.seconds));

		if (newPosString == null)
			return;

		float newPos = Float.parseFloat(newPosString);

		// Make sure the requested position is legal...

		if (newPos < 0) {
			JOptionPane.showMessageDialog(jf,
					"Can't set to negative position!",
					"Illegal anchor position", JOptionPane.ERROR_MESSAGE);
			return;
		}

		ListeElement.Neighborhood<Element_Ancre> range =
				project.elts.getNeighbors(anchor, Element_Ancre.class);

		if (range.prev != null && range.prev.seconds > newPos) {
			JOptionPane.showMessageDialog(jf,
					"Can't move this anchor before the previous anchor\n" +
					"(at " + range.prev.seconds + " seconds).",
					"Illegal anchor position", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (range.next != null && range.next.seconds < newPos) {
			JOptionPane.showMessageDialog(jf,
					"Can't move this anchor past the next anchor\n" +
					"(at " + range.next.seconds + " seconds).",
					"Illegal anchor position", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// ...Alright, we're safe now

		anchor.seconds = newPos;

		project.clearAlignmentAround(anchor);
		setProject(project); // force refresh

		int rc = JOptionPane.showConfirmDialog(jf, "Realign?",
				"Anchor repositioned", JOptionPane.YES_NO_OPTION);

		if (rc == JOptionPane.YES_OPTION)
			alignBetweenAnchorsWithProgress();
	}
	
	void clicAtCaretPosition(int caretPos, int button) {
		int elementIdx = project.elts.getIndiceElementAtTextPosi(caretPos);
		if (elementIdx < 0)
			return;

		Element el = project.elts.get(elementIdx);
		if (el instanceof Element_Ancre) {
			anchorClicked((Element_Ancre)el);
			return;
		} else if (!(el instanceof Element_Mot))
			return;

		int mot = project.elts.getIndiceMotAtTextPosi(caretPos);

			// nouveau player:
			boolean replay = ctrlbox.getPlayerGUI().isPlaying();
			ctrlbox.getPlayerGUI().stopPlaying();
			Thread.yield();

			// position pour le play
			wordSelectedIdx=mot;
			Element_Mot emot = project.elts.getMot(mot);
			int segidx = emot.posInAlign;
			if (segidx>=0) {
				int frame = project.words.getSegmentDebFrame(segidx);
				cursec = TimeConverter.frame2sec(frame);
				long currentSample = TimeConverter.frame2sample(frame);
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
			Element_Mot ew = new Element_Mot(word.word);
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
		List<Element_Mot>  lmots = project.elts.getMots();
		mots = new String[lmots.size()];
		for (int i=0;i<lmots.size();i++) {
			mots[i] = lmots.get(i).getWordString();
		}
		s4aligner.setMots(mots);
		return s4aligner;
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

		if (loader != null)
			m.friendlyLoadMarkup(loader, new File(markupFileName));

		m.setAudioSource(audioFileName);
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
			setProject(Project.fromJson(file));
			caretSensible = true;
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

	public void alignBetweenAnchorsWithProgress() {
		final ProgressDialog progress = new ProgressDialog(jf, null, "Aligning...");
		progress.setRunnable(new Runnable() {
			public void run() {
				new AutoAligner(project, Aligneur.this).alignBetweenAnchors(progress);
			}});
		progress.setVisible(true);
	}

	public void alignAllWithProgress() {
		final ProgressDialog progress = new ProgressDialog(jf, null, "Aligning...");
		progress.setRunnable(new Runnable() {
			public void run() {
				new AutoAligner(project, Aligneur.this).alignRaw(progress);
			}});
		progress.setVisible(true);
	}

	public void setProject(Project project) {
		this.project = project;
		setAudioSource(project.wavname);
		edit.setProject(project);
	}
}
