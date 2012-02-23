/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextPane;
import javax.swing.event.CaretEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.View;
import javax.swing.text.LayeredHighlighter.LayerPainter;

import plugins.speechreco.aligners.sphiinx4.AutoAligner;
import plugins.text.ListeElement;
import plugins.text.elements.Element_Mot;
import plugins.text.elements.Element_Commentaire;
import plugins.text.elements.Element_DebutChevauchement;
import plugins.text.elements.Element_FinChevauchement;
import plugins.text.elements.Element_Ponctuation;
import plugins.text.elements.Segment;
import plugins.text.regexp.TypeElement;

/**
 * permet de selectionner les differents elements pour interpreter le texte:
 * locuteurs, recouvrements, bruits, etc.
 * 
 * En + du texte, on trouve des elements speciaux, qui sont:
 * - locuteur
 * - commentaires
 * - etc
 * 
 * Pour definir les elements "speciaux", il faut ajouter des expressions regulieres
 */
public class TexteEditor extends JTextPane {
	
	//----------------------------------------------------------
	//------------------ Private Fields ------------------------
	//----------------------------------------------------------
	
	//----------- Objects externes -------------
	private ListeElement listeElement;
	
	//----------- Composants ----------
	private List<TypeElement> listeTypes;
	
	//---------- State Flag ---------------
	public boolean textChanged;
	
	public int userSelectedWord = 0;
	
	
	//-------- R�f�rences de travail ---------
	private int nbMot;
	public Element_Mot lastSelectedWord=null;
	public Element_Mot lastSelectedWord2=null;
	
	//ces 4 lignes simplement pour pouvoir 
	//stocker le caractere apostrophe UTF, meme
	// dans un fichier encode en Latin !
	private final static byte[] winAposbytes = {32,25};
	private final static ByteBuffer winAposb = ByteBuffer.wrap(winAposbytes);
	private final static CharBuffer winApost = winAposb.asCharBuffer();
	private final static char winApos = winApost.get();
	
	private File openedTextFile;
	
	StyleContext styler = StyleContext.getDefaultStyleContext();

	// THREAD dont le role est de colorier le texte
	// pour arreter ce thread, il faut appeler colorieur.interrupt()
	class Coloriage extends Thread {
		public Coloriage() {
			super("coloriageThread");
		}
		public void run() {
			try {
				for (;;) {
					ColoriageEvent e = colorOrders.take();
					if (e==ColoriageEvent.endofthread) break;
					e.colorie(textpane);
					e.coloriageDone.put(true);
				}
			} catch (InterruptedException e) {}
			System.out.println("fin du process de coloriage");
		}
	}
	Coloriage colorieur = null;
	public final PriorityBlockingQueue<ColoriageEvent> colorOrders = new PriorityBlockingQueue<ColoriageEvent>();
	private final JTextPane textpane = this;
	
	//----------------------------------------------------------------
	//------------------ Constructor --------------------------------
	//---------------------------------------------------------------
	public TexteEditor() {
		super();
		listeElement = new ListeElement();
		listeTypes=initListeTypes();
		
		textChanged = false;
		colorieur = new Coloriage();
		colorieur.start();
	}//Constructor

	public void fontSize(int size) {
		setFont(Font.decode("timesnewroman-plain-"+size));
		repaint();
	}
	
	/**
	 * je disable toujours le scrolling automatique.
	 * pour le faire quand meme, par exemple lors de la lecture automatique, il faudra l'appeler explicitement !
	 */
	public void scrollRectToVisible(Rectangle aRect){
		return;
	}

