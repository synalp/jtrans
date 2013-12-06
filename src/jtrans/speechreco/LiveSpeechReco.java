package jtrans.speechreco;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor;
import edu.cmu.sphinx.frontend.feature.LiveCMN;
import edu.cmu.sphinx.frontend.filter.Dither;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.frontend.util.WavWriter;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.flat.FlatLinguist;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

import jtrans.speechreco.grammaire.Grammatiseur;
import jtrans.speechreco.s4.*;
import jtrans.utils.ProgressDialog;

public class LiveSpeechReco extends PhoneticForcedGrammar {
	public static LiveSpeechReco gram=null;
	
	// =========== definition du vocabulaire =============
	// vocabulaire dans un fichier: contient seulement la liste de mots
	public static File vocfile = null;
	// alternative: map directement chaque mot du vocab vers une JSGF rule; évite d'utiliser le grammatiseur !
	// Attention: dans ce cas, le vocabulaire est extrait de tinyvocab et vocfile n'est plus utilise !
	public static HashMap<String, String> tinyvocab = null;
	// autres mots qui completent avec une proba plus faible.
	public static HashMap<String, String> vocGarb = null;
	private int finVocMain=0;
	// ====================================================
	
	// si wavfile n'est pas nul, alors la reco sera faite a partir de ce fichier, a la place du micro
	public static String wavfile = "wavout.wav";
	// si wavout n'est pas nul, alors le son capture depuis le micro sera enregistre dans ce fichier
	public static String wavout = null;
	
	// audio mixer a utiliser pour capturer le micro
	public static int mixidx = 4;

	// Si adaptedmods n'est pas nul, alors les modeles acoustiques adaptes par JTrans.MAPAdapt seront utilises
	public static String adaptedmods = null;

	public static float lw=1f;
	// ====================================================
	
	FrameDecoder decoder=null;
	SimpleBreadthFirstSearchManager searchManager=null;
	AcousticModel mods=null;
	Microphone mikeSource=null;
	public Alignment resWords=null, resPhones=null, resStates=null;
	private RecoListener listener=null;
	ArrayList<String> voc = new ArrayList<String>();

	public LiveSpeechReco() throws MalformedURLException, ClassNotFoundException {
		super();
	}

	private boolean stopit=false;
	public void stopit() {
		stopit=true;
	}

