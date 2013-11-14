/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

/*
Copyright Christophe Cerisara, Josselin Pierre (1er septembre 2008)

cerisara@loria.fr

Ce logiciel est un programme informatique servant e aligner un
corpus de parole avec sa transcription textuelle.

Ce logiciel est regi par la licence CeCILL-C soumise au droit franeais et
respectant les principes de diffusion des logiciels libres. Vous pouvez
utiliser, modifier et/ou redistribuer ce programme sous les conditions
de la licence CeCILL-C telle que diffusee par le CEA, le CNRS et l'INRIA 
sur le site "http://www.cecill.info".

En contrepartie de l'accessibilite au code source et des droits de copie,
de modification et de redistribution accordes par cette licence, il n'est
offert aux utilisateurs qu'une garantie limitee.  Pour les memes raisons,
seule une responsabilite restreinte pese sur l'auteur du programme,  le
titulaire des droits patrimoniaux et les concedants successifs.

A cet egard  l'attention de l'utilisateur est attiree sur les risques
associes au chargement,  e l'utilisation,  e la modification et/ou au
developpement et e la reproduction du logiciel par l'utilisateur etant 
donne sa specificite de logiciel libre, qui peut le rendre complexe e 
manipuler et qui le reserve donc e des developpeurs et des professionnels
avertis possedant  des  connaissances  informatiques approfondies.  Les
utilisateurs sont donc invites e charger  et  tester  l'adequation  du
logiciel e leurs besoins dans des conditions permettant d'assurer la
securite de leurs systemes et ou de leurs donnees et, plus generalement, 
e l'utiliser et l'exploiter dans les memes conditions de securite. 

Le fait que vous puissiez acceder e cet en-tete signifie que vous avez 
pris connaissance de la licence CeCILL-C, et que vous en avez accepte les
termes.
 */

package plugins.speechreco.aligners;

import java.util.ArrayList;
import java.util.StringTokenizer;

import plugins.buffer.RoundBufferFrontEnd;
import plugins.speechreco.acousticModels.HMM.HMMSet;
import plugins.speechreco.acousticModels.HMM.HMMState;
import plugins.speechreco.acousticModels.HMM.SingleHMM;
import plugins.speechreco.decoder.Network;
import plugins.speechreco.decoder.TokenPassing;
import plugins.speechreco.grammaire.AlignGram;
import plugins.text.elements.Element_Mot;

public class ForceAlignBlocViterbi extends Thread {
	public HMMSet hmms;
	public RoundBufferFrontEnd bufmfcc;

	public volatile boolean pauseReco = true;
	private boolean killReco = false;

	public final static int NTWIN = 500; // nb de trames pour un bloc de Viterbi
	public final static int NTWINMAX = 3000; // nb de trames MAX pour un bloc de Viterbi
	public static int Nwords = 20; // nb de mots qu'on utilise pour
	// construire la grammaire d'un bloc
	public float loglikebest;
	AlignGram agram;
	int nFrames;
	public AlignListener alignListener=null;
	OldAlignment align0;

	// variables indiquant la trame de debut et le mot de debut de la reco
	int firstMotidx;
	int firstframe;
	Element_Mot firstmot;
	
	public ForceAlignBlocViterbi(HMMSet hmms, RoundBufferFrontEnd mfcc, OldAlignment align0) {
		this.hmms = hmms;
		this.bufmfcc = mfcc;
		this.align0=align0;
		pauseReco=true;
		start();
	}

	public synchronized void killThread() {
		killReco=true;
		this.notifyAll();
	}
	
	/**
	 * cette fonction est appelee dans un autre thread !
	 * @param firstFrame
	 * @param firstMot
	 * @param firstMotIdx
	 */
	private volatile boolean isWaiting=false;
	public synchronized void doReco(int firstFrame, Element_Mot firstMot, int firstMotIdx) {
		this.firstMotidx=firstMotIdx;
		this.firstframe=firstFrame;
		this.firstmot=firstMot;
		this.nFrames=NTWIN;
		// je lance la reco dans le run()
		System.err.println("notify reco !");
		pauseReco=false;
		if (isWaiting)
			this.notify();
	}