	public static List<TypeElement> initListeTypes(){
		List<TypeElement> listeTypes = new ArrayList<TypeElement>();
		
		
		Vector<String> listeLoc = new Vector<String>();
		listeLoc.add("(^|\\n)(\\s)*\\w\\d+\\s");//lettre + chiffres
		listeTypes.add(new TypeElement("Locuteur",listeLoc,Color.GREEN));
		
		Vector<String> listeCom = new Vector<String>();
		listeCom.add("\\{[^\\}]*\\}");// tout ce qui est entre {}
		listeCom.add("\\[[^\\]]*\\]");// tout ce qui est entre []
		listeCom.add("\\+");
		listeTypes.add(new TypeElement("Commentaires",listeCom,Color.YELLOW));
		
		
		Vector<String> listeBruit = new Vector<String>();
		listeBruit.add("(\\w)*\\((\\w)*\\)(\\w)*");
		listeBruit.add("\\s\\*\\*\\*\\s");
		listeBruit.add("\\s\\*\\s");
		listeTypes.add(new TypeElement("Bruits",listeBruit,Color.CYAN));
		
		
		
		Vector<String> listeDebutChevauchement = new Vector<String>();
		listeDebutChevauchement.add("<");
		listeTypes.add(new TypeElement("D�but de chevauchement",listeDebutChevauchement,Color.PINK));
		
		Vector<String> listeFinChevauchement = new Vector<String>();
		listeFinChevauchement.add(">");
		listeTypes.add(new TypeElement("Fin de chevauchement",listeFinChevauchement,Color.PINK));
		
		Vector<String> listePonctuation = new Vector<String>();
		listePonctuation.add("\\?");
		listePonctuation.add("\\:");
		listePonctuation.add("\\;");
		listePonctuation.add("\\,");
		listePonctuation.add("\\.");
		listePonctuation.add("\\!");
		listeTypes.add(new TypeElement("Ponctuation",listePonctuation,Color.ORANGE));
		return listeTypes;
	}//initListeTypes()
	
	
	
	
	//------------------------------------------------------------------------------------
	//------------------------- M�thode de parsing ---------------------------------------
	//-----------------------------------------------------------------------------------

