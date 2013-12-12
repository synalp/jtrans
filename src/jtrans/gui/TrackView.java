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

	private static final AttributeSet ALIGNED_STYLE =
			new SimpleAttributeSet() {{
				addAttribute(StyleConstants.Foreground, Color.BLUE.darker());
				addAttribute(StyleConstants.Italic, true);
			}};

	private static final AttributeSet UNALIGNED_STYLE =
			new SimpleAttributeSet() {{
				addAttribute(StyleConstants.Foreground, Color.RED.darker());
				addAttribute(StyleConstants.Underline, true);
			}};

	private static final AttributeSet HIGHLIGHTED_STYLE =
			new SimpleAttributeSet() {{
				addAttribute(StyleConstants.Background, Color.LIGHT_GRAY);
			}};

    private JTransGUI gui;
	private Project project;
	private Track track;

	private Word highlightedWord = null;
    private JPopupMenu popup = null;


	public TrackView(JTransGUI gui, Track track) {
		super();

		setFont(new Font(DEFAULT_FONT_NAME, Font.PLAIN, DEFAULT_FONT_SIZE));

        this.gui = gui;
		this.project = gui.project;
		this.track = track;
		setEditable(false);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				click(e);
			}
		});

		setTextFromElements();
	}


	//==========================================================================
	// UI EVENTS
	//==========================================================================


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
			if (el instanceof Word)
				selectWord((Word)el);
		}
	}


	private void wordPopupMenu(final Word word, MouseEvent event) {
		popup = new JPopupMenu("Word");

		popup.add(new JMenuItem(String.format("New anchor before '%s'",
				word.getWordString()))
		{{
			addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					newAnchorNextToWord(word, true);
				}
			});
		}});

		popup.add(new JMenuItem(String.format("New anchor after '%s'",
				word.getWordString()))
		{{
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
					repositionAnchor(anchor);
					setTextFromElements();
				}
			});
		}});

		popup.add(new JMenuItem("Clear alignment around") {{
			addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					track.clearAlignmentAround(anchor);
					setTextFromElements();
				}
			});
		}});


		popup.add(new JMenuItem("Delete") {{
			addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					track.clearAlignmentAround(anchor);
					track.elts.remove(anchor);
					setTextFromElements();
				}
			});
		}});

		popup.show(this, event.getX(), event.getY());
	}


	//==========================================================================
	// UI ACTIONS
	//==========================================================================

	/**
	 * Highlights a word and sets the playback position to the beginning of the
	 * word.
	 */
	private void selectWord(Word word) {
		PlayerGUI player = gui.ctrlbox.getPlayerGUI();
		boolean replay = player.isPlaying();
		player.stopPlaying();

		if (word.posInAlign >= 0) {
			highlightWord(word);
			gui.setCurPosInSec(TimeConverter.frame2sec(
					track.words.getSegmentDebFrame(word.posInAlign)));
		} else {
			replay = false;
		}

		if (replay)
			player.startPlaying();
	}


	/**
	 * Dialog box to create an anchor before or after a certain word.
	 * @param before If true, the new anchor will be placed before the word in
	 *               the element list. If false, it'll be placed after the word.
	 */
	private void newAnchorNextToWord(Word word, boolean before) {
		ElementList.Neighborhood<Anchor> range =
				track.elts.getNeighbors(word, Anchor.class);

		float initialPos;

		if (word.posInAlign < 0) {
			float endOfAudio = TimeConverter.frame2sec((int) gui.audioSourceTotalFrames);
			initialPos = before?
					(range.prev!=null? range.prev.seconds: 0) :
					(range.next!=null? range.next.seconds: endOfAudio);
		} else if (before) {
			initialPos = TimeConverter.frame2sec(
					track.words.getSegmentDebFrame(word.posInAlign));
		} else {
			initialPos = TimeConverter.frame2sec(
					track.words.getSegmentEndFrame(word.posInAlign));
		}

		String positionString = JOptionPane.showInputDialog(gui.jf,
				String.format("Enter position for new anchor to be inserted\n"
						+ "%s '%s' (in seconds):",
						before? "before": "after", word.getWordString()),
				initialPos);

		if (positionString == null)
			return;

		float newPos = Float.parseFloat(positionString);

		if (sanitizeAnchorPosition(range, newPos)) {
			Anchor anchor = new Anchor(newPos);
			track.elts.add(track.elts.indexOf(word) + (before?0:1), anchor);
			track.clearAlignmentAround(anchor);
			setTextFromElements();
		}
	}


	/**
	 * Dialog box to prompt the user where to reposition the anchor.
	 * User input is sanitized with sanitizeAnchorPosition().
	 */
	private void repositionAnchor(Anchor anchor) {
		String newPosString = JOptionPane.showInputDialog(gui.jf,
				"Enter new anchor position in seconds:",
				Float.toString(anchor.seconds));

		if (newPosString == null)
			return;

		float newPos = Float.parseFloat(newPosString);

		if (!sanitizeAnchorPosition(
				track.elts.getNeighbors(anchor, Anchor.class), newPos))
		{
			return;
		}

		anchor.seconds = newPos;

		track.clearAlignmentAround(anchor);
	}


	/**
	 * Ensures newPos is a valid position for an anchor within the given
	 * range; if not, informs the user with error messages.
	 * @return true if the position is valid
	 */
	private boolean sanitizeAnchorPosition(
			ElementList.Neighborhood<Anchor> range, float newPos)
	{
		if (newPos < 0) {
			JOptionPane.showMessageDialog(gui.jf,
					"Can't set to negative position!",
					"Illegal anchor position", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if (range.prev != null && range.prev.seconds > newPos) {
			JOptionPane.showMessageDialog(gui.jf,
					"Can't set this anchor before the previous anchor\n" +
							"(at " + range.prev.seconds + " seconds).",
					"Illegal anchor position", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if (range.next != null && range.next.seconds < newPos) {
			JOptionPane.showMessageDialog(gui.jf,
					"Can't set this anchor past the next anchor\n" +
							"(at " + range.next.seconds + " seconds).",
					"Illegal anchor position", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		return true;
	}


	//==========================================================================
	// TEXT STYLING
	//==========================================================================


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
			ex.printStackTrace();
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
}
