/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

package fr.loria.synalp.jtrans.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.cmu.sphinx.result.Result;

import fr.loria.synalp.jtrans.elements.Anchor;
import fr.loria.synalp.jtrans.facade.AutoAligner;
import fr.loria.synalp.jtrans.gui.trackview.MultiTrackTable;
import fr.loria.synalp.jtrans.markup.MarkupLoader;
import fr.loria.synalp.jtrans.speechreco.LiveSpeechReco;

import fr.loria.synalp.jtrans.speechreco.RecoListener;
import fr.loria.synalp.jtrans.speechreco.BiaisAdapt;
import fr.loria.synalp.jtrans.speechreco.s4.S4ForceAlignBlocViterbi;
import fr.loria.synalp.jtrans.speechreco.grammaire.Grammatiseur;
import fr.loria.synalp.jtrans.utils.NicerFileChooser;

public class Menus {
	JTransGUI aligneur;

	String reco;
	boolean[] done = {false};
	LiveSpeechReco gram;
	private Font currentFont = MultiTrackTable.DEFAULT_FONT;

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

	public Menus(JTransGUI main) {
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
		JMenuItem quit    = new JMenuItem("Quit");

		menubar.add(file);
		file.add(open);
		file.add(loadwav);
		file.addSeparator();
		file.add(savejtr);
		file.add(export);
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

				int returnVal = fc.showOpenDialog(aligneur.jf);
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				LoaderChooser loaderChooser = new LoaderChooser(file);
				loaderChooser.setLocationRelativeTo(aligneur.jf);
				loaderChooser.setVisible(true);
				MarkupLoader loader = loaderChooser.getMarkupLoader();

				if (loader == null)
					return;

				aligneur.friendlyLoadMarkup(loader, file, null);
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
						JTransGUI.REIMPLEMENT_DEC2013(); /* TODO PARALLEL TRACKS
						aligneur.project.saveRawText(file);
						*/
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
		JCheckBoxMenuItem minutesInAnchors = new JCheckBoxMenuItem("Show minutes in anchor timestamps");
		JMenu fontSize = new JMenu("Font size");
		JMenu fontFamily = new JMenu("Font family");
		menubar.add(viewMenu);

		viewMenu.add(phonemesInSpectro);
		viewMenu.add(minutesInAnchors);
		viewMenu.addSeparator();
		viewMenu.add(fontSize);
		viewMenu.add(fontFamily);

		phonemesInSpectro.setSelected(aligneur.showPhones);
		minutesInAnchors.setSelected(Anchor.showMinutes);

		phonemesInSpectro.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.showPhones = ((AbstractButton)e.getSource()).isSelected();
				aligneur.sigpan.refreshWords();
			}
		});

		minutesInAnchors.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Anchor.showMinutes = ((AbstractButton)e.getSource()).isSelected();
				aligneur.multitrack.repaint();
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
					currentFont = new Font(currentFont.getName(), Font.PLAIN, points);
					aligneur.multitrack.setViewFont(currentFont);
				}
			});

			if (points == currentFont.getSize())
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
					currentFont = new Font(name, Font.PLAIN, currentFont.getSize());
					aligneur.multitrack.setViewFont(currentFont);
				}
			});

			if (name.equals(currentFont.getName()))
				jmi.setSelected(true);
		}


		// //////////////////////////////////////////////////////////////
		JMenu alignMenu = new JMenu("Align");
		JMenuItem autoAnchors = new JMenuItem("Auto-align between anchors...");
		JMenuItem autoAll = new JMenuItem("Auto-align all (no anchors)...");
		JMenuItem clearAll = new JMenuItem("Clear entire alignment");
		menubar.add(alignMenu);
		alignMenu.add(autoAnchors);
		alignMenu.add(autoAll);
		alignMenu.addSeparator();
		alignMenu.add(clearAll);

		autoAnchors.setAccelerator(KeyStroke.getKeyStroke('A', modifier | InputEvent.SHIFT_MASK));

		autoAnchors.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.alignAllWithProgress(true);
			}
		});

		autoAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.alignAllWithProgress(false);
			}
		});

		clearAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.clearAlign();
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
				JTransGUI.REIMPLEMENT_DEC2013(); /* TODO PARALLEL TRACKS
				BiaisAdapt a = new BiaisAdapt(aligneur);
				a.adaptMAP(aligneur.project.phons);
				*/
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

		JMenu algoMenu = new JMenu("Alignment algorithm");
		ButtonGroup algoGroup = new ButtonGroup();
		for (final AutoAligner.Algorithm algo : AutoAligner.Algorithm.values()) {
			JRadioButtonMenuItem jmi = new JRadioButtonMenuItem(algo.name());
			algoGroup.add(jmi);
			algoMenu.add(jmi);
			jmi.setSelected(algo == AutoAligner.algorithm);

			jmi.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					AutoAligner.algorithm = algo;
				}
			});
		}

		menubar.add(prefs);
		//		JMenuItem mots = new JMenuItem("forward mots");
		//		prefs.add(mots);
		prefs.add(mixers);
		prefs.add(mikerec);
		prefs.add(liveasr);
		prefs.addSeparator();
		prefs.add(algoMenu);
		prefs.addSeparator();
		prefs.add(initGrammar);

		liveasr.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// asynchrone
				LiveSpeechReco.doReco(aligneur);
				// TODO: wait for the user press the ESC key, then stop the reco and put the result in the text panel 
			}
		});

		initGrammar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.setIndeterminateProgress("Initializing grammar...");

				new Thread() {
					public void run() {
						Grammatiseur.getGrammatiseur();
						aligneur.setProgressDone();
					}
				}.start();
			}});


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
				Mic2Wav mic2wav = new Mic2Wav(mixidx);
				mic2wav.addActionListener(new ActionListener() {
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
				new RegExpFrame(aligneur.project);
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
					gram.wavfile = aligneur.project.convertedAudioFile.getAbsolutePath();
					gram.loadVoc(vocfile);
					gram.initGrammar(aligneur);
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
					JTransGUI.REIMPLEMENT_DEC2013(); /* TODO PARALLEL TRACKS
					aligneur.edit.setText(res.toString());
					aligneur.edit.repaint();
					*/
				} catch (Exception ee) {
					ee.printStackTrace();
				}
			}
		});

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

		return menubar;
	}
}