	public synchronized void run() {
		int firstMotOfLastPass=-1;
		while (!killReco) {

			if (pauseReco) {
				System.err.println("waiting for next reco...");
				/*
				 * la speechreco a fini la reconnaissance precedente: le speech recognizer attend
				 * ici jusqu'a ce qu'on ait a nouveau besoin de lui...
				 */
				try {
					isWaiting=true;
					wait();
					if (killReco) return;
					isWaiting=false;
					pauseReco=false;
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
				System.err.println("reco launched !");
			}
			
			bufmfcc.gotoFrame(firstframe);
//			String rule = gram.getGrammarFrom(firstmot);
	String rule="";		
			System.err.println("RULE "+rule);
			
			agram = new AlignGram(rule);

			Network net = agram.expand2monophones(hmms);
			// TODO: il ne faudrait pas recreer le token passing a chaque fois, mais
			// simplement le "vider"
			TokenPassing dec = new TokenPassing(net, nFrames);
			// dec.setPruning(pruning);
			// le buffer d'obs doit toujours repartir de zero !
			float[] x;
			int t;
			for (t = 0; t < nFrames; t++) {
				if (killReco) return;
				if (pauseReco) break;
				if (t % 100 == 0)
					System.out.println("trame " + t + "/" + nFrames);
				x = bufmfcc.getOneVector();
				if (x==null) {
					// fin du fichier: on arrete tout !
					pauseReco=true;
					break;
				}
				float loglikemax = dec.nextObs(x);
//				System.out.println("loglike "+t+" "+loglikemax);
			}
			String ress = dec.backtrackFromBest();
			loglikebest = dec.getLogLikeBest();
			OldAlignment newal;
			if (t<nFrames) {
				// on a atteint la fin du fichier: il faut garder l'alinement complet !
System.out.println("resdebug "+ress);
				ress = dec.backtrack();
				System.out.println("resdebu2 "+ress);
				newal = getAllStateAlign(ress);
			} else {
				// on n'a pas atteint la fin: on ne garde que la moitie de l'alignement
				newal = getHalfStateAlign(ress);
			}
			
			System.out.println("dbeug newal "+newal);
			
			if (newal==null) {
				if (t<nFrames) {
					if (alignListener!=null)
						alignListener.recoFinished();
					continue;
				}
				// pas de mots trouves: on augmente la taille du buffer
				nFrames *=2;
				if (nFrames>=NTWINMAX) {
					System.err.println("aucun mot trouve au-dela de "+NTWINMAX+" frames - abandon !");
					pauseReco=true;
					if (alignListener!=null) alignListener.noAlignFound();
					continue;
				}
			} else {
				newal.getWordsLabels();
				if (newal.wordLabels==null || newal.wordLabels.length==0) {
					if (t<nFrames) {
						if (alignListener!=null)
							alignListener.recoFinished();
						continue;
					}
					// pas de mots trouves: on augmente la taille du buffer
					nFrames *=2;
					if (nFrames>=NTWINMAX) {
						System.err.println("aucun mot trouve au-dela de "+NTWINMAX+" frames - abandon !");
						pauseReco=true;
						if (alignListener!=null) alignListener.noAlignFound();
						continue;
					}
				} else {
					// recopie les nouveaux alignements dans l'ancien
					System.err.println("nouveaux alignements "+newal.wordLabels.length+" "+firstMotidx);
					int i,j;
					firstMotOfLastPass=firstMotidx;
					for (i=firstMotidx, j=0;j<newal.wordLabels.length;j++) {
						if (!newal.wordLabels[j].equals("sil")) {
							align0.setAlignForWord(i, newal.getFrameDeb(j)+firstframe, newal.getFrameFin(j)+firstframe);
							i++;
						}
					}					
					if (t<nFrames) {
						if (alignListener!=null)
							alignListener.recoFinished();
						continue;
					}

					nFrames=NTWIN;
					if (i==firstMotidx) {
						// pas de mots trouves: on augmente la taille du buffer
						nFrames *=2;
						if (nFrames>=NTWINMAX) {
							System.err.println("aucun mot trouve au-dela de "+NTWINMAX+" frames - abandon !");
							pauseReco=true;
							if (alignListener!=null) alignListener.noAlignFound();
							continue;
						}
					} else {
						// prepare pour la reco suivante
//						for (j=firstMotidx;j<i;j++)
//							firstmot = firstmot.nextEltInGram;
						firstMotidx=i;
						firstframe=align0.getFrameFin(i-1)+1;
						// griser le dernier mot aligne
						if (alignListener!=null) alignListener.newAlign(firstMotOfLastPass,firstMotidx-1);
					}
				}
			}
		}
		if (alignListener!=null)
			alignListener.recoFinished();
	}

	/**
	 * recupere l'alignement
	 * 
	 * @param align0
	 * @return
	 */
	public OldAlignment getHalfStateAlign(String align0) {
		// System.out.println("getHalfStateAligne : "+align0);
		StringTokenizer align = new StringTokenizer(align0);

		HMMState prev = null;
		ArrayList<HMMState> etats = new ArrayList<HMMState>();
		ArrayList<Integer> debst = new ArrayList<Integer>();
		ArrayList<Integer> finst = new ArrayList<Integer>();
		ArrayList<String> words = new ArrayList<String>();

		prev = null;
		int tdeb = 0; // ,curstate=0;
		align = new StringTokenizer(align0);
		int i, etat;
		HMMState curetat;
		String word = null;
		for (i = 0; align.hasMoreTokens(); i++) {
			String s = align.nextToken();
			String nomPhoneme = s.substring(0, s.indexOf('['));
			// SingleHMM hmm = hmms.getHMM(nomPhoneme);
			SingleHMM hmm = AlignGram.getHMM(hmms, nomPhoneme);

			etat = Integer.parseInt(s.substring(s.indexOf('[') + 1, s
					.indexOf(']')));
			curetat = hmm.getState(etat);
			if (curetat != prev) {
				if (prev != null) {
					etats.add(prev);
					debst.add(tdeb);
					finst.add(i);
					if (curetat.lab.getState() < prev.lab.getState()) {
						// on a change de phonemes: on peut eventuellement
						// afficher le mot precedent !
						words.add(word);
						word = null;
					} else
						words.add(null);
				}
				tdeb = i;
				prev = curetat;
			}
			int posWord = s.indexOf('(');
			if (posWord >= 0) {
				// on l'affichera _a la fin du dernier etat du phoneme courrant_
				// !
				int pos2 = s.indexOf(',', posWord);
				if (pos2 >= 0)
					word = s.substring(posWord + 1, pos2);
				else
					word = s.substring(posWord + 1, s.indexOf(')'));
			}
		}
		if (prev != null) {
			etats.add(prev);
			debst.add(tdeb);
			finst.add(i);
			words.add(word);
			word = null;
		}

		int idxEtatFin = 0;

		int tempsDebut = debst.get(0);
		int tempsFin = finst.get(finst.size() - 1);
		int tempsMoitie = (tempsDebut + tempsFin) / 2;
		int ii;
		for (ii = words.size() - 1; ii >= 0; ii--) {
			if (words.get(ii) != null && !words.get(ii).equals("sil")) {
				int t = finst.get(ii);
				if (t <= tempsMoitie)
					break;
			}
		}
		if (ii < 0) {
			// pas de mots trouves !
			return null;
		} else {
			idxEtatFin = ii;
		}

		/*
		 * // on s'arrete au premier mot qui a ete trouve while
		 * (idxEtatFin<words.size()&&words.get(idxEtatFin)==null) idxEtatFin++;
		 * if (idxEtatFin>=words.size()) { // aucun mot n'a ete trouve ! return
		 * null; }
		 */
		OldAlignment res = new OldAlignment(idxEtatFin + 1);
		for (i = 0; i <= idxEtatFin; i++) {
			res.labels[i] = etats.get(i).toString();
			if (words.get(i) != null) {
				String motmp = words.get(i);
				res.labels[i] += "(" + motmp + ")";
			}

			res.tramesDeb[i] = debst.get(i);
			res.tramesFin[i] = finst.get(i);
		}
		return res;
	}
	public OldAlignment getAllStateAlign(String align0) {
		// System.out.println("getHalfStateAligne : "+align0);
		StringTokenizer align = new StringTokenizer(align0);

		HMMState prev = null;
		ArrayList<HMMState> etats = new ArrayList<HMMState>();
		ArrayList<Integer> debst = new ArrayList<Integer>();
		ArrayList<Integer> finst = new ArrayList<Integer>();
		ArrayList<String> words = new ArrayList<String>();

		prev = null;
		int tdeb = 0; // ,curstate=0;
		align = new StringTokenizer(align0);
		int i, etat;
		HMMState curetat;
		String word = null;
		for (i = 0; align.hasMoreTokens(); i++) {
			String s = align.nextToken();
			String nomPhoneme = s.substring(0, s.indexOf('['));
			// SingleHMM hmm = hmms.getHMM(nomPhoneme);
			SingleHMM hmm = AlignGram.getHMM(hmms, nomPhoneme);

			etat = Integer.parseInt(s.substring(s.indexOf('[') + 1, s
					.indexOf(']')));
			curetat = hmm.getState(etat);
			if (curetat != prev) {
				if (prev != null) {
					etats.add(prev);
					debst.add(tdeb);
					finst.add(i);
					if (curetat.lab.getState() < prev.lab.getState()) {
						// on a change de phonemes: on peut eventuellement
						// afficher le mot precedent !
						words.add(word);
						word = null;
					} else
						words.add(null);
				}
				tdeb = i;
				prev = curetat;
			}
			int posWord = s.indexOf('(');
			if (posWord >= 0) {
				// on l'affichera _a la fin du dernier etat du phoneme courrant_
				// !
				int pos2 = s.indexOf(',', posWord);
				if (pos2 >= 0)
					word = s.substring(posWord + 1, pos2);
				else
					word = s.substring(posWord + 1, s.indexOf(')'));
			}
		}
		if (prev != null) {
			etats.add(prev);
			debst.add(tdeb);
			finst.add(i);
			words.add(word);
			word = null;
		}

		int idxEtatFin = 0;

		int tempsDebut = debst.get(0);
		int tempsFin = finst.get(finst.size() - 1);
		int tempsMoitie = tempsFin;
		int ii;
		for (ii = words.size() - 1; ii >= 0; ii--) {
			if (words.get(ii) != null && !words.get(ii).equals("sil")) {
				int t = finst.get(ii);
				if (t <= tempsMoitie)
					break;
			}
		}
		if (ii < 0) {
			// pas de mots trouves !
			return null;
		} else {
			idxEtatFin = ii;
		}

		/*
		 * // on s'arrete au premier mot qui a ete trouve while
		 * (idxEtatFin<words.size()&&words.get(idxEtatFin)==null) idxEtatFin++;
		 * if (idxEtatFin>=words.size()) { // aucun mot n'a ete trouve ! return
		 * null; }
		 */
		OldAlignment res = new OldAlignment(idxEtatFin + 1);
		for (i = 0; i <= idxEtatFin; i++) {
			res.labels[i] = etats.get(i).toString();
			if (words.get(i) != null) {
				String motmp = words.get(i);
				res.labels[i] += "(" + motmp + ")";
			}

			res.tramesDeb[i] = debst.get(i);
			res.tramesFin[i] = finst.get(i);
		}
		return res;
	}
}
