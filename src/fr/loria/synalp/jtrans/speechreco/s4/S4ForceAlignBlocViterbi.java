/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

/*
Copyright Christophe Cerisara, Josselin Pierre (1er septembre 2008)

cerisara@loria.fr

Ce logiciel est un programme informatique servant � aligner un
corpus de parole avec sa transcription textuelle.

Ce logiciel est r�gi par la licence CeCILL-C soumise au droit fran�ais et
respectant les principes de diffusion des logiciels libres. Vous pouvez
utiliser, modifier et/ou redistribuer ce programme sous les conditions
de la licence CeCILL-C telle que diffus�e par le CEA, le CNRS et l'INRIA 
sur le site "http://www.cecill.info".

En contrepartie de l'accessibilit� au code source et des droits de copie,
de modification et de redistribution accord�s par cette licence, il n'est
offert aux utilisateurs qu'une garantie limit�e.  Pour les m�mes raisons,
seule une responsabilit� restreinte p�se sur l'auteur du programme,  le
titulaire des droits patrimoniaux et les conc�dants successifs.

A cet �gard  l'attention de l'utilisateur est attir�e sur les risques
associ�s au chargement,  � l'utilisation,  � la modification et/ou au
d�veloppement et � la reproduction du logiciel par l'utilisateur �tant 
donn� sa sp�cificit� de logiciel libre, qui peut le rendre complexe � 
manipuler et qui le r�serve donc � des d�veloppeurs et des professionnels
avertis poss�dant  des  connaissances  informatiques approfondies.  Les
utilisateurs sont donc invit�s � charger  et  tester  l'ad�quation  du
logiciel � leurs besoins dans des conditions permettant d'assurer la
s�curit� de leurs syst�mes et ou de leurs donn�es et, plus g�n�ralement, 
� l'utiliser et l'exploiter dans les m�mes conditions de s�curit�. 

Le fait que vous puissiez acc�der � cet en-t�te signifie que vous avez 
pris connaissance de la licence CeCILL-C, et que vous en avez accept� les
termes.
 */

package fr.loria.synalp.jtrans.speechreco.s4;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import edu.cmu.sphinx.decoder.FrameDecoder;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.decoder.pruner.Pruner;
import edu.cmu.sphinx.decoder.pruner.SimplePruner;
import edu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer;
import edu.cmu.sphinx.decoder.search.PartitionActiveListFactory;
import edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DataStartSignal;
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
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.flat.FlatLinguist;
import edu.cmu.sphinx.util.LogMath;
import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.utils.StdoutProgressDisplay;

/**
 * utiliser Sphinx4 pour aligner et pour n'avoir qu'un seul ensemble de HMMs
 * en memoire...
 * 
 * Je n'utilise pas la methode des "configs XML" preconisee par Sphinx4, car
 * je veux pouvoir utiliser les memes HMMs dans differentes configurations
 * (forced-align et LVCSR) sans avoir a les recharger.
 * J'utilise donc une approche de type "programmation"
 * 
 * @author cerisara
 *
 */
public class S4ForceAlignBlocViterbi extends Thread {
	private ProgressDisplay progress;

	//	S4RoundBufferFrontEnd mfccs = null;
	public S4mfccBuffer mfccs = null;
	public FrameDecoder decoder=null;
	PhoneticForcedGrammar grammar = null;
	String[] mots;
	public SimpleBreadthFirstSearchManager searchManager = null;

	public final int NBMOTS = 20;
	private String wavname=null;
	private AudioFileDataSource wavfile;
	public Microphone mikeSource;

	/**
	 * contient (1) la 1ere trame (2) le 1er mot
	 */
	public ArrayBlockingQueue<S4AlignOrder> input2process = new ArrayBlockingQueue<S4AlignOrder>(10);

	public void setMots(String[] ms) {
		mots=ms;
	}

