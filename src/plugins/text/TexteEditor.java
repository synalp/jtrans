/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;


import facade.Project;
import plugins.applis.SimpleAligneur.Aligneur;
import plugins.text.elements.Element;
import plugins.text.elements.Element_Ancre;
import plugins.text.elements.Element_Mot;

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

	public static final String DEFAULT_FONT_NAME = Font.SANS_SERIF;
	public static final int DEFAULT_FONT_SIZE = 13;

    private Aligneur aligneur;
	private Project project;

	public boolean textChanged;

	private Element_Mot highlightedWord = null;
    private JPopupMenu popup = null;

	//----------------------------------------------------------------
	//------------------ Constructor --------------------------------
	//---------------------------------------------------------------
	public TexteEditor(Aligneur aligneur) {
		super();

		setFont(new Font(DEFAULT_FONT_NAME, Font.PLAIN, DEFAULT_FONT_SIZE));

        this.aligneur = aligneur;
		this.project = aligneur.project;
		
		textChanged = false;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                click(e);
            }
        });
	}

    private void click(MouseEvent e) {
        if (popup != null && popup.isVisible()) {
            popup.setVisible(false);
            popup = null;
            return;
        }

        int caret = viewToModel(e.getPoint());

        int idx = project.elts.getIndiceElementAtTextPosi(caret);
        if (idx < 0)
            return;
        Element el = project.elts.get(idx);

        if (e.isPopupTrigger()) {
            if (el instanceof Element_Ancre) {
                anchorPopupMenu((Element_Ancre)el, e);
            }
        } else {
            if (el instanceof Element_Mot) {
                aligneur.selectWord((Element_Mot) el);
            }
        }
    }

    private void anchorPopupMenu(final Element_Ancre anchor, MouseEvent event) {
        popup = new JPopupMenu("Anchor");

        popup.add(new JMenuItem("Adjust time") {{
            addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    aligneur.repositionAnchor(anchor);
                }
            });
        }});

        popup.add(new JMenuItem("Clear alignment around") {{
            addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    project.clearAlignmentAround(anchor);
                    aligneur.setProject(project); // force refresh
                }
            });
        }});


        popup.add(new JMenuItem("Delete") {{
            addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    project.clearAlignmentAround(anchor);
                    project.elts.remove(anchor);
                    aligneur.setProject(project); // force refresh
                }
            });
        }});

        popup.show(this, event.getX(), event.getY());
    }

	private static final AttributeSet ALIGNED_STYLE = new SimpleAttributeSet() {{
		addAttribute(StyleConstants.Foreground, Color.BLUE.darker());
		addAttribute(StyleConstants.Italic, true);
	}};

	private static final AttributeSet HIGHLIGHTED_STYLE = new SimpleAttributeSet() {{
		addAttribute(StyleConstants.Background, Color.LIGHT_GRAY);
	}};

	/**
	 * Sets text from the list of elements and highlights non-text segments
	 * according to the color defined in their respective types.
	 * @see Project#render()
	 */
	public void setTextFromElements() {
		// Create a new style for each type
		AttributeSet[] attr = new AttributeSet[project.types.size()];
		for (int i = 0; i < project.types.size(); i++) {
			SimpleAttributeSet sas = new SimpleAttributeSet();
			sas.addAttribute(StyleConstants.ColorConstants.Background, project.types.get(i).getColor());
			sas.addAttribute(StyleConstants.ColorConstants.Foreground, Color.gray);
			attr[i] = sas;
		}

		// Create a new document instead of using this instance's document to avoid
		// triggering any listeners, which makes the styling process much faster
		StyledDocument doc = new DefaultStyledDocument();

		try {
			doc.insertString(0, project.render(), null);
		} catch (BadLocationException ex) {
			JOptionPane.showMessageDialog(this, ex.toString(), "BadLocationException", JOptionPane.ERROR_MESSAGE);
		}

		// Apply styles
		for (Element el: project.elts) {
			int type = el.getType();			
			if (type >= 0 && type < project.types.size())
				doc.setCharacterAttributes(el.start, el.end - el.start, attr[type], true);
			if (el instanceof Element_Mot && ((Element_Mot) el).posInAlign >= 0)
				doc.setCharacterAttributes(el.start, el.end - el.start, ALIGNED_STYLE, false);
		}

		setStyledDocument(doc);
	}

	/**
	 * Highlights a word by setting its background to another color.
	 * Only one word may be highlighted at a time.
	 */
	public void highlightWord(Element_Mot word) {
		if (word == null || highlightedWord == word)
			return;

		// Only highlight aligned words
		assert word.posInAlign >= 0;

		// Remove highlight on previously highlighted word
		if (highlightedWord != null) {
			getStyledDocument().setCharacterAttributes(
					highlightedWord.start,
					highlightedWord.end - highlightedWord.start,
					ALIGNED_STYLE,
					true);
		}

		getStyledDocument().setCharacterAttributes(
				word.start,
				word.end - word.start,
				HIGHLIGHTED_STYLE,
				false);

		highlightedWord = word;
	}

	/**
	 * Colorizes range of words with the "aligned" style.
	 * Does not touch non-word elements.
	 * This method does not care whether the word is actually aligned
	 * (posInAlign) or not.
	 *
	 * @param fromWord number of the first word (in a list containing only
	 *                    words) to colorize. This is NOT an element index!
	 * @param toWord number of the last word to colorize (inclusive). This is
	 *               NOT an element index!
	 */
	public void colorizeWords(int fromWord, int toWord) {
		int word = -1;
		int fromCh = -1;
		int toCh = -1;

		for (int i = 0; i < project.elts.size() && word < toWord; i++) {
			Element el = project.elts.get(i);

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
	 * Colorizes all aligned words with the "aligned" style.
	 * This method only colorizes words that are aligned properly (posInAlign).
	 */
	public void colorizeAllAlignedWords() {
		int fromCh = -1;
		int toCh = -1;

		for (int i = 0; i < project.elts.size(); i++) {
			Element el = project.elts.get(i);

			if (el instanceof Element_Mot && ((Element_Mot) el).posInAlign > 0) {
				toCh = el.end;
				if (fromCh < 0)
					fromCh = el.start;
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

	public String getMot(Element_Mot mot) {
		try {
			return getText(mot.start, mot.end-mot.start);
		} catch (BadLocationException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Load text/elements into the text editor and colorize the relevant parts.
	 */
	public void setProject(Project project) {
		this.project = project;
		setEditable(false);
		setTextFromElements();
	}
}//class TextEditor
