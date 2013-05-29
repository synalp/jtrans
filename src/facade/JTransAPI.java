package facade;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import plugins.applis.SimpleAligneur.Aligneur;
import plugins.signalViewers.spectroPanel.SpectroControl;
import plugins.speechreco.aligners.sphiinx4.AlignementEtat;
import plugins.text.ListeElement;
import plugins.text.TexteEditor;
import plugins.text.elements.Element_Mot;

public class JTransAPI {
	
	public static int getNbWords() {
		if (elts==null) return 0;
		initmots();
		return mots.size();
	}
	public static boolean isBruit(int mot) {
		initmots();
		Element_Mot m = elts.getMot(mot);
		return m.isBruit;
	}
	/**
	 * 
	 * This function automatically align all the words until mot
	 * 
	 * @param mot
	 * @param frdeb can be <0, in which case use the last aligned word.
	 * @param frfin
	 */
	public static void setAlignWord(int mot, int frdeb, int frfin) {
		// TODO: detruit les segments recouvrants

		// equi-aligne les mots precedents
		float curendfr=-1;
		int mot0 = getLastMotPrecAligned(mot);
		if (mot0<mot-1) {
			// il y a des mots non alignes avant
			if (mot0>=0) {
				int mot0seg = mots.get(mot0).posInAlign;
				int mot0endfr = alignementWords.getSegmentEndFrame(mot0seg);
				float frdelta = ((float)(frfin-mot0endfr))/(float)(mot+1-mot0);
				int prevseg = mot0seg;
				float curdebfr = alignementWords.getSegmentEndFrame(prevseg);
				curendfr = alignementWords.getSegmentEndFrame(prevseg)+frdelta;
				for (int i=mot0+1;i<mot;i++) {
					if (frdeb>=0&&curendfr>frdeb) curendfr=frdeb;
					int nnewseg = alignementWords.addRecognizedSegment(mots.get(i).getWordString(), (int)curdebfr, (int)curendfr, null, null);
					mots.get(i).posInAlign=nnewseg;
					prevseg=nnewseg;
					curdebfr = curendfr;
					curendfr+=frdelta;
				}
				if (edit!=null) edit.colorizeAlignedWords(mot0, mot);
			} else {
				// TODO: tous les mots avants ne sont pas alignes
			}
		} else {
			if (mot0>=0) {
				int mot0seg = mots.get(mot0).posInAlign;
				curendfr = alignementWords.getSegmentEndFrame(mot0seg);
			} else curendfr=0;
			if (edit!=null) edit.colorizeAlignedWords(mot, mot);
		}
		
		// aligne le dernier mot
		if (frdeb<0) frdeb=(int)curendfr;
		int newseg = alignementWords.addRecognizedSegment(elts.getMot(mot).getWordString(), frdeb, frfin, null, null);
		alignementWords.setSegmentSourceManu(newseg);
		elts.getMot(mot).posInAlign=newseg;
		
		// TODO: phonetiser et aligner auto les phonemes !!
		
		if (edit!=null) edit.repaint();
	}
	public static void setAlignWord(int mot, float secdeb, float secfin) {
		int curdebfr = SpectroControl.second2frame(secdeb);
		int curendfr = SpectroControl.second2frame(secfin);
		setAlignWord(mot, curdebfr, curendfr);
	}
	private static void setSilenceSegment(int curdebfr, int curendfr, AlignementEtat al) {
		// detruit tous les segments existants deja a cet endroit
		ArrayList<Integer> todel = new ArrayList<Integer>();
		clearAlignFromFrame(curdebfr);
		for (int i=0;i<al.getNbSegments();i++) {
			int d=al.getSegmentDebFrame(i);
			if (d>=curendfr) break;
			int f=al.getSegmentEndFrame(i);
			if (f<curdebfr) continue;
			// il y a intersection
			if (d>=curdebfr&&f<=curendfr) {
				// ancient segment inclu dans nouveau
				todel.add(i);
			} else {
				// TODO: faire les autres cas d'intersection
			}
		}
		for (int i=todel.size()-1;i>=0;i--) al.delSegment(todel.get(i));
		int newseg=al.addRecognizedSegment("SIL", curdebfr, curendfr, null, null);
		al.setSegmentSourceManu(newseg);
	}
	public static void setSilenceSegment(float secdeb, float secfin) {
		int curdebfr = SpectroControl.second2frame(secdeb);
		int curendfr = SpectroControl.second2frame(secfin);
		setSilenceSegment(curdebfr, curendfr, alignementWords);
		setSilenceSegment(curdebfr, curendfr, alignementPhones);
	}
	public static void clearAlignFromFrame(int fr) {
		// TODO
	}
	
