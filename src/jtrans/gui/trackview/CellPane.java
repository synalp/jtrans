package jtrans.gui.trackview;

import jtrans.elements.*;
import jtrans.elements.Element;
import jtrans.facade.Project;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

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


	private final Project project;
	private final Color normalBG;
	private Cell cell;
	private Word highlighted;
	private Map<ElementType, AttributeSet> styleCache = new HashMap<ElementType, AttributeSet>();


	public CellPane(Project project) {
		this.project = project;
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

		for (int i = 0; i < cell.elts.size(); i++)
			setDefaultStyle(i, cell.elts.get(i));

		setBackground(normalBG);
	}


	private void setDefaultStyle(int i, Element el) {
		AttributeSet style;

		if (el instanceof Word) {
			style = ((Word) el).posInAlign >= 0 ? ALIGNED_STYLE : UNALIGNED_STYLE;

		} else {
			final ElementType et = project.types.get(el.getType());
			style = styleCache.get(et);

			if (style == null) {
				style = new SimpleAttributeSet() {{
					addAttribute(StyleConstants.Background, et.getColor());
				}};
				styleCache.put(et, style);
			}
		}

		setStyle(i, style, true);
	}


	private void setStyle(int i, AttributeSet style, boolean replace) {
		getStyledDocument().setCharacterAttributes(
				cell.elStart[i],
				cell.elEnd[i] - cell.elStart[i],
				style,
				replace);
	}


	public void highlight(Word w) {
		setBackground(KARAOKE_CELL_BG);

		if (highlighted != null) {
			setDefaultStyle(cell.elts.indexOf(highlighted), highlighted);
		}

		if (w != null) {
			setStyle(cell.elts.indexOf(w), HIGHLIGHTED_STYLE, false);
		}

		highlighted = w;
	}
}