	public static S4ForceAlignBlocViterbi getS4Aligner(String wavname, ProgressDisplay progress) {
		if (aligner == null) {
			aligner = new S4ForceAlignBlocViterbi(wavname, progress);
			aligner.start();
		} else if (wavname==null) {
			return aligner;
		} else if (!wavname.equals(aligner.wavname)) {
			aligner.setNewAudioFile(wavname);
		}
		return aligner;
	}
	private static S4ForceAlignBlocViterbi aligner = null;

	private S4ForceAlignBlocViterbi(String wavname, ProgressDisplay progress) {
		super("Sphinx4 force-align Bloc Viterbi");
		this.wavname=wavname;
		this.progress = progress;

		System.out.println("create S4aligner with "+wavname+" "+Arrays.toString(mots));

		if (wavname!=null) {
			progress.setIndeterminateProgress("Initializing grammar...");
			initS4();
		}
	}

	public void setNewAudioFile(String wavname) {
		this.wavname=wavname;
		if (wavname==null) {
			// use mike
			initS4Mike();
			// shall be called AFTER having defined the recognition grammar (with setMots()), because it starts immediately the mike !
		} else {
			mfccs.clear();
			wavfile.setAudioFile(new File(wavname), null);
		}
	}

	public static FrontEnd getFrontEnd(DataProcessor source) {
		ArrayList<DataProcessor> frontEndList = new ArrayList<DataProcessor>();
		frontEndList.add(source);
		frontEndList.add(new Dither(2,false,Double.MAX_VALUE,-Double.MAX_VALUE));
		frontEndList.add(new DataBlocker(50));
		frontEndList.add(new Preemphasizer(0.97));
		frontEndList.add(new RaisedCosineWindower(0.46f,25.625f,10f));
		frontEndList.add(new DiscreteFourierTransform(512, false));
		frontEndList.add(new MelFrequencyFilterBank(133.33334, 6855.4976, 40));
		frontEndList.add(new DiscreteCosineTransform(40,13));
		frontEndList.add(new LiveCMN(12,100,160));
		frontEndList.add(new DeltasFeatureExtractor(3));
		return new FrontEnd(frontEndList);
	}

	private void initS4Mike() {
		mikeSource = new Microphone(16000, 16, 1, true, true, false, 10, false, "average", 0, "default", 6400);
		mikeSource.initialize();
		mfccs = new S4mfccBuffer();
		mfccs.setSource(getFrontEnd(mikeSource));
	}

