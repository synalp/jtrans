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
import java.util.*;
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

import facade.JTransAPI;

import facade.TextParser;
import plugins.speechreco.aligners.sphiinx4.AutoAligner;
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
	private ArrayList<TypeElement> listeTypes;
	public static final TypeElement DEFAULT_TYPES[] = {
			new TypeElement("Locuteur", Color.GREEN,
					"(^|\\n)(\\s)*\\w\\d+\\s"),

			new TypeElement("Commentaire", Color.YELLOW,
					"\\{[^\\}]*\\}",
					"\\[[^\\]]*\\]",
					"\\+"),

			new TypeElement("Bruit", Color.CYAN,
					"(\\w)*\\((\\w)*\\)(\\w)*",
					"\\s\\*\\*\\*\\s",
					"\\s\\*\\s"),

			new TypeElement("Début de chevauchement", Color.PINK,
					"<"),

			new TypeElement("Fin de chevauchement", Color.PINK,
					">"),

			new TypeElement("Ponctuation", Color.ORANGE,
					"\\?",
					"\\:",
					"\\;",
					"\\,",
					"\\.",
					"\\!"),

			new TypeElement("Ancre", new Color(0xddffaa)),
	};
	
	//---------- State Flag ---------------
	public boolean textChanged;
	
	public int userSelectedWord = 0;
	
	
	//-------- R�f�rences de travail ---------
	private int nbMot;
	public Element_Mot lastSelectedWord=null;
	public Element_Mot lastSelectedWord2=null;
	
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
	
	private static TexteEditor singleton = null;
	public static TexteEditor getTextEditor() {
		return singleton;
	}
	
	//----------------------------------------------------------------
	//------------------ Constructor --------------------------------
	//---------------------------------------------------------------
	public TexteEditor() {
		super();
		listeElement = new ListeElement();
		listeTypes = new ArrayList<TypeElement>(Arrays.asList(DEFAULT_TYPES));
		
		textChanged = false;
		colorieur = new Coloriage();
		colorieur.start();
		singleton=this;
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
	
	
	//------------------------------------------------------------------------------------
	//------------------------- M�thode de parsing ---------------------------------------
	//-----------------------------------------------------------------------------------

	/*
	 * Attention ! reparse() detruit la liste actuelle des elements,
	 * or il y a des ancres dans ListeAlignement qui pointaient vers l'ancienne liste,
	 * et on aura donc des objets-mots  dupliques ! (cf. fin de la fonction)
	 */
	public void reparse(boolean modifytxt) {
		int caretPosition = getCaretPosition();
		nbMot = 0;
		
		setIgnoreRepaint(true);
		setVisible(false);
		//pour d�sactiver le d�filement automatique du texte quand il est pars�

		String normedText = getText();
		if (modifytxt) {
			normedText = TextParser.normalizeText(normedText);
			setText(normedText);
		}

		List<Segment> nonText = TextParser.findNonTextSegments(normedText, listeTypes);
		setListeElement(TextParser.parseString(normedText, nonText));
		highlightNonTextSegments(nonText);
		
		lastSelectedWord = lastSelectedWord2 = null;
		setCaretPosition(caretPosition);
		
		textChanged = false;

		setIgnoreRepaint(false);
		setVisible(true);
	}//reparse


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
			reparse(true);
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
	 * Highlight non-text segments according to the color defined in their
	 * respective types.
	 */
	public void highlightNonTextSegments(List<Segment> nonTextSegments) {
		String text = getText();
		int textLength = text.length();

		// Clear all existing formatting
		selectAll();
		AttributeSet attr = styler.getEmptySet();
		setCharacterAttributes(attr, true);

		// Create a new style for each type
		AttributeSet[] attr2 = new AttributeSet[listeTypes.size()];
		for (int i = 0; i < listeTypes.size(); i++) {
			attr2[i] = styler.addAttribute(attr,
					StyleConstants.ColorConstants.Background,
					listeTypes.get(i).getColor());
		}

		// Highlight segments
		for (Segment seg: nonTextSegments) {
			int start = seg.deb;
			int end = seg.fin;

			while (start < end &&
					start < textLength &&
					Character.isWhitespace(text.charAt(start)))
			{
				start++;
			}

			while (start < end &&
					end < textLength &&
					!Character.isWhitespace(text.charAt(end)))
			{
				end--;
			}

			if (start < end && start < text.length()) {
				select(start, end);
				setCharacterAttributes(attr2[seg.type], true);
			}
		}
	}

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
		AttributeSet b = styler.addAttribute(a, StyleConstants.Underline, true);
		select(mot.posDebInTextPanel,mot.posFinInTextPanel);
		setCharacterAttributes(b, true);
	}
	public void unsouligne(Element_Mot mot) {
		setCaretPosition((mot.posDebInTextPanel+mot.posFinInTextPanel)/2);
		AttributeSet a = getCharacterAttributes();
		AttributeSet b = styler.addAttribute(a, StyleConstants.Underline, false);
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
		
		for (TypeElement type: listeTypes) {
			strBuilder.append(debutType);
			strBuilder.append(type.getName());
			strBuilder.append(finBalise);
			
			strBuilder.append(debutColor);
			strBuilder.append(type.getColor().getRGB());
			strBuilder.append(finBalise);
			
			strBuilder.append(debutRegexp);
			for (Pattern pattern: type.getPatterns()) {
				strBuilder.append(doubleTab);
				strBuilder.append(pattern.pattern());
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
					String name = null;
					Color color = null;
					ArrayList<String> regexps = new ArrayList<String>();
	
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
					name = ligne.substring(premierQuote+1, secondQuote);
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
						color = new Color(Integer.parseInt(
								ligne.substring(premierQuote+1, secondQuote)));
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

						regexps.add(ligne.trim());
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
	
					listeTypes.add(new TypeElement(name, color, regexps));
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
		JTransAPI.setElts(listeElement);
	}

	public void setListeTypes(ArrayList<TypeElement> listeTypes) {
		this.listeTypes = listeTypes;
	}


	
}//class TextEditor
