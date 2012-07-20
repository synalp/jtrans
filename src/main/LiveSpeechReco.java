package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import edu.cmu.sphinx.decoder.FrameDecoder;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.decoder.pruner.Pruner;
import edu.cmu.sphinx.decoder.pruner.SimplePruner;
import edu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer;
import edu.cmu.sphinx.decoder.search.PartitionActiveListFactory;
import edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor;
import edu.cmu.sphinx.frontend.feature.LiveCMN;
import edu.cmu.sphinx.frontend.filter.Dither;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.flat.FlatLinguist;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.LogMath;

import plugins.speechreco.RecoListener;
import plugins.speechreco.aligners.sphiinx4.AlignementEtat;
import plugins.speechreco.aligners.sphiinx4.HMMModels;
import plugins.speechreco.aligners.sphiinx4.PhoneticForcedGrammar;
import plugins.speechreco.aligners.sphiinx4.ProgressDialog;
import plugins.speechreco.aligners.sphiinx4.S4ForceAlignBlocViterbi;
import plugins.speechreco.aligners.sphiinx4.S4mfccBuffer;
import plugins.speechreco.grammaire.Grammatiseur;

public class LiveSpeechReco extends PhoneticForcedGrammar {
	public static LiveSpeechReco gram=null;
	public static File vocfile = null;
	
	FrameDecoder decoder=null;
	AcousticModel mods=null;
	Microphone mikeSource=null;
	public AlignementEtat resWords=null, resPhones=null, resStates=null;
	private RecoListener listener=null;
	ArrayList<String> voc = new ArrayList<String>();

	public LiveSpeechReco() throws MalformedURLException, ClassNotFoundException {
		super();
	}
	