	private void initS4() {
		wavfile = new AudioFileDataSource(3200,null);
		System.out.println("wavname "+wavname);
		wavfile.setAudioFile(new File(wavname), null);
		mfccs = new S4mfccBuffer();
		mfccs.setSource(getFrontEnd(wavfile));

		if (false) {
			// debug pour connaitre le nb de trames
			int nfr=0;
			while (!mfccs.noMoreFramesAvailable) {
				Data x = mfccs.getData();
				if (x instanceof DataStartSignal) {
				} else if (x instanceof DataEndSignal) {
				} else
					nfr++;
			}
			System.out.println("Nb of frames: "+nfr);
		}

		try {
			grammar = new PhoneticForcedGrammar();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static float silprob = 0.1f;
	public static int beamwidth = 0;

	private void initNewGrammar(List<String> words) {
		grammar.setWords(words, progress);

		if (decoder==null) {
			progress.setIndeterminateProgress("Initializing Sphinx...");

			LogMath logMath = HMMModels.getLogMath();
			AcousticModel mods = HMMModels.getAcousticModels();
			FlatLinguist linguist = new FlatLinguist(mods, logMath, grammar, HMMModels.getUnitManager(), 1f, silprob, silprob, 1f, 1f, false, false, false, false, 1f, 1f, mods);

			Pruner pruner = new SimplePruner();
			ThreadedAcousticScorer scorer = new ThreadedAcousticScorer(mfccs, null, 1, false, 1, Thread.NORM_PRIORITY);

			//					PartitionActiveListFactory activeList = new PartitionActiveListFactory(50, 1E-80, logMath);
			PartitionActiveListFactory activeList = new PartitionActiveListFactory(beamwidth, 1E-300, logMath);

			// je n'utilise pas un WordBreadth... car on travaille ici avec des phonemes et non des mots !
			searchManager = new SimpleBreadthFirstSearchManager(logMath, linguist, pruner, scorer, activeList, false, 1E-60, 0, false);

			ArrayList<ResultListener> listeners = new ArrayList<ResultListener>();

			//		decoder = new Decoder(searchManager, true, true, listeners, 50);

			decoder = new FrameDecoder(searchManager, false, true, listeners);

		} // sinon la grammaire est chargee dynamiquement
	}

	// je suppose que l'alignement est complet (a été jusqu'au bout)
	public static Alignment segmentePhonesEnMots(Alignment alignPhone) {
		// on a une liste de phonemes et non pas une liste de mots !
		// il faut retrouver les mots a partir des phonemes...
		System.out.println("liste phones:");
		System.out.println(alignPhone.toString());
		// on transforme maintenant la liste des phonemes en mots
		List<String> phones = new ArrayList<String>();
		for (int i=0;i<alignPhone.getNbSegments();i++)
			phones.add(alignPhone.getSegmentLabel(i));
		List<Integer> ends = new ArrayList<Integer>();
		List<Integer> starts = new ArrayList<Integer>();
		for (int i=0;i<phones.size();i++) ends.add(alignPhone.getSegmentEndFrame(i));
		for (int i=0;i<phones.size();i++) starts.add(alignPhone.getSegmentDebFrame(i));
		
		Alignment alignMots = new Alignment();
		int isinmot = -1;
		int prevwi=-1;
		int firstSegOfWord = -1;
		for (int i=0;i<phones.size();i++) {
			if (phones.get(i).equals("SIL")) {
				if (isinmot>=0) {
					// SIL indique la fin d'un mot, car SIL ne peut que separer deux mots
					alignMots.addRecognizedSegment("XZ"+isinmot, starts.get(firstSegOfWord), ends.get(i-1), null, null);
					isinmot=-1;
				}
				// on ajoute les silences entre les mots
				alignMots.addRecognizedSegment("SIL", starts.get(i), ends.get(i), null, null);
				firstSegOfWord=-1;
			} else if (phones.get(i).startsWith("XZ")) {
				// indice du mot:
				String us = phones.get(i).substring(2);
				int u = us.indexOf('Y');
				assert u>0;
				int wi = Integer.parseInt(us.substring(0,u));
				if (isinmot>=0) {
					// cela peut etre le debut d'un nouveau mot, ou le 2eme phone d'un mot qui commence par un phone optionnel !
					// TODO : ATTENTION ! ceci nous interdit d'avoir 2 fois le meme mot de suite !
					if (wi!=prevwi) {
						// on ajoute le mot precedent
						alignMots.addRecognizedSegment("XZ"+isinmot, starts.get(firstSegOfWord), ends.get(i-1), null, null);
					}
				} // sinon, on a surement un SIL devant, et on a donc deja ajoute le mot precedent
				prevwi=wi;
				isinmot=wi;
				firstSegOfWord=i;
			}
		}
		if (isinmot>=0) {
			alignMots.addRecognizedSegment("XZ"+isinmot, starts.get(firstSegOfWord), ends.get(phones.size()-1), null, null);
			return alignMots;
		} else {
			// si on n'ajoute aucun vrai mot, alors il faut retourner null, sinon autoaligner bouclera indefiniment
			// dans le cas ou le dernier mot est optionnel
			if (prevwi<0) return null;
			else return alignMots;
		}
	}

	@Override
	public void run() {
		int oneBlocLen = 500;
		//		Adaptation adapter = new Adaptation();
		try {
			for (;;) {
				S4AlignOrder order = input2process.take();
				if (order==S4AlignOrder.terminationOrder) {
					System.out.println("fin du S4Aligner detectee");
					synchronized (order) {
						order.notifyAll();
					}
					break;
				}
				if (wavname==null) {
					continue;
				}
				int firstFrame = order.getFirstFrame();
				if (firstFrame<0) break;
				int firstWord = order.getFirstMot();
				if (firstWord<0) firstWord=0;
				int nbmots = NBMOTS;
				int lastfr = -1;
				if (!order.isBlocViterbi) {
					int lastMot = order.getLastMot();
					System.out.println("batch align in S4: "+firstWord+" "+lastMot);
					if (lastMot>=firstWord) {
						nbmots = lastMot+1-firstWord;
						// on veut un real Viterbi complet, et non pas un bloc Viterbi
						lastfr = order.getLastFrame();
						assert lastfr==-1||lastfr>firstFrame;
					}
				}

				// boucle au cas ou on ne trouverait pas de reponse avec cette taille de bloc
				for (;;) {
					// +++++++++++++++++++++++++++++++++++++++++
					// positionne le buffer MFCC
					System.out.println("start align fr="+firstFrame+" word="+firstWord+" blocsize="+oneBlocLen);
					mfccs.gotoFrame(firstFrame);
					mfccs.firstCall=true;

					// +++++++++++++++++++++++++++++++++++++++++
					// construction des mots a partir du debut
					ArrayList<String> ms = new ArrayList<String>();
					for (int i=firstWord;i<mots.length&&i-firstWord<nbmots;i++)
						ms.add(mots[i]);
					System.out.println("grammar "+ms.get(0)+"..."+ms.get(ms.size()-1));
					initNewGrammar(ms);
					searchManager.startRecognition();

					//					if (beamwidth==0) SimpleBreadthFirstSearchManager.noPruning=true;
					//					else SimpleBreadthFirstSearchManager.noPruning=false;

					if (order.isBlocViterbi) {
						for (int t=0;t<oneBlocLen&&!mfccs.noMoreFramesAvailable;t++) {
							decoder.decode(null);
						}
					} else {
						System.out.println("force-align "+firstFrame+" "+lastfr);
						if (lastfr==-1) {
							for (int t=0;!mfccs.noMoreFramesAvailable;t++) {
								decoder.decode(null);
								//								System.out.println("debug frame "+t+" activelist:");
								//								for (Token tok : searchManager.getActiveList().getTokens()) {
								//									System.out.println("\t"+tok);
								//								}
							}
						} else {
							for (int t=0;t<lastfr-firstFrame&&!mfccs.noMoreFramesAvailable;t++) {
								decoder.decode(null);
							}
						}
					}

					// construction de la liste des dernieres trames
					int lastFrameAcceptable = (int)((float)oneBlocLen*0.6f);
					if (!order.isBlocViterbi || mfccs.noMoreFramesAvailable) {
						lastFrameAcceptable=-1;
					}

					Alignment align=null, alignPhones=null, alignStates=null;
					if (order.isBlocViterbi&&lastFrameAcceptable>0) {
						// on a trouve le milieu du bloc: on s'arrete a ce milieu
						Alignment[] als = Alignment.backtrack(searchManager.getActiveList().getBestToken());
						if (als!=null) {
							// en fait, ici on a 1 mot = 1 phone, donc les aligns en mots et phones doivent etre les memes
							alignPhones = als[0];
							alignStates = als[2];
							// TODO: recuperer l'alignement en etats
							align = segmentePhonesEnMots(alignPhones);
							align.cutAfterFrame(lastFrameAcceptable);
							alignPhones.cutAfterFrame(lastFrameAcceptable);
							alignStates.cutAfterFrame(lastFrameAcceptable);
						}
					} else {
						// on n'a pas de milieu de bloc: on backtrack depuis la fin
						Token besttok = null;
						for (Token tok : searchManager.getActiveList().getTokens()) {
							// est-ce le dernier (emitting) token d'un HMM ?
							if (hasNonEmittingFinalPath(tok.getSearchState())) {
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
							align=null;
						} else {
							Alignment[] bestaligns = Alignment.backtrack(besttok);
							if (bestaligns!=null) {
								alignPhones = bestaligns[0];
								align = segmentePhonesEnMots(alignPhones);
								alignStates = bestaligns[2];
							}
						}
					}
					if (align!=null) {
						align.adjustOffset(firstFrame);
						alignPhones.adjustOffset(firstFrame);
						alignStates.adjustOffset(firstFrame);
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
					}

					// pour l'instant, cela ne marche que pour le full-align (pas le bloc-viterbi): il faut l'adapter
					// on peut donc retourner align=null lorsqu'il n'y a pas de besttok final !
					order.alignWords=align;
					if (align==null) {
						order.alignPhones=null;
						order.alignStates=null;
					} else {
						order.alignPhones=alignPhones;
						order.alignStates=alignStates;
					}

					if (order.isBlocViterbi&&lastFrameAcceptable<0) {
						System.out.println("no more frames available ! "+oneBlocLen);
						break;
					}
					if (!order.isBlocViterbi) break;
					boolean containsRealWords = false;
					for (int i=0;i<align.getNbSegments();i++) {
						String s = align.getSegmentLabel(i);
						if (!s.equals("SIL")) {containsRealWords=true; break;}
					}
					if (align!=null&&containsRealWords) break;
					System.out.println("WARNING: increase bloc size "+oneBlocLen);
					oneBlocLen+=300;
					if (oneBlocLen>5000) {
						System.out.println("WARNING: can't fint align - stop it");
						break;
					}
				}
				removePrefixes(order.alignPhones);
				synchronized (order) {
					order.notifyAll();
				}

				searchManager.stopRecognition();
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void removePrefixes(Alignment al) {
		if (al==null) return;
		System.out.println("remove prefixes "+al.getNbSegments());
		for (int i=0;i<al.getNbSegments();i++) {
			String s = al.getSegmentLabel(i);
			if (s.startsWith("XZ")) {
				int j=s.indexOf('Y',3);
				if (j>=0) {
					System.out.println("found a prefix "+s);
					al.setSegmentLabel(i, s.substring(j+1));
				}
			}
		}
	}
	
	public static boolean hasNonEmittingFinalPath(SearchState s) {
		SearchStateArc[] arcs = s.getSuccessors();
		for (SearchStateArc a : arcs) {
			SearchState dest = a.getState();
			if (dest.isFinal()) {
				return true;
			}
			if (!dest.isEmitting()) {
				if (hasNonEmittingFinalPath(dest)) return true;
			}
		}
		return false;
	}

	/**
	 * alignement en ligne de commande qui fournit un alignement au niveau phon�me
	 * 
	 * Il doit y avoir 1 argument = fichier qui contient sur chaque ligne un nom de fichier wav et un nom de fichier texte associ�
	 * Les fichiers textes doivent contenir un mot par ligne
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		// lance un alignement en ligne de commande
		S4ForceAlignBlocViterbi aligner = null;

		BufferedReader fs = new BufferedReader(new FileReader(args[0]));
		for (;;) {
			String s0 = fs.readLine();
			if (s0==null) break;
			String[] ss = s0.split(" ");
			if (ss.length<2) continue;

			String wavfile = ss[0];
			String wordsfile = ss[1];
			System.out.println("aligning "+wavfile+" "+wordsfile);
			BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(wordsfile), Charset.forName("UTF-8")));
			ArrayList<String> mots = new ArrayList<String>();
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				mots.add(s);
			}
			f.close();
			String[] ms = new String[mots.size()];
			mots.toArray(ms);
			aligner = S4ForceAlignBlocViterbi.getS4Aligner(wavfile, new StdoutProgressDisplay());
			aligner.setMots(ms);

			S4AlignOrder order = new S4AlignOrder(0, 0, mots.size()-1, -1);
			aligner.input2process.put(order);
			synchronized (order) {
				order.wait();
			}
			Alignment fullalign = order.alignWords;
//			fullalign.savePho(FileUtils.noExt(wordsfile)+".pho");
		}
		aligner.input2process.put(S4AlignOrder.terminationOrder);
	}
}
