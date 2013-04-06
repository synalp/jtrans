/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

package plugins.speechreco.aligners.sphiinx4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.flat.HMMStateState;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist.LexTreeHMMState;
import facade.JTransAPI;
import plugins.speechreco.aligners.Segment2AudioAlignement;
import plugins.text.elements.Element_Mot;
import plugins.utils.ProgressDialog;

/**
 * contient un alignement complet: N° des etats, triphone, GMM, mot
 * 
 * @author cerisara
 *
 */
public class AlignementEtat {

	public AlignementEtat() {
		System.out.println("============================= CREATION ALIGN");
	}
	
	// ne contient QUE des segments alignés
	private Segment2AudioAlignement segs = new Segment2AudioAlignement();
	private int firstFrame = 0;

	public int[] matchWithText(String[] mots) {
		int[] mots2segidx = new int[mots.length];
		Arrays.fill(mots2segidx, -1);
		int segidx=0, motidx=0;
		for (;motidx<mots.length;motidx++) {
			// on se positionne au prochain mot prononcé
			while (segidx<getNbSegments() && getSegmentLabel(segidx).equals("SIL")) {
				segidx++;
			}
			if (segidx>=getNbSegments()) {
				// l'alignement s'arrete au milieu des mots avec bloc-viterbi
				break;
			}
			// si le mot ne correspond pas, alors on suppose que c'est un mot optionnel
			int walidx=-1;
			if (getSegmentLabel(segidx).trim().equals(mots[motidx].trim())) {
//				System.out.println("matchWithtext: pronunced token ["+mots[motidx]+"] ["+getSegmentLabel(segidx)+"] "+segidx+" "+getSegmentDebFrame(segidx)+" "+getSegmentEndFrame(segidx));
				walidx=segidx++;
			} else {
				System.out.println("matchWithtext: optional token ["+mots[motidx]+"] "+segidx+" ["+getSegmentLabel(segidx)+"]");
				// il ne faut pas qu'un mot ne soit associe a aucun segment !! QUE FAIRE ??
				walidx = segidx;
			}
			mots2segidx[motidx]=walidx;
		}
		System.out.println("matchwithtext ended "+segidx+" "+motidx);
		return mots2segidx;
	}