	//On effectue une reparsing 
	/*
	 * Attention ! reparse() detruit la liste actuelle des elements,
	 * or il y a des ancres dans ListeAlignement qui pointaient vers l'ancienne liste,
	 * et on aura donc des objets-mots  dupliques ! (cf. fin de la fonction)
	 */
	public void reparse(){
		int caretPosition = getCaretPosition();
		nbMot = 0;
		
		setIgnoreRepaint(true);
		setVisible(false);
		//pour d�sactiver le d�filement automatique du texte quand il est pars�
		 
		List<Element_Mot> listeMot = listeElement.getMots();
		int listeMotSize = listeMot.size();

		
		ListeElement listeElts = new ListeElement();
		
		String texte = getText();
		
		// pour remplacer les apostrophes "Unicodes" venant d'un copier/coller depuis Word
		texte = texte.replace(winApos, '\'');
		
		// pour supprimer les carriage return specifiques a Windows...
		texte = texte.replace('\r', ' ');
		
		// pour supprimer les espaces ins�cables ainsi que la ponctuation non d�sir�e
		texte = texte.replaceAll("[\\xA0\"=]"," ");
		
		//On ne rajoute un espace derri�re la virgule que si un caractere de mot le suit imm�diatement
		texte = texte.replaceAll("\'(\\S)", "\' $1"); 
		
		setText(texte);
		selectAll();
		// on annule tout precedent formatage
		AttributeSet att = styler.getEmptySet();
		setCharacterAttributes(att, true);
		
		int listeTypesSize = listeTypes.size();
		//-- Pour tous les types d�finis, on highlight le texte --
		ArrayList<Segment> nonText = new ArrayList<Segment>();		
		Vector<String> regexps = null;
		TypeElement typeElement;
		for(int type = 0; type < listeTypesSize; ++type){
			typeElement = listeTypes.get(type);
			regexps = typeElement.getRegexp();
			AttributeSet att2 = 
				styler.addAttribute(att, 
									StyleConstants.ColorConstants.Background,
									typeElement.getColor());
			for (int i=0;i<regexps.size();i++) {
				Pattern pat = Pattern.compile(regexps.get(i));
				Matcher mat = pat.matcher(texte);
				while (mat.find()) {
					int deb = mat.start();
					int fin = mat.end();
					nonText.add(new Segment(deb,fin,type));
					
					if(type == 0){
						//pour tenter de trim la selection
						while(Character.isWhitespace(getText().charAt(deb))){
							++deb;
						}
						fin = deb;
						while(!Character.isWhitespace(getText().charAt(fin))){
							fin++;
						}
					}
					select(deb,fin);
					setCharacterAttributes(att2, true);
				}
			}
		}//for
		
		int nonTextSize = nonText.size();
		
		// on transforme les elements obtenus par ce parsing en elements pour jtrans
		Collections.sort(nonText);
		int precfin = 0;
		boolean parserCettePartie;
		for(int i = 0; i < nonTextSize; ++i){
			parserCettePartie = true;
			int deb = nonText.get(i).deb;
			int fin = nonText.get(i).fin;
			if (precfin > deb) {
					//cas entrecrois� : {-----------[---}-------]
					//on deplace de facon � avoir : {--------------}[-------]
					if (fin > precfin) deb = precfin;
					
					//cas imbriqu� : {------[---]----------}
					//on ne parse pas l'imbriqu�
					else parserCettePartie = false;
			}//if (precfin > deb)
			
			if(parserCettePartie){
				
				// ligne de texte situ�e avant
				if (deb-precfin>0) {
					String ligne = texte.substring(precfin,deb);
					
					parserListeMot(ligne, precfin, listeElts, listeMot, listeMotSize);
					
				}//if (deb-precfin>0) 
				
				//l'�lement en lui m�me
				switch (nonText.get(i).type) {
					case 0: // LOCUTEUR
						int num=0;
						String loc = texte.substring(deb,fin);
						Matcher p = Pattern.compile("\\d").matcher(loc);
						if (p.find()) {
							int posnum = p.start();
							try {
								num=Integer.parseInt(loc.substring(posnum).trim());
								loc=loc.substring(0,posnum).trim();
							} catch (NumberFormatException e) {
								// e.printStackTrace();
							}
						}
						listeElts.addLocuteurElement(loc, num);
						break;
					case 1: // COMMENT
						listeElts.add(new Element_Commentaire(this,deb,fin));
						break;
					case 2: // BRUIT
						Element_Mot elmot = new Element_Mot(this);
						elmot.posDebInTextPanel=deb;
						elmot.posFinInTextPanel=fin;
						listeElts.add(elmot);
						break;
					case 3 : //Debut chevauchement
						listeElts.add(new Element_DebutChevauchement());
						break;
					case 4 : //Fin de chevauchement
						listeElts.add(new Element_FinChevauchement());
						break;
					case 5 : // ponctuation
						listeElts.add(new Element_Ponctuation(texte.substring(deb,fin).charAt(0)));
						break;
					default : System.err.println("HOUSTON, ON A UN PROBLEME ! TYPE PARSE INCONNU");
				}
				precfin = fin;
				

			}//if (parserCettePartie)

			//setChanged();
			//notifyObservers(i);
		}//for
		//ligne de texte situ�e apr�s le dernier �l�ment
		if (texte.length()-precfin>0) {
			String ligne = texte.substring(precfin);
			parserListeMot(ligne, precfin, listeElts, listeMot, listeMotSize);
			
		}
		
		lastSelectedWord = lastSelectedWord2 = null;
		setCaretPosition(caretPosition);
		
		textChanged = false;
		
		// on ecrase l'ancienne liste d'elements avec la nouvelle
		listeElement = listeElts;
		
		setIgnoreRepaint(false);
		setVisible(true);
	}//reparse

	public static String normtext(String texte) {
		// pour remplacer les apostrophes "Unicodes" venant d'un copier/coller depuis Word
		texte = texte.replace(winApos, '\'');
		
		// pour supprimer les carriage return specifiques a Windows...
		texte = texte.replace('\r', ' ');
		
		// pour supprimer les espaces ins�cables ainsi que la ponctuation non d�sir�e
		texte = texte.replaceAll("[\\xA0\"=/]"," ");
		
		//On ne rajoute un espace derri�re la virgule que si un caractere de mot le suit imm�diatement
		texte = texte.replaceAll("\'(\\S)", "\' $1"); 
		
		return texte;
	}
	