	public static void stopall() {
		if (gram!=null&&gram.mikeSource!=null) {
			gram.mikeSource.stopRecording();
		}
	}
	public static LiveSpeechReco doReco() {
		try {
			gram = new LiveSpeechReco();
			if (vocfile==null) {
				JOptionPane.showMessageDialog(null, "Please open the vocabulary file");
				JFileChooser jfc = new JFileChooser((new File(".")));
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
					if (wavfile!=null) {
						System.out.println("perform wav reco");
						gram.wavReco();
					} else {
						System.out.println("perform live reco");
						gram.liveReco();
					}
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

	public void liveReco() {
		stopit=false;
		
		// FRONTEND
		ArrayList<DataProcessor> frontEndList = new ArrayList<DataProcessor>();
		System.out.println("LIVERECO OPENMIKE mixidx "+mixidx);
		mikeSource = new Microphone(16000, 16, 1, true, true, true, 10, false, "average", 0, ""+mixidx, 6400);

		{
			System.out.println("mikesource created");
		}
		
		frontEndList.add(mikeSource);
		
		WavWriter wavw=null;
		final long wavwritestart = System.currentTimeMillis();
		if (wavout!=null) {
			System.out.println("SAVING IN "+wavout);
			wavw = new WavWriter(wavout, true, 16, true, false);
			wavw.initialize();
			frontEndList.add(wavw);
		}
		
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
			if (adaptedmods!=null) BiaisAdapt.loadAdapted(adaptedmods);
		}
		{
			float silprob = 0.1f;
			int beamwidth = 0;
			
			// S4 DECODER
			FlatLinguist linguist = new FlatLinguist(mods, logMath, gram, HMMModels.getUnitManager(), 1f, silprob, silprob, 1f, 1f, false, false, false, false, 1f, 1f, mods);
			Pruner pruner = new SimplePruner();
			ThreadedAcousticScorer scorer = new ThreadedAcousticScorer(mfcc, null, 1, false, 1, Thread.NORM_PRIORITY);
			PartitionActiveListFactory activeList = new PartitionActiveListFactory(beamwidth, 1E-300, logMath);
			searchManager = new SimpleBreadthFirstSearchManager(logMath, linguist, pruner, scorer, activeList, false, 1E-60, 0, false);
			ArrayList<ResultListener> listeners = new ArrayList<ResultListener>();
			decoder = new FrameDecoder(searchManager, false, true, listeners);
			mikeSource.initialize();
		}

		{
			System.out.println("mikesource initialized");
		}

		
		mikeSource.startRecording();
		searchManager.startRecognition();

		{
			System.out.println("mikesource started");
		}
		
		for (int t=0;;t++) {
			if (stopit) {
				//break;
				// non: je ne sors pas tout de suite, car il faut vider ke buffer du mike !
				System.out.println("delayed stop");
				mikeSource.stopRecording();
			}
			Result r = decoder.decode(null);
			if (r.isFinal()) break;
			if (t%100==0) {
				if (listener!=null) listener.recoEnCours(r);
				System.out.print("mike frame "+(t/100)+" \r");
			}
			// TODO: backtrack apres N trames ?
		}
		
		if (wavw!=null) {
			// stop the wavwriter
			DataProcessor endsig = new DataProcessor() {
				@Override
				public void newProperties(PropertySheet arg0) throws PropertyException {}
				@Override
				public void setPredecessor(DataProcessor arg0) {}
				@Override
				public void initialize() {}
				@Override
				public DataProcessor getPredecessor() {
					return null;
				}
				@Override
				public Data getData() throws DataProcessingException {
					final long dur = System.currentTimeMillis()-wavwritestart;
					final DataEndSignal ends = new DataEndSignal(dur);
					return ends;
				}
			};
			wavw.setPredecessor(endsig);
			wavw.getData();
			wavw=null;
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
			Alignment[] bestaligns = Alignment.backtrack(besttok);
			if (bestaligns!=null) {
				resPhones = bestaligns[0];
				resWords = S4ForceAlignBlocViterbi.segmentePhonesEnMots(gram.resPhones);
				resStates = bestaligns[2];
			}
		}
//		System.out.println("debug res "+resWords);
		if (resWords!=null) {
			for (int i=0;i<resWords.getNbSegments();i++) {
				String s = resWords.getSegmentLabel(i);
				if (s.startsWith("XZ")) {
					int widx = Integer.parseInt(s.substring(2));
					resWords.setSegmentLabel(i, voc.get(widx));
				}
			}
			if (listener!=null) listener.recoFinie(null, resWords.toString());
		} else
			if (listener!=null) listener.recoFinie(null, "");
	}

	public void wavReco() {
		// FRONTEND
		ArrayList<DataProcessor> frontEndList = new ArrayList<DataProcessor>();
		mikeSource=null;
		
		AudioFileDataSource wavsrc = new AudioFileDataSource(3200, null);
		wavsrc.setAudioFile(new File(wavfile), null);
		frontEndList.add(wavsrc);
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
			if (adaptedmods!=null) BiaisAdapt.loadAdapted(adaptedmods);
		}

		
		float silprob = 0.1f;
		int beamwidth = 0;

		// S4 DECODER
		FlatLinguist linguist = new FlatLinguist(mods, logMath, gram, HMMModels.getUnitManager(), 1f, silprob, silprob, 1f, lw, false, false, false, false, 1f, 1f, mods);
		Pruner pruner = new SimplePruner();
		ThreadedAcousticScorer scorer = new ThreadedAcousticScorer(mfcc, null, 1, false, 1, Thread.NORM_PRIORITY);
		PartitionActiveListFactory activeList = new PartitionActiveListFactory(beamwidth, 1E-300, logMath);
		searchManager = new SimpleBreadthFirstSearchManager(logMath, linguist, pruner, scorer, activeList, false, 1E-60, 0, false);
		ArrayList<ResultListener> listeners = new ArrayList<ResultListener>();
		decoder = new FrameDecoder(searchManager, false, true, listeners);
		wavsrc.initialize();
		searchManager.startRecognition();

		for (int t=0;;t++) {
			if (stopit) break;
			Result r = decoder.decode(null);
			if (r.isFinal()) break;
			if (t%100==0) {
				if (listener!=null) listener.recoEnCours(r);
				System.out.println("wav frame "+(t/100));
			}
			// TODO: backtrack apres N trames ?
		}
		System.out.println("WAV DECODE FINISHED !!");

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
			Alignment[] bestaligns = Alignment.backtrack(besttok);
			if (bestaligns!=null) {
				resPhones = bestaligns[0];
				resWords = S4ForceAlignBlocViterbi.segmentePhonesEnMots(gram.resPhones);
				resStates = bestaligns[2];
			}
		}
//		System.out.println("debug res "+resWords);
		if (resWords!=null) {
			for (int i=0;i<resWords.getNbSegments();i++) {
				String s = resWords.getSegmentLabel(i);
				if (s.startsWith("XZ")) {
					int widx = Integer.parseInt(s.substring(2));
					resWords.setSegmentLabel(i, voc.get(widx));
				}
			}
			if (listener!=null) listener.recoFinie(null, resWords.toString());
		}
	}

