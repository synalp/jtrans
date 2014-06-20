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
import static javax.swing.KeyStroke.getKeyStroke;

import edu.cmu.sphinx.result.Result;

import fr.loria.synalp.jtrans.project.Anchor;
import fr.loria.synalp.jtrans.gui.table.ProjectTable;
import fr.loria.synalp.jtrans.markup.in.MarkupLoader;
import fr.loria.synalp.jtrans.markup.out.JTRSaver;
import fr.loria.synalp.jtrans.markup.out.MarkupSaver;
import fr.loria.synalp.jtrans.markup.out.MarkupSaverPool;
import fr.loria.synalp.jtrans.project.TurnProject;
import fr.loria.synalp.jtrans.speechreco.LiveSpeechReco;

import fr.loria.synalp.jtrans.speechreco.RecoListener;
import fr.loria.synalp.jtrans.speechreco.BiaisAdapt;
import fr.loria.synalp.jtrans.speechreco.s4.S4ForceAlignBlocViterbi;
import fr.loria.synalp.jtrans.speechreco.grammaire.Grammatiseur;
import fr.loria.synalp.jtrans.utils.NicerFileChooser;
import fr.loria.synalp.jtrans.utils.Paths;

public class Menus {
	JTransGUI aligneur;

	String reco;
	boolean[] done = {false};
	LiveSpeechReco gram;
	private Font currentFont = ProjectTable.DEFAULT_FONT;

	private static final int[] FONT_SIZES = {
			10, 11, 12, 13, 14, 16, 18, 20, 24, 28, 36
	};

	private static final String[] FONT_FAMILIES = {
			Font.SANS_SERIF,
			Font.SERIF,
			Font.MONOSPACED
	};


	private class SaverFF extends FileFilter {
		final String formatName;
		private String description;

		SaverFF(String formatName) {
			this.formatName = formatName;
			description = MarkupSaverPool.getInstance().getDescription(formatName);
		}

		public boolean accept(File f) {
			return true;
		}

		public String getDescription() {
			return description;
		}
	}


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

		open.setAccelerator(getKeyStroke('O', modifier));
		loadwav.setAccelerator(getKeyStroke('O', modifier | InputEvent.SHIFT_MASK));
		savejtr.setAccelerator(getKeyStroke('S', modifier | InputEvent.SHIFT_MASK));
		export.setAccelerator(getKeyStroke('E', modifier));
		quit.setAccelerator(getKeyStroke('Q', modifier));

		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fc = new NicerFileChooser("open");
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
				NicerFileChooser fc = new NicerFileChooser("save");

				fc.setDialogTitle("Export alignment...");

				int rc = fc.showSaveDialog(aligneur.jf);
				if (rc != JFileChooser.APPROVE_OPTION)
					return;

				File file = fc.getSelectedFile();