	public ListeElement parseString(String normedTexte, List<TypeElement> listeTypes){
		// doit contenir les mots avant le reparsing (?)
		ArrayList<Element_Mot> listeMot = new ArrayList<Element_Mot>();
		ListeElement listeElts = new ListeElement();

		int listeTypesSize = listeTypes.size();
		ArrayList<Segment> nonText = new ArrayList<Segment>();		
		Vector<String> regexps = null;
		TypeElement typeElement;
		for(int type = 0; type < listeTypesSize; ++type){
			typeElement = listeTypes.get(type);
			regexps = typeElement.getRegexp();
			for (int i=0;i<regexps.size();i++) {
				Pattern pat = Pattern.compile(regexps.get(i));
				Matcher mat = pat.matcher(normedTexte);
				while (mat.find()) {
					int deb = mat.start();
					int fin = mat.end();
					nonText.add(new Segment(deb,fin,type));
					
					if(type == 0){
						//pour tenter de trim la selection
						while(Character.isWhitespace(normedTexte.charAt(deb))){
							++deb;
						}
						fin = deb;
						while(!Character.isWhitespace(normedTexte.charAt(fin))){
							fin++;
						}
					}
				}
			}
		}//for
		
		int nonTextSize = nonText.size();
		
		// on transforme les elements obtenus par ce parsing en elements pour jtrans
		Collections.sort(nonText);
		int precfin = 0;
		boolean parserCettePartie;
		for(int i = 0; i < nonTextSize; ++i){
			parserCettePartie = true;
			int deb = nonText.get(i).deb;
			int fin = nonText.get(i).fin;
			if (precfin > deb) {
					//cas entrecrois� : {-----------[---}-------]
					//on deplace de facon � avoir : {--------------}[-------]
					if (fin > precfin) deb = precfin;
					
					//cas imbriqu� : {------[---]----------}
					//on ne parse pas l'imbriqu�
					else parserCettePartie = false;
			}//if (precfin > deb)
			
			if(parserCettePartie){
				
				// ligne de texte situ�e avant
				if (deb-precfin>0) {
					String ligne = normedTexte.substring(precfin,deb);
					parserListeMot(ligne, precfin, listeElts, listeMot, 0);
					
				}//if (deb-precfin>0) 
				
				//l'�lement en lui m�me
				switch (nonText.get(i).type) {
					case 0: // LOCUTEUR
						int num=0;
						String loc = normedTexte.substring(deb,fin);
						Matcher p = Pattern.compile("\\d").matcher(loc);
						if (p.find()) {
							int posnum = p.start();
							try {
								num=Integer.parseInt(loc.substring(posnum).trim());
								loc=loc.substring(0,posnum).trim();
							} catch (NumberFormatException e) {
								// e.printStackTrace();
							}
						}
						listeElts.addLocuteurElement(loc, num);
						break;
					case 1: // COMMENT
						listeElts.add(new Element_Commentaire((JTextPane)null,deb,fin));
						break;
					case 2: // BRUIT
						Element_Mot elmot = new Element_Mot((JTextPane)null);
						elmot.posDebInTextPanel=deb;
						elmot.posFinInTextPanel=fin;
						listeElts.add(elmot);
						break;
					case 3 : //Debut chevauchement
						listeElts.add(new Element_DebutChevauchement());
						break;
					case 4 : //Fin de chevauchement
						listeElts.add(new Element_FinChevauchement());
						break;
					case 5 : // ponctuation
						listeElts.add(new Element_Ponctuation(normedTexte.substring(deb,fin).charAt(0)));
						break;
					default : System.err.println("HOUSTON, ON A UN PROBLEME ! TYPE PARSE INCONNU");
				}
				precfin = fin;
				

			}//if (parserCettePartie)

			//setChanged();
			//notifyObservers(i);
		}//for
		//ligne de texte situ�e apr�s le dernier �l�ment
		if (normedTexte.length()-precfin>0) {
			String ligne = normedTexte.substring(precfin);
			parserListeMot(ligne, precfin, listeElts, listeMot, 0);
			
		}
		return listeElts;
	}//reparse

	
	private void parserListeMot(String ligne, int precfin, 
								ListeElement listeElts, 
								List<Element_Mot> listeMot,
								int listeMotSize){
		int index = 0;
		int debutMot;
		//on parcourt toute la ligne
		while(index < ligne.length()){
			
			//on saute les espaces
			while(index < ligne.length() && 
					Character.isWhitespace(ligne.charAt(index))){
				index++;
			}
			
			debutMot =  index;
			//on avance jusqu'au prochain espace
			
			while((index < ligne.length()) && (!Character.isWhitespace(ligne.charAt(index)))){
					index++;	
			}
			
			if(index > debutMot){
				Element_Mot elmot = new Element_Mot((JTextPane)this);
				elmot.posDebInTextPanel= debutMot+precfin;
				elmot.posFinInTextPanel= index+precfin;
				listeElts.add(elmot);
			}
		}//while(index < ligne.length)
		
	}//parserListeMot
	
	
	public void openTextFile(File textFile){
		try {
			this.openedTextFile = textFile;
			BufferedReader bufferRead = new BufferedReader(new FileReader(textFile));

			StringBuilder strBuilder = new StringBuilder();
			String ligne;
			//pour avoir la premi�re ligne sans \n avant
			if ((ligne = bufferRead.readLine()) != null){
				strBuilder.append(ligne);
			}
			//pour toutes les autres lignes.
			while ((ligne = bufferRead.readLine()) != null){
				strBuilder.append('\n');
				strBuilder.append(ligne);
			}
			bufferRead.close();
			
			setText(strBuilder.toString());
			reparse();
		} catch (FileNotFoundException e) {
			System.err.println("Fichier introuvable : "+textFile.getAbsolutePath());
		} catch (IOException e) {
			System.err.println("Erreur lors de l'ouverture du fichier texte : "
										+textFile.getAbsolutePath());
		}
	}//openTextFile

	
	public void saveTextAs(File file){
		PrintWriter f;
		try {
			f = new PrintWriter(new FileWriter(file.getAbsolutePath()));
			f.println(getText());
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}//saveText
	
	
	public void saveText(){
		saveTextAs(openedTextFile);
	}

	
	public File getOpenedTextFile() {
		return openedTextFile;
	}

	public void setOpenedTextFile(File openedTextFile) {
		this.openedTextFile = openedTextFile;
	}
	
	
	//---------------------------------------------------------------------
	//----------------- M�thode de mise � jour ----------------------------
	//---------------------------------------------------------------------
	/**
	 * @deprecated use Highlighter instead
	 */
	public void inkInColor(Element_Mot mot1, Element_Mot mot2, Color c) {
		// il ne doit pas etre prioritaire par rapport au player
		ColoriageEvent e = new ColoriageEvent(mot1, mot2, c, false, 1);
		colorOrders.add(e);
		try {
			e.waitForColoriageDone();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	public void souligne(Element_Mot mot) {
		setCaretPosition((mot.posDebInTextPanel+mot.posFinInTextPanel)/2);
		AttributeSet a = getCharacterAttributes();
		AttributeSet b = styler.addAttribute(a, StyleConstants.Underline, new Boolean(true));
		select(mot.posDebInTextPanel,mot.posFinInTextPanel);
		setCharacterAttributes(b, true);
	}
	public void unsouligne(Element_Mot mot) {
		setCaretPosition((mot.posDebInTextPanel+mot.posFinInTextPanel)/2);
		AttributeSet a = getCharacterAttributes();
		AttributeSet b = styler.addAttribute(a, StyleConstants.Underline, new Boolean(false));
		select(mot.posDebInTextPanel,mot.posFinInTextPanel);
		setCharacterAttributes(b, true);
	}
	public void degrise() {
		try {
			if (lastSelectedWord != null) {
				if (lastSelectedWord2 != null) {
					ColoriageEvent e = new ColoriageEvent(lastSelectedWord, lastSelectedWord2, Color.white, true, 10);
					colorOrders.add(e);
					e.waitForColoriageDone();
				} else {
					ColoriageEvent e = new ColoriageEvent(lastSelectedWord, lastSelectedWord, Color.white, true, 10);
					colorOrders.add(e);
					e.waitForColoriageDone();
				}
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		lastSelectedWord2=null;
		lastSelectedWord=null;
	}
	/**
	 * on a un acces concurrent au textPane, par le thread du PlayerListerner (qui doit avoir la priorite !)
	 * et par le thread de l'AutoAligner.
	 * 
	 * J'utilise donc une BlockingPriorityQueue pour les evenements de coloriage
	 * et une SynchronizedQueue pour que l'appelant bloque jusqu'a ce que son coloriage ait ete fait
	 * 
	 * @param mot
	 */
	public void griseMot(Element_Mot mot) {
		if (mot==null) return;

		// un seul mot a la fois peut etre grise
		degrise();
		
		// le degrisage est legerement prioritaire, meme si on attend en fait qu'il soit termine !
		ColoriageEvent e = new ColoriageEvent(mot, mot, Color.LIGHT_GRAY, true, 9);
		colorOrders.add(e);
		try {
			e.waitForColoriageDone();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		lastSelectedWord=mot;
	}
	public void griseMotred(Element_Mot mot) {
		degrise();
		lastSelectedWord=mot;
		if (lastSelectedWord==null) return;
		setCaretPosition((lastSelectedWord.posDebInTextPanel+lastSelectedWord.posFinInTextPanel)/2);
		AttributeSet a = getCharacterAttributes();
		AttributeSet b = styler.addAttribute(a, StyleConstants.ColorConstants.Background, Color.MAGENTA);
		select(lastSelectedWord.posDebInTextPanel, lastSelectedWord.posFinInTextPanel);
		setCharacterAttributes(b, true);
	}
	public void colorHighlight(int deb, int fin, Color c) {
		ColoriageEvent e = new ColoriageEvent(deb, fin, c, true, 9);
		colorOrders.add(e);
		try {
			e.waitForColoriageDone();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
    public void colorizeAlignedWords() {
    	ListeElement elts = getListeElement();
    	if (elts!=null) {
    		List<Element_Mot> mots = elts.getMots();
    		if (mots!=null) {
    	    	Element_Mot lastaligned=null;
    	    	for (Element_Mot elmot : mots) {
    	    		if (elmot.posInAlign>=0) lastaligned=elmot;
    	    	}
    	    	if (lastaligned!=null&&mots.size()>0)
    	    		inkInColor(mots.get(0),lastaligned,AutoAligner.alignedColor);
    		}
    	}
    }
	static class FocusHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {

		FocusHighlightPainter(Color color) {
			super(color);
		}

		/**
		 * Paints a portion of a highlight.
		 *
		 * @param g the graphics context
		 * @param offs0 the starting model offset >= 0
		 * @param offs1 the ending model offset >= offs1
		 * @param bounds the bounding box of the view, which is not
		 *        necessarily the region to paint.
		 * @param c the editor
		 * @param view View painting for
		 * @return region in which drawing occurred
		 */
		public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {

			Color color = getColor();

			if (color == null) {
				g.setColor(c.getSelectionColor());
			}
			else {
				g.setColor(color);
			}
			if (offs0 == view.getStartOffset() &&
					offs1 == view.getEndOffset()) {
				// Contained in view, can just use bounds.
				Rectangle alloc;
				if (bounds instanceof Rectangle) {
					alloc = (Rectangle)bounds;
				}
				else {
					alloc = bounds.getBounds();
				}
//				g.drawRect(alloc.x, alloc.y, alloc.width - 1, alloc.height);
				g.drawLine(alloc.x, alloc.y+alloc.height-1, alloc.x+alloc.width-1, alloc.y+alloc.height-1);
				return alloc;
			}
			else {
				// Should only render part of View.
				try {
					// --- determine locations ---
					Shape shape = view.modelToView(offs0, Position.Bias.Forward,
							offs1,Position.Bias.Backward,
							bounds);
					Rectangle r = (shape instanceof Rectangle) ? (Rectangle)shape : shape.getBounds();
//					g.drawRect(r.x, r.y, r.width - 1, r.height);
					g.drawLine(r.x, r.y+r.height-1, r.x+r.width-1, r.y+r.height-1);
					return r;
				} catch (BadLocationException e) {
					// can't render
				}
			}
			// Only if exception
			return null;
		}
	}
	LayerPainter painter4alignedWords = new FocusHighlightPainter(Color.blue);

    public void colorizeAlignedWords(int fromWord, int toWord) {
    	if (fromWord<0||toWord<0) return;
    	Highlighter hh = getHighlighter();
//    	hh.removeAllHighlights();
    	
    	ListeElement elts = getListeElement();
    	if (elts!=null) {
    		List<Element_Mot> mots = elts.getMots();
			try {
				hh.addHighlight(mots.get(fromWord).posDebInTextPanel, mots.get(toWord).posFinInTextPanel, painter4alignedWords);
			} catch (BadLocationException e) {}
    	}    	
/*
    	ListeElement elts = getListeElement();
    	if (elts!=null) {
    		List<Element_Mot> mots = elts.getMots();
        	try {
				hh.addHighlight(mots.get(fromWord).posDebInTextPanel, mots.get(toWord).posFinInTextPanel, DefaultHighlighter.DefaultPainter);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
			*/
/*
			if (mots!=null && fromWord>=0 && toWord>=fromWord && toWord<mots.size()) {
    	    	inkInColor(mots.get(fromWord), mots.get(toWord), AutoAligner.alignedColor);
    		}
    	}
    		*/
    }
	public void griseMotsRed(Element_Mot mot, Element_Mot mot2) {
		degrise();
		lastSelectedWord=mot;
		lastSelectedWord2=mot2;
		if (lastSelectedWord==null) return;
		if (lastSelectedWord2==null) return;
		setCaretPosition((lastSelectedWord.posDebInTextPanel+lastSelectedWord.posFinInTextPanel)/2);
		select(lastSelectedWord.posDebInTextPanel, mot2.posFinInTextPanel);

		setSelectedTextColor(Color.MAGENTA);
		repaint();
		/*
		AttributeSet a = getCharacterAttributes();
		AttributeSet b = styler.addAttribute(a, StyleConstants.ColorConstants.Background, Color.MAGENTA);
		setCharacterAttributes(b, true);
		*/
	}
	
	public boolean textChanged() {
		return textChanged;
	}

	public String getMot(Element_Mot mot) {
		int len = mot.posFinInTextPanel-mot.posDebInTextPanel;
		try {
			return getText(mot.posDebInTextPanel, len);
		} catch (BadLocationException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	//---------------------------------------------------------------------------------
	//-------------- Fonctions de Sauvegarde et Chargement des regexp -----------------
	//---------------------------------------------------------------------------------
	public String exportRegexpAsTxt(){
		StringBuilder strBuilder = new StringBuilder();
		
		final String debutType = "<TYPE=\"";
		final String finBalise = "\">\n";
		final String finType = "</TYPE>\n";
		final String debutColor = "\t<COLOR=\"";
		final String debutRegexp = "\t<REGEXP>\n";
		final String finRegexp = "\t</REGEXP>\n";
		final String doubleTab = "\t\t";
		final char backSlashN = '\n';
		
		for(TypeElement type : listeTypes){
			strBuilder.append(debutType);
			strBuilder.append(type.getNom());
			strBuilder.append(finBalise);
			
			strBuilder.append(debutColor);
			strBuilder.append(type.getColor().getRGB());
			strBuilder.append(finBalise);
			
			strBuilder.append(debutRegexp);
			for(String regexp : type.getRegexp()){
				strBuilder.append(doubleTab);
				strBuilder.append(regexp);
				strBuilder.append(backSlashN);
			}
			strBuilder.append(finRegexp);
			
			strBuilder.append(finType);
		}
		
		return strBuilder.toString();
	}//exportRegexpAsTxt
	
	public void saveRegexpTypesAsFile(String filename){
		try {
			PrintWriter f = new PrintWriter(new FileWriter(filename));
			f.println(exportRegexpAsTxt());
			f.close();
		} catch (IOException e) {
			System.err.println("Erreur lors de la sauvegarde des regexp :\n IOException");
		}
	}//saveRegexpTypeAsFile
	
	public void loadRegexpTypesFromTxt(String txt){
		BufferedReader bufferRead = new BufferedReader(new StringReader(txt));
		parserRegexpFromBufferedReader(bufferRead);
	}//loadRegexpTypeFromTxt(String txt)
	
	public void parserRegexpFromBufferedReader(BufferedReader bufferRead){
		this.listeTypes.clear();
		try {
			String ligne;
			TypeElement type;
			int premierQuote, secondQuote;
			//pour toutes les autres lignes.
			while ((ligne = bufferRead.readLine()) != null){
				if(ligne.length() != 0){
					type = new TypeElement(null,new Vector<String>(),null);
	
	
					//-----------------------------------------
					//------ R�cup�ration du nom du type ------
					//-----------------------------------------
					if (!ligne.trim().startsWith("<TYPE")){
						System.err.println("Erreur de parsing du fichier : \n <TYPE attendu, trouv� : "+ligne);
						return;
					}
	
					premierQuote = ligne.indexOf('"');
					if(premierQuote == -1){
						System.err.println("Erreur de parsing du fichier : \n Nom de type manquant : \n"+ligne);
						return;
					}
					secondQuote = ligne.indexOf('"',premierQuote+1);
					if(secondQuote == -1){
						System.err.println("Erreur de parsing du fichier : \n Nom de type manquant : \n"+ligne);
						return;
					}
					type.setNom(ligne.substring(premierQuote+1, secondQuote));
					//-------------------------------------
					//------ R�cup�ration de la couleur ----
					//-----------------------------------------
					ligne = bufferRead.readLine();
					if (ligne == null){
						System.err.println("Erreur de parsing du fichier : \n Fin de fichier non attendue");
						return;
					}
	
					if (!ligne.trim().startsWith("<COLOR")){
						System.err.println("Erreur de parsing du fichier : \n <COLOR attendu, trouv� : "+ligne);
						return;
					}
	
	
					premierQuote = ligne.indexOf('"');
					if(premierQuote == -1){
						System.err.println("Erreur de parsing du fichier : \n Num�ro de couleur manquant : \n"+ligne);
						return;
					}
					secondQuote = ligne.indexOf('"',premierQuote+1);
					if(secondQuote == -1){
						System.err.println("Erreur de parsing du fichier : \n Num�ro de couleur manquant : \n"+ligne);
						return;
					}
	
					try {
						type.setColor(new Color(Integer.parseInt(ligne.substring(premierQuote+1,secondQuote))));
					}
					catch (NumberFormatException nfe){
						System.err.println("Erreur de parsing du fichier : \n Num�ro de couleur manquant, ce n'est pas un chiffre : \n"+ligne);
						return;
					}
	
					//-----------------------------------------
					//------- R�cup�ration des regexp ----------
					//-----------------------------------------
					ligne = bufferRead.readLine();
					if (ligne == null){
						System.err.println("Erreur de parsing du fichier : \n Fin de fichier non attendue \n il manque les regexp !");
						return;
					}
	
					if(!ligne.trim().equals("<REGEXP>")){
						System.err.println("Erreur de parsing du fichier : \n <REGEXP> attendu, trouv� : \n"+ligne);
						return;
					}
	
					while( !(ligne = bufferRead.readLine()).trim().equals("</REGEXP>") ){
						if (ligne == null){
							System.err.println("Erreur de parsing du fichier : \n Fin de fichier non attendue \n il manque les regexp !");
							return;
						}
	
						type.getRegexp().add(ligne.trim());
					}//whileREGEXP
	
	
					//-----------------------------------------
					//---- Verification de la balise fin -------
					//-----------------------------------------
	
					ligne = bufferRead.readLine();
					if (ligne == null){
						System.err.println("Erreur de parsing du fichier : \n Fin de fichier non attendue \n il manque les regexp !");
						return;
					}
	
					if(!ligne.trim().equals("</TYPE>")){
						System.err.println("Erreur de parsing du fichier : \n </TYPE> attendu, trouv� : \n"+ligne);
						return;
					}
	
					listeTypes.add(type);
				}
			}//while
		} catch (IOException e){
			System.err.println("Erreur lors de l'ouverture du fichier de regexp :\n IOException");
		}
	}//loadRegexpTypesFromBufferedReader(BufferedReader bufferRead){

	public void loadRegexpTypesFromFile(String filename){
		this.listeTypes.clear();
		BufferedReader bufferRead;
		try {
			bufferRead = new BufferedReader(new FileReader(filename));
			parserRegexpFromBufferedReader(bufferRead);
			bufferRead.close();
		} catch (FileNotFoundException e) {
			System.err.println("Erreur lors de l'ouverture du fichier de regexp :\n FileNotFound");
		} catch (IOException e) {
			System.err.println("Erreur lors de l'ouverture du fichier de regexp :\n IOException");
		}
	}//loadRegexpTypesFromFile
	
	
	
	//---------------------------------------------------------------------------------
	//--------------------------- Classes internes ------------------------------------
	//---------------------------------------------------------------------------------

	public int getIndiceMotUnderCaret(CaretEvent e) {
		return listeElement.getIndiceElementAtTextPosi(e.getDot());
	}
	
	public List<TypeElement> getListeTypes() {
		return listeTypes;
	}

	public ListeElement getListeElement() {
		return listeElement;
	}

	public void setListeElement(ListeElement listeElement) {
		this.listeElement = listeElement;
	}

	public void setListeTypes(ArrayList<TypeElement> listeTypes) {
		this.listeTypes = listeTypes;
	}


	
}//class TextEditor
