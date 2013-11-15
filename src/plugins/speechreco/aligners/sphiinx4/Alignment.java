/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

package plugins.speechreco.aligners.sphiinx4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.flat.HMMStateState;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist.LexTreeHMMState;

/**
 * Contient des segments, représentés par des strings, alignés avec de l'audio
 * (en frames).
 * 
 * @author cerisara
 */
public class Alignment implements Serializable {

	// Type codes for alignment sources
	public static final byte ALIGNMENT_SOURCE_AUTOMATIC = 0;
	public static final byte ALIGNMENT_SOURCE_MANUAL = 1;
	public static final byte ALIGNMENT_SOURCE_LINEAR = 2;

	/**
	 * First frame.
	 * The values contained in segmentsDeb/segmentsFin start from 0, therefore
	 * they must be offset by frameOffset to be meaningful.
	 */
	private int frameOffset;

	private int firstSegmentModified;

	// ne contient QUE des segments alignés
	private List<String> segments;
	private List<Integer> segmentsDeb;
	private List<Integer> segmentsFin;
	private List<Byte> segmentsSource;

	/**
	 * L'index contient les limites des segments EN RELATIF par rapport au
	 * début du fichier !!!
 	 */
	private int[] segEndFrames = null;

	public Alignment() {
		clear();
	}

	/**
	 * Copy constructor - performs a SHALLOW copy of the segments!
	 */
	public Alignment(Alignment other) {
		frameOffset = other.frameOffset;
		firstSegmentModified = other.firstSegmentModified;
		segments = new ArrayList<String>(other.segments);
		segmentsDeb = new ArrayList<Integer>(other.segmentsDeb);
		segmentsFin = new ArrayList<Integer>(other.segmentsFin);
		segmentsSource = new ArrayList<Byte>(other.segmentsSource);
	}

	public void clear() {
		frameOffset = 0;
		firstSegmentModified = -1;
		segments = new ArrayList<String>();
		segmentsDeb = new ArrayList<Integer>();
		segmentsFin = new ArrayList<Integer>();
		segmentsSource = new ArrayList<Byte>();
		clearIndex();
	}

	/**
	 * Map word indices to segment indices.
	 */
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
			int walidx;
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

