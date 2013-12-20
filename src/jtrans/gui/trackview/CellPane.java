package jtrans.gui.trackview;

import jtrans.elements.Word;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Component representing a Cell in a MultiTrackTable.
 *
 * In compliance with JTable guidelines, a unique instance of this class can
 * (and should) be reused for an entire MultiTrackTable instance.
 */
public class CellPane extends JTextPane {
	private static final Color KARAOKE_CELL_BG = new Color(0xF085B0);

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
				addAttribute(StyleConstants.Background, Color.WHITE);
			}};


	private final Color normalBG;
	private Cell cell;
	private Word highlighted;


	public CellPane() {
		normalBG = getBackground();

		setEditable(false);
		setBorder(BorderFactory.createCompoundBorder(
				getBorder(),
				new EmptyBorder(5, 5, 5, 5)));
	}


	public void setCell(Cell c) {
		if (cell == c)
			return;

		cell = c;
		highlighted = null;

		StyledDocument sd = new DefaultStyledDocument();
		setStyledDocument(sd);
		try {
			sd.insertString(0, cell.text, null);
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}

		for (int i = 0; i < cell.words.size(); i++)
			setDefaultWordStyle(i, cell.words.get(i));

		setBackground(normalBG);
	}


	private void setDefaultWordStyle(int i, Word w) {
		setWordStyle(i, w,
				w.posInAlign >= 0 ? ALIGNED_STYLE : UNALIGNED_STYLE,
				true);
	}


	private void setWordStyle(int i, Word w, AttributeSet style, boolean replace) {
		getStyledDocument().setCharacterAttributes(cell.wordStart[i],
				w.getWordString().length(), style,
				replace);
	}


	public void highlight(Word w) {
		setBackground(KARAOKE_CELL_BG);

		if (highlighted != null) {
			setDefaultWordStyle(cell.words.indexOf(highlighted), highlighted);
		}

		if (w != null) {
			setWordStyle(cell.words.indexOf(w), w, HIGHLIGHTED_STYLE, false);
		}

		highlighted = w;
	}
}
