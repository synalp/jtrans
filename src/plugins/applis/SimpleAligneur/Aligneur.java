/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

package plugins.applis.SimpleAligneur;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import facade.JTransAPI;

import main.JTrans;
import main.SpeechReco;

//import plugins.blackboard.Blackboard;
//import plugins.blackboard.BlackboardListener;
import plugins.buffer.RoundBuffer;
import plugins.buffer.RoundBufferFrontEnd;
import plugins.signalViewers.spectroPanel.SpectroControl;
import plugins.signalViewers.temporalSigPanel.TemporalSigPanel;
import plugins.signalViewers.temporalSigPanel.ToolBarTemporalSig;
import plugins.sourceSignals.TemporalSigFromWavFile;
import plugins.speechreco.acousticModels.HMM.LogMath;
import plugins.speechreco.adaptation.BiaisAdapt;
import plugins.speechreco.aligners.Alignement;
import plugins.speechreco.aligners.ForceAlignBlocViterbi;
import plugins.speechreco.aligners.sphiinx4.AlignementEtat;
import plugins.speechreco.aligners.sphiinx4.AutoAligner;
import plugins.speechreco.aligners.sphiinx4.ControlBox;
import plugins.speechreco.aligners.sphiinx4.S4AlignOrder;
import plugins.speechreco.aligners.sphiinx4.S4ForceAlignBlocViterbi;
import plugins.speechreco.confidenceMeasure.AcousticCM;
import plugins.text.ColoriageEvent;
import plugins.text.GriserWhilePlaying;
import plugins.text.ListeElement;
import plugins.text.PonctParser;
import plugins.text.TexteEditor;
import plugins.text.elements.Element;
import plugins.text.elements.Element_Commentaire;
import plugins.text.elements.Element_Mot;
import plugins.text.elements.Element_Ponctuation;
import plugins.utils.FileUtils;
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
@SuppressWarnings("deprecation")
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
	private static boolean parseWithRegexp = false;

	private boolean withgui = true;
	private String sourceTxtfile = null;
	public String getSourceTxt() {return sourceTxtfile;}

	public int mixidx=0;
	public boolean useS4aligner = true;

	public AutoAligner autoAligner = null;
	public TexteEditor edit;
	//	public Player player;
	public AudioFormat audioFormat = null;
	public TemporalSigPanel sigPanel = null;
	public ToolBarTemporalSig toolbar = null;
	public KeysManager kmgr = null;
	PlayerListener playerController;
	JScrollPane scrollPane;
	JSplitPane splitPane = null;
	Box textBox, sigBox;
	public String wavname=null;
	public RoundBuffer audiobuf = new RoundBuffer(this, 10000000);
	public RoundBufferFrontEnd mfccbuf;
	TemporalSigFromWavFile wavForMFCC;
	ForceAlignBlocViterbi blocViterbi = null;

	// position lue pour la derniere fois dans le flux audio
	long currentSample = 0;
	int nFrames = -1;
	public AlignementEtat alignement = new AlignementEtat();
	public AlignementEtat alignementPhones = new AlignementEtat();
	int wordSelectedIdx = -1;
	public boolean caretSensible = false;
	int state = 0;
	JLabel infoLabel = new JLabel("                                      ");
	/**
	 * ces variables ne sont utilisees que avec un "User simulator": elles contiennent la reference
	 */
	String stmfile = null, trsfile = null;
	Alignement refAlign = null;
	/**
	 * mesure de confiance
	 */
	AcousticCM conf = new AcousticCM(this);
	public float cmthr = -Float.MAX_VALUE;

	/*
	 * pour l'alignement semi-automatique
	 */
	int wordBeforeBeginReco = 0, wordEndReco = 0;
	boolean userShouldConfirm = false;
	LogMath logmath = new LogMath();

	public float getCurPosInSec() {return cursec;}
	public void setCurPosInSec(float sec) {
		cursec=sec;
		int fr = SpectroControl.second2frame(cursec);
		int seg = alignement.getSegmentAtFrame(fr);
		// nouveau panel
		sigpan.setAudioInputStream(getCurPosInSec(), getAudioStreamFromSec(getCurPosInSec()));
		if (showPhones) {
			int segphidx = alignementPhones.getSegmentAtFrame(fr);
			sigpan.setAlign(alignementPhones);
			sigpan.setFirstSeg(segphidx);
		} else {
			sigpan.setFirstSeg(seg);
			sigpan.setAlign(alignement);
		}
		repaint();
		updateViewers();
	}

	public void print(String msg) {
		printInStatusBar(msg);
	}

	//	public void GUIfast() {
	//		sigPanel.removeAndDestroy();
	//		sigPanel = null;
	//		toolbar = null;
	//		splitPane = null;
	//		sigBox = null;
	//		getContentPane().removeAll();
	//		getContentPane().add(textBox);
	//		validate();
	//		repaint();
	//	}

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
	public void savetxt(File txtout) {
		List<Element_Mot> mots = edit.getListeElement().getMots();
		try {
			PrintWriter fout = FileUtils.writeFileUTF(txtout.getAbsolutePath());
			for (Element_Mot m : mots)
				fout.println(m);
			fout.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void savewav(File wavout) {
		File f = new File(wavname);
		boolean res = f.renameTo(wavout);
		System.out.println("renaming done "+res);
	}

	public void setWavSource(String url) {
		wavname=url;
	}
	
	public AudioInputStream getAudioStreamFromSec(float sec) {
		if (wavname==null) return null;
		try {
			AudioInputStream ais = FileUtils.findAudioFileOrUrl(wavname);
			if (ais==null) {
				JOptionPane.showMessageDialog(null, "ERROR no audiostream from "+wavname);
				return null;
			}
			AudioFormat format = ais.getFormat();
			float frPerSec = format.getFrameRate();
			int byPerFr = format.getFrameSize();
			float fr2skip = frPerSec*sec;
			long by2skip = (long)(fr2skip*(float)byPerFr);
			ais.skip(by2skip);
			return ais;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
    public void rewindMFCC() {
    wavForMFCC.rewind();
    MFCC mfcc = new MFCC(wavForMFCC);
    mfccbuf = new RoundBufferFrontEnd(10000, mfcc.getNcoefs());
    mfccbuf.setSource(mfcc);
    }
	 */
	public void loadtxt(File file) {
		try  {
			//			BufferedReader f=new BufferedReader(new FileReader(file));
			BufferedReader f = FileUtils.openFileUTF(file.getAbsolutePath());
			StringBuilder sb = new StringBuilder();
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				sb.append(s+"\n");
			}
			f.close();

			// check encoding
			if (sb.toString().indexOf('é')<0) {
				System.err.println("loadtxt: suspect ISO file, retry...");
				f = FileUtils.openFileISO(file.getAbsolutePath());
				sb = new StringBuilder();
				for (;;) {
					String s = f.readLine();
					if (s==null) break;
					sb.append(s+"\n");
				}
				f.close();
				if (sb.toString().indexOf('é')<0) {
					System.err.println("cannot decide on encoding... maybe ASCII ?");
				} else {
					plugins.text.TextSegments.isISO=true;
					System.err.println("... iso seems ok !");
				}
			}

			setText(sb.toString());
			sourceTxtfile=file.getAbsolutePath();
			System.out.println("loadtext source "+sourceTxtfile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void loadtxt(InputStream in) {
		try  {
			BufferedReader f=new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
			StringBuilder sb = new StringBuilder();
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				sb.append(s+"\n");
			}
			f.close();
			setText(sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void loadtxt(String url) {
		try  {
			BufferedReader f = FileUtils.openFileURL(url);
			StringBuilder sb = new StringBuilder();
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				sb.append(s+"\n");
			}
			f.close();

			// check encoding
			if (sb.toString().indexOf('é')<0) {
				System.err.println("loadtxt: suspect ISO file, retry...");
				f = FileUtils.openFileURL(url);
				sb = new StringBuilder();
				for (;;) {
					String s = f.readLine();
					if (s==null) break;
					sb.append(s+"\n");
				}
				f.close();
				if (sb.toString().indexOf('é')<0) {
					System.err.println("cannot decide on encoding... maybe ASCII ?");
				} else {
					plugins.text.TextSegments.isISO=true;
					System.err.println("... iso seems ok !");
				}
			}

			setText(sb.toString());
			sourceTxtfile=null;
			System.out.println("loadtext source "+sourceTxtfile);
		} catch (Exception e) {
			e.printStackTrace();
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

	public AudioInputStream getWavSourceFromStart() {
		AudioInputStream in = FileUtils.findAudioFileOrUrl(wavname);
		return in;
	}

	public void griseSelectedWord() {
		caretSensible = false;
		Element_Mot mot = edit.getListeElement().getWordElement(wordSelectedIdx);
		/*
        if (alignement.getAncres().contains(wordSelectedIdx)) {
            edit.souligne(mot);
        }
		 */
		edit.griseMot(mot);
		caretSensible = true;
	}

	private AutoAligner getAutoAligner() {
		// TODO: support for URL !!
		return AutoAligner.getAutoAligner(wavname,null,edit,alignement,alignementPhones);
	}

	public void batch() {
		AutoAligner.batch=!AutoAligner.batch;
		s4fastAutoAlign();
	}

	public void s4fastAutoAlign() {
		if (!useS4aligner) return;
		autoAligner = getAutoAligner();
	}

	public void souligneAncres() {
		/*
        for (int i : alignement.getAncres()) {
            edit.souligne(edit.getListeElement().getWordElement(i));
        }
		 */
	}
	public void colorizeAlignedWords() {
		/*
    	for (int i=0;i<alignement.getNbMotsAligned();i++) {
    		if (alignement.getWordFirstFrame(i)>=0) {
    			edit.inkInColor(edit.getListeElement().getMot(i),edit.getListeElement().getMot(i),AutoAligner.alignedColor);
    		}
    	}
		 */
	}
	private void saveCurrentTextInSourcefile() {
		try {
			sourceTxtfile="tmpsource.txt";
			System.out.println("save text in new source "+sourceTxtfile);
			PrintWriter f = FileUtils.writeFileUTF(sourceTxtfile);
			String s = edit.getText();
			f.println(s);
			f.close();
			System.out.println("source text saved in "+sourceTxtfile);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void colorieElements() {
		for (Element elt : edit.getListeElement()) {
			if (elt instanceof Element_Ponctuation) {
				Element_Ponctuation e = (Element_Ponctuation)elt;
				edit.colorHighlight((int)e.posdeb,(int)e.posfin, Color.orange);
			} else if (elt instanceof Element_Commentaire) {
				Element_Commentaire e = (Element_Commentaire)elt;
				edit.colorHighlight(e.posDebInTextPanel,e.posFinInTextPanel, Color.green);
			}
		}
		edit.repaint();
		System.out.println("fin colorie elements");
	}

	/**
	 * cree une liste d'elements a partir d'un texte brut
	 * @param thenew
	 */
	public void parse(final boolean thenew) {
		parseWithRegexp = !thenew;
		edit.setEditable(false);
		caretSensible = false;
		final Aligneur main = this;

		// pas de thread, car il faut attendre la fin dans le loadProject !
		
//		Thread t = new Thread(new Runnable() {
//			public void run() {
				if (thenew) {
					PonctParser parser = new PonctParser(main);
					if (thenew) {
						// version avec un texte immutable
						// verifie si un fichier-source texte existe
						// si oui, on suppose que toute modification manuelle du texte dans jtrans a déclenché l'enregistrement
						// d'un nouveau fichier-source texte; donc ce dernier est toujours a jour !
						// TODO: mettre a jour ce sourcetxtfile lorsqu'il y a une edition manuelle

						if( sourceTxtfile==null && withgui==false) {
							// cas d'une applet:
							JOptionPane.showMessageDialog(null, "WARNING: impossible to save a file with an applet !");
							return;
						}
						
						if (sourceTxtfile==null || !(new File(sourceTxtfile).exists())) {
							saveCurrentTextInSourcefile();
						}
						ListeElement elts = parser.parseimmutable(sourceTxtfile);
						edit.setListeElement(elts);
						colorieElements();
					} else {
						// version avec un texte mutable mais un parser indep du TextEditor
						parser.parse();
					}
				} else {
					// vieille version avec le parser du TexteEditor
/*
					List<Element_Mot> oldmots = edit.getListeElement().getMots();
					ArrayList<Integer> sav = new ArrayList<Integer>();
					for (int i=0;i<oldmots.size();i++)
						if (oldmots.get(i).posInAlign<0) break;
						else sav.add(oldmots.get(i).posInAlign);
					Integer[] savealign = sav.toArray(new Integer[sav.size()]);
					*/
					edit.reparse();
					/*
					int i=0;
					for (Element_Mot m : edit.getListeElement().getMots()) {
						if (i>=savealign.length) break;
						m.posInAlign=savealign[i++];
					}
					System.out.println("reparse: saved naligns "+i);
					*/
				}
				if (alignement!=null) {
					// matche les Element_Mot avec l'alignement existant
					int[] match = alignement.matchWithText(edit.getListeElement().getMotsInTab());
					edit.getListeElement().importAlign(match,0);
					if (edit!=null) edit.colorizeAlignedWords(0,getLastMotAligned());
					repaint();
				}
				caretSensible = true;
//			}
//		});
//		t.start();
	}

	void loadProject() {
		String nom = "jtransalign.txt";
		JFileChooser filechooser = new JFileChooser(new File("."));
		filechooser.validate();
		filechooser.setApproveButtonText("Ouvrir");
		int returnVal = filechooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = filechooser.getSelectedFile();
			if (file.exists()) {
				nom=file.getAbsolutePath();
			} else return;
		}
		loadProject(nom);
	}

	void saveProject() {
		JFileChooser filechooser = new JFileChooser(new File("."));
		filechooser.validate();
		filechooser.setApproveButtonText("Sauver");
		int returnVal = filechooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = filechooser.getSelectedFile();
			String regexp="";
			if (edit!=null) {
				regexp=edit.exportRegexpAsTxt();
			}
			if (sourceTxtfile==null) saveCurrentTextInSourcefile();
			saveProject(edit.getListeElement(),file.getAbsolutePath(),sourceTxtfile,wavname,alignement,alignementPhones,regexp);
		}
	}

	/**
	 * les regexp peuvent être vides: les regexp par defaut sont alors chargees dans jtrans
	 * 
	 * J'ai besoin d'une methode static car je sauve des projets depuis l'exterieur
	 * 
	 * @param outfile
	 * @param wavname
	 * @param alWords
	 * @param texte
	 * @param regexp
	 */
	public static void saveProject(ListeElement elts, String outfile, String txtfile, String wavname, AlignementEtat alWords, AlignementEtat alPhones, String regexp) {
		try {
			PrintWriter f = FileUtils.writeFileUTF(outfile);
			f.println("wavname= " + wavname);
			f.println("txtname= " + txtfile);
			if (parseWithRegexp) f.println("parse with regexp");
			else f.println("parse with new");
			if (elts!=null) elts.save(f);
			else f.println("listeelements 0");
			alWords.save(f);
			alPhones.save(f);
			f.println(regexp);
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void setEditionMode() {
		caretSensible=false;
		edit.setEditable(true);
		edit.getHighlighter().removeAllHighlights();
		System.out.println("seteditionmode");
		sourceTxtfile=null;
	}
	public void loadProject(String nom) {
		try {
			System.out.println("loading project "+nom);
			BufferedReader f = FileUtils.openFileOrURL(nom);
			String s = f.readLine();
			assert s.startsWith("wavname= ");
			wavname = s.substring(9);
			s = f.readLine();
			assert s.startsWith("txtname= ");
			sourceTxtfile = s.substring(9);
			s = f.readLine();
			assert s.startsWith("parse with ");
			parseWithRegexp = false;
			if (s.startsWith("parse with regexp")) parseWithRegexp = true;
			else if (s.startsWith("parse with new")) parseWithRegexp = false;
			else System.out.println("ERROR jtr format "+s);

			InputStream in = FileUtils.findFileOrUrl(sourceTxtfile);
			loadtxt(in);
//			in = FileUtils.findFileOrUrl(wavname);
			updateViewers();

			System.out.println("loadproject source "+sourceTxtfile);
			edit.getListeElement().clear();
			// on ecrase la tokenisation realisee dans loadtxt()
			// ici, on charge les elements et on les met en correspondance avec le textArea, MAIS PAS AVEC l'ALIGNEMENT !
			edit.getListeElement().load(f, edit);
			// ici, on charge les segments, mais ils ne sont toujours pas mis en correspondance avec les elements !
			alignement = AlignementEtat.load(f);
			JTransAPI.alignementWords=alignement;
			alignementPhones = AlignementEtat.load(f);
			JTransAPI.alignementPhones=alignementPhones;
			System.out.println("align loaded "+alignement.getNbSegments());
			// ici, on affiche les segments sur le spectro
			sigpan.setAlign(alignement);

			// regexps
			edit.parserRegexpFromBufferedReader(f);

			parse(!parseWithRegexp);
			f.close();
			System.out.println("text parsing done");

			//            goToLastAlignedWord();
			caretSensible = true;

			// force la construction de l'index
			alignement.clearIndex();
			alignement.getSegmentAtFrame(0);
			System.out.println("align index built");
			alignementPhones.clearIndex();
			alignementPhones.getSegmentAtFrame(0);
			edit.getListeElement().refreshIndex();
			System.out.println("project load finish");

			printInStatusBar("project loaded !");

			// souligne les ancres
			//          for (int widx : alignement.getAncres()) {
			//             edit.souligne(edit.getListeElement().getWordElement(widx));
			//        }
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<Element_Mot> mts = edit.getListeElement().getMots();
		System.out.println("end align "+alignement.getNbSegments()+" "+mts.size());
		String[] mots = new String[mts.size()];
		for (int i=0;i<mots.length;i++) {
			mots[i]=mts.get(i).getWordString().trim();
		}
		int[] mots2segidx = alignement.matchWithText(mots);
		System.err.println("mots2segidx "+mots.length+" "+mots2segidx.length);
		int lastMotAligned = edit.getListeElement().importAlign(mots2segidx,0);
		System.err.println("last mot aligned "+lastMotAligned+" "+edit);
		System.err.println("debug lastremot aligned "+getLastMotAligned());
		if (edit!=null) edit.colorizeAlignedWords(0,lastMotAligned-1);
	}

	private float lastSecClickedOnSpectro = 0;
	public void clicOnSpectro(float frf) {
		float prevsec = getCurPosInSec();
		float sec = Alignement.frame2second((int)frf);
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

	private void adjustPanel() {
		textBox.setPreferredSize(new Dimension(getWidth(),getHeight()*2/3));
		sigpan.setPreferredSize(new Dimension(getWidth(),getHeight()*1/3));
		splitPane.setDividerLocation(0.7);
		repaint();
	}

	public Aligneur(boolean withGUI) {
		withgui=withGUI;
		initPanel();
		if (withGUI) createJFrame();
	}

	public Aligneur() {
		JTransAPI.alignementWords=alignement;
		JTransAPI.alignementPhones=alignementPhones;
		JTransAPI.aligneur=this;
		withgui=true;
		initPanel();
		createJFrame();
	}

	public JMenuBar createMenus() {
		JMenuBar menubar = (new Menus(this)).menus();
		return menubar;
	}

	private void createJFrame() {
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
		jf.setSize(800, 700);
		jf.setVisible(true);
	}

	private void initPanel() {
		setSize(800,700);
		// partie texte
		edit = new TexteEditor();
		JTransAPI.edit=edit;
		scrollPane = new JScrollPane(edit,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		textBox = Box.createVerticalBox();
		ctrlbox = new ControlBox(this);
		playergui = ctrlbox.getPlayerGUI();
		textBox.add(ctrlbox);
		textBox.add(scrollPane);
		textBox.add(infoLabel);

		// partie signal
		sigpan = new SpectroControl(this);
		AudioInputStream aud = getAudioStreamFromSec(getCurPosInSec());
		if (aud!=null) sigpan.setAudioInputStream(getCurPosInSec(),aud);

		// empilage des deux
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, textBox, sigpan);
		final Dimension d0 = new Dimension(0, 0);
		textBox.setMinimumSize(d0);
		sigpan.setMinimumSize(d0);
		splitPane.setDividerLocation(500);

		add(splitPane);
		adjustPanel();
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
        int lastAlignedWord = alignement.getLastWordAligned();
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
		if (alignement!=null) {
			System.out.println("newplaystarted "+alignement.getNbSegments());
			griserwhenplaying = new GriserWhilePlaying(playergui, edit);
			griserwhenplaying.setAlignement(alignement);
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
				if (alignement != null) {
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
			//        } else if (alignement != null) {
			//            int lastAlignedWord = alignement.getLastWordAligned();
			//            if (wordSelectedIdx < 0 || alignement.getFrameFin(wordSelectedIdx) < 0) {
			//                if (lastAlignedWord < 0) {
			//                    currentSample = 0;
			//                } else {
			//                    currentSample = Alignement.frame2sample(alignement.getFrameFin(lastAlignedWord));
			//                }
			//            } else {
			//                currentSample = Alignement.frame2sample(alignement.getFrameFin(wordSelectedIdx));
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
		if (alignement != null) {
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
		currentSample = Alignement.second2sample(nsec);
		caretSensible = true;

		if (currentSample < 0) {
			currentSample = 0;
		}
		// positionne l'audio buffer
		audiobuf.samplePosition = currentSample;
		System.err.println("play from " + Alignement.sample2second(currentSample));
		// relance le thread qui grise les mots
		if (alignement != null) {
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
		alignement.clear();
		alignementPhones.clear();
	}
	void clearAlignFrom(int mot) {
		if (autoAligner!=null) autoAligner.stopAutoAlign();
		// cherche le prochain mot qui est aligné
		int seg4mot=-1;
		List<Element_Mot> mots = edit.getListeElement().getMots();
		for (;mot<mots.size();mot++) {
			seg4mot = mots.get(mot).posInAlign;
			if (seg4mot>=0&&!alignement.getSegmentLabel(seg4mot).equals("SIL")) break;
		}
		if (seg4mot<0) return; // rien n'est aligne
		// supprimer les alignements
		int fr = alignement.getSegmentDebFrame(seg4mot);
		alignement.cutAfterFrame(fr);
		alignementPhones.cutAfterFrame(fr);
		alignement.clearIndex();
		alignementPhones.clearIndex();
		
		// supprimer la correspondance mots/segments
		for (int i=mot;i<edit.getListeElement().getMots().size();i++) {
			edit.getListeElement().getMot(i).posInAlign=-1;
		}
		// decolorier les mots qui ne sont plus alignes:
		if (edit!=null) {
			edit.getHighlighter().removeAllHighlights();
			if (mot>0) edit.colorizeAlignedWords(0,mot-1);
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
				JTransAPI.setAlignWord(0, 0f, sec);
			} else {
				// ajouter un silence devant
				// TODO: est-ce que le prochain alignement auto, lorsqu'il verra qu'il n'y a aucun mot
				// de deja aligne, prendra en compte le premier silence qui est aligne ?
				
				JTransAPI.setSilenceSegment(0f, sec);
			}
			return;
		}
		// TODO: refaire la suite avec JTransAPI
		JTransAPI.setAlignWord(mot, -1, sec);
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
					if (deb>=0) alignement.setSegmentDebFrame(segmentDuMot, SpectroControl.second2frame(deb));
					if (end>=0) alignement.setSegmentEndFrame(segmentDuMot, SpectroControl.second2frame(end));
				} else {
					// mot non encore aligne
					if (mot>0) {
						// ce n'est pas le 1er mot: j'aligne auto les precedents jusqu'au debut
						// TODO
					} else {
						// premier mot
						alignement.clear();
						int curdebfr = 0;
						if (deb>0) {
							curdebfr = SpectroControl.second2frame(deb);
							if (curdebfr>0) {
								alignement.addRecognizedSegment("SIL", 0, curdebfr, null, null);
							}
						}
						if (end>=0) {
							int newseg = alignement.addRecognizedSegment(edit.getListeElement().getMot(0).getWordString(),
									curdebfr, SpectroControl.second2frame(end), null, null);
							edit.getListeElement().getMot(0).posInAlign=newseg;
						} else {
							// TODO
						}
						System.out.println("debug segs  \n"+alignement.toString());
					}
				}
				System.out.println("last aligned word "+getLastMotAligned());
				if (edit!=null) edit.colorizeAlignedWords(0,mot);
				repaint();
			} else { // juste un SHIFT-clic
				// position pour le play
				wordSelectedIdx=mot;
				Element_Mot emot = edit.getListeElement().getMot(mot);
				int segidx = emot.posInAlign;
				if (segidx>=0) {
					int frame = alignement.getSegmentDebFrame(segidx);
					cursec = Alignement.frame2second(frame);
					long currentSample = Alignement.frame2sample(frame);
					if (currentSample<0) currentSample=0;
					edit.griseMot(emot);
					// vieux panel
					if (sigPanel!=null) {
						sigPanel.setProgressBar(currentSample);
					}
					// nouveau panel
					sigpan.setAudioInputStream(getCurPosInSec(), getAudioStreamFromSec(getCurPosInSec()));
					if (showPhones) {
						int segphidx = alignementPhones.getSegmentAtFrame(frame);
						sigpan.setAlign(alignementPhones);
						sigpan.setFirstSeg(segphidx);
					} else {
						sigpan.setFirstSeg(segidx);
						sigpan.setAlign(alignement);
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
				System.out.println("set end of segment "+segmentDuMot+" "+alignement.getSegmentLabel(segmentDuMot));
				System.out.println("set at frame "+SpectroControl.second2frame(lastSecClickedOnSpectro));
				alignement.setSegmentEndFrame(segmentDuMot, SpectroControl.second2frame(lastSecClickedOnSpectro));
			}
			if (edit!=null) edit.colorizeAlignedWords(0,mot);
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
				int frame = alignement.getSegmentDebFrame(segidx);
				cursec = Alignement.frame2second(frame);
				long currentSample = Alignement.frame2sample(frame);
				if (currentSample<0) currentSample=0;
				edit.griseMot(emot);
				// vieux panel
				if (sigPanel!=null) {
					sigPanel.setProgressBar(currentSample);
				}
				// nouveau panel
				sigpan.setAudioInputStream(getCurPosInSec(), getAudioStreamFromSec(getCurPosInSec()));
				if (showPhones) {
					int segphidx = alignementPhones.getSegmentAtFrame(frame);
					sigpan.setAlign(alignementPhones);
					sigpan.setFirstSeg(segphidx);
				} else {
					sigpan.setFirstSeg(segidx);
					sigpan.setAlign(alignement);
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
			firstFrame = alignement.getSegmentEndFrame(segidx);
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
				int nsegsbefore = alignement.merge(order.alignWords);
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
				alignementPhones.merge(order.alignPhones);
				if (lastMotAligned>=0) {
					if (firstMot==0)
						edit.colorizeAlignedWords(firstMot,lastMotAligned-1);
					else
						edit.colorizeAlignedWords(firstMot-1,lastMotAligned-1);
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
		final int fr = JTrans.millisec2frame(absms);
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
				//		        startPlayingFrom(Alignement.frame2second(lastFrame)-2);
				repaint();
			}
		}).start();

		/*
		final int lastFrame = Alignement.sample2frame(player.getLastSamplePlayed());
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
				//		        startPlayingFrom(Alignement.frame2second(lastFrame)-2);
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
		edit.colorizeAlignedWords(0,lmots.size()-1);
		repaint();
	}

	private List<String> getRecoResultOld(main.SpeechReco asr) {
		StringBuilder sb = new StringBuilder();
		ArrayList<Integer> frdebs = new ArrayList<Integer>();
		ArrayList<Integer> frfins = new ArrayList<Integer>();
		ArrayList<String> lmots = new ArrayList<String>();

		ListeElement elts = new ListeElement();
		alignement.clear();
		for (RecoWord word : asr.resRecoPublic) {
			String[] phones = new String[word.frameEnd-word.frameDeb];
			int[] states = new int[word.frameEnd-word.frameDeb];
			for (int t=0;t<word.frameEnd-word.frameDeb;t++) {
				phones[t] = word.getPhone(t);
				states[t] = word.getState(t);
				// TODO : ajouter les GMM qui ont ete perdues dans RecoWord
			}
			alignement.addRecognizedSegment(word.word,word.frameDeb,word.frameEnd,phones,states);
			//    		alignement.words.add(word.word);
			//    		alignement.wordsEnd.add(word.frameEnd);
			if (word.word.charAt(0)=='<') continue;
			int posdebinpanel = sb.length();
			sb.append(word.word+" ");
			lmots.add(word.word);
			//    		alignement.wordsIdx.add(alignement.words.size()-1);
			int posfininpanel = sb.length();
			elts.add(new Element_Mot(word.word, posdebinpanel, posfininpanel, false));
			frdebs.add(word.frameDeb);
			frfins.add(word.frameEnd);
		}
		edit.setListeElement(elts);

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
		//    	System.out.println(alignement.alignedGMMs);
		//   	System.out.println(alignement.alignedGMMs.size());
		//   	System.out.println(alignement.phones.size());
		//  	System.out.println(alignement.states.size());
		// 	SpeechReco asr = SpeechReco.getSpeechReco();
		//   	asr.getGMMs(alignement);
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
				asr.doReco(wavname, "");
				getRecoResult(asr);
			}
		}, "please wait: transcribing...");
		waiting.setVisible(true);
	}

	String[] mots;
	public S4ForceAlignBlocViterbi getS4aligner() {
		// TODO: support for URL !!
		S4ForceAlignBlocViterbi s4aligner = S4ForceAlignBlocViterbi.getS4Aligner(wavname);
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

		new Thread(new Runnable() {
			@Override
			public void run() {
				edit.griseMotsRed(edit.getListeElement().getMot(mot1), edit.getListeElement().getMot(mot2));
				clearAlignFrom(mot1);
				System.out.println("REALIGN BEFORE");
				if (mot1-1>=0)
					realignBeforeAnchor(mot1-1, Alignement.sample2frame(sdeb)-1);
				S4AlignOrder order = new S4AlignOrder(mot1, Alignement.sample2frame(sdeb), mot2, Alignement.sample2frame(send));
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
					//					alignement.merge(order.fullalign,mot1);

					stopAlign();

					// quand on selectionne une zone du signal, on suppose qu'il n'y a pas de silence aux bouts !
					//					alignement.setEndFrameForWord(mot2, Alignement.sample2frame(send));

					edit.griseMotsRed(edit.getListeElement().getMot(mot1), edit.getListeElement().getMot(mot2));
					edit.setSelectedTextColor(null);
					//	    	        alignement.addManualAnchorv2(mot1);
					//	    	        alignement.addManualAnchorv2(mot2);
					edit.souligne(edit.getListeElement().getMot(mot1));
					edit.colorizeAlignedWords(mot1,mot2);
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
		m.inputControls();
		if (args.length>0) {
			if (args[0].endsWith(".jtr")) m.loadProject(args[0]);
			else if (args[0].endsWith(".wav")) {
				m.wavname=args[0]; // on a juste besoin de faire ca pour que ca se charge ???
				if (args.length>1) {
					if (args[1].endsWith(".trs")) {
						m.loadTRSWithProgress(args[1]);
					} else {
						m.sourceTxtfile=args[1];
						InputStream in = FileUtils.findFileOrUrl(m.sourceTxtfile);
						m.loadtxt(in);
						m.updateViewers();
					}
				}
			} else {
				System.out.println("args error: dont know what to do with "+args[0]);
			}
		}
		m.repaint();
	}

	public void loadTRSWithProgress(final String trsfile) {
		final ProgressDialog progress = new ProgressDialog(jf, null, "Loading TRS");
		progress.setRunnable(new Runnable() {
			public void run() { JTransAPI.loadTRS(trsfile, progress); }});
		progress.setVisible(true);
	}
}
