/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

package plugins.applis.SimpleAligneur;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.cmu.sphinx.result.Result;

import facade.Project;
import markup.*;
import main.LiveSpeechReco;

import plugins.sourceSignals.Mike2wav;
import plugins.text.elements.Element_Ancre;
import speechreco.RecoListener;
import speechreco.adaptation.BiaisAdapt;
import speechreco.aligners.sphiinx4.S4ForceAlignBlocViterbi;
import speechreco.grammaire.Grammatiseur;
import plugins.text.TexteEditor;
import utils.NicerFileChooser;
import utils.ProgressDialog;

public class Menus {
	Aligneur aligneur;

	String reco;
	boolean[] done = {false};
	LiveSpeechReco gram;

	private static final FileFilter
			filterJTR = new FileNameExtensionFilter("JTrans Project (*.jtr, *.json)", "jtr", "json"),
			filterTRS = new FileNameExtensionFilter("Transcriber (*.trs)", "trs"),
			filterTextGrid = new FileNameExtensionFilter("Praat TextGrid (*.textgrid)", "textgrid"),
			filterTextGridWordsOnly = new FileNameExtensionFilter("Praat TextGrid (*.textgrid) - Words only", "textgrid"),
			filterTextGridWordsAndPhons = new FileNameExtensionFilter("Praat TextGrid (*.textgrid) - Words + Phonemes", "textgrid"),
			filterTXT = new FileNameExtensionFilter("Raw Text (*.txt)", "txt");

	private static final int[] FONT_SIZES = {
			10, 11, 12, 13, 14, 16, 18, 20, 24, 28, 36
	};

	private static final String[] FONT_FAMILIES = {
			Font.SANS_SERIF,
			Font.SERIF,
			Font.MONOSPACED
	};

	public Menus(Aligneur main) {
		aligneur=main;
	}

