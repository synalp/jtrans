package main;

import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;

import plugins.speechreco.aligners.sphiinx4.AlignementEtat;
import plugins.utils.FileUtils;

import speechreco.AlignTokenPassing;
import speechreco.RecoUtterance;
import speechreco.RecoUtteranceImmutable;
import speechreco.RecoWord;
import utils.SuiteDeMots;

import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.util.StreamCepstrumSource;
import edu.cmu.sphinx.instrumentation.AccuracyTracker;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.Context;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.LeftRightContext;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.GaussianMixture;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Senone;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneHMMState;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.flat.HMMStateState;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist.LexTreeHMMState;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.ConfidenceResult;
import edu.cmu.sphinx.result.ConfidenceScorer;
import edu.cmu.sphinx.result.ConfusionSet;
import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.LatticeOptimizer;
import edu.cmu.sphinx.result.MAPConfidenceScorer;
import edu.cmu.sphinx.result.Node;
import edu.cmu.sphinx.result.Path;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.Sausage;
import edu.cmu.sphinx.result.SausageMaker;
import edu.cmu.sphinx.result.SimpleWordResult;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.NISTAlign;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

/**
 * 
 * @author xtof
 *
 * XP ASRSphinx. Voici ce que j'obtiens avec doRecoNbest et nbest=1
   Words: 4939   Matches: 4022    WER: 21,097%
 *
 */
public class SpeechReco {
	ConfigurationManager cm = null;
	Recognizer recognizer = null;
	URL cfg;

	/**
	 * contient toujours un résultat de reco qui est "final", i.e., qui ne sera
	 * plus modifié jusqu'au point où il arrive.
	 * Quand tout est terminé, ces 2 variables contiennent le même résultat
	 */
	private RecoUtterance resRecoFinalSaved;
	public RecoUtterance resRecoPublic = new RecoUtterance();

	boolean debug=true;

	private static SpeechReco asr = null;
	public static SpeechReco getSpeechReco() {
		if (asr==null) asr = new SpeechReco();
		return asr;
	}
	
	/*
	 * recupere les GMMs depuis un alignementEtat qui ne les possede pas !
	 * (par ex. lorsqu'il est obtenu apres une reco ou un load)
	 */
/*
	public List<GaussianMixture> getGMMs(AlignementEtat align) {
		if (align.alignedGMMs!=null&&align.alignedGMMs.size()==align.states.size())
			return align.alignedGMMs;
		AcousticModel acmods = (AcousticModel)cm.lookup("acousticModel");
		UnitManager unitmgr = (UnitManager)cm.lookup("unitManager");
		assert unitmgr!=null;
		assert acmods!=null;
		
		assert align.states!=null;
		assert align.states.size()>0;
		assert align.phones!=null;
		assert align.phones.size()>0;
		assert align.phones.size()==align.states.size();
		align.alignedGMMs=new ArrayList<GaussianMixture>();
		for (int t=0;t<align.states.size();t++) {
			String phone = align.phones.get(t);
			String phCI = phone;
			Context ctxt = Context.EMPTY_CONTEXT;
			boolean filler=false;
			if (phone.charAt(0)=='*') {
				filler=true;
				phone = phone.substring(1);
			}
			int i=phone.indexOf('[');
			if (i>=0) {
				phCI = phone.substring(0,i);
				String cc = phone.substring(i+1);
				String[] ccc = cc.split(",]");
				assert ccc.length>2;
				Unit[] left={unitmgr.getUnit(ccc[0], false, Context.EMPTY_CONTEXT)};
				Unit[] right={unitmgr.getUnit(ccc[1], false, Context.EMPTY_CONTEXT)};
				ctxt = LeftRightContext.get(left, right);
			}
			Unit unit = unitmgr.getUnit(phCI, filler, ctxt);
			HMM hmm = acmods.lookupNearestHMM(unit, HMMPosition.UNDEFINED, false);
			// TODO: a terminer
		}
		return null;
	}
	*/
	
