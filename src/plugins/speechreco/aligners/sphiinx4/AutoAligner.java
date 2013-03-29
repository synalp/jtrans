/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

package plugins.speechreco.aligners.sphiinx4;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import plugins.speechreco.confidenceMeasure.CMStats;
import plugins.text.TexteEditor;
import plugins.text.elements.Element_Mot;

/**
 * Cette classe réalise un bloc-Viterbi:
 * - lance l'alignement sur un segment avec S4ForceAlignBlocViterbi
 * - merge le resultat dans l'alignement global
 * - recalcule le nouveau debut et itere
 * 
 * Attention : AlignementEtat ne contient QUE les mots prononcés (et alignés) et les silences
 * AutoAligner fait le lien entre ces mots prononcés et la liste de mots affichée, qui peut contenir des
 * mots optionnels non prononcés.
 * 
 * @author xtof
 *
 */
public class AutoAligner extends Thread {
	public static boolean batch = false;
	S4ForceAlignBlocViterbi s4blocViterbi = null;
	String wavname;
	TexteEditor editor=null;
	AlignementEtat alignementMots, alignementPhones, alignementEtats;
	private boolean killthread = false;
	final private Thread autoalignerthread = this;

	// contient les mots du texte qui peuvent être prononcés:
	String[] mots;
	// contient l'index du segment audio dans alignement ou -1 si le mot n'est pas prononcé
	private int[] mots2segidx;

	public final static Color alignedColor = Color.decode("0x3B3BC9");

	public S4ForceAlignBlocViterbi getS4aligner() {
		if (s4blocViterbi==null) {
			s4blocViterbi=S4ForceAlignBlocViterbi.getS4Aligner(wavname);
			s4blocViterbi.setMots(mots);
		}
		return s4blocViterbi;
	}

	private AutoAligner(String wavfile, String[] xmots, TexteEditor edit, AlignementEtat alignement, AlignementEtat alPhones){
		this(wavfile,xmots,edit,alignement,alPhones,null);
	}
	private AutoAligner(String wavfile, String[] xmots, TexteEditor edit, AlignementEtat alignement, AlignementEtat alPhones, AlignementEtat alStates){
		super("AutoAligner");
		wavname=wavfile;
		editor=edit;
		alignementMots=alignement;
		alignementPhones=alPhones;
		alignementEtats=alStates;
		if (xmots==null) {
			// calcul des mots a partie de l'editeur
			List<Element_Mot>  lmots = editor.getListeElement().getMots();
			xmots = new String[lmots.size()];
			for (int i=0;i<lmots.size();i++) {
				xmots[i] = lmots.get(i).getWordString();
			}
		}
		mots=xmots;
		System.out.println("getautoaligner nwors "+mots.length);
		mots2segidx=new int[mots.length];
		Arrays.fill(mots2segidx, -1);
		// si alignement existe deja, le recopier ici !
		mots2segidx = alignement.matchWithText(mots);
	}
	private static AutoAligner unik = null;
	public static AutoAligner getAutoAligner(String wavfile, String[] mots, TexteEditor edit, AlignementEtat alignement, AlignementEtat alPhones) {
		if (unik==null) {
			unik = new AutoAligner(wavfile, mots, edit, alignement, alPhones);
		} else {
			unik.stopAutoAlign();
			unik = new AutoAligner(wavfile, mots, edit, alignement, alPhones);
		}
		unik.killthread=false;
		unik.start();
		return unik;
	}