	public void initGrammar() {
		if (tinyvocab==null) {
			System.out.println("loading grammatiseur... "+tinyvocab);
			if (super.grammatiseur==null) {
				ProgressDialog waiting = new ProgressDialog((JFrame)null, new Runnable() {
					@Override
					public void run() {
						grammatiseur = Grammatiseur.getGrammatiseur();
					}
				}, "please wait: initializing grammars...");
				waiting.setVisible(true);
			}
			finVocMain = voc.size();
		} else {
			// quand on utilise tinyvoc, on reset le voc aux mots definis dans tinyvoc
			voc.clear();
			voc.addAll(tinyvocab.keySet());
			// non, il ne faut pas ajouter vocGarb au main voc !
			finVocMain = voc.size();
			if (vocGarb!=null) voc.addAll(vocGarb.keySet());
		}

		//		// on commence toujours par un silence !
		//		n = createGrammarNode("SIL");
		//		n.setFinalNode(false);
		//		initialNode = n;

		StringBuilder gramstring = new StringBuilder();
		gramstring.append("[ sil ] ( sil | <mots> )* [ sil ];\n");
		gramstring.append("<mots> = ");

		for (int wi=0;wi<finVocMain;wi++) {
			String w = voc.get(wi);
			String rule = analyzRule(w,wi,tinyvocab);
			if (rule.length()>0) {
				//nouvelle version de la grammaire qui separe chaque mot
				gramstring.append("/1000/ "+rule+" | ");
			}
		}
		if (vocGarb!=null) {
			ArrayList<String> gw = new ArrayList<String>();
			gw.addAll(vocGarb.keySet());
			// il faut un ordre fixe, donc j'utilise l'ordre alphabetique; sinon, il faudrait remplacer le HashMap par une liste, mais pour le moment ca ira
			Collections.sort(gw);
			// ajouter les autres mots "garbage" avec une proba de 1 et des wi > voc.size()
			for (int wi=0;wi<gw.size();wi++) {
				String w = gw.get(wi);
				String rule = analyzRule(w,wi+finVocMain,vocGarb);
				if (rule.length()>0) {
					//nouvelle version de la grammaire qui separe chaque mot
//					gramstring.append(rule+" | ");
					gramstring.append("/1/ "+rule+" | ");
				}
			}
		}
		
		// supprime le dernier | car il n'y a plus d'options suivantes
		int i=gramstring.lastIndexOf("|");
		gramstring.deleteCharAt(i);
		gramstring.trimToSize();
		gramstring.append(";");

		System.out.println("gramstring "+gramstring);

		{
			try {
				PrintWriter f = new PrintWriter(new FileWriter("detgrammar.gram"));
				f.println("#JSGF V1.0;");
				f.println("grammar detgrammar;");
				f.println("public <a> = "+gramstring.toString());
				f.close();

				loadJSGF("detgrammar");
				//				System.out.println("GRAMMAR JSGF");
				//				getInitialNode().dump();

				System.out.println("nb of grammar nodes "+getGrammarNodes().size());
				System.out.println("final nodes:");
				for (GrammarNode n : getGrammarNodes()) {
					if (n.isFinalNode()) {
						System.out.println("\t final "+n);
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

	private String analyzRule(String w, int wi, HashMap<String, String> localvoc) {
		w=w.replace('_', ' ');
		String rule;
		if (localvoc!=null) {
			rule = localvoc.get(w);
			if (rule==null) System.out.println("ERROR VOCAB NO RULE FOR "+w);
		} else 
			rule = grammatiseur.getGrammar(w);
		// on recupere toujours un silence optionnel au debut et a la fin, que je supprime:
		//			rule = rule.substring(4,rule.length()-8).trim();
		System.out.println("rule for word "+w+" "+rule);
		if (rule==null || rule.length()==0) {
			System.out.println("ERROR PHONETISEUR mot "+w);
			return null;
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
		return rule;
	}
	
	// cette fonction charge tinyvoc a partir d'un fichier
	public static void loadPhoneDico(String nom) {
		HashMap<String, String> mot2rule = new HashMap<String, String>();
		try {
			BufferedReader f = new BufferedReader(new FileReader(nom));
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				s=s.trim();
				int i=s.indexOf(',');
				if (i<0) continue;
				String w = s.substring(0,i);
				String r = s.substring(i+1);
				mot2rule.put(w, r);
			}
			f.close();
			LiveSpeechReco.tinyvocab=mot2rule;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	// cette fonction charge vocGarb a partir d'un fichier
	public static void loadGarbDico(String nom) {
		HashMap<String, String> mot2rule = new HashMap<String, String>();
		try {
			BufferedReader f = new BufferedReader(new FileReader(nom));
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				s=s.trim();
				int i=s.indexOf(',');
				if (i<0) continue;
				String w = s.substring(0,i);
				String r = s.substring(i+1);
				mot2rule.put(w, r);
			}
			f.close();
			LiveSpeechReco.vocGarb=mot2rule;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {
//		mixidx=4;
		LiveSpeechReco.adaptedmods="../emospeech/adaptCorpus/xtofall";
		LiveSpeechReco.wavfile="../emospeech/wavout7.wav";
		LiveSpeechReco.vocfile=new File("../emospeech/res/voc0.txt");
		LiveSpeechReco.lw=10f;
		
		loadPhoneDico("../emospeech/res/vocrules0.txt");
		loadGarbDico("../emospeech/res/lex2rules.txt");
		
		int i=0;
		for (;i<args.length;i++) {
			if (args[i].equals("-wavout")) {
				++i; LiveSpeechReco.wavout=args[i];
			}
		}
		testRecoNoGUI();
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
		vocfile=new File("voc.txt");
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

	// test: fait la reco 2 fois
	public static void testRecoNoGUI() {
		LiveSpeechReco r = doReco();
		r.addResultListener(new RecoListener() {
			@Override
			public void recoFinie(Result finalres, String res) {
				System.out.println("reco fin "+res);
			}
			@Override
			public void recoEnCours(Result tmpres) {
				System.out.println("reco en cours"+tmpres);
			}
		});
		try {
			System.out.println("WAITING...");
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("stopping...");
		r.stopit();
		System.out.println("after stop : relaunch");

		r = doReco();
		r.addResultListener(new RecoListener() {
			@Override
			public void recoFinie(Result finalres, String res) {
				System.out.println("reco fin "+res);
			}
			@Override
			public void recoEnCours(Result tmpres) {
				System.out.println("reco en cours"+tmpres);
			}
		});
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("stopping again...");
		r.stopit();

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