				try {
					new JTRSaver().save(aligneur.project, file);
				} catch (IOException ex) {
					aligneur.errorMessage("Couldn't save!", ex);
				}
			}
		});

		export.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MarkupSaverPool pool = MarkupSaverPool.getInstance();

				JOptionPane.showMessageDialog(aligneur.jf,
						"By exporting the alignment to a foreign format,\n" +
						"some JTrans-specific information may be lost.\n\n" +
						"To keep this information intact, save your work\n" +
						"as a JTrans project instead.",
						"Exporting to a foreign format",
						JOptionPane.INFORMATION_MESSAGE);

				NicerFileChooser fc = new NicerFileChooser("export");

				fc.setDialogTitle("Export alignment...");
				for (String str: pool.getNames()) {
					fc.addChoosableFileFilter(new SaverFF(str));
				}
				fc.setAcceptAllFileFilterUsed(false);
				fc.setFileFilter(fc.getChoosableFileFilters()[0]);

				int rc = fc.showSaveDialog(aligneur.jf);

				if (rc != JFileChooser.APPROVE_OPTION)
					return;

				SaverFF ff = (SaverFF)fc.getFileFilter();
				File file = fc.getSelectedFile();
				System.out.println(ff);
				System.out.println(ff.formatName);
				try {
					MarkupSaver saver = pool.make(ff.formatName);
					saver.save(aligneur.project, file);
				} catch (Exception ex) {
					aligneur.errorMessage("Couldn't export!", ex);
				}
			}
		});

		// //////////////////////////////////////////////////////////////
		JMenu actionsm = new JMenu("Edit");
		JMenuItem gototime = new JMenuItem("Go to time [sec]");
		JMenuItem findWord = new JMenuItem("Find word...");
		JMenuItem findNext = new JMenuItem("Find next");
		JMenuItem findPrev = new JMenuItem("Find previous");
		JMenuItem findNextAnon = new JMenuItem("Find next anonymous word");
		JMenuItem findPrevAnon = new JMenuItem("Find previous anonymous word");
		menubar.add(actionsm);
		actionsm.add(gototime);
		actionsm.addSeparator();
		actionsm.add(findWord);
		actionsm.add(findNext);
		actionsm.add(findPrev);
		actionsm.addSeparator();
		actionsm.add(findNextAnon);
		actionsm.add(findPrevAnon);

		findWord.setAccelerator(getKeyStroke('F', modifier));
		findNext.setAccelerator(getKeyStroke("F3"));
		findPrev.setAccelerator(getKeyStroke("shift F3"));
		findNextAnon.setAccelerator(getKeyStroke("alt F3"));
		findPrevAnon.setAccelerator(getKeyStroke("shift alt F3"));

		findWord.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.contentWordFinder.prompt();
			}
		});

		findNext.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.contentWordFinder.next();
			}
		});

		findPrev.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.contentWordFinder.previous();
			}
		});

		findNextAnon.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.anonWordFinder.next();
			}
		});

		findPrevAnon.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.anonWordFinder.previous();
			}
		});

		// //////////////////////////////////////////////////////////////
		JMenu viewMenu = new JMenu("View");
		JCheckBoxMenuItem minutesInAnchors = new JCheckBoxMenuItem("Show minutes in anchor timestamps");
		JMenu fontSize = new JMenu("Font size");
		JMenu fontFamily = new JMenu("Font family");
		menubar.add(viewMenu);

		viewMenu.add(minutesInAnchors);
		viewMenu.addSeparator();
		viewMenu.add(fontSize);
		viewMenu.add(fontFamily);

		minutesInAnchors.setSelected(Anchor.showMinutes);

		minutesInAnchors.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Anchor.showMinutes = ((AbstractButton)e.getSource()).isSelected();
				aligneur.table.repaint();
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
					aligneur.table.setViewFont(currentFont);
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
					aligneur.table.setViewFont(currentFont);
				}
			});

			if (name.equals(currentFont.getName()))
				jmi.setSelected(true);
		}


		// //////////////////////////////////////////////////////////////
		JMenu alignMenu = new JMenu("Align");
		final JMenuItem align = new JMenuItem("Align...");
		JMenuItem clearAlignment = new JMenuItem("Clear alignment");
		JMenuItem clearAnchors = new JMenuItem("Clear anchors...");
		JMenuItem inferAnchors = new JMenuItem("Infer anchor timing from alignment...");
		menubar.add(alignMenu);

		alignMenu.add(align);
		alignMenu.add(clearAlignment);
		alignMenu.addSeparator();
		alignMenu.add(clearAnchors);
		alignMenu.add(inferAnchors);

		align.setAccelerator(getKeyStroke('A', modifier | InputEvent.SHIFT_MASK));

		align.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.alignAll();
			}
		});

		clearAlignment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.project.clearAlignment();
				aligneur.setProject(aligneur.project); // force refresh
			}
		});

		clearAnchors.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!(aligneur.project instanceof TurnProject)) {
					aligneur.errorMessage(
							"Anchor timing may only be cleared\n" +
									"in turn-based projects.\n\n" +
									"Track-based projects are rendered useless\n" +
									"once the anchors have been cleared.",
							null
					);
					return;
				}

				int rc = JOptionPane.showConfirmDialog(aligneur.jf,
						"This is a lossy operation." +
								"\nManual anchor times will be lost." +
								"\nProceed anyway?",
						"Align without anchor times",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE);

				if (rc != JOptionPane.OK_OPTION) {
					return;
				}

				((TurnProject)aligneur.project).clearAnchorTimes();
				aligneur.setProject(aligneur.project); // force refresh
			}
		});

		inferAnchors.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!(aligneur.project instanceof TurnProject)) {
					aligneur.errorMessage(
							"Anchor timing may only be inferred\n" +
									"in turn-based projects.",
							null);
					return;
				}

				int inferred = ((TurnProject)aligneur.project).inferAnchors();

				String message = "Timing information inferred for "
						+ inferred + " anchor(s).";

				if (inferred == 0) {
					message = "Couldn't infer timing information.\n" +
							"Please align the file once before " +
							"inferring anchor times.";
				}

				JOptionPane.showMessageDialog(
						aligneur.jf,
						message,
						"Infer anchor timing",
						JOptionPane.INFORMATION_MESSAGE);

				aligneur.setProject(aligneur.project); // force refresh
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
						aligneur.setAudioSource(new File(wavnom));
						aligneur.setCurPosInSec(0);
						aligneur.repaint();
					}
				});
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
					File vocfile = new File(Paths.RES_DIR, "voc.txt");
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
				JFileChooser filechooser = new NicerFileChooser("load WAV");
				filechooser.setDialogTitle("Load .WAV...");
				int returnVal = filechooser.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = filechooser.getSelectedFile();
					if (file.exists()) {
						aligneur.setAudioSource(file);
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