	public static AlignementEtat load(BufferedReader f) {
		AlignementEtat a = new AlignementEtat();
		try {
			String s = f.readLine();
			if (s==null || s.trim().length()==0) return null;
			a.firstFrame=Integer.parseInt(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
		a.segs.load(f);
		return a;
	}

	public String toString() {
		return segs.toString(firstFrame);
	}

	public void save(PrintWriter f) {
		f.println(firstFrame);
		segs.save(f);
	}

	/**
	 * suppose que le nouvel align commence juste apres un segment precedent
	 * 
	 * @param al
	 * @return
	 */
	public int merge(AlignementEtat al) {
		int nsegsConservesDuPremier=getNbSegments();
		System.out.println("merging "+nsegsConservesDuPremier+" "+firstFrame+" "+al.firstFrame);
		if (firstFrame<al.firstFrame) {
			// il ne faut plus ajuster, car on l'a deja fait et on utilise les methodes de haut niveau pour acceder aux limites temporelles !
			//			al.segs.addTimeOffset(al.firstFrame-firstFrame);
		} else {
			System.out.println("WARNING: merge alignement plus ancien !!");
			segs.addTimeOffset(firstFrame-al.firstFrame);
			firstFrame=al.firstFrame;
		}
		if (al.getNbSegments()<=0) return nsegsConservesDuPremier;
		if (getNbSegments()>0&&al.getSegmentDebFrame(0)<getSegmentEndFrame(getNbSegments()-1)) {
			// recouvrant
			int lastSeg2keep = getSegmentAtFrame(al.firstFrame-1);
			if (getSegmentEndFrame(lastSeg2keep)!=al.firstFrame) {
				// on n'a pas les memes segments dans le vieux et nouvel alignements
				// ce qui ne devrait jamais arriver, car on commence toujours l'alignement suivant à la fin d'un
				// segment de l'alignement précédent !
				// mais on peut avoir "sauté" des segments ??
				System.out.println("PB MERGE "+al.firstFrame+" "+firstFrame+" "+lastSeg2keep+" "+getSegmentEndFrame(lastSeg2keep));
				System.out.println("debug segments\n"+toString());
			}
			// pas de probleme
			cutAfterFrame(al.firstFrame);
			nsegsConservesDuPremier=getNbSegments();
		}
		for (int i=0;i<al.getNbSegments();i++) {
			addRecognizedSegment(al.getSegmentLabel(i), al.getSegmentDebFrame(i)-firstFrame, al.getSegmentEndFrame(i)-firstFrame, null, null);
		}
		segs.setFirstSegmentAltered(nsegsConservesDuPremier);

		return nsegsConservesDuPremier;
	}

	public void clearIndex() {
		segEndFrames=null;
	}

	// nouvelle version de l'index
	// l'index contient les limites des segments EN RELATIF par rapport au debut du fichier !!!
	private int[] segEndFrames = null;
	public void buildIndex() {
		System.out.println("building index");
		segEndFrames = new int[segs.getNbSegments()];
		for (int i=0;i<segEndFrames.length;i++) {
			segEndFrames[i]=segs.getSegmentFinFrame(i);
		}
		if (segEndFrames.length>0)
			System.out.println("index updated "+segEndFrames.length+" "+segEndFrames[segEndFrames.length-1]);
		segs.setFirstSegmentAltered(-1);
	}
	/**
	 * @param fr trame ABSOLUE
	 * @return
	 */
	public int getSegmentAtFrame(int fr) {
		if (segEndFrames==null || segs.getFirstSegmentAltered()>=0)
			buildIndex();
		int s = Arrays.binarySearch(segEndFrames, fr-firstFrame);
		if (s>=0) return s+1;
		return -s-1;
	}

	// premiere version de l'index: tres long a charger au debut
	private int[] fr2seg = null;
	public int getSegmentAtFrame1(int fr) {
		// TODO: il faut mettre a jour l'index a la moindre modif !
		final Boolean isDone=false;
		if (fr2seg==null) {
			final ProgressDialog pd = new ProgressDialog((JFrame)null,null,"indexing...");
			pd.isDeterminate=true;
			pd.setRunnable(new Runnable() {
				@Override
				public void run() {
					int nfrs = segs.getSegmentFinFrame(segs.getNbSegments()-1);
					fr2seg = new int[nfrs];
					Arrays.fill(fr2seg,-1);
					int curseg=0;
					for (int i=0;i<fr2seg.length;i++) {
						pd.setProgress((float)i/(float)fr2seg.length);
						while (curseg<getNbSegments()&&i>=getSegmentEndFrame(curseg)) curseg++;
						// on avance les segments jusqu'a depasser la trame i
						if (curseg<getNbSegments()) {
							if (i>=getSegmentDebFrame(curseg)) fr2seg[i]=curseg;
						} else break;
					}
					synchronized (isDone) {
						isDone.notifyAll();
					}
				}
			});
			pd.setVisible(true);
			synchronized (isDone) {
				try {
					isDone.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					return -1;
				}
			}
			return fr2seg[fr-firstFrame];
		} else return fr2seg[fr-firstFrame];
	}

	public int getNbSegments() {
		return segs.getNbSegments();
	}

	public String getSegmentLabel(int segidx) {
		if (segidx<0||segidx>=segs.getNbSegments()) return null;
		String s = segs.getSegmentLabel(segidx);
		return s;
	}

	public void setSegmentDebFrame(int segidx, int frame) {
		if (segidx<0||segidx>=segs.getNbSegments()) return;
		segs.setSegmentDebFrame(segidx, frame);
	}
	public void setSegmentEndFrame(int segidx, int frame) {
		if (segidx<0||segidx>=segs.getNbSegments()) return;
		segs.setSegmentFinFrame(segidx, frame);
	}
	public void setSegmentSourceManu(int segidx) {segs.setSegmentSourceManu(segidx);}
	public void setSegmentSourceEqui(int segidx) {segs.setSegmentSourceEqui(segidx);}
	public int getSegmentEndFrame(int segidx) {
		if (segidx<0||segidx>=segs.getNbSegments()) return -1;
		return segs.getSegmentFinFrame(segidx)+firstFrame;
	}
	public int getSegmentDebFrame(int segidx) {
		if (segidx<0||segidx>=segs.getNbSegments()) return -1;
		return segs.getSegmentDebFrame(segidx)+firstFrame;
	}
	public void delSegment(int segidx) {
		segs.delSeg(segidx);
	}

	public void adjustOffset(int tr) {
		firstFrame=tr;
	}

	public int addRecognizedSegment(String mot, int framedeb, int frameend, String[] phones, int[] states) {
		assert framedeb>=0;
		assert frameend>framedeb;
		int segidx = segs.addSegment(mot, framedeb, frameend);
		return segidx;
	}

	public void setSegmentLabel(int segidx, String newlabel) {
		segs.setSegmentLabel(segidx,newlabel);
	}

	public int getStartFrame() {return firstFrame;}

	public void clear() {
		segs = new Segment2AudioAlignement();
		clearIndex();
	}

	/**
	 * @param lastFrameAcceptable = der trame a conserver, en absolu !
	 */
	public void cutAfterFrame(int lastFrameAcceptable) {
		segs.cutAfterFrame(lastFrameAcceptable-firstFrame);
	}
	public void cutBeforeFrame(int fr) {
		segs.cutBeforeFrame(fr);
	}

	public static String getInfoOneFrame(Token tok) {
		String w="", p="", s="";
		if (tok.isWord()) w=tok.getWord().getSpelling();
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
				p=etatHMM.getHMM().getUnit().getName();
				s=""+etatHMM.getState();
			}
		}
		return w+":"+p+":"+s;
	}

