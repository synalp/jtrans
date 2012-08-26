/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.applis.SimpleAligneur;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import main.JTrans;
import main.LiveSpeechReco;

import plugins.sourceSignals.Mike2wav;
import plugins.speechreco.adaptation.BiaisAdapt;
import plugins.speechreco.aligners.sphiinx4.S4ForceAlignBlocViterbi;
import plugins.utils.TextInputWindow;
import plugins.utils.UserInputProcessor;

public class Menus {
	Aligneur aligneur;
	public Menus(Aligneur main) {
		aligneur=main;
	}
	public JMenuBar menus() {
		final JMenuBar menubar = new JMenuBar();

		// //////////////////////////////////////////////////////////////
		JMenu file = new JMenu("file");
		menubar.add(file);
		JMenuItem loadwav = new JMenuItem("load wav");
		file.add(loadwav);
		JMenuItem loadtxt = new JMenuItem("load txt");
		file.add(loadtxt);
		JMenuItem savewav = new JMenuItem("save wav as...");
		file.add(savewav);
		JMenuItem savetxt = new JMenuItem("save text as...");
		file.add(savetxt);
		savetxt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser filechooser = new JFileChooser(new File("."));
				filechooser.validate();
				filechooser.setApproveButtonText("Save");
				int returnVal = filechooser.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = filechooser.getSelectedFile();
					aligneur.savetxt(file);
				}
			}
		});

		JMenuItem save = new JMenuItem("save project");
		file.add(save);
		JMenuItem loadp = new JMenuItem("load project");
		file.add(loadp);
//		JMenuItem loadtrs = new JMenuItem("load ref TRS");
//		file.add(loadtrs);
//		JMenuItem loadstm = new JMenuItem("load STM");
//		file.add(loadstm);
//		JMenuItem savetrs = new JMenuItem("save TRS");
//		file.add(savetrs);
		JMenuItem savepho = new JMenuItem("save .pho");
		savepho.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
//				aligneur.alignement.savePho("align.pho");
				System.out.println("saving: "+(new File("align.pho")).getAbsolutePath());
			}
		});
		file.add(savepho);
		JMenuItem savepraat = new JMenuItem("save for Praat");
		savepraat.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (aligneur.showPhones) {
					JTrans.savePraat("out.textGrid", aligneur.alignementPhones);
					System.out.println("saving phones: "+(new File("out.textGrid")).getAbsolutePath());
				} else {
					JTrans.savePraat("out.textGrid", aligneur.alignement);
					System.out.println("saving words: "+(new File("out.textGrid")).getAbsolutePath());
				}
			}
		});
		file.add(savepraat);
		JMenuItem quit = new JMenuItem("quit");
		file.add(quit);

		// //////////////////////////////////////////////////////////////
		JMenu actionsm = new JMenu("edit");
		menubar.add(actionsm);
		JMenuItem parsestd = new JMenuItem("parse text standard");
		actionsm.add(parsestd);
		JMenuItem parse = new JMenuItem("parse text regexp");
		actionsm.add(parse);
		JMenuItem editb = new JMenuItem("edit text");
		actionsm.add(editb);
		JMenuItem regexp = new JMenuItem("regexps");
		actionsm.add(regexp);
		JMenuItem gototime = new JMenuItem("goto time [sec]");
		actionsm.add(gototime);
		
		// //////////////////////////////////////////////////////////////
		JMenu sig = new JMenu("Process");
		menubar.add(sig);
		
		JMenuItem bias = new JMenuItem("biasAdapt");
		sig.add(bias);

		bias.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.biasAdapt();
			}
		});
		
		JMenuItem map = new JMenuItem("MAP adapt");
		sig.add(map);
		map.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				BiaisAdapt a = new BiaisAdapt(aligneur);
				a.adaptMAP(aligneur.alignementPhones);
			}
		});
		JMenuItem mapload = new JMenuItem("load Adapted models");
		sig.add(mapload);
		mapload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				BiaisAdapt a = new BiaisAdapt(aligneur);
				a.loadAdapted(null);
			}
		});

		JMenuItem clear = new JMenuItem("clear all align");
		sig.add(clear);
		JMenuItem clearfrom = new JMenuItem("clear align from selected word");
		sig.add(clearfrom);
//		JMenuItem batchalign= new JMenuItem("batch align between anchors");
//		actionsm.add(batchalign);
		JMenuItem asr= new JMenuItem("Automatic Speech Recognition");
		sig.add(asr);
		JMenuItem batch= new JMenuItem("batch align all");
		sig.add(batch);
		JMenuItem beam= new JMenuItem("set beam");
		sig.add(beam);
		beam.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String s = JOptionPane.showInputDialog("beam value (0=no beam)");
				if (s==null) return;
				s=s.trim();
				S4ForceAlignBlocViterbi.beamwidth=Integer.parseInt(s);
			}
		});
		
		sig.addSeparator();
		JMenuItem playfrom = new JMenuItem("play from");
		sig.add(playfrom);
		playfrom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TextInputWindow tt = new TextInputWindow("position en secondes:",new UserInputProcessor() {
					public void processInput(String ii) {
						float nsec = Float.parseFloat(ii);
						aligneur.startPlayingFrom(nsec);
					}
				});
			}
		});

		// //////////////////////////////////////////////////////////////
		JMenu prefs = new JMenu("options");
		menubar.add(prefs);
