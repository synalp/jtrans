/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
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

/**
 * aligns a sequence of phones (with a short grammar description) with a speech signal
 * using token-passing
 * 
 * Note: la grammaire AlignGram peut avoir des etats non-emetteurs, mais elle est ensuite
 * transformee en reseau de decodage (1 noeud = 1 etat de HMM) qui lui, n'a que des etats
 * emetteurs !
 * 
 * @author cerisara
 *
 */
public class SimpleForceAlign {
	HMMSet hmms;
	public int pruning=200;
	/**
	 * shall be set to remove incorrect phones:
	 * - sentences for which last node has been pruned
	 * - QCQ phones
	 */
	public boolean keepOnlyGoodPhones=false;
	RoundBufferFrontEnd mfccbuf;
	OldAlignment align0;
	
	public SimpleForceAlign(HMMSet hmms, RoundBufferFrontEnd mfcc, OldAlignment align0) {
		this.hmms=hmms;
		mfccbuf=mfcc;
		this.align0=align0;
	}

	public boolean killReco=false;
	
	public OldAlignment getAllStateAlign(String align0) {
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

	public OldAlignment align(String rule, int nFrames) {
		AlignGram agram = new AlignGram(rule);
		Network net = agram.expand2monophones(hmms);
		// TODO: il ne faudrait pas recreer le token passing a chaque fois, mais
		// simplement le "vider"
		TokenPassing dec = new TokenPassing(net, nFrames);
		float[] x;
		int t;
		for (t = 0; t < nFrames; t++) {
			if (killReco) return null;
			if (t % 100 == 0)
				System.out.println("trame " + t + "/" + nFrames);
			x = mfccbuf.getOneVector();
			if (x==null) {
				// fin du fichier: on arrete tout !
				break;
			}
			dec.nextObs(x);
		}
		String ress = dec.backtrack();
		OldAlignment newal;
		newal = getAllStateAlign(ress);
		newal.loglike=dec.getLogLike();
		return newal;
	}
	
	public void align(int firstFrame, Element_Mot firstMot, int firstMotIdx, int nFrames, int nWords) {
		System.err.println("aligndebug "+firstFrame+" "+firstMotIdx+" "+nFrames+" "+nWords);
		if (nWords<=0||nFrames<=0) return;
		mfccbuf.gotoFrame(firstFrame);
		// TODO:
		
		String rule = ""; //txtgram.getGrammarBetween(firstMot,nWords);
		// on ajoute un silence final optionnel
		// rule += " [ sil ] ";
		System.err.println("ruledebug "+rule);
		OldAlignment newal = align(rule,nFrames);
		if (newal==null) {
			// pas de mots trouves:
			System.err.println("ERROR batch align at frame "+firstFrame);
			return;
		} else {
			newal.getWordsLabels();
			if (newal.wordLabels==null || newal.wordLabels.length==0) {
				// pas de mots trouves:
				System.err.println("ERROR batch align at frame "+firstFrame);
				return;
			} else {
				// recopie les nouveaux alignements dans l'ancien
				int i,j;
				for (i=firstMotIdx, j=0;j<newal.wordLabels.length;j++) {
					if (!newal.wordLabels[j].equals("sil")) {
						align0.setAlignForWord(i, newal.getFrameDeb(j)+firstFrame, newal.getFrameFin(j)+firstFrame);
						i++;
					}
				}
				if (i==firstMotIdx) {
					// pas de mots trouves:
					System.err.println("ERROR batch align at frame "+firstFrame);
					return ;
				}
			}
		}
	}
	public OldAlignment getStateAlign(String align0) {
		StringTokenizer align = new StringTokenizer(align0);

		HMMState prev = null;
		// compte nb d'etats total differents
		int nstates=0;
		for (int i=0;align.hasMoreTokens();i++) {
			String s = align.nextToken();
			String nom = s.substring(0,s.indexOf('['));
//			SingleHMM hmm = hmms.getHMM(nom);
			SingleHMM hmm = AlignGram.getHMM(hmms,nom);
			int etat = Integer.parseInt(s.substring(s.indexOf('[')+1,s.indexOf(']')));
			HMMState cur = hmm.getState(etat);
			if (cur!=prev) {
				if (prev!=null) nstates++;
				prev=cur;
			}
		}
		if (prev!=null) nstates++;
		
		OldAlignment res = new OldAlignment(nstates);
		prev = null;
		String prevnom="", curnom;
		int tdeb=0,curstate=0;
		align = new StringTokenizer(align0);
		int i;
		String word = null;
		for (i=0;align.hasMoreTokens();i++) {
			String s = align.nextToken();
			String nomPhoneme = s.substring(0,s.indexOf('['));
//			SingleHMM hmm = hmms.getHMM(nomPhoneme);
			SingleHMM hmm = AlignGram.getHMM(hmms,nomPhoneme);
			int etat = Integer.parseInt(s.substring(s.indexOf('[')+1,s.indexOf(']')));
			HMMState curetat = hmm.getState(etat);
			curnom = curetat.getLab().toString();
			if (nomPhoneme.startsWith("QCQ")) {
				int a = curnom.indexOf('[');
				if (a>=0) {
					// il y a des etats:
					curnom = nomPhoneme+curnom.substring(a);
				} else
					curnom = nomPhoneme;
			}
			if (curetat!=prev) {
				if (prev!=null) {
					res.labels[curstate]=""+prevnom;
					res.tramesDeb[curstate]=tdeb;
					res.tramesFin[curstate++]=i;
					if (curetat.lab.getState()<prev.lab.getState()) {
						// on a change de phonemes: on peut eventuellement afficher le mot precedent !
						if (word!=null) {
							res.labels[curstate-1]+="("+word+")";
							word=null;
						}
					}
				}
				tdeb=i;
				prev=curetat; prevnom=curnom;
			}
			int posWord = s.indexOf('(');
			if (posWord>=0) {
				// on l'affichera _a la fin du dernier etat du phoneme courrant_ !
				int pos2 = s.indexOf(',',posWord);
				if (pos2>=0)
					word = s.substring(posWord+1,pos2);
				else
					word = s.substring(posWord+1,s.indexOf(')'));
			}
		}
		if (prev!=null) {
			res.labels[curstate]=prev.getLab().toString();
			res.tramesDeb[curstate]=tdeb;
			res.tramesFin[curstate++]=i;
			if (word!=null) {
				res.labels[curstate-1]+="("+word+")";
				word=null;
			}
		}
		System.out.println("parsing found "+curstate);
		return res;
	}
}