	public String toString() {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < segments.size(); i++)
			buf.append(String.format("%d : %d-%d:%s\n",
					i,
					segmentsDeb.get(i) + frameOffset,
					segmentsFin.get(i) + frameOffset,
					segments.get(i)));
		return buf.toString();
	}

	public static Alignment load(BufferedReader f) {
		Alignment a = new Alignment();
		try {
			String s = f.readLine();
			if (s==null || s.trim().length()==0) return null;
			a.frameOffset = Integer.parseInt(s);
			// ------
			s = f.readLine();
			int n = Integer.parseInt(s);
			for (int i=0;i<n;i++) {
				s = f.readLine();
				if (a.firstSegmentModified<0) a.firstSegmentModified=a.segments.size();
				a.segments.add(s);
				s = f.readLine();
				String[] ss = s.split(" ");
				a.segmentsDeb.add(Integer.parseInt(ss[0]));
				a.segmentsFin.add(Integer.parseInt(ss[1]));
				byte src=0;
				if (ss.length>2) src=Byte.parseByte(ss[2]);
				a.segmentsSource.add(src);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return a;
	}

	public void save(PrintWriter f) {
		f.println(frameOffset);
		f.println(segments.size());
		for (int i=0;i<segments.size();i++) {
			f.println(segments.get(i));
			f.println(segmentsDeb.get(i)+" "+segmentsFin.get(i)+" "+segmentsSource.get(i));
		}
	}

	/**
	 * suppose que le nouvel align commence juste apres un segment precedent
	 *
	 * @return Index at which the first segment was inserted
	 */
	public int merge(Alignment al) {
		int nsegsConservesDuPremier=getNbSegments();
		System.out.println("merging "+nsegsConservesDuPremier+" "+ frameOffset +" "+al.frameOffset);
		if (frameOffset < al.frameOffset) {
			// il ne faut plus ajuster, car on l'a deja fait et on utilise les methodes de haut niveau pour acceder aux limites temporelles !
			//			al.segs.addTimeOffset(al.frameOffset-frameOffset);
		} else {
			System.out.println("WARNING: merge alignement plus ancien !!");
			// move segments
			int deltat = frameOffset - al.frameOffset;
			for (int i = 0; i < segments.size(); i++) {
				segmentsDeb.set(i, segmentsDeb.get(i) + deltat);
				segmentsFin.set(i, segmentsFin.get(i) + deltat);
			}
			frameOffset = al.frameOffset;
		}
		if (al.getNbSegments()<=0) return nsegsConservesDuPremier;
		if (getNbSegments()>0&&al.getSegmentDebFrame(0)<getSegmentEndFrame(getNbSegments()-1)) {
			// recouvrant
			int lastSeg2keep = getSegmentAtFrame(al.frameOffset - 1);
			if (getSegmentEndFrame(lastSeg2keep) != al.frameOffset) {
				// on n'a pas les memes segments dans le vieux et nouvel alignements
				// ce qui ne devrait jamais arriver, car on commence toujours l'alignement suivant à la fin d'un
				// segment de l'alignement précédent !
				// mais on peut avoir "sauté" des segments ??
				System.out.println("PB MERGE "+al.frameOffset +" "+ frameOffset +" "+lastSeg2keep+" "+getSegmentEndFrame(lastSeg2keep));
				System.out.println("debug segments\n"+toString());
			}
			// pas de probleme
			cutAfterFrame(al.frameOffset);
			nsegsConservesDuPremier=getNbSegments();
		}
		for (int i=0;i<al.getNbSegments();i++) {
			addRecognizedSegment(al.getSegmentLabel(i), al.getSegmentDebFrame(i) - frameOffset, al.getSegmentEndFrame(i)- frameOffset, null, null);
		}
		setFirstSegmentAltered(nsegsConservesDuPremier);

		return nsegsConservesDuPremier;
	}

	/**
	 * Overwrites part of this alignment with a smaller alignment.
	 */
	public void overwrite(Alignment small) {
		int fromFrame = small.getStartFrame();
		int toFrame   = small.getSegmentEndFrame(small.getNbSegments()-1);

		assert fromFrame >= getStartFrame();
		assert toFrame <= getSegmentEndFrame(getNbSegments()-1);

		Alignment after = new Alignment(this);
		cutAfterFrame(fromFrame);
		after.cutBeforeFrame(toFrame);

		merge(small);
		merge(after);
	}

	public void clearIndex() {
		segEndFrames=null;
	}

	public void buildIndex() {
		System.out.println("building index");
		segEndFrames = new int[getNbSegments()];
		for (int i=0;i<segEndFrames.length;i++) {
			segEndFrames[i]=getSegmentEndFrame(i);
		}
		if (segEndFrames.length>0)
			System.out.println("index updated "+segEndFrames.length+" "+segEndFrames[segEndFrames.length-1]);
		setFirstSegmentAltered(-1);
	}

	/**
	 * @param fr trame ABSOLUE
	 */
	public int getSegmentAtFrame(int fr) {
		if (segEndFrames==null || getFirstSegmentAltered()>=0)
			buildIndex();
		int s = Arrays.binarySearch(segEndFrames, fr - frameOffset);
		if (s>=0) return s+1;
		return -s-1;
	}

	public int getFirstSegmentAltered() {return firstSegmentModified;}
	public void setFirstSegmentAltered(int seg) {firstSegmentModified=seg;}

	public int getNbSegments() {
		return segments.size();
	}

	public String getSegmentLabel(int segidx) {
		if (segidx<0||segidx>=getNbSegments()) return null;
		assert segidx>=0;
		assert segidx<segments.size();
		return segments.get(segidx);
	}

	public void setSegmentDebFrame(int segidx, int frame) {
		if (segidx<0||segidx>=getNbSegments()) return;
		segmentsDeb.set(segidx, frame);
	}

	public void setSegmentEndFrame(int segidx, int frame) {
		if (segidx<0||segidx>=getNbSegments()) return;
		segmentsFin.set(segidx, frame);
	}

	public void setSegmentSourceManu(int segidx) {
		segmentsSource.set(segidx, ALIGNMENT_SOURCE_MANUAL);
	}

	public void setSegmentSourceEqui(int segidx) {
		segmentsSource.set(segidx, ALIGNMENT_SOURCE_LINEAR);
	}

	public int getSegmentEndFrame(int segidx) {
		if (segidx<0||segidx>=getNbSegments()) return -1;
		return segmentsFin.get(segidx) + frameOffset;
	}

	public int getSegmentDebFrame(int segidx) {
		if (segidx<0||segidx>=getNbSegments()) return -1;
		return segmentsDeb.get(segidx) + frameOffset;
	}

	public void delSegment(int segidx) {
		segments.remove(segidx);
		segmentsDeb.remove(segidx);
		segmentsFin.remove(segidx);
		segmentsSource.remove(segidx);
	}

	public void adjustOffset(int tr) {
		frameOffset =tr;
	}

	/**
	 * @return new segment ID
	 */
	public int addRecognizedSegment(String mot, int framedeb, int frameend, String[] phones, int[] states) {
		assert framedeb>=0;
		assert frameend>framedeb;

		for (int i=0;i<segments.size();i++) {
			if (framedeb<segmentsFin.get(i)) {
				if (frameend<segmentsDeb.get(i)) {
					segments.add(i, mot);
					segmentsDeb.add(i, framedeb);
					segmentsFin.add(i, frameend);
					segmentsSource.add(i, ALIGNMENT_SOURCE_AUTOMATIC);
					if (firstSegmentModified<0) firstSegmentModified=i;
					return i;
				} else {
					throw new Error("ERREUR ADD SEGMENT "+framedeb+" "+frameend+
							" prev: "+i+" "+segmentsDeb.get(i)+"--"+segmentsFin.get(i));
				}
			}
		}
		if (firstSegmentModified<0) firstSegmentModified=segments.size();
		segments.add(mot);
		segmentsDeb.add(framedeb);
		segmentsFin.add(frameend);
		segmentsSource.add(ALIGNMENT_SOURCE_AUTOMATIC);
		return segments.size()-1;
	}

	public void setSegmentLabel(int segidx, String newlabel) {
		assert segidx<segments.size();
		segments.set(segidx, newlabel);
	}

	public int getStartFrame() {return frameOffset;}

	/**
	 * @param lastFrameAcceptable = der trame a conserver, en absolu !
	 */
	public void cutAfterFrame(int lastFrameAcceptable) {
		for (int i=0;i<segments.size();i++) {
			if (segmentsFin.get(i) > lastFrameAcceptable - frameOffset) {
				if (firstSegmentModified<0) firstSegmentModified=i;
				segments = segments.subList(0, i);
				segmentsDeb = segmentsDeb.subList(0, i);
				segmentsFin = segmentsFin.subList(0, i);
				segmentsSource =  segmentsSource.subList(0, i);
				break;
			}
		}
	}

	public void cutBeforeFrame(int fr) {
		for (int i=segments.size()-1;i>=0;i--) {
			if (segmentsDeb.get(i) < fr - frameOffset) {
				i++;
				int z = segments.size();
				segments = segments.subList(i,z);
				segmentsDeb = segmentsDeb.subList(i,z);
				segmentsFin = segmentsFin.subList(i,z);
				segmentsSource =  segmentsSource.subList(i, z);
				break;
			}
		}
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

	/**
	 * @return An array of three alignments (words, phonemes, states)
	 */
	public static Alignment[] backtrack(Token tok) {
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
			Alignment alignMots = new Alignment();
			Alignment alignPhones = new Alignment();
			Alignment alignStates = new Alignment();
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

			return new Alignment[] {alignMots,alignPhones,alignStates};
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
