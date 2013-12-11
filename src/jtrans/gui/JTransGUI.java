/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

package jtrans.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;

import jtrans.elements.Anchor;
import jtrans.elements.Word;
import jtrans.facade.AutoAligner;
import jtrans.facade.Cache;
import jtrans.facade.Project;
import jtrans.facade.Track;
import jtrans.speechreco.SpeechReco;
import jtrans.markup.*;

import jtrans.markup.TextGridLoader;
import jtrans.buffer.RoundBuffer;
import jtrans.buffer.RoundBufferFrontEnd;
import jtrans.gui.signalViewers.spectroPanel.SpectroControl;
import jtrans.elements.ElementList;
import jtrans.speechreco.BiaisAdapt;
import jtrans.speechreco.s4.S4ForceAlignBlocViterbi;
import jtrans.utils.CancelableProgressDialog;
import jtrans.utils.FileUtils;
import jtrans.utils.ProgressDisplay;
import jtrans.utils.TimeConverter;
import org.fuin.utils4j.Utils4J;

/**
 * Main panel.
 */
public class JTransGUI extends JPanel implements ProgressDisplay {

	/*
	 * TODO THIS NEEDS TO BE FIXED ASAP
	 */
	public static void REIMPLEMENT_DEC2013() {
		// TODO
		System.err.println("REIMPLEMENT ME!");
		new Throwable().printStackTrace();
		JOptionPane.showMessageDialog(null,
				"REIMPLEMENT ME!\n\n" +
				"This feature is temporarily unavailable due\n" +
				"to the ongoing (dec. 2013) refactoring to introduce\n" +
				"truly parallel tracks.\n\n" +
				"Please checkout the master branch if you\n" +
				"need a stable version.\n\n" +
				"Stack trace on stderr.",
				"TODO",
				JOptionPane.ERROR_MESSAGE);
	}



	public static final int KARAOKE_UPDATE_INTERVAL = 50; // milliseconds

	private static final String RESOURCE_URL =
			"http://talc1.loria.fr/users/cerisara/jtrans/jtrans_res_20131209.zip";

	public JFrame jf=null;

	/**
	 * position courante en millisecondes centralisee: mise a jour par les
	 * differents plugins qui peuvent la modifier;
	 */
	private float cursec = 0;
	public ControlBox ctrlbox;
	private SpectroControl sigpan=null;
	public boolean showPhones=false;

	/* IMPORTANT: the karaoke highlighter *has* to be a Swing timer, not a
	java.util.Timer. This is to ensure that the callback is called from
	Swing's event dispatch thread. */
	private Timer karaokeHighlighter = null;

	private PlayerGUI playergui;

	public int mixidx=0;

	//	public Player player;
	/* TODO PARALLEL TRACKS
	public TemporalSigPanel sigPanel = null;
	public ToolBarTemporalSig toolbar = null;
	*/

	List<TrackView> views;

	/** Audio file in a suitable format for processing */
	public File convertedAudioFile = null;
	public long audioSourceTotalFrames = -1;

	public RoundBuffer audiobuf = new RoundBuffer(this, 10000000);
	public RoundBufferFrontEnd mfccbuf;

	// position lue pour la derniere fois dans le flux audio
	long currentSample = 0;
	public Project project = new Project();
	private JProgressBar progressBar = new JProgressBar(0, 1000);
	private JLabel infoLabel = new JLabel();

	public float getCurPosInSec() {
        return cursec;
    }

	public void setCurPosInSec(float sec) {
		cursec = sec;
		REIMPLEMENT_DEC2013(); /* TODO PARALLEL TRACKS
		int frame = TimeConverter.second2frame(cursec);
        // vieux panel
        if (sigPanel!=null) {
            long currentSample = TimeConverter.frame2sample(frame);
            if (currentSample < 0) currentSample = 0;
            sigPanel.setProgressBar(currentSample);
        }

		// nouveau panel
		sigpan.setAudioInputStream(cursec, getAudioStreamFromSec(cursec));
		if (showPhones) {
			sigpan.setAlign(project.phons);
			sigpan.setFirstSeg(project.phons.getSegmentAtFrame(frame));
		} else {
			sigpan.setAlign(project.words);
            sigpan.setFirstSeg(project.words.getSegmentAtFrame(frame));
        }
*/

		updateViewers();
	}