	/**
	 * cette fonction est appelée lorsqu'on utilise le menu "File - Quit"
	 */
	public void terminateAll() {
		if (s4blocViterbi==null) return;
		try {
			s4blocViterbi.input2process.put(S4AlignOrder.terminationOrder);
			synchronized (S4AlignOrder.terminationOrder) {
				S4AlignOrder.terminationOrder.wait();
			}
			System.exit(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void stopAutoAlign() {
		killthread=true;
		// on n'attend pas si le thread est mort
		if (!autoalignerthread.isAlive()) return;
		// attend que l'aligner soit stoppe
		synchronized (autoalignerthread) {
			try {
				autoalignerthread.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void wait4finished() {
		try {
			synchronized (autoalignerthread) {
				autoalignerthread.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		s4blocViterbi=S4ForceAlignBlocViterbi.getS4Aligner(wavname);
		s4blocViterbi.setMots(mots);
		try {
			System.out.println("autoaligner thread started "+killthread);
			while (!killthread) {
				int nmots = mots.length;
				
				// cherche le premier mot a partir duquel il faut aligner
				int premierMotNotAligned = mots.length-1;
				while (premierMotNotAligned>=0&&mots2segidx[premierMotNotAligned]<0) premierMotNotAligned--;
				premierMotNotAligned++;
				if (premierMotNotAligned>=nmots) {
					System.out.println("ALL WORDS ALIGNED ! "+nmots);
					break;
				}
				
				// cherche la premiere trame a partir de laquelle il faut aligner
				int tr=0;
				{
					int motidx = premierMotNotAligned-1;
					while (motidx>=0&&mots2segidx[motidx]<0) motidx--;
					if (motidx>=0) {
						tr=alignementMots.getSegmentEndFrame(mots2segidx[motidx]);
						System.out.println("lookfrom frame "+tr+" "+mots2segidx[motidx]+" "+mots[motidx]);
					}
				}
				System.out.println("looking for align from word "+premierMotNotAligned+" "+mots[premierMotNotAligned]+" "+tr);
				// il faut detruire les SIL qui peuvent rester de l'iteration precedente a partir de tr ??
				// alignement.cutAfterFrame(tr);
				S4AlignOrder order = new S4AlignOrder(premierMotNotAligned, tr);
				if (batch) {
					order = new S4AlignOrder(premierMotNotAligned, tr, nmots, -1);
				}
				s4blocViterbi.input2process.put(order);
				synchronized(order) {
					order.wait();
				}
				if (order.alignWords!=null) {
					order.alignWords.adjustOffset(tr);
					order.alignPhones.adjustOffset(tr);
					order.alignStates.adjustOffset(tr);
					System.out.println("================================= ALIGN FOUND");
					System.out.println(order.alignWords.toString());
					String[] wordsThatShouldBeAligned = Arrays.copyOfRange(mots, premierMotNotAligned, mots.length);
					System.out.println("wordsthatshouldbealigned "+Arrays.toString(wordsThatShouldBeAligned));
					int[] locmots2segidx = order.alignWords.matchWithText(wordsThatShouldBeAligned);
					int nsegsbefore = alignementMots.merge(order.alignWords);
					for (int i=0;i<locmots2segidx.length;i++) {
						if (locmots2segidx[i]>=0)
							mots2segidx[premierMotNotAligned+i]=locmots2segidx[i]+nsegsbefore;
						else
							mots2segidx[premierMotNotAligned+i]=-1;
					}
					System.out.println("mots2segs "+locmots2segidx.length+" "+Arrays.toString(mots2segidx));
					int lastMotAligned = -1;
					if (editor!=null) {
						lastMotAligned=editor.getListeElement().importAlign(mots2segidx,premierMotNotAligned);
						editor.getListeElement().refreshIndex();
					}
					alignementPhones.merge(order.alignPhones);
					if (alignementEtats!=null) alignementEtats.merge(order.alignStates);
					System.out.println("merge "+order.alignWords.getNbSegments()+" "+alignementMots.getNbSegments());
					System.out.println(alignementMots);
					if (lastMotAligned>=0) {
						if (premierMotNotAligned==0)
							editor.colorizeAlignedWords(premierMotNotAligned,lastMotAligned-1);
						else
							editor.colorizeAlignedWords(premierMotNotAligned-1,lastMotAligned-1);
					}
					CMStats.newAlignedSegment(premierMotNotAligned, lastMotAligned-1, order.alignWords, nmots);
				} else {
					System.out.println("================================= ALIGN FOUND null");
					// TODO
					// on arrete autoaligner ?
					break;
				}
			}
			synchronized (alignementMots) {
				alignementMots.notifyAll();
			}
			Thread.sleep(100);
			synchronized (autoalignerthread) {
				autoalignerthread.notifyAll();
			}
			System.out.println("autoaligner thread killed");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	// cette fonction n'est plus appelle depuis nulle part: cf. AlignementEtat.matchWithText
	public void checkWithText(AlignementEtat align, int premierMotDuTexte) {
		int segidx=0;
		List<Element_Mot> lmots=null;
		if (editor!=null)
			lmots = editor.getListeElement().getMots();
		for (int motidx=premierMotDuTexte;motidx<mots.length;motidx++) {
			// on se positionne au prochain mot prononcé
			while (segidx<align.getNbSegments() && align.getSegmentLabel(segidx).equals("SIL")) {
				segidx++;
			}
			if (segidx>=align.getNbSegments()) {
				// l'alignement s'arrete au milieu des mots avec bloc-viterbi
				break;
			}
			// si le mot ne correspond pas, alors on suppose que c'est un mot optionnel
			int walidx=-1;
			if (align.getSegmentLabel(segidx).equals(mots[motidx])) {
				System.out.println("checkwithtext: pronunced token "+mots[motidx]+" "+segidx);
				walidx=segidx++;
			} else {
				System.out.println("checkwithtext: optional token "+mots[motidx]);
			}
			mots2segidx[motidx]=walidx;
			if (editor!=null) {
				lmots.get(motidx).posInAlign=walidx;
			}
		}
	}
}
