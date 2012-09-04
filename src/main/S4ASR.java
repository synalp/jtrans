package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import plugins.speechreco.aligners.sphiinx4.HMMModels;
import plugins.utils.DET;
import plugins.utils.FileUtils;
import plugins.utils.SpeechRecoAccuracy;
import utils.SuiteDeMots;

import edu.cmu.sphinx.decoder.FrameDecoder;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.decoder.pruner.Pruner;
import edu.cmu.sphinx.decoder.pruner.SimplePruner;
import edu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer;
import edu.cmu.sphinx.decoder.search.ActiveListFactory;
import edu.cmu.sphinx.decoder.search.ActiveListManager;
import edu.cmu.sphinx.decoder.search.PartitionActiveListFactory;
import edu.cmu.sphinx.decoder.search.SimpleActiveListManager;
import edu.cmu.sphinx.decoder.search.WordActiveListFactory;
import edu.cmu.sphinx.decoder.search.WordPruningBreadthFirstSearchManager;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.SignalListener;
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor;
import edu.cmu.sphinx.frontend.feature.LiveCMN;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.FastDictionary;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;
import edu.cmu.sphinx.linguist.language.ngram.large.LargeTrigramModel;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist;
import edu.cmu.sphinx.result.ConfidenceResult;
import edu.cmu.sphinx.result.MAPConfidenceScorer;
import edu.cmu.sphinx.result.Path;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.util.LogMath;

/**
 * nouveau ASR base sur Sphinx4 mais sans fichiers de configuration.
 * Utilisation directe des resources dans le .jar !
 * 
 * @author xtof
 *
 */
public class S4ASR implements SignalListener {

	double wip = 0.3;
	float lw = 10;
	int beam = 70000;
	double relbeam = 1E-80;
	int wordBeam = 700;
	double relWordBeam = 1E-80;

	public boolean quiet=false;

	AudioFileDataSource wavsource;
	FrontEnd mfcc=null;
	Dictionary dico;
//	LanguageModel lm;
	Linguist linguist;
	Pruner pruner;
	ActiveListManager almgr;
	FrameDecoder decoder;
	WordPruningBreadthFirstSearchManager search;

	private boolean signalEndReached=false;

	Result res=null;

	private String basepath = "/home/xtof/git/jtrans/res/";

	public S4ASR() {
		initRes();
		initS4();
	}

	private void initRes() {
		try {
			InetAddress addr = InetAddress.getLocalHost(); 
			String hostname = addr.getHostName();
			if (hostname.startsWith("clustertalc")) {
				basepath = "/home/parole/cerisara/res/";
			}
		}
		catch (UnknownHostException e) { }
	}

	public String reco(String wavfile) {
		if (mfcc==null)
			initFronEnd(wavfile);
		else {
			wavsource.setAudioFile(new File(wavfile), null);
		}

		search.startRecognition();
		for (int t=0;;t++) {
			res = decoder.decode(null);
			if (!quiet) {
				System.out.println("decode fr="+t+" res="+res);
			}
			if (signalEndReached) {
				search.stopRecognition();
				signalEndReached=false;
				return SpeechRecoAccuracy.normalize(res.toString());
			}
		}
	}

	void printConf(String ref) {
		List<RecoWord> ws = getWords();
		for (RecoWord w : ws) {
			System.out.print(w+" ");
		}
		System.out.println();
		
		SpeechRecoAccuracy.normalizeMots(ws,false);
		String[] recmots = new String[ws.size()];
		for (int i=0;i<ws.size();i++) {
			recmots[i]=ws.get(i).word;
		}
		DET det = new DET();
		SuiteDeMots srec = new SuiteDeMots(recmots);
		SuiteDeMots sref = new SuiteDeMots(ref);
		srec.align(sref);
		for (int i=0;i<srec.getNmots();i++) {
			int[] linkedWords = srec.getLinkedWords(i);
			boolean ok=false;
			for (int lw : linkedWords) {
				if (sref.getMot(lw).equals(srec.getMot(i))) {
					ok=true; break;
				}
			}
			det.updateExample(ok, (float)ws.get(i).conf);
		}
		System.out.println("EER="+det.computeEERold());
		det.showDET2();
	}

	public List<RecoWord> getWords() {
		ArrayList<RecoWord> ws = new ArrayList<RecoWord>();
		try {
			MAPConfidenceScorer conf = new MAPConfidenceScorer(1, false, false);
			ConfidenceResult confres = conf.score(res);
			Path p = confres.getBestHypothesis();
			WordResult[] words = p.getWords();
			for (int i = 0; i < words.length; i++) {
				WordResult wordResult = (WordResult) words[i];
				double wordConfidence = wordResult.getConfidence();
				ws.add(new RecoWord(wordResult.getPronunciation().getWord().getSpelling(),wordConfidence));
			}
		} catch (Error e) {
			e.printStackTrace();
		}
		return ws;
	}

