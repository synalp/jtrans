package jtrans.gui.trackview;

import jtrans.elements.Word;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;


class CellRenderer
		extends JTextPane implements TableCellRenderer
{
	private static final Color KARAOKE_CELL_BG = new Color(0xF085B0);

	private static final AttributeSet HIGHLIGHTED_STYLE =
		new SimpleAttributeSet() {{
			addAttribute(StyleConstants.Background, Color.WHITE);
		}};

	public CellRenderer() {
		super();
//		setLineWrap(true);
//		setWrapStyleWord(true);
		setBorder(BorderFactory.createCompoundBorder(
				getBorder(),
				new EmptyBorder(5, 5, 5, 5)));
	}


	@Override
	public Component getTableCellRendererComponent(
			JTable table, Object value,
			boolean isSelected, boolean hasFocus,
			int row, int column)
	{
		setText(value==null? "": value.toString());

		MultiTrackTableModel mttm = (MultiTrackTableModel)table.getModel();
		if (row == mttm.getHighlightedRow(column)) {
			setBackground(KARAOKE_CELL_BG);
			Word w = mttm.getHighlightedWord(column);
			if (w != null) {
				Cell c = mttm.getValueAt(row, column);
				getStyledDocument().setCharacterAttributes(
						c.wordStart[c.words.indexOf(w)],
						w.getWordString().length(),
						HIGHLIGHTED_STYLE,
						false);
			}
		} else
			setBackground(table.getBackground());

		return this;
	}
}
