package jtrans.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;


class TextAreaCellRenderer extends JTextArea implements TableCellRenderer {
	private static final Color KARAOKE_CELL_BG = new Color(0xF085B0);


	public TextAreaCellRenderer() {
		super();
		setLineWrap(true);
		setWrapStyleWord(true);
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
		if (row == mttm.getHighlightedRow(column))
			setBackground(KARAOKE_CELL_BG);
		else
			setBackground(table.getBackground());

		return this;
	}
}