	@Override
	public void setIndeterminateProgress(final String message) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				infoLabel.setText(message);
				progressBar.setEnabled(true);
				progressBar.setIndeterminate(true);
			}
		});
	}

	@Override
	public void setProgress(final String message, final float f) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				infoLabel.setText(message);
				progressBar.setEnabled(true);
				progressBar.setIndeterminate(false);
				progressBar.setValue((int)(f*1000f));
			}
		});
	}

	@Override
	public void setProgressDone() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				infoLabel.setText("Ready");
				progressBar.setEnabled(false);
				progressBar.setIndeterminate(false);
				progressBar.setValue(0);
			}
		});
	}

	public void quit() {
		if (karaokeHighlighter != null)
			karaokeHighlighter.stop();
		jf.dispose();
		System.exit(0);
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
			setIndeterminateProgress("Loading audio from " + path + "...");
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

		updateViewers();

		setProgressDone();
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

	public JTransGUI() {
		initPanel();
		createJFrame();
	}

	private void createJFrame() {
		// Use OS X menu bar if possible
		if (System.getProperty("os.name").toLowerCase().contains("mac"))
			System.setProperty("apple.laf.useScreenMenuBar", "true");

		jf = new JFrame("JTrans");
		JMenuBar menubar = (new Menus(this)).menus();
		jf.setJMenuBar(menubar);
		jf.setContentPane(this);

		jf.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});
		jf.pack();
		jf.setLocationByPlatform(true);
		jf.setVisible(true);
	}

	private void initPanel() {
		removeAll();
		setLayout(new BorderLayout());

		JPanel centerPane = new JPanel(new GridLayout(1, project.tracks.size()));

		views = new ArrayList<TrackView>(project.tracks.size());

		for (Track t: project.tracks) {
			final TrackView area = new TrackView(this, t);
			final JScrollPane scroll = new JScrollPane(area);
			final JLabel speakerLabel = new JLabel(t.speakerName,
					SwingConstants.CENTER);
			speakerLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 24));
			speakerLabel.setEnabled(false);

			views.add(area);

			centerPane.add(
					new JPanel(new BorderLayout()) {{
						add(speakerLabel, BorderLayout.NORTH);
						add(scroll, BorderLayout.CENTER);
					}}
			);
		}

		centerPane.setPreferredSize(new Dimension(1000, 500));

		ctrlbox = new ControlBox(this);
		playergui = ctrlbox.getPlayerGUI();

		infoLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));

		final JPanel status = new JPanel(new BorderLayout(5, 0)) {{
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			add(new JSeparator(), BorderLayout.NORTH);
			add(progressBar, BorderLayout.WEST);
			add(infoLabel, BorderLayout.CENTER);
		}};

		sigpan = new SpectroControl(this);
		AudioInputStream aud = getAudioStreamFromSec(getCurPosInSec());
		if (aud!=null) sigpan.setAudioInputStream(getCurPosInSec(),aud);

		// Add everything to the panel

		add(ctrlbox, BorderLayout.NORTH);
		add(centerPane, BorderLayout.CENTER);

		add(new JPanel(new BorderLayout()) {{
			add(sigpan, BorderLayout.NORTH);
			add(status, BorderLayout.SOUTH);
		}}, BorderLayout.SOUTH);
	}

	void goHome() {
		REIMPLEMENT_DEC2013(); /* TODO PARALLEL TRACKS
		//		if (player.isPlaying()) return;
		currentSample=0;
		if (sigPanel!=null) {
			sigPanel.setProgressBar(0);
		}
		Word firstmot = project.elts.getMot(0);
		edit.highlightWord(firstmot);
*/
	}

	/**
	 * sorte de "reinit" pour rendre la main au user
	 */
	void stopAll() {
		stopPlaying();
//		stopAlign();
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

	public void newplaystarted() {
		REIMPLEMENT_DEC2013(); /* TODO PARALLEL TRACKS
		if (project.words != null) {
			karaokeHighlighter = new Timer(KARAOKE_UPDATE_INTERVAL, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					long curt = System.currentTimeMillis();
					long t0 = playergui.getTimePlayStarted();
					int curfr = TimeConverter.millisec2frame(curt-t0);
					// ajoute le debut du segment joué
					curfr += playergui.getRelativeStartingSec()*100;
					int segidx = project.words.getSegmentAtFrame(curfr);
					if (segidx < 0)
						return;
					edit.highlightWord(project.elts.getMotAtSegment(segidx));
				}
			});
			karaokeHighlighter.start();
		}
*/
	}
	public void newplaystopped() {
		if (karaokeHighlighter != null) {
			karaokeHighlighter.stop();
			karaokeHighlighter = null;
		}
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
		project.clearAlignment();
		setProject(project); // force refresh
	}

/* TODO PARALLEL TRACKS
	void clearAlignFrom(int mot) {
		// cherche le prochain mot qui est aligné
		int seg4mot=-1;
		List<Word> mots = project.elts.getMots();
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
*/

	/**
	 * Ensures newPos is a valid position for an anchor within the given
	 * range; if not, informs the user with error messages.
	 * @return true if the position is valid
	 */
	public boolean enforceLegalAnchor(ElementList.Neighborhood<Anchor> range, float newPos) {
		if (newPos < 0) {
			JOptionPane.showMessageDialog(jf,
					"Can't set to negative position!",
					"Illegal anchor position", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if (range.prev != null && range.prev.seconds > newPos) {
			JOptionPane.showMessageDialog(jf,
					"Can't set this anchor before the previous anchor\n" +
							"(at " + range.prev.seconds + " seconds).",
					"Illegal anchor position", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if (range.next != null && range.next.seconds < newPos) {
			JOptionPane.showMessageDialog(jf,
					"Can't set this anchor past the next anchor\n" +
							"(at " + range.next.seconds + " seconds).",
					"Illegal anchor position", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		return true;
	}

	public void repositionAnchor(Anchor anchor) {
		REIMPLEMENT_DEC2013(); /* TODO PARALLEL TRACKS
		String newPosString = JOptionPane.showInputDialog(jf,
				"Enter new anchor position in seconds:",
				Float.toString(anchor.seconds));

		if (newPosString == null)
			return;

		float newPos = Float.parseFloat(newPosString);

		if (!enforceLegalAnchor(
				project.elts.getNeighbors(anchor, Anchor.class), newPos))
			return;

		anchor.seconds = newPos;

		project.clearAlignmentAround(anchor);
		setProject(project); // force refresh
*/
	}

	public void selectWord(Word word) {
		REIMPLEMENT_DEC2013(); /* TODO PARALLEL TRACKS
        boolean replay = ctrlbox.getPlayerGUI().isPlaying();
        ctrlbox.getPlayerGUI().stopPlaying();
        Thread.yield();

        if (word.posInAlign >= 0) {
            edit.highlightWord(word);
            setCurPosInSec(TimeConverter.frame2sec(
                    project.words.getSegmentDebFrame(word.posInAlign)));
        } else {
            System.err.println("warning: pas de segment associé au mot " + word.getWordString());
            replay=false;
        }

        if (replay)
            ctrlbox.getPlayerGUI().startPlaying();
            */
	}

	private void getRecoResult(SpeechReco asr) {
		List<String> lmots = getRecoResultOld(asr);
		//    	alignement.merge(asr.fullalign,0);
		//   	alignement.checkWithText(lmots, 0);

		//    	System.out.println("debuglmots "+lmots.size()+" "+alignement.wordsIdx.size()+" "+alignement.wordsEnd.size());
		REIMPLEMENT_DEC2013(); /*
		edit.colorizeWords(0, lmots.size() - 1);
		repaint();
		*/
	}

	private List<String> getRecoResultOld(SpeechReco asr) {
		REIMPLEMENT_DEC2013();
/*
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
			Word ew = new Word(word.word);
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
		edit.setCaretPosition(0);
		edit.textChanged = false;
		edit.setIgnoreRepaint(false);
		edit.repaint();
		return lmots;
*/ return null;
	}

	public void asr() {
		setIndeterminateProgress("Transcribing...");
		new Thread() {
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
				setProgressDone();
			}
		}.start();
	}

	public S4ForceAlignBlocViterbi getS4aligner(Track track) {
		// Create an array of word strings
		List<Word> wordElements = track.elts.getMots();
		String[] wordStrings = new String[wordElements.size()];
		for (int i = 0; i < wordElements.size(); i++)
			wordStrings[i] = wordElements.get(i).getWordString();
		// Create the aligner
		S4ForceAlignBlocViterbi s4aligner = S4ForceAlignBlocViterbi.getS4Aligner(
				convertedAudioFile.getAbsolutePath(), this);
		s4aligner.setMots(wordStrings);
		return s4aligner;
	}

	public void biasAdapt() {
		BiaisAdapt b=new BiaisAdapt(this);
		b.calculateBiais();
	}
	
	public static void main(String args[]) {
		checkResources();

		JTransGUI m = new JTransGUI();
		MarkupLoader loader = null;
		String markupFileName = null;
		String audioFileName = null;

		for (String arg: args) {
			String lcarg = arg.toLowerCase();

			if (lcarg.endsWith(".wav")) {
				audioFileName = arg;
			}

			else if (lcarg.endsWith(".jtr")) {
				loader = new JTRLoader();
				markupFileName = arg;
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

		if (loader != null) {
			m.friendlyLoadMarkup(loader,
					new File(markupFileName),
					audioFileName==null? null: new File(audioFileName));
		} else {
			m.setAudioSource(audioFileName);
		}
	}


	/**
	 * Checks that the resources are installed. If not, suggests downloading
	 * and installing the resources automatically. If the user declines, the
	 * program is aborted.
	 */
	private static void checkResources() {
		if (new File("res").exists())
			return;

		int rc = JOptionPane.showConfirmDialog(null,
				"JTrans resources are not installed.\n" +
				"Would you like to install them now?\n\n" +
				"Warning: this will trigger a 400 MB download.\n" +
				"Once installed, the resources take up about\n" +
				"1 GB of disk space.",
				"Install resources",
				JOptionPane.YES_NO_OPTION);

		if (rc != JOptionPane.YES_OPTION)
			System.exit(1);

		final CancelableProgressDialog progress =
				new CancelableProgressDialog("Installing JTrans resources", true);

		progress.setTask(new Callable() {
			@Override
			public Object call() throws Exception {
				URL url = new URL(RESOURCE_URL);
				File zip = File.createTempFile("jtrans-res-", ".zip");
				zip.deleteOnExit();
				FileUtils.downloadFile(url, zip, progress);
				progress.setCancelable(false);
				progress.setIndeterminateProgress("Decompressing...");
				Utils4J.unzip(zip, new File("."));
				zip.delete();
				progress.setProgressDone();
				return null;
			}
		});

		try {
			progress.executeInForeground();
			return;
		} catch (CancellationException ignore) {
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null,
					"Error while installing the resources.\n\n" + ex,
					"Error",
					JOptionPane.ERROR_MESSAGE);
		}

		System.exit(1);
	}


	/**
	 * Loads text markup file, refreshes indexes and updates the UI.
	 * Displays a message dialog if an error occurs.
	 * @param loader loader for the adequate markup format
	 * @param markupFile file to be parsed
	 * @param forcedAudioFile Sets this file as the project's audio source
	 *                        even if the project specifies its own source.
	 *                        If null, the project's own audio source is used.
	 * @return true if the file was loaded with no errors
	 */
	public boolean friendlyLoadMarkup(MarkupLoader loader, File markupFile, File forcedAudioFile) {
		setIndeterminateProgress("Parsing " + loader.getFormat() + "...");

		Project previousProject = project;

		try {
			setProject(loader.parse(markupFile));
		} catch (Exception ex) {
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

			setProgressDone();
			return false;
		}

		setProgressDone();
		jf.setTitle(markupFile.getName());

		if (forcedAudioFile != null) {
			setAudioSource(forcedAudioFile.getAbsolutePath());
		} else if (project.wavname == null) {
			// Try to detect audio file from the project's file name
			String pfn = markupFile.getName();
			File possibleAudio = new File(markupFile.getParentFile(),
					pfn.substring(0, pfn.lastIndexOf('.')) + ".wav");

			if (possibleAudio.exists()) {
				int rc = JOptionPane.showConfirmDialog(jf,
						"Found an audio file with a similar name:\n" +
						possibleAudio + "\n\n" +
						"Would you like to load it?\n\n" +
						"If you refuse, you can always set an audio source\n" +
						"manually through \"File → Load audio.\"",
						"Possible audio file",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE);
				if (rc == JOptionPane.YES_OPTION)
					setAudioSource(possibleAudio.getAbsolutePath());
			}

			// Try to recycle the previous project's audio file
			if (project.wavname == null &&
					previousProject != null &&
					previousProject.wavname != null)
			{
				String[] choices =
						"Keep this audio source;Use empty audio source".split(";");

				int rc = JOptionPane.showOptionDialog(jf,
						"The project you have just loaded has no audio file attached to it.\n\n" +
						"However, this audio file was loaded up until now:\n" +
						previousProject.wavname + "\n\n" +
						"Would you like to keep it as the new project's audio source?",
						"No audio source",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null,
						choices,
						choices[0]);

				if (rc == 0)
					setAudioSource(previousProject.wavname);
			}
		}

		return true;
	}

	public void alignBetweenAnchorsWithProgress() {
		new Thread() {
			@Override
			public void run() {
				for (int i = 0; i < project.tracks.size(); i++) {
					new AutoAligner(project,
							project.tracks.get(i),
							JTransGUI.this,
							views.get(i))
					.alignBetweenAnchors();
				}
				setProgressDone();
			}
		}.start();
	}

	public void alignAllWithProgress() {
		REIMPLEMENT_DEC2013(); /* TODO PARALLEL TRACKS
		new Thread() {
			@Override
			public void run() {
				new AutoAligner(project, JTransGUI.this).alignRaw();
				setProgressDone();
			}
		}.start();
*/
	}

	public void setProject(Project project) {
		this.project = project;
		setAudioSource(project.wavname);
		initPanel();
		jf.setContentPane(this);
	}

	/**
	 * Forces refreshing the project view.
	 */
	public void refresh() {
		// TODO - just a tad overkill?
		setProject(project);
	}
}