	public static void main(String args[]) {
		ArrayList<String> wavfiles = new ArrayList<String>();
		if (args[0].equals("-l")) {
			// list
			try {
				BufferedReader f = FileUtils.openFileUTF(args[1]);
				for (;;) {
					String s = f.readLine();
					if (s==null) break;
					wavfiles.add(s);
				}
				f.close();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else wavfiles.add(args[0]);
		S4ASR m = new S4ASR();
		SpeechRecoAccuracy acc = new SpeechRecoAccuracy();
		for (String wavf:wavfiles) {
			String res = m.reco(wavf);
			String ref = SpeechRecoAccuracy.loadFromLab(FileUtils.noExt(wavf)+".lab");
//			if (m.res!=null) m.printConf(ref);
			acc.printAccuracy(res, ref);
		}
		acc.printAccuracy();
	}

	private void initS4() {
		LogMath logMath = HMMModels.getLogMath();
		AcousticModel mods = HMMModels.getAcousticModels();

		try {
			String bp = basepath;
			dico = new FastDictionary("file://"+bp+"/Lex_phon.africain.dic_sans_hh.utf8", "file://"+bp+"/ESTER2_Train_373f_s01.f04_align.filler", null, false, null, false, false, HMMModels.getUnitManager());
//			lm = new LargeTrigramModel("arpa",new URL("file://"+bp+"LM_africain_3g.sorted.arpa.utf8.dmp"),null,100000,50000,false,-1,logMath,dico,false,lw,wip,0.5f,false);
//			linguist = new LexTreeLinguist(mods, logMath, HMMModels.getUnitManager(), lm, dico, true, false, wip, 0.1, 1E-10, 1, lw, false, false, 1f, 0);
			pruner = new SimplePruner();
			ArrayList<ActiveListFactory> alfacts = new ArrayList<ActiveListFactory>();
			alfacts.add(new PartitionActiveListFactory(beam, relbeam, HMMModels.getLogMath()));
			alfacts.add(new WordActiveListFactory(wordBeam, relWordBeam, HMMModels.getLogMath(), 0, 1));
			alfacts.add(new WordActiveListFactory(wordBeam, relWordBeam, HMMModels.getLogMath(), 0, 1));
			alfacts.add(new PartitionActiveListFactory(beam, relbeam, HMMModels.getLogMath()));
			alfacts.add(new PartitionActiveListFactory(beam, relbeam, HMMModels.getLogMath()));
			alfacts.add(new PartitionActiveListFactory(beam, relbeam, HMMModels.getLogMath()));
			almgr = new SimpleActiveListManager(alfacts, false);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void initFronEnd(String wavname) {
		ArrayList<DataProcessor> frontEndList = new ArrayList<DataProcessor>();
		wavsource = new AudioFileDataSource(3200,null);
		System.out.println("wavname "+wavname);
		wavsource.setAudioFile(new File(wavname), null);
		frontEndList.add(wavsource);
		frontEndList.add(new DataBlocker(50));
		frontEndList.add(new Preemphasizer(0.97));
		frontEndList.add(new RaisedCosineWindower(0.46f,25.625f,10f));
		frontEndList.add(new DiscreteFourierTransform(512, false));
		frontEndList.add(new MelFrequencyFilterBank(133.33334, 6855.4976, 40));
		frontEndList.add(new DiscreteCosineTransform(40,13));
		frontEndList.add(new LiveCMN(12,100,160));
		frontEndList.add(new DeltasFeatureExtractor(3));

		mfcc = new FrontEnd(frontEndList);
		signalEndReached=false;
		mfcc.addSignalListener(this);

		ThreadedAcousticScorer scorer = new ThreadedAcousticScorer(mfcc, null, 1, false, 1, Thread.NORM_PRIORITY);
		search = new WordPruningBreadthFirstSearchManager(HMMModels.getLogMath(), linguist, pruner, scorer, almgr, false, 1E-80, 0, false, true, 100, 1.7f, false);
		ArrayList<ResultListener> listeners = new ArrayList<ResultListener>();
		decoder = new FrameDecoder(search, false, true, listeners);
	}

	@Override
	public void signalOccurred(Signal signal) {
		if (signal instanceof DataEndSignal) signalEndReached=true;
	}

}
