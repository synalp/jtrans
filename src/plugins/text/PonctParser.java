package plugins.text;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import plugins.applis.SimpleAligneur.Aligneur;
import plugins.text.elements.Element_Commentaire;
import plugins.text.elements.Element_Mot;
import plugins.text.elements.Element_Ponctuation;

public class PonctParser {
	Aligneur aligneur;
	String texte;

	public PonctParser(Aligneur al) {
		aligneur=al;
	}

	public void parse() {
		parse(null);
	}

	void creePonct(char p, ListeElement le) {
		Element_Ponctuation pct = new Element_Ponctuation(p);
		le.add(pct);
//		System.out.println("cree ponct "+p);
	}

	void creeMot(int deb, int finExclue, ListeElement le, Aligneur aligneur) {
		Element_Mot elmot;
		if (aligneur==null)
			elmot = new Element_Mot(texte);
		else
			elmot = new Element_Mot(aligneur.edit);
		elmot.posDebInTextPanel= deb;
		elmot.posFinInTextPanel= finExclue;
		le.add(elmot);
	}

	private final static byte[] winAposbytes = {32,25};
	private final static ByteBuffer winAposb = ByteBuffer.wrap(winAposbytes);
	private final static CharBuffer winApost = winAposb.asCharBuffer();
	private final static char winApos = winApost.get();

	static final char[] charsAisolerGauche = {'*','+','/'};
	static final char[] charsAisolerDroite = {'\'','*','+','/'};

	public static void main(String args[]) {
		// pour connaitre l'ordre des chars
		char[] c = Arrays.copyOf(charsAisolerDroite, charsAisolerDroite.length);
		Arrays.sort(c);
		System.out.println("sorted: "+Arrays.toString(c));
	}

	String ajouteEspaces(String s) {
		if (s==null||s.length()<2) return s;
		StringBuilder res = new StringBuilder();
		{
			char c = s.charAt(0);
			res.append(c);
			if (Arrays.binarySearch(charsAisolerDroite, c)>=0) {
				if (!Character.isWhitespace(s.charAt(1))) res.append(" ");
			}
		}
		for (int i=1;i<s.length()-1;i++) {
			char c = s.charAt(i);
			if (Arrays.binarySearch(charsAisolerGauche, c)>=0) {
				if (!Character.isWhitespace(s.charAt(i-1))) res.append(" ");
			}
			res.append(c);
			if (Arrays.binarySearch(charsAisolerDroite, c)>=0) {
				if (!Character.isWhitespace(s.charAt(i+1))) res.append(" ");
			}
		}
		{
			char c = s.charAt(s.length()-1);
			if (Arrays.binarySearch(charsAisolerGauche, c)>=0) {
				if (!Character.isWhitespace(s.charAt(s.length()-2))) res.append(" ");
			}
			res.append(c);
		}
		return res.toString();
	}

	StyleContext styler=null;
	AttributeSet attpct=null, attcmt=null;