	// =========================
	// variables below are duplicate (point to) of variables in the mess of the rest of the code...
	private static ListeElement elts =  null;
	public static AlignementEtat alignementWords = null;
	public static AlignementEtat alignementPhones = null;
	public static TexteEditor edit = null;
	public static Aligneur aligneur = null;
	
	public static void setElts(ListeElement e) {
		elts=e;
		mots = elts.getMots();
	}
	
	// =========================
	private static List<Element_Mot> mots = null;
	
	// =========================
	private static void initmots() {
		if (elts!=null)
			if (mots==null) {
				mots=elts.getMots();
			}
	}
	private static int getLastMotPrecAligned(int midx) {
		initmots();
		for (int i=midx;i>=0;i--) {
//System.out.println("ZZZZZZZZZ "+i+" "+mots.get(i).posInAlign);
			if (mots.get(i).posInAlign>=0) return i;
		}
		return -1;
	}
	
	private static ArrayList<Integer> charpos = null;
	private static ArrayList<Float> secpos = null;
	private static StringBuffer alltext = new StringBuffer();
	private static int debturn=0,endturn=-1;
	private static boolean handleLine(String s, boolean speech) {
		int i=s.indexOf("<Turn ");
		if (i>=0) {
			// TODO: deb/end turn
			if (speech) handleLine(s.substring(0, i), true);
			int j=s.indexOf(">", i);
			return handleLine(s.substring(j+1),true);
		}
		i=s.indexOf("<Sync time=");
		if (i>=0) {
			int j=s.indexOf('"',i)+1;
			int k=s.indexOf('"',j);
			float sec = Float.parseFloat(s.substring(j,k));
			if (secpos.size()==0||secpos.get(secpos.size()-1)<sec) {
				secpos.add(sec);
				charpos.add(alltext.length());
			}
			j=s.indexOf("/>", k);
			return handleLine(s.substring(j+2),true);
		}
		i=s.indexOf("<Event");
		if (i>=0) {
			if (speech) handleLine(s.substring(0, i), true);
			int j=s.indexOf(">", i);
			return handleLine(s.substring(j+1),speech);
		}
		i=s.indexOf("</Turn>");
		if (i>=0) {
			if (speech) {
				handleLine(s.substring(0, i), true);
				alltext.append('\n');
			}
			return handleLine(s.substring(i+7), false);
		}
		if (speech) {
			s=s.trim();
			if (s.length()>0) {
				if (alltext.length()>0) alltext.append(' ');
				alltext.append(s);
				alltext.append(' ');
			}
		}
		return speech;
	}
	public static void loadTRS(String trsfile) {
		// extract all the texts into the text window + keeps time pointers to (the preceding) char
		// Then parse
		// Then, the user MUST PARSE MANUALLY with the menus; this will run setElts(), which will check whether
		// time pointers have been defined
		charpos = new ArrayList<Integer>();
		secpos = new ArrayList<Float>();
		alltext = new StringBuffer();
		try {
			BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(trsfile),Charset.forName("ISO-8859-1")));
			boolean speech=false;
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				speech=handleLine(s,speech);
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// charpos et secpos ont ete bien lus (CHECKED)
		
		TexteEditor zonetexte = TexteEditor.getTextEditor();
		String ss = alltext.toString();
		zonetexte.setText(ss);
		zonetexte.setEditable(false);
		// il ne faut pas que reparse modifie le texte !!!
		zonetexte.reparse(false);
		System.out.println("apres parsing: nelts="+elts.size()+" ancres="+secpos.size());
		// Now that the listElts is known, maps the time pointers to the elts in the liste
		for (int i=0;i<secpos.size();i++) {
			int caretPos = charpos.get(i);
			while (caretPos>0&&(alltext.charAt(caretPos)==' '||alltext.charAt(caretPos)=='\n')) caretPos--;
			int mot = edit.getListeElement().getIndiceMotAtTextPosi(caretPos);
			while (caretPos>0&&mot<0) {
				// TODO: pourquoi "euh" n'est pas un mot ?
				mot = edit.getListeElement().getIndiceMotAtTextPosi(--caretPos);
			}
			setAlignWord(mot, -1, secpos.get(i));
		}
		
		aligneur.caretSensible = true;

		// force la construction de l'index
		alignementWords.clearIndex();
		alignementWords.getSegmentAtFrame(0);
		System.out.println("align index built");
		alignementPhones.clearIndex();
		alignementPhones.getSegmentAtFrame(0);
		edit.getListeElement().refreshIndex();
	}
}
