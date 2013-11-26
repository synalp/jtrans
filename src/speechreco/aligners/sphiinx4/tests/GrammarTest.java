/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package speechreco.aligners.sphiinx4.tests;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;

import speechreco.aligners.sphiinx4.HMMModels;
import speechreco.aligners.sphiinx4.PhoneticForcedGrammar;
import edu.cmu.sphinx.decoder.Decoder;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.decoder.pruner.Pruner;
import edu.cmu.sphinx.decoder.pruner.SimplePruner;
import edu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer;
import edu.cmu.sphinx.decoder.search.PartitionActiveListFactory;
import edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager;
import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.GaussianMixture;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.MixtureComponent;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Senone;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneHMM;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneSequence;
import edu.cmu.sphinx.linguist.flat.FlatLinguist;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.LogMath;
import junit.framework.TestCase;

public class GrammarTest extends TestCase {
	public void test() {
		PhoneticForcedGrammar gram;
		try {
			gram = new PhoneticForcedGrammar();
			String rule = "( i | [ au ] ko ( po | a ) | a )";
			GrammarNode n[]=gram.convertRule2S4(rule, HMMModels.getLogMath());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private Data getDataFor(String phone) {
		Iterator<Unit> it = mods.getContextIndependentUnitIterator();
		Unit u = null;
		while (it.hasNext()) {
			Unit unit = it.next();
			if (unit.getName().equals(phone)) {
				u=unit; break;
			}
		}
		if (u==null) {
			System.out.println("ERROR unit not found "+phone);
			return null;
		} else {
			HMM hmm = mods.lookupNearestHMM(u, HMMPosition.UNDEFINED, false);
			if (hmm instanceof SenoneHMM) {
				SenoneHMM shmm = (SenoneHMM)hmm;
				SenoneSequence sseq = shmm.getSenoneSequence();
				Senone[] sens = sseq.getSenones();
				if (sens==null||sens.length==0) {
					System.out.println("NO SENONE "+shmm);
					return null;
				} else {
					if (sens[0] instanceof GaussianMixture) {
						GaussianMixture gmm = (GaussianMixture)sens[0];
						float[] weights = gmm.getComponentWeights();
						int imax=0;
						for (int i=1;i<weights.length;i++)
							if (weights[i]>weights[imax]) imax=i;
						MixtureComponent[] gaussians = gmm.getMixtureComponents();
						float[] x = gaussians[imax].getMean();
						double[] xx = new double[x.length];
						for (int i=0;i<x.length;i++)
							xx[i]=x[i];
						Data d = new DoubleData(xx);
						return d;
					} else {
						System.out.println("not a gaussian mixture "+sens[0].getClass().getName());
						return null;
					}
				}
			} else {
				System.out.println("HMM no senones ! "+hmm.getClass().getName());
				return null;
			}
		}
	}
	AcousticModel mods;
	
	/**
	 * test d'alignement force sur 2 phonemes avec un front-end artificiel qui generer les moyennes des gaussiennes
	 */
	public void test2() {
		PhoneticForcedGrammar gram=null;
		try {
			gram = new PhoneticForcedGrammar();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		String rule = "a i";
		LogMath logMath = HMMModels.getLogMath();
		GrammarNode n[]=gram.convertRule2S4(rule, logMath);
		n[1].setFinalNode(true);
		System.out.println("gram");
		n[0].dump();
		System.out.println("gram2");
		gram.setGram(n[0]);
		
		mods = HMMModels.getAcousticModels();
		FlatLinguist linguist = new FlatLinguist(mods, logMath, gram, HMMModels.getUnitManager(), 1f, 1f, 1f, 1f, 1f, false, false, false, false, 1f, 1f, mods);

		BaseDataProcessor mfcc = new BaseDataProcessor() {
			Data[] datas = null;
			int fr=0;
			@Override
			public Data getData() throws DataProcessingException {
				if (datas==null) {
					datas=new Data[20];
					datas[0]=new DataStartSignal(100000);
					Data da = getDataFor("a");
					Data di = getDataFor("i");
					Data dsil = getDataFor("SIL");
					for (int i=1;i<4;i++) datas[i]=dsil;
					for (int i=4;i<10;i++) datas[i]=da;
					for (int i=10;i<16;i++) datas[i]=di;
					for (int i=16;i<19;i++) datas[i]=dsil;
					datas[19]=new DataEndSignal(100);
				}
				if (fr<datas.length) return datas[fr++];
				else return null;
			}
		};

		Pruner pruner = new SimplePruner();
		ThreadedAcousticScorer scorer = new ThreadedAcousticScorer(mfcc, null, 1, false, 1, Thread.NORM_PRIORITY);
		PartitionActiveListFactory activeList = new PartitionActiveListFactory(50, 1E-80, logMath);
		// je n'utilise pas un WordBreadth... car on travaille ici avec des phonemes et non des mots !
		SimpleBreadthFirstSearchManager searchManager = new SimpleBreadthFirstSearchManager(logMath, linguist, pruner, scorer, activeList, false, 1E-60, 0, false);
		ArrayList<ResultListener> listeners = new ArrayList<ResultListener>();
		Decoder decoder = new Decoder(searchManager, true, true, listeners, 50);
		
		Result res;
		for (;;) {
			res = decoder.decode(null);
			if (res.isFinal()) 
				break;
		}
		
//		Token tok = res.getBestToken();
//		while (tok!=null) {
//			System.out.println("TOK "+tok+" w="+tok.isWord()+" e="+tok.isEmitting()+" f="+tok.isFinal());
//			tok=tok.getPredecessor();
//		}
		
		{
			ArrayList<String> words = new ArrayList<String>();
			ArrayList<Integer> debframes = new ArrayList<Integer>();
			ArrayList<Integer> endframes = new ArrayList<Integer>();
//			res.getBestWordsWithFrames(words, debframes, endframes);
			for (int i=0;i<words.size();i++) {
				System.out.println("resi "+i+" "+words.get(i)+" "+debframes.get(i)+" "+endframes.get(i));
			}
			assertEquals(4,words.size());
			assertTrue(endframes.get(0)==3);
			assertTrue(endframes.get(1)==9);
			assertTrue(endframes.get(2)==15);
			assertTrue(endframes.get(3)==18);
		}
	}
	
	public static void main(String[] args) {
		GrammarTest m = new GrammarTest();
		m.test2();
	}
}