	/**
	 * nouvelle version qui considere la source immutable.
	 * je voudrais aussi lui associer une source, mais on veut aussi pouvoir éditer
	 * le texte dans JTrans... Ce n'est pas grave, car il faut dans tous les cas sauver
	 * le texte final dans un fichier à part, qui devient alors la source !
	 * Le TextSegment n'a pas besoin de connaître la véritable source, du moment que
	 * JTrans la connaît au moment de sauver le fichier .jtrans: c'est dans ce fichier
	 * que la véritable source sera sauvegardée, le texte lui-même ne l'étant pas !
	 * 
	 * Il faudra aussi ajouter jsafran.jar aux libs requises pour compiler jtrans
	 * 
	 * @param textfile
	 * @return
	 */
	public ListeElement parseimmutable(String textfile) {
		ListeElement lst = new ListeElement();
		TextSegments uttSegmenter = new TextSegments();
		uttSegmenter.preloadTextFile(textfile);
		int nmots=0, npcts=0, ncmts=0;
		for (int i=0;i<uttSegmenter.getNbSegments();i++) {
			TextSegments utt = uttSegmenter.tokenizeBasic(i);
			utt.tokenizePonct();
			utt.tokenizeComments();
			// transfo des segments en ListeElement
			for (int j=0;j<utt.getNbSegments();j++) {
				switch (utt.getSegmentType(j)) {
				case ponct:
					Element_Ponctuation pct = new Element_Ponctuation(utt.getSegment(j).trim().charAt(0));
					pct.posdeb=(int)utt.getSegmentStartPos(j);
					pct.posfin=(int)utt.getSegmentEndPos(j);
					lst.add(pct);
					npcts++;
					break;
				case mot:
					Element_Mot mot = new Element_Mot(utt.getSegment(j),(int)utt.getSegmentStartPos(j),(int)utt.getSegmentEndPos(j));
					lst.add(mot);
					nmots++;
					break;
				case comment:
				case bruit:
					int pdeb = (int)utt.getSegmentStartPos(j);
					int pfin = (int)utt.getSegmentEndPos(j);
					Element_Commentaire e = new Element_Commentaire(utt.getSegment(j), pdeb, pfin);
					lst.add(e);
					ncmts++;
					break;
				default:
				}
			}
		}
		System.out.println("parseImmutable: found "+nmots+" "+npcts+" "+ncmts);
		return lst;
	}
	/**
	 * le parsing suivant recupere bien les debuts et fins des mots, mais pour un but d'affichage,
	 * et non de preservation de la source immutable !
	 * En fait, le texte d'origine est transformé avec les quotes, fins de ligne,...
	 * TODO: il faut donc remplacer ce parsing par celui de TextSegments !!
	 * 
	 * @param text
	 * @return
	 */
	public ListeElement parse(String text) {
		texte=text;
		if (aligneur!=null) aligneur.edit.setEditable(false);
		if (aligneur!=null && texte==null) texte=aligneur.edit.getText();
		// pour remplacer les apostrophes "Unicodes" venant d'un copier/coller depuis Word
		texte = texte.replace(winApos, '\'');
		// pour supprimer les carriage return specifiques a Windows...
		texte = texte.replace('\r', ' ');
		texte = ajouteEspaces(texte);
		texte = texte.replace("aujourd' hui", "aujourd'hui");
		if (aligneur!=null) aligneur.edit.setText(texte);

		ListeElement listelts = new ListeElement();
		
		if (aligneur!=null) {
			styler = StyleContext.getDefaultStyleContext();
			// on annule tout precedent formatage
			aligneur.edit.selectAll();
			AttributeSet att = styler.getEmptySet();
			aligneur.edit.setCharacterAttributes(att, true);
			// prepare couleur de ponctuation
			attpct = styler.addAttribute(att, StyleConstants.ColorConstants.Background,Color.orange);
			attcmt = styler.addAttribute(att, StyleConstants.ColorConstants.Background,Color.green);
		}


		int debmot=-1;
		int debcroch=-1, debpar=-1;
		for (int c=0;c<texte.length();c++) {
			char ch = texte.charAt(c);
			switch (ch) {
			case ' ':
			case '\n':
			case '\t':
				if (debmot>=0) {
					creeMot(debmot,c,listelts,aligneur);
					debmot=-1;
				}
				break;
			case ',':
			case '.':
				if (c>0&&c<texte.length()-1) {
					char next=texte.charAt(c+1);
					if (Character.isLetterOrDigit(next) && debmot>=0) {
						// c'est un chiffre ou un acronyme !
					} else {
						creePonct(ch,listelts);
						if (aligneur!=null) {
							aligneur.edit.select(c,c+1);
							aligneur.edit.setCharacterAttributes(attpct, true);
						}
					}
				} else {
					if (debmot>=0) {
						creeMot(debmot,c,listelts,aligneur);
						debmot=-1;
					}
					creePonct(ch,listelts);
					if (aligneur!=null) {
						aligneur.edit.select(c,c+1);
						aligneur.edit.setCharacterAttributes(attpct, true);
					}
				}
				break;
			case '?':
			case '!':
			case ':':
			case '=':
			case ';':
				creePonct(ch,listelts);
				if (aligneur!=null) {
					aligneur.edit.select(c,c+1);
					aligneur.edit.setCharacterAttributes(attpct, true);
				}
				break;
			case '[':
				if (debmot>=0) {
					creeMot(debmot,c,listelts,aligneur);
					debmot=-1;
				}
				debcroch=c;
				break;
			case ']':
				if (debmot>=0) {
					creeMot(debmot,c,listelts,aligneur);
					debmot=-1;
				}
				if (debcroch<0) debcroch=c;
				if (aligneur!=null) {
					aligneur.edit.select(debcroch,c+1);
					aligneur.edit.setCharacterAttributes(attcmt, true);
				}
				debcroch=-1;
				break;
			case '(':
				if (debmot>=0) {
					creeMot(debmot,c,listelts,aligneur);
					debmot=-1;
				}
				debpar=c;
				break;
			case ')':
				if (debmot>=0) {
					creeMot(debmot,c,listelts,aligneur);
					debmot=-1;
				}
				if (debpar<0) debpar=c;
				if (aligneur!=null) {
					aligneur.edit.select(debpar,c+1);
					aligneur.edit.setCharacterAttributes(attcmt, true);
				}
				debpar=-1;
				break;
			default:
				if (debpar<0&&debcroch<0) {
					if (debmot<0) debmot=c;
				}
			}
		}
		// il ne faut pas oublier les fins de ligne
		if (debmot>=0 && debmot<texte.length()) {
			creeMot(debmot,texte.length(),listelts,aligneur);
			debmot=-1;
		}
		System.out.println("all parsed");
		for (Element_Mot e : listelts.getMots()) {
			System.out.println(e.getWordString());
		}
		if (aligneur!=null) {
			aligneur.edit.setListeElement(listelts);
			aligneur.repaint();
		}
		return listelts;
	}
}
