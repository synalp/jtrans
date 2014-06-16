/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

package fr.loria.synalp.jtrans.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.List;
import javax.sound.sampled.*;
import javax.swing.*;

import fr.loria.synalp.jtrans.JTrans;
import fr.loria.synalp.jtrans.align.Aligner;
import fr.loria.synalp.jtrans.gui.trackview.ProjectTable;
import fr.loria.synalp.jtrans.markup.in.MarkupLoader;
import fr.loria.synalp.jtrans.markup.in.ParsingException;
import fr.loria.synalp.jtrans.project.Project;
import fr.loria.synalp.jtrans.project.TrackProject;
import fr.loria.synalp.jtrans.project.TurnProject;
import fr.loria.synalp.jtrans.speechreco.SpeechReco;

import fr.loria.synalp.jtrans.gui.spectro.SpectroControl;
import fr.loria.synalp.jtrans.speechreco.BiaisAdapt;
import fr.loria.synalp.jtrans.utils.*;

/**
 * Main panel.
 */
public class JTransGUI extends JPanel implements ProgressDisplay {


	public static void REIMPLEMENT_DEC2013() {
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




	public JFrame jf=null;

	/**
	 * position courante en millisecondes centralisee: mise a jour par les
	 * differents plugins qui peuvent la modifier;
	 */
	private float cursec = 0;
	public ControlBox ctrlbox;
	public SpectroControl spectro;
	private KaraokeHighlighter karaoke;

	public int mixidx=0;

	public ProjectTable table;

	public Project project = new TrackProject();
	private JProgressBar progressBar = new JProgressBar(0, 1000);
	private JLabel infoLabel = new JLabel();

	public WordFinder contentWordFinder = new WordFinder.ByContent(this);
	public WordFinder anonWordFinder = new WordFinder.Anonymous(this);


	public float getCurPosInSec() {
        return cursec;
    }

	public void setCurPosInSec(float sec) {
		cursec = sec;
		spectro.setAudioInputStreamPosition(cursec);
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
		stopKaraoke();
		jf.dispose();
		System.exit(0);
	}


	/**
	 * Sets the sound source file and converts it to a suitable format for
	 * JTrans if needed.
	 */
	public void setAudioSource(File soundFile) {
		setIndeterminateProgress("Loading audio from " + soundFile + "...");
		project.setAudio(soundFile);
		spectro.setAudioInputStream(getAudioStreamFromSec(0));
		setCurPosInSec(0);
		setProgressDone();
	}
	
	public AudioInputStream getAudioStreamFromSec(float sec) {
		if (project.convertedAudioFile == null)
			return null;

		AudioInputStream ais;

		try {
			ais = AudioSystem.getAudioInputStream(project.convertedAudioFile);
		} catch (UnsupportedAudioFileException ex) {
			ex.printStackTrace();
			ais = null;
		} catch (IOException ex) {
			ex.printStackTrace();
			ais = null;
		}

		if (ais == null) {
			JOptionPane.showMessageDialog(jf,
					"No audio stream from " + project.convertedAudioFile,
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

	public void clicOnSpectro(float frf) {
		float prevsec = getCurPosInSec();
		float sec = TimeConverter.frame2sec((int) frf);
		sec += prevsec;
		// on lit une seconde avant la pos
		setCurPosInSec(sec-1);
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

	public JTransGUI(JTrans cli) {
		this();

		if (cli.loader != null) {
			friendlyLoadMarkup(cli.loader, cli.inputFile, cli.audioFile);
		} else {
			setAudioSource(cli.audioFile);
		}
	}

	private void createJFrame() {
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
		jf.setLocationRelativeTo(null);
		jf.setVisible(true);
	}

	private void initPanel() {
		removeAll();
		setLayout(new BorderLayout());

		table = new ProjectTable(project, this, true);
		JScrollPane multiTrackScrollPane = new JScrollPane(table);
		multiTrackScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		ctrlbox = new ControlBox(this);

		infoLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));

		final JPanel status = new JPanel(new BorderLayout(5, 0)) {{
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			add(new JSeparator(), BorderLayout.PAGE_START);
			add(progressBar, BorderLayout.LINE_START);
			add(infoLabel, BorderLayout.CENTER);
		}};

		spectro = new SpectroControl(this);
		spectro.setAudioInputStreamPosition(getCurPosInSec());

		// Add everything to the panel

		add(ctrlbox, BorderLayout.PAGE_START);
		add(multiTrackScrollPane, BorderLayout.CENTER);

		add(new JPanel(new BorderLayout()) {{
			add(spectro, BorderLayout.PAGE_START);
			add(status, BorderLayout.PAGE_END);
		}}, BorderLayout.PAGE_END);
	}


	public void startKaraoke() {
		karaoke = new KaraokeHighlighter(this);
		karaoke.start();
	}


	public void stopKaraoke() {
		if (karaoke != null) {
			karaoke.stop();
			karaoke = null;
		}
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
				asr.doReco(project.convertedAudioFile.getAbsolutePath(), "");
				getRecoResult(asr);
				setProgressDone();
			}
		}.start();
	}

	public void biasAdapt() {
		BiaisAdapt b=new BiaisAdapt(this);
		b.calculateBiais();
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

		String title = markupFile.getName();
		if (project instanceof TurnProject) {
			title += " [turn-based]";
		} else if (project instanceof TrackProject) {
			title += " [track-based]";
		} else {
			title += " [" + project.getClass().getSimpleName() + "]";
		}
		jf.setTitle(title);

		if (forcedAudioFile != null) {
			setAudioSource(forcedAudioFile);
		} else if (project.audioFile == null) {
			// Try to detect audio file from the project's file name
			File possibleAudio = FileUtils.detectHomonymousFile(
					markupFile, JTrans.AUDIO_EXTENSIONS);

			if (possibleAudio != null) {
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
					setAudioSource(possibleAudio);
			}
		}

		return true;
	}

	public void alignAll() {
		if (project.audioFile == null) {
			JOptionPane.showMessageDialog(
					jf,
					"Can't align because no audio file is attached to this project.\n" +
					"Please set an audio file (File -> Load audio).",
					"No audio file",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		new Thread() {
			@Override
			public void run() {
				Aligner aligner;

				try {
					aligner = project.getStandardAligner(JTransGUI.this, false);
				} catch (ReflectiveOperationException|IOException ex) {
					errorMessage("Couldn't create aligner!", ex);
					return;
				}

				try {
					project.align(aligner, null);
				} catch (Exception ex) {
					errorMessage("An error occured during the alignment!", ex);
				}

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						table.refreshModel();
					}
				});

				setProgressDone();
			}
		}.start();
	}

	public void setProject(Project project) {
		this.project = project;
		setAudioSource(project.audioFile);
		initPanel();
		jf.setContentPane(this);
		anonWordFinder.reset();
		contentWordFinder.reset();
	}


	/**
	 * Shows an error dialog, optionally describing a Throwable or its cause
	 * (if any) and printing its stack trace.
	 * @param t may be null
	 */
	public void errorMessage(String message, final Throwable t) {
		String title;

		if (t != null) {
			Throwable shown = t.getCause();
			if (shown == null) {
				shown = t;
			}
			String name = t.getClass().getSimpleName();

			message += "\n\n" + name;
			if (shown.getMessage() != null) {
				message += "\n" + shown.getMessage();
			}

			title = name;
			t.printStackTrace();
		} else {
			title = "Error";
		}

		final String fMessage = message, fTitle = title;

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JOptionPane.showMessageDialog(
						jf, fMessage, fTitle, JOptionPane.ERROR_MESSAGE);
			}
		});
	}

}