	private SpeechReco() {
//		URL cfg0 = FileUtils.getRessourceAsURL("main.SpeechReco", "ressources/config.xml");
		URL cfg0;
		try {
			cfg0 = (new File("ressources/config.xml")).toURI().toURL();
			cfg = fixCfgPaths(cfg0);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	public SpeechReco(URL cfg) {
		this.cfg = fixCfgPaths(cfg);
	}

	private void initSphinx4(URL cfg) {
		if (recognizer==null) {
			cm = new ConfigurationManager(cfg);
			recognizer = (Recognizer) cm.lookup("recognizer");
			recognizer.allocate();
		}
	}
	
	private class MyResultListener implements ResultListener {
		@Override
		public void newProperties(PropertySheet ps) throws PropertyException {
		}
		@Override
		public void newResult(Result result) {
			System.out.println("result listener: "+result);
			List<RecoWord> curres = convertResult2RecoWords(result);
			adjustOffset(curres,onewavs.get(wavchunk).offsetInFrames);
			mergeSegmentsRecouvrants(curres);
			if (recoListener!=null) {
				recoListener.actionPerformed(null);
			}
		}
	}
	MyResultListener myResultListener = new MyResultListener();
	private Result launchSphinx4(String ref) {
		if (recognizer==null) {
			initSphinx4(cfg);
		}
		
		recognizer.removeResultListener(myResultListener);
		System.out.println("reco add result listener "+recognizer.getClass().getName());
		recognizer.addResultListener(myResultListener);
		
		Result result;
		if (ref==null)
			result = recognizer.recognize();
		else
			result = recognizer.recognize(ref);
		return result;
	}

	private URL fixCfgPaths(URL cfg) {
		System.out.println("debug fix path "+cfg);
		File wdf = new File(".");
		String wd = wdf.getAbsolutePath();
		wd=wd.replace('\\', '/');
		try {
			BufferedReader f = new BufferedReader(new InputStreamReader(cfg.openStream()));
			PrintWriter ff = new PrintWriter(new FileWriter("sr.cfg"));
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				s=s.replace("file:///xtof/libs/sphinx4/", "file:///"+wd+"/../jtrans2/res/");
				ff.println(s);
			}
			ff.close();
			f.close();
			return new URL("file:///"+wd+"/sr.cfg");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private class FichSegments {
		String segmentWavFileName;
		int offsetInFrames;
	}

	private List<FichSegments> splitWavFile(String wavfile, int chunkSizeInSecs, int shiftInSecs) {
		// TODO: recuperer cette valeur dans le fichier de config ou le defaut !
		float mfccFrameRate = 100f;

		System.out.println("decoupe wav");
		ArrayList<FichSegments> wavout = new ArrayList<FichSegments>();
		try {
			long startBytes = 0;
			float startSec = 0;
			for (int i=0;;i++) {
				AudioInputStream ais = AudioSystem.getAudioInputStream(new File(wavfile));
				AudioFormat af = ais.getFormat();
				int bitsPerSec = (int)(af.getSampleSizeInBits()*af.getSampleRate());
				int bytesPerSec = bitsPerSec/8;
				int nbytesPerChunk = bytesPerSec*chunkSizeInSecs;
				int nsamplesPerChunk = nbytesPerChunk/2;

				ais.skip(startBytes);

				AudioInputStream bis = new AudioInputStream(ais, af, nsamplesPerChunk);
				if (bis.available()==0) break;
				String outfile = "out"+i+".wav";
				File f2rm = new File(outfile);
				if (f2rm.exists()) f2rm.delete();
				int n=AudioSystem.write(bis, AudioFileFormat.Type.WAVE, new File(outfile));
				if (n==0) break;
				FichSegments seg = new FichSegments();
				seg.segmentWavFileName=outfile;
				seg.offsetInFrames=(int)(startSec*mfccFrameRate);
				wavout.add(seg);
				ais.close();

				int nbytesPerShift=bytesPerSec*shiftInSecs;
				startBytes += nbytesPerShift;
				startSec += shiftInSecs;
			}
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("decoupe wav fini "+wavout.size());
		return wavout;
	}

	private int getFrameEnd(ConfusionSet cs) {
		for (Set<WordResult> swr : cs.values())
			for (WordResult wr : swr) 
				if (wr.getEndFrame()>0) return wr.getEndFrame();
		return -1;
	}
	private int getFrameDeb(ConfusionSet cs) {
		for (Set<WordResult> swr : cs.values())
			for (WordResult wr : swr) 
				if (wr.getStartFrame()>0) return wr.getStartFrame();
		return -1;
	}

	Sausage mergeSausages(Sausage s0, Sausage s1, int s1FrameOffset) {
		Sausage res;

		int posinS0=0;
		boolean conflictFound=false;
		for (ConfusionSet confNode : s0) {
			int frEnd = getFrameEnd(confNode);
			if (frEnd>s1FrameOffset) {
				// debut conflits
				conflictFound=true;
				break;
			}
			posinS0++;
		}
		if (!conflictFound) {
			res = new Sausage(s0.size()+s1.size());
			int respos=0;
			for (ConfusionSet cs : s0) {
				for (Map.Entry<Double, Set<WordResult>> e : cs.entrySet()) {
					for (WordResult wr : e.getValue()) {
						res.addWordHypothesis(respos, wr.toString(), (double)e.getKey(), wr.getLogMath());
					}
				}
				respos++;
			}
			for (ConfusionSet cs : s1) {
				for (Map.Entry<Double, Set<WordResult>> e : cs.entrySet()) {
					for (WordResult wr : e.getValue()) {
//						((SimpleWordResult)wr).startFrame+=s1FrameOffset;
//						((SimpleWordResult)wr).endFrame+=s1FrameOffset;
						res.addWordHypothesis(respos, wr.toString(), (double)e.getKey(), wr.getLogMath());
					}
				}
				respos++;
			}
		} else {
			// la derniere pos est celle du </s> qui n'est pas emetteur !
			int lastpos=s0.size()-2;
			int endFrs0 = getFrameEnd(s0.getConfusionSet(lastpos));
			int middlefr = (endFrs0+s1FrameOffset)/2;

			System.out.println("MIDDLE "+middlefr+" "+endFrs0+" "+s1FrameOffset);

			{
				// calcul de la taille totale
				int respos=0;
				for (ConfusionSet cs : s0) {
					int frend = getFrameEnd(cs);
					respos++;
					if (frend>middlefr) break;
				}
				for (ConfusionSet cs : s1) {
					int frdeb = s1FrameOffset+getFrameDeb(cs);
					if (frdeb>=middlefr) respos++;
				}
				res = new Sausage(respos);
			}

			// on supprime les anciens mots apres middlefr
			int respos=0;
			for (ConfusionSet cs : s0) {
				for (Map.Entry<Double, Set<WordResult>> e : cs.entrySet()) {
					for (WordResult wr : e.getValue()) {
						res.addWordHypothesis(respos, wr.toString(), (double)e.getKey(), wr.getLogMath());
					}
				}
				respos++;
				int frend = getFrameEnd(cs);
				if (frend>middlefr) break;
			}
			// et on ajoute les nouveaux mots apres middlefr
			for (ConfusionSet cs : s1) {
				int frdeb = s1FrameOffset+getFrameDeb(cs);
				if (frdeb>=middlefr) {
					for (Map.Entry<Double, Set<WordResult>> e : cs.entrySet()) {
						for (WordResult wr : e.getValue()) {
//							((SimpleWordResult)wr).startFrame+=s1FrameOffset;
//							((SimpleWordResult)wr).endFrame+=s1FrameOffset;
							res.addWordHypothesis(respos, wr.toString(), (double)e.getKey(), wr.getLogMath());
						}
					}
					respos++;
				}
			}

			// debug check
			for (ConfusionSet cs : res) {
				System.out.println("DEBCHK "+cs.size()+" "+cs.values());
			}
		}
		return res;
	}

	/**
	 * les indications temporelles des <s> et </s> ne sont pas bonnes. Fixe cela !
	 */
	void fixExtremes(List<RecoWord> curres) {
		if (curres.size()>2) {
			RecoWord x = curres.get(curres.size()-1);
			if (x.word.equals("</s>")) {
				int frend = curres.get(curres.size()-2).frameEnd;
				x.frameDeb=x.frameEnd=frend;
			}
		}
	}
	// on peut l'appeler plusieurs fois de suite avec des resultats non finaux, car il se
	// base toujours sur le resultat stabe precedent.
	void mergeSegmentsRecouvrants(List<RecoWord> curres) {
		if (curres.size()==0) return;
		if (resRecoPublic.size()==0) resRecoPublic.addAll(curres);
		else {
			resRecoPublic.clear();
			resRecoPublic.addAll(resRecoFinalSaved);
			int f0 = curres.get(0).frameDeb;
			int wordInConflict =  0;
			while (wordInConflict<resRecoPublic.size()&&resRecoPublic.get(wordInConflict).frameEnd<f0)
				wordInConflict++;
			if (wordInConflict<resRecoPublic.size()) {
				int middlefr = (resRecoPublic.get(wordInConflict).frameDeb+resRecoPublic.get(resRecoPublic.size()-1).frameEnd)/2;
				
				// TODO: il faudrait faire un alignement des mots pour chercher une zone "commune" vers le milieu !
				
				// on supprime les anciens mots apres middlefr
				for (int i=resRecoPublic.size()-1;i>=0;i--) {
					if (resRecoPublic.get(i).frameEnd>middlefr) resRecoPublic.remove(i);
					else break;
				}
				{
					int i=0;
					// et on ajoute les nouveaux mots apres middlefr
					for (;i<curres.size();i++) {
						if (curres.get(i).frameEnd>=middlefr) {
							RecoWord w = curres.get(i);
							if (resRecoPublic.size()>0)
								w.frameDeb=resRecoPublic.get(resRecoPublic.size()-1).frameEnd+1;
							resRecoPublic.add(w);
							++i;
							break;
						}
					}
					for (;i<curres.size();i++) {
						resRecoPublic.add(curres.get(i));
					}
				}
			} else
				resRecoPublic.addAll(curres);
		}

		System.out.println("MERGE "+curres);
		System.out.println("GOT "+resRecoPublic);
	}

	void fixExtremes(Sausage s) {
		if (s.size()>2) {
			for (Map.Entry<Double,Set<WordResult>> y : s.getConfusionSet(s.size()-1).entrySet())
				for (WordResult x : y.getValue()) {
					if (x.toString().equals("</s>")) {
						int frend = s.getConfusionSet(s.size()-2).values().iterator().next().iterator().next().getEndFrame();
						y.getValue().remove(x);
						SimpleWordResult xx = new SimpleWordResult(new Word(x.toString(), null, true), x.getStartFrame(), frend, x.getScore(), y.getKey(), x.getLogMath());
						y.getValue().add(xx);
					}
				}
		}
	}

	private Sausage loadSo6(String nom) {
		LogMath logMath = new LogMath();
		try {
			BufferedReader f = FileUtils.openFileUTF(nom);
			int ncs = Integer.parseInt(f.readLine());
			ConfusionSet css[] = new ConfusionSet[ncs];
			Sausage so6 = new Sausage(ncs);
			for (int i=0;i<ncs;i++) {
				css[i] = new ConfusionSet();
//				so6.confusionSets.set(i, css[i]);
				int csz = Integer.parseInt(f.readLine());
				for (int j=0;j<csz;j++) {
					String s = f.readLine();
					String[] ss = s.split(" ");
					double prob = Double.parseDouble(ss[0]);
					int nw = Integer.parseInt(ss[1]);
					for (int k=0;k<nw;k++) {
						s = f.readLine();
						ss = s.split(" ");
						WordResult w = new SimpleWordResult(new Word(ss[2], null, ss[2].charAt(0)=='<'),
								Integer.parseInt(ss[0]), Integer.parseInt(ss[1]), prob, prob, logMath);
						css[i].addWordHypothesis(w);
					}
				}
			}
			f.close();
			return so6;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void save(Sausage s, String nom) {
		try {
			PrintWriter f = FileUtils.writeFileUTF(nom);
			f.println(s.size());
			for (ConfusionSet cs : s) {
				f.println(cs.size());
				for (Map.Entry<Double, Set<WordResult>> e : cs.entrySet()) {
					f.println(e.getKey()+" "+e.getValue().size());
					for (WordResult w : e.getValue()) {
						f.println(w.getStartFrame()+" "+w.getEndFrame()+" "+w.toString());
					}
				}
			}
			f.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	int nbest = 10000;
	AlignTokenPassing aligner = new AlignTokenPassing();

	public void doRecoNbest(String wav, String refString, int nbest) {
		this.nbest=nbest;
		List<FichSegments> onewavs = splitWavFile(wav, 15, 10);

		// conserve la distrib du nb de mots dans un confusion set
		HashMap<Integer, Integer> nmots2noccs = new HashMap<Integer, Integer>();

		Sausage[] seqso6 = new Sausage[onewavs.size()];
		for (int i=0;i<onewavs.size();i++) {
			String wavfile = onewavs.get(i).segmentWavFileName;
			URL audioURL;
			try {
				audioURL = new File(wavfile).toURI().toURL();
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return;
			}
			AudioFileDataSource dataSource = (AudioFileDataSource) cm.lookup("audioFileDataSource");
			dataSource.setAudioFile(audioURL, null);
			Result result = launchSphinx4(null);

			Lattice lat = new Lattice(result);

			lat.dump(wav+"_i.lat");

			LatticeOptimizer optimizer = new LatticeOptimizer(lat);
			optimizer.optimize();           
			lat.computeNodePosteriors(1);
			SausageMaker mangu = new SausageMaker(lat);
			Sausage saucisse = mangu.makeSausage();

			//            PivotSausageMaker mangu = new PivotSausageMaker();
			//            Sausage saucisse = (Sausage)mangu.score(result);

			fixExtremes(saucisse);
			//	        save(saucisse,wav+".so"+i);
			if (debug) {
				// debug: affiche la distrib du nb de mots dans un confusion set
				for (ConfusionSet cs : saucisse)  {
					int nw=0;
					for (Set<WordResult> ws : cs.values())
						nw+=ws.size();
					Integer n = nmots2noccs.get(nw);
					int co=1;
					if (n!=null) co+=n;
					nmots2noccs.put(nw,co);
				}
				System.out.print("CONFSETSIZE ");
				int ntot=0;
				for (int nw : nmots2noccs.keySet())
					ntot+=nmots2noccs.get(nw);
				for (int nw : nmots2noccs.keySet()) {
					float r=(float)nmots2noccs.get(nw)/(float)ntot;
					System.out.print(nw+":"+nmots2noccs.get(nw)+"("+r+") ");
				}
				System.out.println();
			}

			if (i==0) seqso6[0]=saucisse;
			else {
				// verifie s'il y a recouvrement
				int firstNewFrame = onewavs.get(i).offsetInFrames+getFrameDeb(saucisse.getConfusionSet(0));
				int wordInConflict =  0;
				int endCurWord = onewavs.get(i-1).offsetInFrames+getFrameEnd(seqso6[i-1].getConfusionSet(wordInConflict));
				while (wordInConflict<seqso6[i-1].size()&&endCurWord<firstNewFrame)
					endCurWord = onewavs.get(i-1).offsetInFrames+getFrameEnd(seqso6[i-1].getConfusionSet(++wordInConflict));
				if (wordInConflict<seqso6[i-1].size()) {
					// il y a recouvrement
					int middlefr = (firstNewFrame+
							onewavs.get(i-1).offsetInFrames+getFrameEnd(seqso6[i-1].getConfusionSet(seqso6[i-1].size()-1)))/2;

					// on supprime les anciens mots apres middlefr
					for (int j=seqso6[i-1].size()-1;j>=wordInConflict;j--) {
						endCurWord = onewavs.get(i-1).offsetInFrames+getFrameEnd(seqso6[i-1].getConfusionSet(j));
						if (endCurWord>middlefr) {
//							seqso6[i-1].confusionSets.remove(j);
						} else break;
					}

					// coupe le debut de la saucisse suivante
					for (int j=saucisse.size()-1;j>=0;j--) {
						endCurWord = onewavs.get(i).offsetInFrames+getFrameEnd(saucisse.getConfusionSet(j));
						if (endCurWord<middlefr) {
//							saucisse.confusionSets.remove(j);
						}
					}
					seqso6[i]=saucisse;
				}
			}
		}

		// ici on a decoupe les saucisses: il ne reste plus qu'a les assembler !
		int nslots = 0;
		for (int i=0;i<seqso6.length;i++) {
			// il ne faut surtout pas supprimer les <noop> pour calculer l'accuracy !
			//			seqso6[i].removeFillers();
			nslots += seqso6[i].size();
		}
		Sausage concat = new Sausage(nslots);
//		concat.confusionSets.clear();
//		for (int i=0;i<seqso6.length;i++) {
//			for (ConfusionSet cs : seqso6[i]) {
//				concat.confusionSets.add(cs);
//			}
//		}

		SuiteDeMots refs = new SuiteDeMots(refString);
		SuiteDeMots as = refs.normaliseForAccuracy();

		concat = normaliseForAccuracy(concat);
		aligner.alignSausage(as.getMots(), concat);

		/*
		NISTAlign aligner = ((SausageAccuracyTracker)cm.lookup("accuracySo6Tracker")).getAligner();
		aligner.alignSausage(as.toString(), concat);
		aligner.printNISTSentenceSummary();
		aligner.printTotalSummary();
		 */

		/*
		// token-passing
		List<RecoUtteranceImmutable> nbests = tokenPassing(seqso6,nbest);

		if (debug) {
			for (RecoUtteranceImmutable u : nbests)
				System.out.println("DEBUG IMMU "+u);
		}

		HashSet<RecoUtteranceImmutable> nbestres = new HashSet<RecoUtteranceImmutable>();
		nbestres.addAll(nbests);
		System.out.println("testnbest "+nbestres.size());

		// calcul du meilleur nbest
		RecoUtteranceImmutable bestu=null;
		float bestwer=Float.MAX_VALUE;

		for (RecoUtteranceImmutable u : nbestres) {
			String[] rec = u.words;

			NISTAlign al = new NISTAlign(false, false);
			al.resetTotals();
			SuiteDeMots recs = new SuiteDeMots(rec);
			SuiteDeMots bs = recs.normaliseForAccuracy();
			al.align(as.toString(),bs.toString());
			float wer = al.getTotalWordErrorRate();
			if (wer<bestwer) {
				bestwer=wer; bestu=u;
			}
		}

		System.out.println("CHOSEN BEST "+bestu);

		NISTAlign aligner = ((AccuracyTracker)cm.lookup("accuracyTracker")).getAligner();
		String[] rec = bestu.words;
		SuiteDeMots recs = new SuiteDeMots(rec);
		SuiteDeMots bs = recs.normaliseForAccuracy();
		aligner.align(as.toString(),bs.toString());

		aligner.printNISTSentenceSummary();
		aligner.printTotalSummary();

		 */

	}


	// zap la reco pour aller plus vite
	public String doRecoNbestFromSo6(String wav, String refString) {
		List<FichSegments> onewavs = splitWavFile(wav, 15, 10);

		// conserve la distrib du nb de mots dans un confusion set
		HashMap<Integer, Integer> nmots2noccs = new HashMap<Integer, Integer>();

		Sausage[] seqso6 = new Sausage[onewavs.size()];
		for (int i=0;i<onewavs.size();i++) {
			Sausage saucisse = loadSo6(wav+".so"+i);
			if (debug) {
				// debug: affiche la distrib du nb de mots dans un confusion set
				for (ConfusionSet cs : saucisse)  {
					int nw=0;
					for (Set<WordResult> ws : cs.values())
						nw+=ws.size();
					Integer n = nmots2noccs.get(nw);
					int co=1;
					if (n!=null) co+=n;
					nmots2noccs.put(nw,co);
				}
				System.out.print("CONFSETSIZE ");
				int ntot=0;
				for (int nw : nmots2noccs.keySet())
					ntot+=nmots2noccs.get(nw);
				for (int nw : nmots2noccs.keySet()) {
					float r=(float)nmots2noccs.get(nw)/(float)ntot;
					System.out.print(nw+":"+nmots2noccs.get(nw)+"("+r+") ");
				}
				System.out.println();
			}

			if (i==0) seqso6[0]=saucisse;
			else {
				// verifie s'il y a recouvrement
				int firstNewFrame = onewavs.get(i).offsetInFrames+getFrameDeb(saucisse.getConfusionSet(0));
				int wordInConflict =  0;
				int endCurWord = onewavs.get(i-1).offsetInFrames+getFrameEnd(seqso6[i-1].getConfusionSet(wordInConflict));
				while (wordInConflict<seqso6[i-1].size()&&endCurWord<firstNewFrame)
					endCurWord = onewavs.get(i-1).offsetInFrames+getFrameEnd(seqso6[i-1].getConfusionSet(++wordInConflict));
				if (wordInConflict<seqso6[i-1].size()) {
					// il y a recouvrement
					int middlefr = (firstNewFrame+
							onewavs.get(i-1).offsetInFrames+getFrameEnd(seqso6[i-1].getConfusionSet(seqso6[i-1].size()-1)))/2;

					// on supprime les anciens mots apres middlefr
					for (int j=seqso6[i-1].size()-1;j>=wordInConflict;j--) {
						endCurWord = onewavs.get(i-1).offsetInFrames+getFrameEnd(seqso6[i-1].getConfusionSet(j));
						if (endCurWord>middlefr) {
//							seqso6[i-1].confusionSets.remove(j);
						} else break;
					}

					// coupe le debut de la saucisse suivante
					for (int j=saucisse.size()-1;j>=0;j--) {
						endCurWord = onewavs.get(i).offsetInFrames+getFrameEnd(saucisse.getConfusionSet(j));
						if (endCurWord<middlefr) {
//							saucisse.confusionSets.remove(j);
						}
					}
					seqso6[i]=saucisse;
				}
			}
		}

		// ici on a decoupe les saucisses: il ne reste plus qu'a les assembler !
		int nslots = 0;
		for (int i=0;i<seqso6.length;i++) {
			// il ne faut surtout pas supprimer les <noop> pour calculer l'accuracy !
			//			seqso6[i].removeFillers();
			nslots += seqso6[i].size();
		}
		Sausage concat = new Sausage(nslots);
//		concat.confusionSets.clear();
		for (int i=0;i<seqso6.length;i++) {
			for (ConfusionSet cs : seqso6[i]) {
//				concat.confusionSets.add(cs);
			}
		}

		if (refString!=null) {
			SuiteDeMots refs = new SuiteDeMots(refString);
			SuiteDeMots as = refs.normaliseForAccuracy();

			concat = normaliseForAccuracy(concat);

			concat = filtreNbest(concat,-1);

		}

		RecoUtterance res = extractOneBest(concat, 0);
		return res.toString();
	}


	/**
	 * ne retient dans un slot qu'un maximum de nbest mots
	 */
	static Sausage filtreNbest(Sausage s, int nbestPerSlot) {
		if (nbestPerSlot<=0) return s;
		for (ConfusionSet cs : s) {
			ArrayList<Double> scores = new ArrayList<Double>();
			scores.addAll(cs.keySet());
			Collections.sort(scores);
			int n=0;
			for (int i=scores.size()-1;i>=0;i--) {
				Set<WordResult> words = cs.getWordSet(scores.get(i));
				for (WordResult w : words) {
					if (n++>nbestPerSlot) {
						words.remove(w);
					}
				}
				if (words.size()==0)
					cs.remove(scores.get(i));
			}
		}
		return s;
	}

	/**
	 * cette version calcule des nbests sur chaque segment en ne retenant au plus qu'un seul mot a
	 * remplacer dans le segment, puis utilse un token-passing sur la sequence de segments-nbest.
	 * Ne retenir qu'un seul mot est trop limitant: j'ai donc etendu le token-passing egalement au
	 * mots presents dans le reseau de confusion: cf. doRecoNBest() !!
	 * 
	 * @return
	 */
	public String doRecoSausage(String wav, String refString) {
		List<FichSegments> onewavs = splitWavFile(wav, 15, 10);

		/**
		 * contient la sequence des candidats n-best
		 */
		ArrayList<Set<RecoUtterance>> pos2candidats = new ArrayList<Set<RecoUtterance>>();

		for (int i=0;i<onewavs.size();i++) {
			String wavfile = onewavs.get(i).segmentWavFileName;
			URL audioURL;
			try {
				audioURL = new File(wavfile).toURI().toURL();
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return null;
			}
			AudioFileDataSource dataSource = (AudioFileDataSource) cm.lookup("audioFileDataSource");
			dataSource.setAudioFile(audioURL, null);

			Result result = launchSphinx4(null);

			Lattice lat = new Lattice(result);
			LatticeOptimizer optimizer = new LatticeOptimizer(lat);
			optimizer.optimize();           
			lat.computeNodePosteriors(1);
			SausageMaker mangu = new SausageMaker(lat);
			Sausage saucisse = mangu.makeSausage();

			Set<RecoUtterance> recs = extractNBests(saucisse, onewavs.get(i).offsetInFrames, 1);
			for (RecoUtterance onerec : recs) fixExtremes(onerec);

			if (debug) {
				for (RecoUtterance u : recs)
					System.out.println("DEBUG RECS "+u);
			}

			if (pos2candidats.size()==0) {
				pos2candidats.add(recs);
			} else {
				RecoUtterance previousInstance = pos2candidats.get(pos2candidats.size()-1).iterator().next().clone();
				RecoUtterance newInstance = recs.iterator().next().clone();

				// coupe la fin des anciens
				Set<RecoUtterance> anciens = pos2candidats.remove(pos2candidats.size()-1);
				HashSet<RecoUtterance> anciensCoupes = new HashSet<RecoUtterance>();
				for (RecoUtterance utt : anciens) {
					utt.cutAndAppendSegmentsRecouvrants(newInstance, true, false, false);
					anciensCoupes.add(utt);
				}
				pos2candidats.add(anciensCoupes);

				// coupe le debut des nouveaux
				HashSet<RecoUtterance> newpos = new HashSet<RecoUtterance>();
				for (RecoUtterance utt : recs) {
					previousInstance.cutAndAppendSegmentsRecouvrants(utt, false, true, false);
					newpos.add(utt);
				}
				pos2candidats.add(newpos);
			}
		}

		// Viterbi pour chercher les nbest avec les sequences de candidats coupes
		int nbest = 1;
		List<RecoUtteranceImmutable> nbests = tokenPassing0(pos2candidats,nbest);

		if (debug) {
			for (RecoUtteranceImmutable u : nbests)
				System.out.println("DEBUG IMMU "+u);
		}

		HashSet<RecoUtteranceImmutable> nbestres = new HashSet<RecoUtteranceImmutable>();
		nbestres.addAll(nbests);
		System.out.println("testnbest "+nbestres.size());

		// calcul du meilleur nbest
		SuiteDeMots refs = new SuiteDeMots(refString);
		SuiteDeMots as = refs.normaliseForAccuracy();
		RecoUtteranceImmutable bestu=null;
		float bestwer=Float.MAX_VALUE;

		for (RecoUtteranceImmutable u : nbestres) {
			String[] rec = u.words;

			NISTAlign al = new NISTAlign(false, false);
			al.resetTotals();
			SuiteDeMots recs = new SuiteDeMots(rec);
			SuiteDeMots bs = recs.normaliseForAccuracy();
			al.align(as.toString(),bs.toString());
			float wer = al.getTotalWordErrorRate();
			if (wer<bestwer) {
				bestwer=wer; bestu=u;
			}
		}

		System.out.println("CHOSEN BEST "+bestu);

		NISTAlign aligner = ((AccuracyTracker)cm.lookup("accuracyTracker")).getAligner();
		String[] rec = bestu.words;

		SuiteDeMots recs = new SuiteDeMots(rec);
		SuiteDeMots bs = recs.normaliseForAccuracy();
		aligner.align(as.toString(),bs.toString());

		aligner.printNISTSentenceSummary();
		aligner.printTotalSummary();

		return "";
	}

	// retient a chaque position la liste des nbest meilleurs tokens
	static class ListeDeTokens extends ArrayList<RecoUtteranceImmutable> {
		/** ajoute une suite a toutes les phrases de la liste
		 */
		public void ajoute(RecoUtterance utt) {
			for (int i=0;i<size();i++) {
				RecoUtteranceImmutable t = get(i);
				RecoUtteranceImmutable tt = RecoUtteranceImmutable.getUtterance(t.originalUtt, utt);
				set(i,tt);
			}
		}
		public void ajoute(RecoUtteranceImmutable utt) {
			for (int i=0;i<size();i++) {
				RecoUtteranceImmutable t = get(i);
				RecoUtteranceImmutable tt = RecoUtteranceImmutable.getUtterance(t.originalUtt, utt.originalUtt);
				set(i,tt);
			}
		}
		public boolean add(RecoUtterance utt) {
			RecoUtteranceImmutable t = RecoUtteranceImmutable.getUtterance(utt);
			return add(t);
		}
		public ListeDeTokens clone() {
			ListeDeTokens lt = new ListeDeTokens();
			for (RecoUtteranceImmutable utt : this) lt.add(utt);
			return lt;
		}
		public void insertSortCut(ListeDeTokens lt, int nbest) {
			for (RecoUtteranceImmutable utt : lt) {
				int i=0;
				for (;i<size();i++) {
					if (utt.getScore()>get(i).getScore()) {
						// il faut l'inserer avant
						add(i, utt);
						break;
					}
				}
				if (i>=size()) {
					// on l'insere a la fin
					add(utt);
				}
				while (size()>nbest) remove(size()-1);
			}
		}
	}

	private int countWordsInConfusionSet(ConfusionSet set) {
		int co=0;
		for (Map.Entry<Double, Set<WordResult>> e : set.entrySet()) {
			co+=e.getValue().size();
		}
		return co;
	}

	class NBestToken {
		public NBestToken(int limit) {nbest=limit;}
		final int nbest;

		ArrayList<String> nbestUtterances = new ArrayList<String>();
		// liste ordonee decroissante des scores
		ArrayList<Float> scoresUnnormalized = new ArrayList<Float>();
		ArrayList<Integer> denominator = new ArrayList<Integer>();
	}

	/**
	 * ajoute une nouvelle hypothese (prefixe+word) dans le token tok (dans la liste des nbest ordonnes)
	 * 
	 */
	void insertHypIntoToken(NBestToken tok, String prefixe, float prefScoreUnormed, int prefDenominator, String word2add, double wordScore) {
		// calcul du nouveau score
		int newDenom = prefDenominator;
		if (!(word2add==null||word2add.length()==0)) newDenom++;
		float newscoreUnnormed = (prefScoreUnormed+(float)wordScore);
		float newscore = newDenom>0?newscoreUnnormed/(float)newDenom:0;

		// calcul de la nouvelle String
		String newPhrase = prefixe;
		if (word2add!=null&&word2add.length()>0&word2add.charAt(0)!='<')
			newPhrase+=" "+word2add;

		//		System.out.println("DEBUGPHRASE "+newPhrase);

		// check duplicate
		for (int j=0;j<tok.nbestUtterances.size();j++) {
			String oldPhrase = tok.nbestUtterances.get(j);
			if (oldPhrase.equals(newPhrase)) {
				float oldscore = tok.denominator.get(j)>0?tok.scoresUnnormalized.get(j)/(float)tok.denominator.get(j):0;
				if (newscore>oldscore) {
					// on supprime l'ancienne solution
					tok.nbestUtterances.remove(j);
					tok.scoresUnnormalized.remove(j);
					tok.denominator.remove(j);
					// puis on reinsere la nouvelle a sa bonne place
					break;
				} else return;
			}
		}

		// trouve la place
		int pos=0;
		for (;pos<tok.scoresUnnormalized.size();pos++) {
			float oldscore = tok.denominator.get(pos)>0?tok.scoresUnnormalized.get(pos)/(float)tok.denominator.get(pos):0;
			if (newscore>oldscore) break;
		}
		// on l'ajoute si on reste dans les Nbest
		if (pos<tok.nbest) {
			tok.scoresUnnormalized.add(pos, newscoreUnnormed);
			tok.denominator.add(pos,newDenom);
			tok.nbestUtterances.add(pos,newPhrase);
			// on supprime les elts decales qui depassent la limite Nbest
			while (tok.scoresUnnormalized.size()>tok.nbest) {
				tok.scoresUnnormalized.remove(nbest);
				tok.nbestUtterances.remove(nbest);
			}
		}
	}

	List<RecoUtteranceImmutable> tokenPassing(Sausage[] nodes, int nbest) {
		int so6=0, confset=0;
		NBestToken[] toks;

		// init
		int nNodesAtT = countWordsInConfusionSet(nodes[so6].getConfusionSet(confset));
		toks = new NBestToken[nNodesAtT];
		for (int i=0;i<toks.length;i++) {
			toks[i]=new NBestToken(nbest);
			for (Map.Entry<Double, Set<WordResult>> e : nodes[so6].getConfusionSet(confset).entrySet())
				for (WordResult w : e.getValue())
					insertHypIntoToken(toks[i],"",0,0,w.toString(),e.getKey());
		}

		if (++confset>=nodes[so6].size()) {
			confset=0;
			if (++so6>=nodes.length) return null;
		}

		// TODO: accelerer tout ca !

		// iteration
		for (int t=1;;t++) {
			nNodesAtT = countWordsInConfusionSet(nodes[so6].getConfusionSet(confset));
			//			if (t%100==0)
			System.out.println("token passing frame "+t+" "+so6+" "+confset+" "+nNodesAtT);
			NBestToken[] newtoks = new NBestToken[nNodesAtT];
			for (int i=0;i<newtoks.length;i++) {
				newtoks[i]=new NBestToken(nbest);
				for (int j=0;j<toks.length;j++) {
					for (int k=0;k<toks[j].scoresUnnormalized.size();k++) {
						for (Map.Entry<Double, Set<WordResult>> e : nodes[so6].getConfusionSet(confset).entrySet())
							for (WordResult w : e.getValue())
								insertHypIntoToken(newtoks[i],toks[j].nbestUtterances.get(k),toks[j].scoresUnnormalized.get(k),toks[j].denominator.get(k),w.toString(),e.getKey());
					}
				}
			}
			if (++confset>=nodes[so6].size()) {
				confset=0;
				if (++so6>=nodes.length) break;
			}
			toks=newtoks;
		}

		// rassemble tous les noeuds terminaux
		for (int i=1;i<toks.length;i++)
			for (int j=0;j<toks[i].scoresUnnormalized.size();j++) {
				insertHypIntoToken(toks[0], toks[i].nbestUtterances.get(j),toks[i].scoresUnnormalized.get(j), toks[i].denominator.get(j),null,0);
			}

		// convertit les nbest en RecoUtteranceImmutable
		ArrayList<RecoUtteranceImmutable> res = new ArrayList<RecoUtteranceImmutable>();
		for (int i=0;i<toks[0].scoresUnnormalized.size();i++) {
			RecoUtterance u0 = new RecoUtterance();
			String[] ws = toks[0].nbestUtterances.get(i).split(" ");
			for (int j=0;j<ws.length;j++) {
				RecoWord w = new RecoWord();
				// tous les mots ont le meme score
				w.score=toks[0].denominator.get(i)>0?toks[0].scoresUnnormalized.get(i)/(float)toks[0].denominator.get(i):0;
				w.word = ws[j];
				u0.add(w);
			}
			RecoUtteranceImmutable u = RecoUtteranceImmutable.getUtterance(u0);
			res.add(u);
		}
		return res;
	}

	static List<RecoUtteranceImmutable> tokenPassing0(ArrayList<Set<RecoUtterance>> utts, int nbest) {
		// convertit tout en immutable
		ArrayList<RecoUtteranceImmutable>[] pos2utt = new ArrayList[utts.size()];
		for (int i=0;i<pos2utt.length;i++) {
			ArrayList<RecoUtteranceImmutable> uu = new ArrayList<RecoUtteranceImmutable>();
			pos2utt[i]=uu;
			for (RecoUtterance u : utts.get(i)) {
				uu.add(RecoUtteranceImmutable.getUtterance(u));
			}
			Collections.sort(uu);
		}

		// init
		ArrayList<RecoUtteranceImmutable> firstpos = pos2utt[0];
		// toks contient la liste des nbest dans chaque noeud du treillis de Viterbi
		ListeDeTokens[] toks = new ListeDeTokens[firstpos.size()];
		int i=0;
		for (RecoUtteranceImmutable u : firstpos) {
			toks[i]=new ListeDeTokens();
			toks[i++].add(u);
		}
		// iteration
		for (int t=1;t<pos2utt.length;t++) {
			ArrayList<RecoUtteranceImmutable> uttsAtT  = pos2utt[t];
			ListeDeTokens[] newtoks = new ListeDeTokens[uttsAtT.size()];
			for (i=0;i<newtoks.length;i++)
				newtoks[i]=new ListeDeTokens();
			// propagation des tokens
			for (int j=0;j<toks.length;j++) {
				i=0;
				for (RecoUtteranceImmutable u : uttsAtT) {
					ListeDeTokens prev = toks[j].clone();
					prev.ajoute(u);
					newtoks[i++].insertSortCut(prev,nbest);
				}
			}
			toks = newtoks;
		}
		// on concatene les solutions dans tous les etats finaux
		for (i=1;i<toks.length;i++)
			toks[0].insertSortCut(toks[i],nbest);
		return toks[0];
	}

	/**
	 * retourne les nbests dans l'ordre de posterior decroissant
	 */
	private Set<RecoUtterance> extractNBests(Sausage saucisse, int offset, int n) {
		HashSet<RecoUtterance> nbests = new HashSet<RecoUtterance>();
		// on trie tous les mots candidats par leur proba
		class MotProba implements Comparable<MotProba> {
			SimpleWordResult mot;
			double proba;
			int pos;
			@Override
			public int compareTo(MotProba o) {
				if (o.proba<proba) return -1;
				else if (o.proba>proba) return 1;
				return 0;
			}
		}
		ArrayList<MotProba> motprobs = new ArrayList<MotProba>();
		for (int i=0;i<saucisse.size();i++) {
			ConfusionSet cs = saucisse.getConfusionSet(i);
			for (Map.Entry<Double, Set<WordResult>> e : cs.entrySet()) {
				for (WordResult wr : e.getValue()) {
					MotProba mp = new MotProba(); mp.mot=(SimpleWordResult)wr;
					mp.proba=e.getKey(); mp.pos=i;
					motprobs.add(mp);
				}
			}
		}
		Collections.sort(motprobs);

		// les nbest sont calcules mot par mot
		for (int i=0;i<motprobs.size()&&i<n;i++) {
			MotProba candidat = motprobs.get(i);
			int pos = candidat.pos;

			RecoWord rr = new RecoWord(); rr.word=candidat.mot.toString();
			rr.frameDeb=candidat.mot.getStartFrame()+offset;
			rr.frameEnd=candidat.mot.getEndFrame()+offset;
			rr.score=(float)candidat.proba;

			// recopie hypothese de base
			RecoUtterance curres = new RecoUtterance();
			for (int j=0;j<saucisse.size();j++) {
				ConfusionSet cs = saucisse.getConfusionSet(j);
				WordResult wr = cs.getBestHypothesis();
				RecoWord r = new RecoWord();
				r.word=wr.toString();
				r.frameDeb=wr.getStartFrame()+offset;
				r.frameEnd=wr.getEndFrame()+offset;
				r.score=(float)cs.getBestPosterior();
				curres.add(r);
			}
			curres.set(pos,rr);
			for (int j=curres.size()-1;j>=0;j--) {
				RecoWord r = curres.get(j);
				if (r.word.length()==0 || r.word.equals("<noop>"))
					curres.remove(j);
			}
			nbests.add(curres);
		}
		return nbests;
	}

	private RecoUtterance extractOneBest(Sausage saucisse, int offset) {
		RecoUtterance curres = new RecoUtterance();
		for (int j=0;j<saucisse.size();j++) {
			ConfusionSet cs = saucisse.getConfusionSet(j);
			WordResult wr = cs.getBestHypothesis();
			RecoWord r = new RecoWord();
			r.word=wr.toString();
			if (r.word.length()==0 || r.word.equals("<noop>")) continue;
			r.frameDeb=wr.getStartFrame()+offset;
			r.frameEnd=wr.getEndFrame()+offset;
			r.score=(float)cs.getBestPosterior();
			curres.add(r);
		}
		return curres;
	}

	private int posskip=-1;
	private Sausage getBestSausageWithSkips(String ref, Sausage hyp) {
		NISTAlign localAligner = new NISTAlign(false, false);
		Sausage shyp=hyp;
		for (int pos=0;pos<shyp.size();) {
			System.out.println("gestbestsausage "+pos+" "+posskip+" "+shyp.size());
			// pour chaque confusion set qui a une transition "skip", on calcule l'accuracy
			// en supprimant ce confusion set.
			Sausage testhyp = delSkipTransAfter(shyp,pos);
			if (testhyp==null) {
				// plus de skips restants: shyp est la meilleure saucisse
				break;
			}
			// on test d'abord sans supprimer le noeud:
			localAligner.resetTotals();
			localAligner.alignSausage(ref,shyp);
			float werWithNode = localAligner.getTotalWordErrorRate();
			// puis en supprimant le noeud:
			localAligner.resetTotals();
			localAligner.alignSausage(ref,testhyp);
			if (localAligner.getTotalWordErrorRate()<werWithNode) {
				shyp=testhyp;
				pos=posskip;
			} else
				pos=posskip+1;
		}
		return shyp;
	}
	private Sausage delSkipTransAfter(Sausage s, int pos) {
		for (int i=pos;i<s.size();i++) {
			ConfusionSet cs = s.getConfusionSet(i);
			boolean hasSkip=false;
			b2:
				for (Map.Entry<Double, Set<WordResult>> e : cs.entrySet()) {
					for (WordResult wr : e.getValue()) {
						if (wr.toString().equals("<noop>")||wr.toString().length()==0) {
							hasSkip=true; break b2;
						}
					}
				}
			if (hasSkip) {
				Sausage news = new Sausage(s.size()-1);
				for (int j=0;j<i;j++) {
					cs = s.getConfusionSet(j);
					for (Map.Entry<Double, Set<WordResult>> e : cs.entrySet()) {
						for (WordResult wr : e.getValue()) {
							news.addWordHypothesis(j, wr.toString(), e.getKey(), wr.getLogMath());
						}
					}
				}
				for (int j=i+1;j<s.size();j++) {
					cs = s.getConfusionSet(j);
					for (Map.Entry<Double, Set<WordResult>> e : cs.entrySet()) {
						for (WordResult wr : e.getValue()) {
							news.addWordHypothesis(j-1, wr.toString(), e.getKey(), wr.getLogMath());
						}
					}
				}
				posskip=i;
				return news;
			}
		}
		return null;
	}

	static class DetNode {
		double proba;
		String word;
		LogMath logmath;
	};
	/**
	 * duplique les confusions sets avec des _-'...
	 */
	private static Sausage normaliseForAccuracy(Sausage s) {
		ArrayList<ArrayList<DetNode>> nodes = new ArrayList<ArrayList<DetNode>>();
		int pos=0;
		for (ConfusionSet cs : s) {
			// contient tous les nodes issus de la normalisation de ce confusion set
			ArrayList<ArrayList<DetNode>> oneCS2nodes = new ArrayList<ArrayList<DetNode>>();
			for (Map.Entry<Double, Set<WordResult>> e : cs.entrySet()) {
				for (WordResult wr : e.getValue()) {
					String word = wr.toString();
					// je ne supprime pas les <noop> car ils sont utilises dans le token passing
					// je ne fais que decouper - le reste de la normalisation est faite ensuite
					//					String word = SuiteDeMots.normalizeWord(w);
					// il faut decouper/dupliquer les nodes pour le scoring
					// ceci peut creer des chemins supplementaires, mais ce n'est pas grave pour le scoring
					String[] wd;
					if (word.charAt(0)=='<') {wd=new String[1]; wd[0]=word;}
					else wd = SuiteDeMots.normalizeNumbers(word).split("[ -=_\']");
					// ajoute aux nodes existants
					for (int wdi=0;wdi<wd.length;wdi++) {
						ArrayList<DetNode> newset;
						if (wdi<oneCS2nodes.size())
							newset = oneCS2nodes.get(wdi);
						else {
							newset = new ArrayList<DetNode>();
							oneCS2nodes.add(newset);
						}
						DetNode dn = new DetNode(); dn.word=wd[wdi]; dn.logmath=wr.getLogMath();
						if (wdi==0) dn.proba=e.getKey();
						else dn.proba=e.getKey()-20;
						newset.add(dn);
					}
				}
			}
			pos++;
			nodes.addAll(oneCS2nodes);
		}
		// cree la saucisse et
		// complete les nouveaux nodes avec des transitions vides (pour que som_prob=1)
		Sausage res = new Sausage(nodes.size());
		pos=0;
		LogMath logmath = null;
		for (ArrayList<DetNode> dns : nodes) {
			for (DetNode dn : dns) {
				if (logmath==null) logmath=dn.logmath;
				res.addWordHypothesis(pos, dn.word, dn.proba, dn.logmath);
			}
			pos++;
		}
		if (logmath!=null)
			res.fillInBlanks(logmath);
		return res;
	}

	private List<RecoWord> loadBestPhoneSeq(String filename, int wavoffset) {
		try {
			ArrayList<RecoWord> res = new ArrayList<RecoWord>();
			BufferedReader f= FileUtils.openFileUTF(filename);
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				String[] ss = s.split(" ");
				for (int i=0;i<ss.length;) {
					int deb = Integer.parseInt(ss[i++]);
					int fin = Integer.parseInt(ss[i++]);
					String mot = "";
					while (!ss[i].equals("##")) mot+=ss[i++]+" ";
					mot=mot.trim();
					// on saute les phonemes
					ArrayList<String> phones = new ArrayList<String>();
					for (i++;;i++) {
						if (i>=ss.length||ss[i].matches("^[0-9].*")) break;
						phones.add(ss[i]);
					}
					RecoWord r = new RecoWord();
					r.phones=phones.toArray(new String[phones.size()]);
					r.word=mot;
					r.frameDeb=deb+wavoffset;
					r.frameEnd=fin+wavoffset;
					res.add(r);
				}
			}
			return res;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private AlignementEtat getFullAlign(Result result) {
		Token tok = result.getBestToken();

		AlignementEtat al = AlignementEtat.backtrack(tok)[0];
		try {
			PrintWriter f = FileUtils.writeFileUTF("debug.tok");
//			al.save(f);
			f.close();
		} catch (Exception e) {
		}
		return al;
	}
	
	private void saveBestPhoneSeq(Result result, String filename) {
		try {
			Token tok = result.getBestToken();
			ArrayList<String> motsetpron = new ArrayList<String>();
			ArrayList<Integer> frfins = new ArrayList<Integer>();
			while (tok != null) {
				if (tok.isWord()) {
					WordSearchState wordState =
						(WordSearchState) tok.getSearchState();
					Pronunciation pron = wordState.getPronunciation();
					Word word = wordState.getPronunciation().getWord();
					Unit[] u = pron.getUnits();
					StringBuilder sb = new StringBuilder();
					for (int i = 0;i<u.length;i++)
						sb.append(u[i].getName()+" ");
					motsetpron.add(word.getSpelling()+" ## "+sb.toString());
					int frfin = tok.getFrameNumber();
					frfins.add(frfin);
				}
				tok = tok.getPredecessor();
			}
			PrintWriter f = FileUtils.writeFileUTF(filename);
			int frdeb=0;
			for (int i=motsetpron.size()-1;i>=0;i--) {
				int frfin = frfins.get(i);
				f.print(frdeb+" "+frfin+" "+motsetpron.get(i));
				frdeb=frfin+1;
			}
			f.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	NISTAlign tmpaligner = null;
	
	public String doRecoFromToks(String wav, String refString) {
		resRecoPublic.clear();
		resRecoFinalSaved = new RecoUtterance();

		List<FichSegments> onewavs = splitWavFile(wav, 15, 10);

		if (tmpaligner==null) tmpaligner=new NISTAlign(true, true);

		for (int i=0;i<onewavs.size();i++) {
			List<RecoWord> curres = loadBestPhoneSeq(wav+"_"+i+".toks",onewavs.get(i).offsetInFrames);

			System.out.print("RESTMP ");
			for (int j=0;j<curres.size();j++)
				System.out.print(curres.get(j)+" ");
			System.out.println();

			mergeSegmentsRecouvrants(curres);

			System.out.println("APRES MERGING");
			for (int j=0;j<resRecoPublic.size();j++)
				System.out.print(resRecoPublic.get(j)+" ");
			System.out.println();

			// on a le résultat final, on peut le recopier
			resRecoFinalSaved.clear();
			resRecoFinalSaved.addAll(resRecoPublic);
		}

		String[] rec = new String[resRecoPublic.size()];
		String ress = "";
		for (int i=0;i<rec.length;i++) {
			rec[i]=resRecoPublic.get(i).word;
			ress += rec[i]+" ";
		}

		System.out.println("aligner... "+rec.length);
		
		SuiteDeMots refs = new SuiteDeMots(refString);
		SuiteDeMots recs = new SuiteDeMots(rec);
		SuiteDeMots as = refs.normaliseForAccuracy();
		SuiteDeMots bs = recs.normaliseForAccuracy();
		tmpaligner.align(as.toString(),bs.toString());
		tmpaligner.printNISTSentenceSummary();
		tmpaligner.printTotalSummary();

		resRecoPublic.saveLab(FileUtils.noExt(wav)+".rec");

		ress = ress.replace("<noop>", "");

		return ress;
	}

	/**
	 * may be set to be warned whenever the next incremental sequence of words is found
	 */
	public ActionListener recoListener = null;
	List<FichSegments> onewavs;
	int wavchunk;
	String wavfich;
	
	public AudioFileDataSource getAudioStream(String wavfile) {
		URL audioURL;
		try {
			audioURL = new File(wavfile).toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		initSphinx4(cfg);
		AudioFileDataSource dataSource = (AudioFileDataSource) cm.lookup("audioFileDataSource");
		dataSource.setAudioFile(audioURL, null);
		return dataSource;
	}
	
	private void adjustOffset(List<RecoWord> res, int offtr) {
		for (RecoWord w : res) {
			w.frameDeb+=offtr;
			w.frameEnd+=offtr;
		}
	}
	
	/**
	 * Ceci est la baseline: calcul de l'accuracy en 1-best
	 * on obtient sur les 17 fichiers WER = 21.1 % sur 4939 mots
	 * 
	 * Cette fonction est appelée par JTrans lorsqu'on clique sur "AutoTranscript"
	 * 
	 * @param wav
	 * @param refString
	 * @return
	 */
	public String doReco(String wav, String refString) {
		wavfich=wav;
		// recores contient le résultat complet, après merging des segments recouvrants
		resRecoPublic.clear();
		resRecoFinalSaved = new RecoUtterance();

		System.out.println("do reco from "+wav);

		onewavs = splitWavFile(wav, 15, 10);
		for (wavchunk=0;wavchunk<onewavs.size();wavchunk++) {
			String wavfile = onewavs.get(wavchunk).segmentWavFileName;
			URL audioURL;
			try {
				audioURL = new File(wavfile).toURI().toURL();
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return null;
			}
			initSphinx4(cfg);
			AudioFileDataSource dataSource = (AudioFileDataSource) cm.lookup("audioFileDataSource");
			dataSource.setAudioFile(audioURL, null);

			Result result = launchSphinx4(null);
			
			List<RecoWord> curres = convertResult2RecoWords(result);
			adjustOffset(curres,onewavs.get(wavchunk).offsetInFrames);
			mergeSegmentsRecouvrants(curres);

			System.out.println("APRES MERGING");
			for (int j=0;j<resRecoPublic.size();j++)
				System.out.print(resRecoPublic.get(j)+" ");
			System.out.println();

			// on a le résultat final, on peut le recopier
			resRecoFinalSaved.clear();
			resRecoFinalSaved.addAll(resRecoPublic);

			if (recoListener!=null) {
				recoListener.actionPerformed(null);
			}
			
		}

		String[] rec = new String[resRecoPublic.size()];
		String ress = "";
		for (int i=0;i<rec.length;i++) {
			rec[i]=resRecoPublic.get(i).word;
			ress += rec[i]+" ";
		}

		NISTAlign aligner = ((AccuracyTracker)cm.lookup("accuracyTracker")).getAligner();
		SuiteDeMots refs = new SuiteDeMots(refString);
		SuiteDeMots recs = new SuiteDeMots(rec);
		SuiteDeMots as = refs.normaliseForAccuracy();
		SuiteDeMots bs = recs.normaliseForAccuracy();
		aligner.align(as.toString(),bs.toString());
		aligner.printNISTSentenceSummary();
		aligner.printTotalSummary();

		resRecoPublic.saveLab(FileUtils.noExt(wav)+".rec");

		ress = ress.replace("<noop>", "");

		return ress;
	}
	
	
	public AlignementEtat fullalign=null;
	
	private List<RecoWord> convertResult2RecoWords(Result result) {
		ArrayList<RecoWord> curres = new ArrayList<RecoWord>();
		
		if (true) {
			// version qui utilise le best path
			Token tok = result.getBestToken();

			ArrayList<String> mots = new ArrayList<String>();
			// liste des phonemes par mot
			ArrayList<String[]> phPerMot = new ArrayList<String[]>();
			ArrayList<Integer> motsTrFin = new ArrayList<Integer>();
			// quel phoneme aligne avec chaque trame ?
			ArrayList<String> phPerTr = new ArrayList<String>();
			// quel etat ?
			ArrayList<Integer> statePerTr = new ArrayList<Integer>();
			// quel GMM ?
			ArrayList<GaussianMixture> GMMPerTr = new ArrayList<GaussianMixture>();
			
			while (tok != null) {
				if (tok.isWord()) {
					WordSearchState wordState = (WordSearchState) tok.getSearchState();
					Pronunciation pron = wordState.getPronunciation();
					Word word = wordState.getPronunciation().getWord();
					Unit[] u = pron.getUnits();
					String[] phs = new String[u.length];
					for (int i = 0;i<u.length;i++)
						phs[i]=u[i].getName();
					phPerMot.add(0,phs);
					mots.add(0, word.getSpelling());
					motsTrFin.add(0,tok.getFrameNumber());
				}
				if (tok.getSearchState()!=null && tok.isEmitting()) {
					SearchState etatNetwork = tok.getSearchState();
					HMMState etatHMM=null;
					if (etatNetwork instanceof HMMStateState) {
						etatHMM = ((HMMStateState)etatNetwork).getHMMState();
					}  else if (etatNetwork instanceof LexTreeHMMState) {
						LexTreeHMMState etatnet = (LexTreeHMMState)etatNetwork;
						etatHMM = etatnet.getHMMState();
					} else {
						System.err.println("ERROR: alignement etat non pris en charge: "+etatNetwork.getClass().getName());
					}
					if (etatHMM!=null) {
						phPerTr.add(0,etatHMM.getHMM().getUnit().toString());
						statePerTr.add(0,etatHMM.getState());
						if (etatHMM instanceof SenoneHMMState) {
							Senone senone = ((SenoneHMMState)etatHMM).getSenone();
							if (senone instanceof GaussianMixture) {
								GMMPerTr.add(0,(GaussianMixture)senone);
							} else {
								System.err.println("ERROR: alignement senone non pris en charge: "+senone.getClass().getName());
							}
						} else {
							System.err.println("ERROR: alignement HMMetat non pris en charge: "+etatHMM.getClass().getName());
						}
					}

				}
				tok = tok.getPredecessor();
			}
			// creation de la liste de mots
			int trdeb=0;
			for (int i=0;i<mots.size();i++) {
				RecoWord w = new RecoWord();
				w.word=mots.get(i);
				w.phones=phPerMot.get(i);
				w.frameDeb=trdeb;
				int trfin = motsTrFin.get(i);
				w.frameEnd=trfin;
				w.phstates=new String[trfin-trdeb];
				for (int t=0;t<trfin-trdeb;t++) {
					w.phstates[t]=phPerTr.get(t+trdeb)+":"+statePerTr.get(t+trdeb);
				}
				curres.add(w);
				trdeb=trfin;
			}
		} else {
			
			{
				MAPConfidenceScorer scorer = new MAPConfidenceScorer(1f, false, false);
				ConfidenceResult confidenceResult = scorer.score(result);
				// on a les mots avec les confiances, on veut aussi les tokens de fin de chaque mot
				// pour pouvoir calculer l'alignement trame par trame
		        List<Token> wordTokens = new LinkedList<Token>();
		        Token token = result.getBestToken();
		        // je veux les trames de fin du mot !
		        wordTokens.add(token);
		        while (token != null) {
		        	boolean newword = token.isWord();
		            token = token.getPredecessor();
		            if (newword) wordTokens.add(0, token);
		        }

				Path bestPath = confidenceResult.getBestHypothesis();
				WordResult[] words = bestPath.getWords();
				assert words.length==wordTokens.size();
				
				for (int i = 0; i < words.length; i++) {
					WordResult wordResult = (WordResult) words[i];
					double wordConfidence = wordResult.getConfidence();
					Token tok = wordTokens.get(i);
					// TODO: verifier que le mot courant est le meme dans tok et dans wordResult
					RecoWord w = new RecoWord();
					w.word=tok.getWord().getSpelling();
					w.score=(float)wordConfidence;
					w.frameEnd=wordResult.getEndFrame();
					w.frameDeb=wordResult.getStartFrame();
					
					assert tok.getFrameNumber()==w.frameEnd-1;
					
					// backtrack pour avoir les phones
					ArrayList<String> phst = new ArrayList<String>();
					while (tok!=null&&!tok.isWord()) {
						String s = AlignementEtat.getInfoOneFrame(tok);
						String[] ss = s.split(":");
						
						if (ss[0].length()>0) {
							// debut du mot !
							WordSearchState wordState = (WordSearchState) tok.getSearchState();
							Pronunciation pron = wordState.getPronunciation();
							Unit[] u = pron.getUnits();
							String[] phs = new String[u.length];
							for (int ii = 0;ii<u.length;ii++)
								phs[ii]=u[ii].getName();
							w.phones=phs;
							phst.add(ss[1]+":"+ss[2]);
							assert ss[0].equals(w.word);
							break;
						}
						tok=tok.getPredecessor();
					}
					w.phstates=phst.toArray(new String[phst.size()]);
					assert w.phstates.length==w.frameEnd-w.frameDeb;
					curres.add(w);
				}
			}
			
			
			// premiere version
			
			// version qui utilise le lattice
			fullalign = getFullAlign(result);
//			saveBestPhoneSeq(result, wavfich+"_"+wavchunk+".toks");
			Lattice lat = new Lattice(result);
			LatticeOptimizer optim = new LatticeOptimizer(lat);
			optim.optimize();

			// est-ce que la insertion penalty est prise en compte dans le LM des Nodes du Lattice ? ==> oui !
			lat.computeNodePosteriors(1);
			// affichage des Nodes du Lattice
			List<Node> bp = lat.getViterbiPath();
			for (Node n : bp)
				System.out.println(n.toString()+"="+n.getViterbiScore());
			// sauvegarde du Lattice
			lat.dump(wavfich+"_"+wavchunk+".lat");

			ArrayList<String> words = new ArrayList<String>();
			ArrayList<Integer> frdeb = new ArrayList<Integer>();
			ArrayList<Integer> frend = new ArrayList<Integer>();
			
			// ci-dessous: ne retourne rien ? 
			// result.getBestWordsWithFrames(words, frdeb, frend);
			// alternative:
			for (Node n : bp) {
				words.add(n.getWord().getSpelling());
				frdeb.add(n.getBeginTime());
				frend.add(n.getEndTime());
			}
			for (int j=0;j<words.size();j++) {
				RecoWord r = new RecoWord();
				r.word=words.get(j);
				r.frameDeb=frdeb.get(j)+onewavs.get(wavchunk).offsetInFrames;
				r.frameEnd=frend.get(j)+onewavs.get(wavchunk).offsetInFrames;
				curres.add(r);
			}
		}
		return curres;
	}
	
	public String doRecoFromLat(String wav, String refString) {
		resRecoPublic.clear();

		List<FichSegments> onewavs = splitWavFile(wav, 15, 10);

		for (int i=0;i<onewavs.size();i++) {
			String wavfile = onewavs.get(i).segmentWavFileName;

			Lattice lat = new Lattice(wav+"_"+i+".lat");

			lat.computeNodePosteriors(1);
			List<Node> path = lat.getViterbiPath();

			ArrayList<RecoWord> curres = new ArrayList<RecoWord>();
			for (int j=0;j<path.size();j++) {
				RecoWord r = new RecoWord();
				r.word=path.get(j).getWord().getSpelling();
				r.frameDeb=path.get(j).getBeginTime()+onewavs.get(i).offsetInFrames;
				r.frameEnd=path.get(j).getEndTime()+onewavs.get(i).offsetInFrames;
				curres.add(r);
			}

			System.out.print("RESTMP ");
			for (int j=0;j<curres.size();j++)
				System.out.print(curres.get(j)+" ");
			System.out.println();

			mergeSegmentsRecouvrants(curres);

			System.out.println("APRES MERGING");
			for (int j=0;j<resRecoPublic.size();j++)
				System.out.print(resRecoPublic.get(j)+" ");
			System.out.println();

		}

		String[] rec = new String[resRecoPublic.size()];
		String ress = "";
		for (int i=0;i<rec.length;i++) {
			rec[i]=resRecoPublic.get(i).word;
			ress += rec[i]+" ";
		}

		resRecoPublic.saveLab(FileUtils.noExt(wav)+".rec");

		ress = ress.replace("<noop>", "");

		return ress;
	}

	public String doRecoMFCC(String mfcc, String refString, boolean isBigEndian) {
		resRecoPublic.clear();

		System.out.println("RECOMFCC ");
		initSphinx4(cfg);

		StreamCepstrumSource dataSource = (StreamCepstrumSource) cm.lookup("mfccFileDataSource");
		try {
			dataSource.setInputStream(new FileInputStream(mfcc), isBigEndian);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		Result result = launchSphinx4(null);
		ArrayList<String> words = new ArrayList<String>();
		ArrayList<Integer> frdeb = new ArrayList<Integer>();
		ArrayList<Integer> frend = new ArrayList<Integer>();
//		result.getBestWordsWithFrames(words, frdeb, frend);
		ArrayList<RecoWord> curres = new ArrayList<RecoWord>();
		for (int j=0;j<words.size();j++) {
			RecoWord r = new RecoWord();
			r.word=words.get(j);
			r.frameDeb=frdeb.get(j);
			r.frameEnd=frend.get(j);
			curres.add(r);
		}

		System.out.print("RESTMP ");
		for (int j=0;j<curres.size();j++)
			System.out.print(curres.get(j)+" ");
		System.out.println();

		resRecoPublic.addAll(curres);

		String[] rec = new String[resRecoPublic.size()];
		String ress = "";
		for (int i=0;i<rec.length;i++) {
			rec[i]=resRecoPublic.get(i).word;
			ress += rec[i]+" ";
		}

		NISTAlign aligner = ((AccuracyTracker)cm.lookup("accuracyTracker")).getAligner();
		SuiteDeMots refs = new SuiteDeMots(refString);
		SuiteDeMots recs = new SuiteDeMots(rec);
		SuiteDeMots as = refs.normaliseForAccuracy();
		SuiteDeMots bs = recs.normaliseForAccuracy();
		aligner.align(as.toString(),bs.toString());
		aligner.printNISTSentenceSummary();
		aligner.printTotalSummary();

		resRecoPublic.saveLab(FileUtils.noExt(mfcc)+".rec");

		ress = ress.replace("<sil>","").replace("</s>", "").replace("<s>", "").replace("<noop>", "");

		return ress;
	}

	public String doRecoWhole(String wavfile, String refString) {
		URL audioURL;
		try {
			audioURL = new File(wavfile).toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		AudioFileDataSource dataSource = (AudioFileDataSource) cm.lookup("audioFileDataSource");
		dataSource.setAudioFile(audioURL, null);

		/* This method will return when the end of speech
		 * is reached. Note that the endpointer will determine
		 * the end of speech.
		 */
		String ress="";
		for (;;) {
			Result result;
			if (refString==null)
				result = launchSphinx4(null);
			else
				result = launchSphinx4(refString);

			String resultText = result.getBestResultNoFiller();
			System.out.println("DEBUGRES "+resultText);
			ress+=resultText+" ";
			if (result.isFinal()) {
				break;
			}
		}
		return ress;

		//        System.out.println("rec: "+resultText);
		//        System.out.println(result.getBestPronunciationResult());
		//        Token t = result.getBestToken();
		//        while (t!=null) {
		//        	SearchState ss = t.getSearchState();
		//        	if (ss!=null)
		//        		System.out.println("token "+t.getFrameNumber()+" "+t.getSearchState().toPrettyString());
		//        	t=t.getPredecessor();
		//        }
	}

	public static String normalize(String s) {
		String s1 = s.replace('_', ' ');
		return s1;
	}
	
	// ne sert a rien pour le moment; sera utile pour post-processer le resultat de la reco afin de pouvoir calculer un WER
	public static String postProcess(String transcription) {
		return null;
	}
	

	public static void main(String args[]) throws Exception {
		File f = new File(".");
		System.out.println("basedir "+f.getAbsolutePath());
		boolean isBigEndian=true;

		String cfg = null;
		String wavlist = null, mfclist=null;

		if (args.length==0) {
			System.out.println("Usage:");
			System.out.println("java -Xmx1000m -cp jtrans.jar:sphinx4.jar main.SpeechReco -cfg config.xml -wavlist inputs.wavl");;
			System.out.println("ou");
			System.out.println("java -Xmx1000m -cp jtrans.jar:sphinx4.jar main.SpeechReco -cfg config.xml -mfcclist inputs.mfcl [-le]");;
			System.out.println();
			System.out.println("les fichiers .wavl ou .mfcl contiennent sur chaque ligne le nom d'un fichier suivi eventuellement de la chaine de référence (séparateurs = espace)");
			System.exit(1);
		}

		for (int i=0;i<args.length;i++) {
			if (args[i].equals("-cfg")) {
				cfg = args[++i];
			} else if (args[i].equals("-wavlist")) {
				wavlist = args[++i];
			} else if (args[i].equals("-mfclist")) {
				mfclist = args[++i];
			} else if (args[i].equals("-le")) {
				isBigEndian=false;
			}
		}

		SpeechReco asr;
		System.out.println("allocating sphinx4...");
		if (cfg==null) {
			asr = new SpeechReco();
		} else
			asr = new SpeechReco(new File(cfg).toURI().toURL());

		if (wavlist!=null) {
			BufferedReader fin = new BufferedReader(new FileReader(wavlist));
			for (;;) {
				String s = fin.readLine();
				if (s==null) break;
				int i = s.indexOf(' ');
				String res;
				if (i>=0) {
					res = asr.doReco(s.substring(0, i), s.substring(i+1));
				} else {
					res = asr.doReco(s,null);
				}
				System.out.println("RECO "+res);
			}
			fin.close();
		} else if (mfclist!=null) {
			BufferedReader fin = new BufferedReader(new FileReader(mfclist));
			for (;;) {
				String s = fin.readLine();
				if (s==null) break;
				int i = s.indexOf(' ');
				String res;
				if (i>=0) {
					res = asr.doRecoMFCC(s.substring(0, i), s.substring(i+1),isBigEndian);
				} else {
					res = asr.doRecoMFCC(s,null,isBigEndian);
				}
				System.out.println("RECO "+res);
			}
			fin.close();
		}

	}
}