	// TODO: il faut retourner 3 alignements: pour les mots, les phones et les etats
	public static AlignementEtat[] backtrack(Token tok) {
		try {
			if (tok==null) {
				System.out.println("ERROR: no best token !");
				return null;
			}

			// variables locales pour le mot courant:
			int endword;
			ArrayList<String> phones=new ArrayList<String>();
			ArrayList<Integer> states=new ArrayList<Integer>();

			// accumulation des segments vus a reculon
			ArrayList<String> labs = new ArrayList<String>();
			ArrayList<Integer> labsdeb = new ArrayList<Integer>();
			ArrayList<Integer> labsfin = new ArrayList<Integer>();
			ArrayList<ArrayList<String>> labsphones = new ArrayList<ArrayList<String>>();
			ArrayList<ArrayList<Integer>> labsstates = new ArrayList<ArrayList<Integer>>();

			// on commence a la trame 0, puis on recule -1 -2 ...
			int fr=0;
			endword=0;
			while (tok!=null) {
				if (tok.isWord()) {
					// nous sommes dans la trame de DEBUT (!!) du mot !
					labs.add(tok.getWord().getSpelling());
					labsdeb.add(fr); labsfin.add(endword);
					labsphones.add(phones); labsstates.add(states);
					endword=fr;
					phones=new ArrayList<String>(); states=new ArrayList<Integer>();
				}
				if (tok.getSearchState()!=null && tok.isEmitting() && tok.getData()!=null) {
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
						phones.add(0,etatHMM.getHMM().getUnit().getName());
						states.add(0,etatHMM.getState());
					}
					fr--;
				}
				tok=tok.getPredecessor();
			}

			// creation alignement
			AlignementEtat alignMots = new AlignementEtat();
			AlignementEtat alignPhones = new AlignementEtat();
			AlignementEtat alignStates = new AlignementEtat();
			for (int i=labs.size()-1;i>=0;i--) {
				String[] phs = new String[labsphones.get(i).size()];
				phs = labsphones.get(i).toArray(phs);
				assert phs.length==labsfin.get(i)-labsdeb.get(i);
				int[] sts = new int[labsstates.get(i).size()];
				assert sts.length==labsfin.get(i)-labsdeb.get(i);
				for (int j=0;j<sts.length;j++) sts[j]=labsstates.get(i).get(j);
				alignMots.addRecognizedSegment(labs.get(i), labsdeb.get(i)-fr, labsfin.get(i)-fr, null, null);

				// complete les aligns en phones et states
				int phoneTrDeb=labsdeb.get(i)-fr, stateTrDeb=phoneTrDeb;
				for (int j=1;j<phs.length;j++) {
					if (phs[j].equals(phs[j-1])) {
						if (sts[j]==sts[j-1]) {
							// meme phone, meme etat
						} else if (sts[j]<sts[j-1]) {
							// nouveau phone, nouvel etat
							int tr = labsdeb.get(i)-fr+j;
							alignPhones.addRecognizedSegment(phs[j-1], phoneTrDeb, tr, null,null);
							phoneTrDeb=tr;
							alignStates.addRecognizedSegment(""+sts[j-1], stateTrDeb, tr, null,null);
							stateTrDeb=tr;
						} else if (sts[j]>sts[j-1]) {
							// meme phone, nouvel etat
							int tr = labsdeb.get(i)-fr+j;
							alignStates.addRecognizedSegment(""+sts[j-1], stateTrDeb, tr, null,null);
							stateTrDeb=tr;
						}
					} else {
						// nouveau phone, nouvel etat
						int tr = labsdeb.get(i)-fr+j;
						alignPhones.addRecognizedSegment(phs[j-1], phoneTrDeb, tr, null,null);
						phoneTrDeb=tr;
						alignStates.addRecognizedSegment(""+sts[j-1], stateTrDeb, tr, null,null);
						stateTrDeb=tr;
					}
				}
				// reste le dernier
				if (phs.length>0) {
					int tr = labsfin.get(i)-fr;
					alignPhones.addRecognizedSegment(phs[phs.length-1], phoneTrDeb, tr, null,null);
					alignStates.addRecognizedSegment(""+sts[sts.length-1], stateTrDeb, tr, null,null);
				}
			}

			System.out.println("debug align after backtrack "+alignMots);

			AlignementEtat[] res = {alignMots,alignPhones,alignStates};
			return res;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