	public JMenuBar menus() {
		final JMenuBar menubar = new JMenuBar();

		final int modifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		// //////////////////////////////////////////////////////////////
		JMenu file        = new JMenu("File");

		JMenuItem open    = new JMenuItem("Open project, markup or text...");
		JMenuItem loadwav = new JMenuItem("Load audio...");
		JMenuItem savejtr = new JMenuItem("Save project as...");
		JMenuItem export  = new JMenuItem("Export alignment...");
		JMenuItem savewav = new JMenuItem("Export audio...");
		JMenuItem quit    = new JMenuItem("Quit");

		menubar.add(file);
		file.add(open);
		file.addSeparator();
		file.add(savejtr);
		file.add(export);
		file.addSeparator();
		file.add(loadwav);
		file.add(savewav);
		file.addSeparator();
		file.add(quit);

		open.setAccelerator(KeyStroke.getKeyStroke('O', modifier));
		loadwav.setAccelerator(KeyStroke.getKeyStroke('O', modifier | InputEvent.SHIFT_MASK));
		savejtr.setAccelerator(KeyStroke.getKeyStroke('S', modifier | InputEvent.SHIFT_MASK));
		export.setAccelerator(KeyStroke.getKeyStroke('E', modifier));
		quit.setAccelerator(KeyStroke.getKeyStroke('Q', modifier));

		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fc = new NicerFileChooser();
				fc.setDialogTitle("Open project, markup or text...");

				fc.addChoosableFileFilter(filterJTR);
				fc.addChoosableFileFilter(filterTRS);
				fc.addChoosableFileFilter(filterTextGrid);
				fc.addChoosableFileFilter(filterTXT);
				fc.setAcceptAllFileFilterUsed(false);
				fc.setFileFilter(filterJTR);

				int returnVal = fc.showOpenDialog(aligneur.jf);
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;
				FileFilter ff = fc.getFileFilter();
				File file = fc.getSelectedFile();

				if (ff == filterJTR) {
					aligneur.friendlyLoadProject(file);
				} else if (ff == filterTRS) {
					aligneur.friendlyLoadMarkup(new TRSLoader(), file);
				} else if (ff == filterTextGrid) {
					aligneur.friendlyLoadMarkup(new TextGridLoader(), file);
				} else if (ff == filterTXT) {
					aligneur.friendlyLoadMarkup(new RawTextLoader(), file);
				} else {
					JOptionPane.showMessageDialog(aligneur.jf, "Unknown filter " + ff);
				}
			}
		});

		savejtr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				NicerFileChooser fc = new NicerFileChooser();

				fc.setDialogTitle("Export alignment...");
				fc.addChoosableFileFilter(filterJTR);
				fc.setAcceptAllFileFilterUsed(false);
				fc.setFileFilter(filterJTR);

				int rc = fc.showSaveDialog(aligneur.jf);

				if (rc != JFileChooser.APPROVE_OPTION)
					return;

				FileFilter ff = fc.getFileFilter();
				File file = fc.getSelectedFile();

				try {
					aligneur.project.saveJson(file);
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(aligneur.jf,
							"Couldn't save. An I/O error occured.\n\n" + ex,
							"IOException",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		export.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(aligneur.jf,
						"By exporting the alignment to a foreign format,\n" +
						"some JTrans-specific information may be lost.\n\n" +
						"To keep this information intact, save your work\n" +
						"as a JTrans project instead.",
						"Exporting to a foreign format",
						JOptionPane.INFORMATION_MESSAGE);

				NicerFileChooser fc = new NicerFileChooser();

				fc.setDialogTitle("Export alignment...");
				fc.addChoosableFileFilter(filterTextGridWordsOnly);
				fc.addChoosableFileFilter(filterTextGridWordsAndPhons);
				fc.addChoosableFileFilter(filterTXT);
				fc.setAcceptAllFileFilterUsed(false);
				fc.setFileFilter(filterTextGridWordsOnly);

				int rc = fc.showSaveDialog(aligneur.jf);

				if (rc != JFileChooser.APPROVE_OPTION)
					return;

				FileFilter ff = fc.getFileFilter();
				File file = fc.getSelectedFile();

				try {
					if (ff == filterTXT) {
						aligneur.project.saveRawText(file);
					} else if (ff == filterTextGridWordsOnly) {
						aligneur.project.savePraat(file, true, false);
					} else if (ff == filterTextGridWordsAndPhons) {
						aligneur.project.savePraat(file, true, true);
					} else {
						JOptionPane.showMessageDialog(aligneur.jf, "Unknown filter " + ff);
					}
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(aligneur.jf,
							"Couldn't export. An I/O error occured.\n\n" + ex,
							"IOException",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		// //////////////////////////////////////////////////////////////
		JMenu actionsm = new JMenu("Edit");
		JMenuItem regexp = new JMenuItem("Regexps");
		JMenuItem gototime = new JMenuItem("Go to time [sec]");
		menubar.add(actionsm);
		actionsm.add(regexp);
		actionsm.add(gototime);

		// //////////////////////////////////////////////////////////////
		JMenu viewMenu = new JMenu("View");
		JCheckBoxMenuItem phonemesInSpectro = new JCheckBoxMenuItem("Show phonemes in spectro");
		JCheckBoxMenuItem minutesInAnchors = new JCheckBoxMenuItem("Show minutes in anchors");
		JCheckBoxMenuItem linebreakBeforeAnchors = new JCheckBoxMenuItem("Linebreak before anchors");
		JMenu fontSize = new JMenu("Font size");
		JMenu fontFamily = new JMenu("Font family");
		menubar.add(viewMenu);

		viewMenu.add(phonemesInSpectro);
		viewMenu.add(minutesInAnchors);
		viewMenu.add(linebreakBeforeAnchors);
		viewMenu.addSeparator();
		viewMenu.add(fontSize);
		viewMenu.add(fontFamily);

		phonemesInSpectro.setSelected(aligneur.showPhones);
		minutesInAnchors.setSelected(Element_Ancre.showMinutes);
		linebreakBeforeAnchors.setSelected(Project.linebreakBeforeAnchors);

		phonemesInSpectro.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.showPhones = ((AbstractButton)e.getSource()).isSelected();
			}
		});

		minutesInAnchors.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Element_Ancre.showMinutes = ((AbstractButton)e.getSource()).isSelected();
				aligneur.edit.setTextFromElements();
			}
		});

		linebreakBeforeAnchors.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Project.linebreakBeforeAnchors =  ((AbstractButton)e.getSource()).isSelected();
				aligneur.edit.setTextFromElements();
			}
		});


		ButtonGroup fontSizeGroup = new ButtonGroup();
		for (final int points: FONT_SIZES) {
			final JRadioButtonMenuItem jmi = new JRadioButtonMenuItem(points + " pt");
			fontSize.add(jmi);
			fontSizeGroup.add(jmi);

			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Font currentFont = aligneur.edit.getFont();
					aligneur.edit.setFont(new Font(currentFont.getName(), Font.PLAIN, points));
				}
			});

			if (points == TexteEditor.DEFAULT_FONT_SIZE)
				jmi.setSelected(true);
		}

		ButtonGroup fontFamilyGroup = new ButtonGroup();
		for (final String name: FONT_FAMILIES) {
			final JRadioButtonMenuItem jmi = new JRadioButtonMenuItem(name);
			fontFamily.add(jmi);
			fontFamilyGroup.add(jmi);

			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Font currentFont = aligneur.edit.getFont();
					aligneur.edit.setFont(new Font(name, Font.PLAIN, currentFont.getSize()));
				}
			});

			if (name.equals(TexteEditor.DEFAULT_FONT_NAME))
				jmi.setSelected(true);
		}


		// //////////////////////////////////////////////////////////////
		JMenu alignMenu = new JMenu("Align");
		JMenuItem autoAnchors = new JMenuItem("Auto-align between anchors...");
		JMenuItem autoAll = new JMenuItem("Auto-align all (no anchors)...");
		JMenuItem clearAll = new JMenuItem("Clear entire alignment");
		JMenuItem clearFrom  = new JMenuItem("Clear alignment from selected word");
		menubar.add(alignMenu);
		alignMenu.add(autoAnchors);
		alignMenu.add(autoAll);
		alignMenu.addSeparator();
		alignMenu.add(clearAll);
		alignMenu.add(clearFrom);

		autoAnchors.setAccelerator(KeyStroke.getKeyStroke('A', modifier | InputEvent.SHIFT_MASK));

		autoAnchors.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.alignBetweenAnchorsWithProgress();
			}
		});

		autoAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.alignAllWithProgress();
			}
		});

		clearAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.clearAlign();
				aligneur.repaint();
			}
		});

		clearFrom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.clearAlignFrom(aligneur.wordSelectedIdx);
			}
		});


		// //////////////////////////////////////////////////////////////
		JMenu sig = new JMenu("Process");
		JMenuItem bias = new JMenuItem("Bias adapt");
		JMenuItem map = new JMenuItem("MAP adapt");
		JMenuItem mapload = new JMenuItem("Load adapted models");
		JMenuItem asr= new JMenuItem("Automatic Speech Recognition");
		JMenuItem asrJSAPI= new JMenuItem("JSAPI Speech Recognition");
		JMenuItem beam= new JMenuItem("Set beam");
		JMenuItem playfrom = new JMenuItem("Play from");
		menubar.add(sig);
		sig.add(bias);
		sig.add(map);
		sig.add(mapload);
		sig.add(asr);
		sig.add(asrJSAPI);
		sig.add(beam);
		sig.addSeparator();
		sig.add(playfrom);

		bias.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.biasAdapt();
			}
		});

		map.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				BiaisAdapt a = new BiaisAdapt(aligneur);
				a.adaptMAP(aligneur.project.phons);
			}
		});

		mapload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				BiaisAdapt a = new BiaisAdapt(aligneur);
				a.loadAdapted(null);
			}
		});

		beam.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String s = JOptionPane.showInputDialog("Beam value (0=no beam)");
				if (s==null) return;
				s=s.trim();
				S4ForceAlignBlocViterbi.beamwidth=Integer.parseInt(s);
			}
		});

		playfrom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String pos = JOptionPane.showInputDialog(
						aligneur.jf,
						"Start playing from second:",
						"0.0");
				if (pos == null)
					return;
				float nsec = Float.parseFloat(pos);
				aligneur.startPlayingFrom(nsec);
			}
		});

		// //////////////////////////////////////////////////////////////
		JMenu prefs = new JMenu("Options");
		JMenuItem mixers = new JMenuItem("Audio mixers");
		JMenuItem mikerec = new JMenuItem("Record from mic");
		JMenuItem liveasr = new JMenuItem("Live ASR");
		JMenuItem initGrammar = new JMenuItem("Initialize grammar...");
		menubar.add(prefs);
		//		JMenuItem mots = new JMenuItem("forward mots");
		//		prefs.add(mots);
		prefs.add(mixers);
		prefs.add(mikerec);
		prefs.add(liveasr);
		prefs.addSeparator();
		prefs.add(initGrammar);

		liveasr.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// asynchrone
				LiveSpeechReco.doReco();
				// TODO: wait for the user press the ESC key, then stop the reco and put the result in the text panel 
			}
		});

		initGrammar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ProgressDialog waiting = new ProgressDialog((JFrame) null, new Runnable() {
					@Override
					public void run() {
						Grammatiseur.getGrammatiseur();
					}
				}, "please wait: initializing grammars...");
				waiting.setVisible(true);
			}
		}
		);

		// //////////////////////////////////////////////////////////////
		JMenu help = new JMenu("Help");
		menubar.add(help);
		JMenuItem tuto = new JMenuItem("Tutorial");
		help.add(tuto);
		tuto.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				try {
					JEditorPane pane = new JEditorPane((new File("tutorial.html")).toURI().toURL());
					pane.setEditable(false);
					pane.setSize(800, 800);
					JFrame helpframe = new JFrame("Help JTrans");
					JScrollPane hh = new JScrollPane(pane);
					helpframe.getContentPane().add(hh);
					helpframe.setSize(600, 800);
					helpframe.setVisible(true);
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				//				HTMLEditorKit helpwin = new HTMLEditorKit();
				//				helpwin.in
				//				View v = helpwin.getViewFactory().create(helpwin.createDefaultDocument().getDefaultRootElement());
			}
		});

		// //////////////////////////////////////////////////////////////
		// //////////////////////////////////////////////////////////////


		mixers.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Mixer.Info[] mix = AudioSystem.getMixerInfo();
				for (int i=0;i<mix.length;i++)
					System.out.println("mixer "+i+" "+mix[i]);
				JMenu mixmen = new JMenu("mixers");
				menubar.add(mixmen);
				JMenuItem[] mi = new JMenuItem[mix.length];
				for (int i=0;i<mix.length;i++) {
					final int j = i;
					mi[i] = new JMenuItem(mix[i].toString());
					mixmen.add(mi[i]);
					mi[i].addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							aligneur.mixidx=j;
						}
					});
				}
				menubar.validate();
				menubar.repaint();
			}
		});
		mikerec.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int mixidx = aligneur.mixidx;
				Mike2wav mike2sav = Mike2wav.getMike2wav(mixidx);
				mike2sav.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						String wavnom = e.getActionCommand().split(" ")[1];
						System.out.println("wavsource for mike : "+wavnom);
						aligneur.setAudioSource(wavnom);
						aligneur.setCurPosInSec(0);
						aligneur.repaint();
					}
				});
			}
		});

		regexp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new plugins.text.regexp.graphique.RegExpFrame(aligneur.project);
			}
		});

		gototime.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.gototime();
			}
		});

		asr.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.asr();
			}
		});
		asrJSAPI.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					File vocfile = new File("res/voc.txt");
					Grammatiseur.fastLoading=true;
					Grammatiseur.grammatiseur=null;
					gram = new LiveSpeechReco();
					gram.wavfile = aligneur.convertedAudioFile.getAbsolutePath();
					gram.loadVoc(vocfile);
					gram.initGrammar();
					gram.gram=gram;
					gram.addResultListener(new RecoListener() {
						@Override
						public void recoFinie(Result finalres, String res) {
							System.out.println("reco fin "+Thread.currentThread().getName()+" "+res);
							reco=res;
							synchronized (done) {
								done[0]=true;
								done.notify();
							}
						}
						@Override
						public void recoEnCours(Result tmpres) {
							System.out.println("reco en cours"+tmpres);
						}
					});
					Thread recothread = new Thread(new Runnable() {
						@Override
						public void run() {
							gram.wavReco();
							// au cas ou la reco s'arrete sans terminer completement
							synchronized (done) {
								done[0]=true;
								done.notify();
							}
						}
					});
					recothread.start();
					for (;;) {
						synchronized (done) {
							System.out.println("thread waiting: "+Thread.currentThread().getName());
							done.wait();
							System.out.println("done waiting: "+Thread.currentThread().getName());
							if (done[0]) break;
						}
					}
					String[] ss = reco.split("\n");
					StringBuilder res = new StringBuilder();
					for (int i=0;i<ss.length;i++) {
						String[] x = ss[i].split(":");
						if (x.length==3 && !x[2].equals("SIL")) {
							res.append(x[2]+" ");
						}
					}
					aligneur.edit.setText(res.toString());
					aligneur.edit.repaint();
				} catch (Exception ee) {
					ee.printStackTrace();
				}
			}
		});

		aligneur.playerController = new PlayerListener(aligneur, 100);


		quit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.quit();
			}
		});
		loadwav.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser filechooser = new NicerFileChooser();
				filechooser.setDialogTitle("Load .WAV...");
				int returnVal = filechooser.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = filechooser.getSelectedFile();
					if (file.exists()) {
						aligneur.setAudioSource(file.getAbsolutePath());
						aligneur.setCurPosInSec(0);
						return;
					}
				}
				System.out.println("load wav pb");
			}
		});
		savewav.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser filechooser = new NicerFileChooser();
				filechooser.setDialogTitle("Save .WAV...");
				filechooser.setSelectedFile(new File("out.wav"));
				int returnVal = filechooser.showSaveDialog(aligneur.jf);
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = filechooser.getSelectedFile();
				try {
					aligneur.saveWave(file);
				} catch (IOException ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(aligneur.jf,
							"I/O error when saving WAVE file",
							"Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		return menubar;
	}
}
