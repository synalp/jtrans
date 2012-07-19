package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JFrame;

import edu.cmu.sphinx.decoder.FrameDecoder;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.decoder.pruner.Pruner;
import edu.cmu.sphinx.decoder.pruner.SimplePruner;
import edu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer;
import edu.cmu.sphinx.decoder.search.PartitionActiveListFactory;
import edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.flat.FlatLinguist;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import edu.cmu.sphinx.util.LogMath;

import plugins.speechreco.aligners.sphiinx4.AlignementEtat;
import plugins.speechreco.aligners.sphiinx4.HMMModels;
import plugins.speechreco.aligners.sphiinx4.PhoneticForcedGrammar;
import plugins.speechreco.aligners.sphiinx4.ProgressDialog;
import plugins.speechreco.aligners.sphiinx4.S4AlignOrder;
import plugins.speechreco.aligners.sphiinx4.S4ForceAlignBlocViterbi;
import plugins.speechreco.grammaire.Grammatiseur;

public class LiveSpeechReco extends PhoneticForcedGrammar {
	public static LiveSpeechReco gram=null;
	public static boolean stopit = false;
	
	public AlignementEtat resWords=null, resPhones=null, resStates=null;
	
	public LiveSpeechReco() throws MalformedURLException, ClassNotFoundException {
		super();
	}
	public static void doReco() {
		S4ForceAlignBlocViterbi s4 = S4ForceAlignBlocViterbi.getS4Aligner(null);
		// TODO: define the vocabulary here
		String[] voc = {"un","deux","trois","quatre","cinq"};
		s4.setMots(voc);
		try {
			gram = new LiveSpeechReco();
			gram.initGrammar(voc);
			System.out.println("********* MIKE GRAMMAR DEFINED");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		// starts live speech reco from mike:
		s4.setNewAudioFile(null);
		System.out.println("********* MIKE MFCC STARTED");
		S4AlignOrder o = new S4AlignOrder(0, 0);
		try {
			s4.input2process.put(o);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("********* MIKE ORDER SENT !");
	}
	
	private void initGrammar(String[] mots) {
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
		gramstring.append("[ sil ] ( ");

		for (int wi=0;wi<mots.length;wi++) {
			String w = mots[wi];
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
	
	/**
	 * synchronous (blocking !) function for live reco from mike
	 */
	public static void liveMikeReco(final S4ForceAlignBlocViterbi s4) {
		if (gram==null) {
			ProgressDialog waiting = new ProgressDialog((JFrame)null, new Runnable() {
				@Override
				public void run() {
					System.out.println("loading acoustic models...");
					LogMath logMath = HMMModels.getLogMath();
					AcousticModel mods = HMMModels.getAcousticModels();
					FlatLinguist linguist = new FlatLinguist(mods, logMath, gram, HMMModels.getUnitManager(), 1f, s4.silprob, s4.silprob, 1f, 1f, false, false, false, false, 1f, 1f, mods);

					Pruner pruner = new SimplePruner();
					ThreadedAcousticScorer scorer = new ThreadedAcousticScorer(s4.mfccs, null, 1, false, 1, Thread.NORM_PRIORITY);

					//					PartitionActiveListFactory activeList = new PartitionActiveListFactory(50, 1E-80, logMath);
					PartitionActiveListFactory activeList = new PartitionActiveListFactory(s4.beamwidth, 1E-300, logMath);

					// je n'utilise pas un WordBreadth... car on travaille ici avec des phonemes et non des mots !
					s4.searchManager = new SimpleBreadthFirstSearchManager(logMath, linguist, pruner, scorer, activeList, false, 1E-60, 0, false);

					ArrayList<ResultListener> listeners = new ArrayList<ResultListener>();
					s4.decoder = new FrameDecoder(s4.searchManager, false, true, listeners);
				}
			}, "please wait: initializing sphinx4...");
			waiting.setVisible(true);
		}
		// TODO
		
		// boucle jusqu'a interruption (TODO)
		s4.searchManager.startRecognition();
		stopit=false;
		for (int t=0;;t++) {
			s4.decoder.decode(null);
			if (stopit) break;
			// TODO: backtrack apres N trames ?
		}
		// on backtrack depuis la fin
		Token besttok = null;
		for (Token tok : s4.searchManager.getActiveList().getTokens()) {
			// est-ce le dernier (emitting) token d'un HMM ?
			if (s4.hasNonEmittingFinalPath(tok.getSearchState())) {
				if (besttok==null||besttok.getScore()<tok.getScore())
					besttok=tok;
			}
		}
		if (besttok==null) {
			System.err.println("WARNING: pas de best tok final ! Je tente le premier token venu...");
			for (Token tok : s4.searchManager.getActiveList().getTokens()) {
				System.out.println("\t DEBUG ActiveList "+tok);
			}
			// faut-il recuperer l'alignement partial que l'on a, meme si on sait qu'il est mauvais ?
			besttok=s4.searchManager.getActiveList().getBestToken();
		}
		if (besttok==null) {
			System.err.println("ERROR: meme pas de best token !");
			gram.resPhones=gram.resStates=gram.resWords=null;
		} else {
			AlignementEtat[] bestaligns = AlignementEtat.backtrack(besttok);
			if (bestaligns!=null) {
				gram.resPhones = bestaligns[0];
				gram.resWords = s4.segmentePhonesEnMots(gram.resPhones);
				gram.resStates = bestaligns[2];
			}
		}
		System.out.println("debug res "+gram.resWords);
		if (gram.resWords!=null) {
/*
			// recopie l'identite des mots alignes
			int midx=firstWord;
			System.out.println("recopie mots dans les places vides de l'alignement "+midx+" "+align.getNbSegments());
			for (int i=0;i<align.getNbSegments();i++) {
				// on a stocké l'indice du mot (car certains mots sont optionnels)
				String s = align.getSegmentLabel(i);
				if (s.startsWith("XZ")) {
					int widx = Integer.parseInt(s.substring(2));
					align.setSegmentLabel(i, mots[midx+widx]);
				}
			}
			System.out.println("copie finie");
*/
		}
	}
	
	public static void main(String args[]) {
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