//		JMenuItem mots = new JMenuItem("forward mots");
//		prefs.add(mots);
		JMenuItem mixers = new JMenuItem("audio mixers");
		prefs.add(mixers);
		JMenuItem mikerec = new JMenuItem("record from mike");
		prefs.add(mikerec);
		JMenuItem liveasr = new JMenuItem("live ASR");
		prefs.add(liveasr);
		liveasr.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// asynchrone
				LiveSpeechReco.doReco();
				// TODO: wait for the user press the ESC key, then stop the reco and put the result in the text panel 
			}
		});
//		JMenuItem gui1 = new JMenuItem("GUI: view text only");
//		prefs.add(gui1);
//		JMenuItem gui2 = new JMenuItem("GUI: view signal");
//		prefs.add(gui2);
		JMenuItem gui3 = new JMenuItem("GUI: toggle words/phones");
		prefs.add(gui3);
		JMenuItem font = new JMenuItem("font size");
		prefs.add(font);

		// //////////////////////////////////////////////////////////////
		JMenu help = new JMenu("help");
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
//		gui1.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				aligneur.GUIfast();
//			}
//		});
//		gui2.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				aligneur.GUIslow();
//			}
//		});
		gui3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.toggleShowPhones();
//				TemporalSigPanel.showPhones=!TemporalSigPanel.showPhones;
//				aligneur.repaint();
			}
		});
		font.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final String[] fonts = {"10","12","16","18","20","24","30"};
				
				final JFrame fl = new JFrame("choose font size");
				final JList jl = new JList(fonts);
				jl.addListSelectionListener(new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						aligneur.edit.fontSize(Integer.parseInt((String)jl.getSelectedValue()));
						fl.dispose();
					}
				});
				fl.getContentPane().add(jl);
				fl.setSize(100, 100);
				fl.setVisible(true);
			}
		});

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
						aligneur.setWavSource(wavnom);
						aligneur.setCurPosInSec(0);
						aligneur.repaint();
					}
				});
			}
		});
		
//		mots.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				new TextInputWindow("enter nb of words for auto. align",new UserInputProcessor() {
//					public void processInput(String txt) {
//						aligneur.setNwordsForward(Integer.parseInt(txt));
//					}
//				});
//			}
//		});
//		
		regexp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new plugins.text.regexp.graphique.RegExpFrame(aligneur.edit);
			}
		});
		parse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.parse(false);
			}
		});
		parsestd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.parse(true);
			}
		});
		gototime.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.gototime();
			}
		});
		clear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.clearAlign();
				aligneur.repaint();
			}
		});
		clearfrom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.clearAlignFrom(aligneur.wordSelectedIdx);
			}
		});
		editb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.setEditionMode();
			}
		});

//		batchalign.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				aligneur.batchAlign();
//			}
//		});

		batch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.batch();
			}
		});

		asr.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aligneur.asr();
			}
		});
		
		aligneur.playerController = new PlayerListener(aligneur, 100);

		
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.saveProject();
			}
		});
		loadp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.loadProject();
			}
		});
//		loadstm.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				JFileChooser filechooser = new JFileChooser();
//				filechooser.validate();
//				filechooser.setApproveButtonText("Ouvrir");
//				int returnVal = filechooser.showOpenDialog(null);
//				if (returnVal == JFileChooser.APPROVE_OPTION) {
//					File file = filechooser.getSelectedFile();
//					if (file.exists()) {
////						aligneur.loadSTM reference(file);
//					}
//				}
//			}
//		});
//		savetrs.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				JFileChooser filechooser = new JFileChooser();
//				filechooser.validate();
//				filechooser.setApproveButtonText("Ouvrir");
//				int returnVal = filechooser.showOpenDialog(null);
//				if (returnVal == JFileChooser.APPROVE_OPTION) {
//					File file = filechooser.getSelectedFile();
//					// TODO
//				}
//			}
//		});
		quit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aligneur.quit();
			}
		});
		loadwav.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser filechooser = new JFileChooser(new File("."));
				filechooser.validate();
				filechooser.setApproveButtonText("Ouvrir");
				int returnVal = filechooser.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = filechooser.getSelectedFile();
					if (file.exists()) {
						aligneur.setWavSource(file.getAbsolutePath());
						System.out.println("wav set");
						aligneur.setCurPosInSec(0);
						return;
					}
				}
				System.out.println("load wav pb");
			}
		});
		savewav.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser filechooser = new JFileChooser(new File("."));
				filechooser.validate();
				filechooser.setApproveButtonText("Sauver");
				int returnVal = filechooser.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = filechooser.getSelectedFile();
					aligneur.savewav(file);
				}
			}
		});
		loadtxt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser filechooser = new JFileChooser(new File("."));
				filechooser.validate();
				filechooser.setApproveButtonText("Ouvrir");
				int returnVal = filechooser.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = filechooser.getSelectedFile();
					if (file.exists()) {
						aligneur.loadtxt(file);
					}
				}
			}
		});
		return menubar;
	}


}
