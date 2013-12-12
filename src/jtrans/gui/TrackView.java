/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package jtrans.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;


import jtrans.elements.Word;
import jtrans.facade.Project;
import jtrans.elements.ElementList;
import jtrans.elements.Element;
import jtrans.elements.Anchor;
import jtrans.facade.Track;
import jtrans.utils.TimeConverter;

public class TrackView extends JTextPane {

	public static final String DEFAULT_FONT_NAME = Font.SANS_SERIF;
	public static final int DEFAULT_FONT_SIZE = 13;

    private JTransGUI aligneur;
	private Project project;
	private Track track;

	private Word highlightedWord = null;
    private JPopupMenu popup = null;

	//----------------------------------------------------------------
	//------------------ Constructor --------------------------------
	//---------------------------------------------------------------
	public TrackView(JTransGUI aligneur, Track track) {
		super();

		setFont(new Font(DEFAULT_FONT_NAME, Font.PLAIN, DEFAULT_FONT_SIZE));

        this.aligneur = aligneur;
		setTrack(aligneur.project, track);

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

		int idx = track.elts.getIndiceElementAtTextPosi(caret);
		if (idx < 0)
			return;
		Element el = track.elts.get(idx);

		if (e.isPopupTrigger()) {
			if (el instanceof Word) {
				wordPopupMenu((Word)el, e);
			} else if (el instanceof Anchor) {
				anchorPopupMenu((Anchor)el, e);
			}
		} else {
			if (el instanceof Word) {
				aligneur.selectWord((Word)el, track, this);
			}
		}
	}

	/**
	 * Dialog box to create an anchor before or after a certain word.
	 * @param before If true, the new anchor will be placed before the word in
	 *               the element list. If false, it'll be placed after the word.
	 */
	private void newAnchorNextToWord(Word word, boolean before) {
		JTransGUI.REIMPLEMENT_DEC2013(); /* TODO PARALLEL TRACKS
		ElementList.Neighborhood<Anchor> range =
				project.elts.getNeighbors(word, Anchor.class);

		float initialPos;

		if (word.posInAlign < 0) {
			float endOfAudio = TimeConverter.frame2sec((int)aligneur.audioSourceTotalFrames);
			initialPos = before?
					(range.prev!=null? range.prev.seconds: 0) :
					(range.next!=null? range.next.seconds: endOfAudio);
		} else if (before) {
			initialPos = TimeConverter.frame2sec(
					project.words.getSegmentDebFrame(word.posInAlign));
		} else {
			initialPos = TimeConverter.frame2sec(
					project.words.getSegmentEndFrame(word.posInAlign));
		}

		String positionString = JOptionPane.showInputDialog(aligneur.jf,
				String.format("Enter position for new anchor to be inserted\n%s '%s' (in seconds):",
						before? "before": "after", word.getWordString()),
				initialPos);

		if (positionString == null)
			return;

		float newPos = Float.parseFloat(positionString);

		if (aligneur.enforceLegalAnchor(range, newPos)) {
			Anchor anchor = new Anchor(newPos);
			project.elts.add(project.elts.indexOf(word) + (before?0:1), anchor);
			project.clearAlignmentAround(anchor);
			aligneur.setProject(project);
		}
		*/
	}

	private void wordPopupMenu(final Word word, MouseEvent event) {
		popup = new JPopupMenu("Word");

		popup.add(new JMenuItem("New anchor before '" + word.getWordString() + "'") {{
			addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					newAnchorNextToWord(word, true);
				}
			});
		}});

		popup.add(new JMenuItem("New anchor after '" + word.getWordString() + "'") {{
			addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					newAnchorNextToWord(word, false);
				}
			});
		}});

		popup.show(this, event.getX(), event.getY());
	}

    private void anchorPopupMenu(final Anchor anchor, MouseEvent event) {
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
			JTransGUI.REIMPLEMENT_DEC2013(); /* TODO PARALLEL TRACKS
            addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    project.clearAlignmentAround(anchor);
                    aligneur.setProject(project); // force refresh
                }
            });
            */
        }});


        popup.add(new JMenuItem("Delete") {{
			JTransGUI.REIMPLEMENT_DEC2013(); /* TODO PARALLEL TRACKS
            addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    project.clearAlignmentAround(anchor);
                    project.elts.remove(anchor);
                    aligneur.setProject(project); // force refresh
                }
            });
            */
        }});

        popup.show(this, event.getX(), event.getY());
    }

	private static final AttributeSet ALIGNED_STYLE = new SimpleAttributeSet() {{
		addAttribute(StyleConstants.Foreground, Color.BLUE.darker());
		addAttribute(StyleConstants.Italic, true);
	}};

	private static final AttributeSet UNALIGNED_STYLE = new SimpleAttributeSet() {{
		addAttribute(StyleConstants.Foreground, Color.RED.darker());
		addAttribute(StyleConstants.Underline, true);
	}};

	private static final AttributeSet HIGHLIGHTED_STYLE = new SimpleAttributeSet() {{
		addAttribute(StyleConstants.Background, Color.LIGHT_GRAY);
	}};

	/**
	 * Sets text from the list of elements and highlights non-text segments
	 * according to the color defined in their respective types.
	 * @see Track#render()
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
			doc.insertString(0, track.render(), null);
		} catch (BadLocationException ex) {
			JOptionPane.showMessageDialog(this, ex.toString(), "BadLocationException", JOptionPane.ERROR_MESSAGE);
		}

		// Apply styles
		for (Element el: track.elts) {
			int type = el.getType();
			if (type >= 0 && type < project.types.size())
				doc.setCharacterAttributes(el.start, el.end - el.start, attr[type], true);
			if (el instanceof Word && ((Word) el).posInAlign >= 0)
				doc.setCharacterAttributes(el.start, el.end - el.start, ALIGNED_STYLE, false);
		}

		setStyledDocument(doc);
	}

	/**
	 * Highlights a word by setting its background to another color.
	 * Only one word may be highlighted at a time.
	 */
	public void highlightWord(Word word) {
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
	 * Colorizes a range of words with the "aligned" or "unaligned" style
	 * according to their posInAlign.
	 * Does not touch non-word elements.
	 *
	 * @param fromWord number of the first word (in a list containing only
	 *                    words) to colorize. This is NOT an element index!
	 * @param toWord number of the last word to colorize (inclusive). This is
	 *               NOT an element index!
	 */
	public void colorizeWords(int fromWord, int toWord) {
		int word = 0;
		StyledDocument doc = getStyledDocument();

		for (int i = 0; i < track.elts.size() && word <= toWord; i++) {
			Element el = track.elts.get(i);
			Word elWord = el instanceof Word? (Word)el: null;

			if (elWord != null) {
				if (word >= fromWord) {
					doc.setCharacterAttributes(
							elWord.start,
							elWord.end-elWord.start,
							elWord.posInAlign >= 0? ALIGNED_STYLE: UNALIGNED_STYLE,
							true);
				}
				word++;
			}
		}
	}

	/**
	 * Colorize range of characters with the "aligned" style
	 * @param from first character to colorize
	 * @param to last character to colorize (exclusive)
	 */
	private void colorizeAlignedChars(int from, int to) {
		getStyledDocument().setCharacterAttributes(from, to-from, ALIGNED_STYLE, true);
	}

	public String getMot(Word mot) {
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
	public void setTrack(Project project, Track track) {
		this.project = project;
		this.track = track;
		setEditable(false);
		setTextFromElements();
	}
}//class TextEditor