	public static void stopall() {
		if (gram!=null&&gram.mikeSource!=null) gram.mikeSource.stopRecording();
	}
	public static LiveSpeechReco doReco() {
		try {
			gram = new LiveSpeechReco();
			if (vocfile==null) {
				JOptionPane.showMessageDialog(null, "Please open the vocabulary file");
				JFileChooser jfc = new JFileChooser();
				int res = jfc.showOpenDialog(null);
				if (res==JFileChooser.APPROVE_OPTION) {
					vocfile = jfc.getSelectedFile();
				}
			}
			gram.loadVoc(vocfile);
			gram.initGrammar();
			System.out.println("********* MIKE GRAMMAR DEFINED");
			
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					gram.liveReco();
				}
			},"liveRecoThread");
			t.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return gram;
	}
	
	public void loadVoc(File f) {
		try {
			vocfile=f;
			voc.clear();
			BufferedReader bf = new BufferedReader(new FileReader(f));
			for (;;) {
				String s=bf.readLine();
				if (s==null) break;
				s=s.trim();
				if (s.length()>0) voc.add(s);
			}
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void addResultListener(RecoListener l) {
		listener = l;
	}
	
	private void liveReco() {
		// FRONTEND
		ArrayList<DataProcessor> frontEndList = new ArrayList<DataProcessor>();
		mikeSource = new Microphone(16000, 16, 1, true, true, false, 10, false, "average", 0, "default", 6400);
		frontEndList.add(mikeSource);
		frontEndList.add(new Dither(2,false,Double.MAX_VALUE,-Double.MAX_VALUE));
		frontEndList.add(new DataBlocker(50));
		frontEndList.add(new Preemphasizer(0.97));
		frontEndList.add(new RaisedCosineWindower(0.46f,25.625f,10f));
		frontEndList.add(new DiscreteFourierTransform(512, false));
		frontEndList.add(new MelFrequencyFilterBank(133.33334, 6855.4976, 40));
		frontEndList.add(new DiscreteCosineTransform(40,13));
		frontEndList.add(new LiveCMN(12,100,160));
		frontEndList.add(new DeltasFeatureExtractor(3));
		BaseDataProcessor mfcc = new FrontEnd(frontEndList);

		LogMath logMath = HMMModels.getLogMath();
		if (mods==null) {
			// ACCMODS
			System.out.println("loading acoustic models...");
			mods = HMMModels.getAcousticModels();
		}
		float silprob = 0.1f;
		int beamwidth = 0;

		// S4 DECODER
		FlatLinguist linguist = new FlatLinguist(mods, logMath, gram, HMMModels.getUnitManager(), 1f, silprob, silprob, 1f, 1f, false, false, false, false, 1f, 1f, mods);
		Pruner pruner = new SimplePruner();
		ThreadedAcousticScorer scorer = new ThreadedAcousticScorer(mfcc, null, 1, false, 1, Thread.NORM_PRIORITY);
		PartitionActiveListFactory activeList = new PartitionActiveListFactory(beamwidth, 1E-300, logMath);
		SimpleBreadthFirstSearchManager searchManager = new SimpleBreadthFirstSearchManager(logMath, linguist, pruner, scorer, activeList, false, 1E-60, 0, false);
		ArrayList<ResultListener> listeners = new ArrayList<ResultListener>();
		decoder = new FrameDecoder(searchManager, false, true, listeners);
		
		mikeSource.initialize();
		mikeSource.startRecording();
		searchManager.startRecognition();

		for (int t=0;;t++) {
			Result r = decoder.decode(null);
			if (r.isFinal()) break;
			if (t%100==0) {
				if (listener!=null) listener.recoEnCours(r);
				System.out.println("mike frame "+(t/100));
			}
			// TODO: backtrack apres N trames ?
		}
		System.out.println("MIKE AND DECODE FINISHED !!");
		mikeSource.stopRecording();
		
		// on backtrack depuis la fin
		Token besttok = null;
		for (Token tok : searchManager.getActiveList().getTokens()) {
			// est-ce le dernier (emitting) token d'un HMM ?
			if (S4ForceAlignBlocViterbi.hasNonEmittingFinalPath(tok.getSearchState())) {
				if (besttok==null||besttok.getScore()<tok.getScore())
					besttok=tok;
			}
		}
		if (besttok==null) {
			System.err.println("WARNING: pas de best tok final ! Je tente le premier token venu...");
			for (Token tok : searchManager.getActiveList().getTokens()) {
				System.out.println("\t DEBUG ActiveList "+tok);
			}
			// faut-il recuperer l'alignement partial que l'on a, meme si on sait qu'il est mauvais ?
			besttok=searchManager.getActiveList().getBestToken();
		}
		if (besttok==null) {
			System.err.println("ERROR: meme pas de best token !");
			resPhones=resStates=resWords=null;
		} else {
			AlignementEtat[] bestaligns = AlignementEtat.backtrack(besttok);
			if (bestaligns!=null) {
				resPhones = bestaligns[0];
				resWords = S4ForceAlignBlocViterbi.segmentePhonesEnMots(gram.resPhones);
				resStates = bestaligns[2];
			}
		}
		System.out.println("debug res "+resWords);
		if (resWords!=null) {
			for (int i=0;i<resWords.getNbSegments();i++) {
				String s = resWords.getSegmentLabel(i);
				if (s.startsWith("XZ")) {
					int widx = Integer.parseInt(s.substring(2));
					resWords.setSegmentLabel(i, voc.get(widx));
				}
			}
		}
		if (listener!=null) listener.recoFinie(null, resWords.toString());
	}
	
	private void initGrammar() {
		if (super.grammatiseur==null) {
			ProgressDialog waiting = new ProgressDialog((JFrame)null, new Runnable() {
				@Override
				public void run() {
					grammatiseur = Grammatiseur.getGrammatiseur();
				}
			}, "please wait: initializing grammars...");
			waiting.setVisible(true);
		}
		
//		// on commence toujours par un silence !
//		n = createGrammarNode("SIL");
//		n.setFinalNode(false);
//		initialNode = n;
		
		StringBuilder gramstring = new StringBuilder();
		gramstring.append("[ sil ] ( sil | ");

		for (int wi=0;wi<voc.size();wi++) {
			String w = voc.get(wi);
			w=w.replace('_', ' ');
			String rule = grammatiseur.getGrammar(w);
			// on recupere toujours un silence optionnel au debut et a la fin, que je supprime:
//			rule = rule.substring(4,rule.length()-8).trim();
			System.out.println("rule for word "+w+" "+rule);
			if (rule==null || rule.length()==0) {
				System.out.println("ERROR PHONETISEUR mot "+w);
				continue;
			}
			if (rule.charAt(0)=='#') {
				// le phonetiseur n'a pas marche: on suppose que c'est un bruit
				rule = "xx "+rule;
			}
			// conversion des phonemes
			StringBuilder rule2 = new StringBuilder();
			StringTokenizer st = new StringTokenizer(rule);
			while (st.hasMoreTokens()) {
				String s = st.nextToken();
				if (s.startsWith("#")) {
				} else if (s.equals("(") || s.equals("[") || s.equals(")") || s.equals("]") || s.equals("|")) {
					rule2.append(s+" ");
				} else {
					s=super.convertPhone(s);
					rule2.append(s+" ");
				}
			}
			// regle pour _un_ mot:
			rule = rule2.toString().trim();
			// j'aoute un prefixe pour reperer les 1ers phones de chaque mot
			rule = super.annoteFirstPhones(rule,wi);
			if (rule.length()>0)
				gramstring.append(rule+" | ");
		}
		int i=gramstring.lastIndexOf("|");
		gramstring.deleteCharAt(i);
		gramstring.trimToSize();
		// TODO: add a filler + weights
		gramstring.append(" )* [ sil ]");

		System.out.println("gramstring "+gramstring);
		
		{
			try {
				PrintWriter f = new PrintWriter(new FileWriter("detgrammar.gram"));
				f.println("#JSGF V1.0;");
				f.println("grammar detgrammar;");
				f.println("public <a> = "+gramstring.toString()+";");
				f.close();
				
				loadJSGF("detgrammar");
//				System.out.println("GRAMMAR JSGF");
//				getInitialNode().dump();
				
				System.out.println("nb of grammar nodes "+getGrammarNodes().size());
				System.out.println("final nodes:");
				for (GrammarNode n : getGrammarNodes()) {
					if (n.isFinalNode()) {
						System.out.println("\t"+n);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSGFGrammarParseException e) {
				e.printStackTrace();
			} catch (JSGFGrammarException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String args[]) {
		debug2();
	}
	private static void debug2() {
		// FRONTEND
		ArrayList<DataProcessor> frontEndList = new ArrayList<DataProcessor>();
		Microphone mikeSource = new Microphone(16000, 16, 1, true, true, false, 10, false, "average", 0, "default", 6400);
		frontEndList.add(mikeSource);
		frontEndList.add(new Dither(2,false,Double.MAX_VALUE,-Double.MAX_VALUE));
		frontEndList.add(new DataBlocker(50));
		frontEndList.add(new Preemphasizer(0.97));
		frontEndList.add(new RaisedCosineWindower(0.46f,25.625f,10f));
		frontEndList.add(new DiscreteFourierTransform(512, false));
		frontEndList.add(new MelFrequencyFilterBank(133.33334, 6855.4976, 40));
		frontEndList.add(new DiscreteCosineTransform(40,13));
		frontEndList.add(new LiveCMN(12,100,160));
		frontEndList.add(new DeltasFeatureExtractor(3));
		BaseDataProcessor mfcc = new FrontEnd(frontEndList);
		//		mfccs = new S4RoundBufferFrontEnd(null, 10000);
		S4mfccBuffer mfccs = new S4mfccBuffer();
		mfccs.setSource(mfcc);

		// ACCMODS
		System.out.println("loading acoustic models...");
		LogMath logMath = HMMModels.getLogMath();
		AcousticModel mods = HMMModels.getAcousticModels();
		float silprob = 0.1f;
		int beamwidth = 0;

		// LANGMODS
		vocfile=new File("tmpvoc.txt");
		try {
			gram = new LiveSpeechReco();
			gram.initGrammar();
			System.out.println("********* MIKE GRAMMAR DEFINED");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		// S4 DECODER
		FlatLinguist linguist = new FlatLinguist(mods, logMath, gram, HMMModels.getUnitManager(), 1f, silprob, silprob, 1f, 1f, false, false, false, false, 1f, 1f, mods);
		Pruner pruner = new SimplePruner();
		ThreadedAcousticScorer scorer = new ThreadedAcousticScorer(mfccs, null, 1, false, 1, Thread.NORM_PRIORITY);
		PartitionActiveListFactory activeList = new PartitionActiveListFactory(beamwidth, 1E-300, logMath);
		SimpleBreadthFirstSearchManager searchManager = new SimpleBreadthFirstSearchManager(logMath, linguist, pruner, scorer, activeList, false, 1E-60, 0, false);
		ArrayList<ResultListener> listeners = new ArrayList<ResultListener>();
		FrameDecoder decoder = new FrameDecoder(searchManager, false, true, listeners);
		
		mikeSource.initialize();
		mikeSource.startRecording();
		searchManager.startRecognition();
	}
	private static void debug1() {
		JSGFGrammar gram = new JSGFGrammar();
		try {
			File f = new File(".");
			gram.setBaseURL(f.toURI().toURL());
			gram.loadJSGF("detgrammar");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSGFGrammarParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSGFGrammarException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
