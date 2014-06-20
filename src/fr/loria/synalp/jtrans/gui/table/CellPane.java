package fr.loria.synalp.jtrans.gui.table;

import fr.loria.synalp.jtrans.project.Token;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Component representing a TextCell in a MultiTrackTable.
 *
 * In compliance with JTable guidelines, a unique instance of this class can
 * (and should) be reused for an entire MultiTrackTable instance.
 */
public class CellPane extends JTextPane {

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

	private static final AttributeSet ALIGNED_ANONYMOUS_STYLE =
			new SimpleAttributeSet(ALIGNED_STYLE) {{
				addAttribute(StyleConstants.StrikeThrough, true);
				addAttribute(StyleConstants.Bold, true);
			}};

	private static final AttributeSet UNALIGNED_ANONYMOUS_STYLE =
			new SimpleAttributeSet(UNALIGNED_STYLE) {{
				addAttribute(StyleConstants.StrikeThrough, true);
				addAttribute(StyleConstants.Bold, true);
			}};

	private static final AttributeSet HIGHLIGHTED_STYLE =
			new SimpleAttributeSet() {{
				addAttribute(StyleConstants.Background, new Color(0xF085B0));
			}};


	private TextCell cell;
	private Token highlighted;


	private static final Map<Token.Type, AttributeSet> styleCache =
			new HashMap<Token.Type, AttributeSet>()
	{
		private void put(Token.Type type, final Color color) {
			put(type, new SimpleAttributeSet() {{
				addAttribute(StyleConstants.Background, color);
			}});
		}

		{
			put(Token.Type.COMMENT,            Color.YELLOW);
			put(Token.Type.NOISE,              Color.CYAN);
			put(Token.Type.PUNCTUATION,        Color.ORANGE);
			put(Token.Type.OVERLAP_START_MARK, Color.PINK);
			put(Token.Type.OVERLAP_END_MARK,   Color.PINK);
			put(Token.Type.SPEAKER_MARK,       Color.GREEN.brighter());
		}
	};


	public CellPane() {
		setEditable(false);
		setBorder(BorderFactory.createCompoundBorder(
				getBorder(),
				new EmptyBorder(5, 5, 5, 5)));
	}


	public void setCell(TextCell c) {
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

		for (int i = 0; i < cell.tokens.size(); i++)
			setDefaultStyle(i, cell.tokens.get(i));
	}


	private void setDefaultStyle(int i, Token token) {
		AttributeSet style;

		if (token.isAlignable()) {
			boolean aligned = token.isAligned();
			if (token.shouldBeAnonymized()) {
				style = aligned? ALIGNED_ANONYMOUS_STYLE: UNALIGNED_ANONYMOUS_STYLE;
			} else {
				style = aligned? ALIGNED_STYLE: UNALIGNED_STYLE;
			}
		} else {
			style = styleCache.get(token.getType());
			if (style == null) {
				System.err.println("WARNING: missing style for comment type "
						+ token.getType());
			}
		}

		if (style != null)
			setStyle(i, style, true);
	}


	private void setStyle(int i, AttributeSet style, boolean replace) {
		getStyledDocument().setCharacterAttributes(
				cell.tokStart[i],
				cell.tokEnd[i] - cell.tokStart[i],
				style,
				replace);
	}


	public void highlight(Token token) {
		if (highlighted != null) {
			setDefaultStyle(cell.tokens.indexOf(highlighted), highlighted);
		}

		if (token != null) {
			setStyle(cell.tokens.indexOf(token), HIGHLIGHTED_STYLE, false);
		}

		highlighted = token;
	}
}
