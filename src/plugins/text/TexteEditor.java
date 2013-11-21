/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.text.*;

import facade.JTransAPI;

import markup.RawTextLoader;
import plugins.text.elements.Element;
import plugins.text.elements.Element_Mot;
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
			normedText = RawTextLoader.normalizeText(normedText);
			setText(normedText);
		}

		setListeElement(RawTextLoader.parseString(normedText, listeTypes));
		
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

	private static final AttributeSet ALIGNED_STYLE = new SimpleAttributeSet() {{
		addAttribute(StyleConstants.CharacterConstants.Foreground, new Color(0x2018a8));
		addAttribute(StyleConstants.CharacterConstants.Italic, true);
	}};

	/**
	 * Sets text from the list of elements and highlights non-text segments
	 * according to the color defined in their respective types.
	 * @see ListeElement#render()
	 */
	public void setTextFromElements() {
		// Create a new style for each type
		AttributeSet[] attr = new AttributeSet[listeTypes.size()];
		for (int i = 0; i < listeTypes.size(); i++) {
			SimpleAttributeSet sas = new SimpleAttributeSet();
			sas.addAttribute(StyleConstants.ColorConstants.Background, listeTypes.get(i).getColor());
			sas.addAttribute(StyleConstants.ColorConstants.Foreground, Color.gray);
			attr[i] = sas;
		}

		// Create a new document instead of using this instance's document to avoid
		// triggering any listeners, which makes the styling process much faster
		StyledDocument doc = new DefaultStyledDocument();

		try {
			doc.insertString(0, listeElement.render(), null);
		} catch (BadLocationException ex) {
			JOptionPane.showMessageDialog(this, ex.toString(), "BadLocationException", JOptionPane.ERROR_MESSAGE);
		}

		// Apply styles
		for (Element el: listeElement) {
			int type = el.getType();			
			if (type >= 0 && type < listeTypes.size())
				doc.setCharacterAttributes(el.start, el.end - el.start, attr[type], true);
		}

		setStyledDocument(doc);
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

	/**
	 * Colorizes range of words with the "aligned" style.
	 * Does not touch non-word elements.
	 * @param fromWord number of the first word (in a list containing only
	 *                    words) to colorize. This is NOT an element index!
	 * @param toWord number of the last word to colorize (inclusive). This is
	 *               NOT an element index!
	 */
	public void colorizeAlignedWords(int fromWord, int toWord) {
		int word = -1;
		int fromCh = -1;
		int toCh = -1;

		for (int i = 0; i < listeElement.size() && word < toWord; i++) {
			Element el = listeElement.get(i);

			if (el instanceof Element_Mot) {
				word++;
				if (word >= fromWord) {
					toCh = el.end;
					if (fromCh < 0)
						fromCh = el.start;
				}
			} else {
				if (fromCh > 0)
					colorizeAlignedChars(fromCh, toCh);
				fromCh = -1;
			}
		}

		if (fromCh > 0)
			colorizeAlignedChars(fromCh, toCh);
	}

	/**
	 * Colorize range of characters with the "aligned" style
	 * @param from first character to colorize
	 * @param to last character to colorize (exclusive)
	 */
	private void colorizeAlignedChars(int from, int to) {
		getStyledDocument().setCharacterAttributes(from, to-from, ALIGNED_STYLE, true);
	}

	public void griseMotsRed(Element_Mot mot, Element_Mot mot2) {
		degrise();
		lastSelectedWord=mot;
		lastSelectedWord2=mot2;
		if (lastSelectedWord==null) return;
		if (lastSelectedWord2==null) return;
		setCaretPosition((lastSelectedWord.start + lastSelectedWord.end)/2);
		select(lastSelectedWord.start, mot2.end);

		setSelectedTextColor(Color.MAGENTA);
		repaint();
		/*
		AttributeSet a = getCharacterAttributes();
		AttributeSet b = styler.addAttribute(a, StyleConstants.ColorConstants.Background, Color.MAGENTA);
		setCharacterAttributes(b, true);
		*/
	}

	public String getMot(Element_Mot mot) {
		try {
			return getText(mot.start, mot.end-mot.start);
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

	/**
	 * Load text/elements into the text editor and colorize the relevant parts.
	 */
	public void setListeElement(ListeElement listeElement) {
		setEditable(false);
		this.listeElement = listeElement;
		JTransAPI.setElts(listeElement);
		setTextFromElements();
	}

	public void setListeTypes(ArrayList<TypeElement> listeTypes) {
		this.listeTypes = listeTypes;
	}
}//class TextEditor
