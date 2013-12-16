package jtrans.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;


class TextAreaCellRenderer extends JTextArea implements TableCellRenderer {
	private static Color oddColor = new Color(0xE6E4E3);


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
		if (isSelected) {
			setForeground(table.getSelectionForeground());
			setBackground(table.getSelectionBackground());
		} else {
			setForeground(table.getForeground());
			setBackground(row%2==1? oddColor: table.getBackground());
		}
		setText(value==null? "": value.toString());
		return this;
	}
}
