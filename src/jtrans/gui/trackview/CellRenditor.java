package jtrans.gui.trackview;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * "Renderer" + "Editor" for Cells in a MultiTrackTable.
 *
 * This is a purely boilerplate class gluing MultiTrackTable with the components
 * that represent the individual cells in a MultiTrackTable.
 *
 * In compliance with JTable guidelines, a unique instance of this class can
 * (and should) be reused for an entire MultiTrackTable instance.
 *
 * @see CellPane the actual component
 */
public class CellRenditor
	extends AbstractCellEditor
	implements TableCellRenderer, TableCellEditor
{
	private JPanel emptyPane;
	private CellPane renderPane;
	private CellPane editorPane;
	private Object editorValue;
	private Cell editorCell;


	public CellRenditor() {
		renderPane = new CellPane();
		editorPane = new CellPane();

		emptyPane = new JPanel();
		emptyPane.setBackground(Color.DARK_GRAY);
		emptyPane.setForeground(Color.LIGHT_GRAY);
	}


	@Override
	public Component getTableCellEditorComponent(
			JTable table, Object value, boolean isSelected,
			int row, int column)
	{
		editorValue = value;

		if (value instanceof Cell) {
			editorCell = (Cell)value;
			editorPane.setCell(editorCell);
			return editorPane;
		} else {
			return null;
		}
	}


	@Override
	public Component getTableCellRendererComponent(
			JTable table, Object value,
			boolean isSelected, boolean hasFocus,
			int row, int column)
	{
		if (value instanceof Cell) {
			renderPane.setCell((Cell) value);

			MultiTrackTableModel model = (MultiTrackTableModel)table.getModel();
			if (model.getHighlightedRow(column) == row) {
				renderPane.highlight(model.getHighlightedWord(column));
			}

			return renderPane;
		} else {
			return emptyPane;
		}
	}


	@Override
	public Object getCellEditorValue() {
		return editorValue;
	}


	public void setFont(Font font) {
		renderPane.setFont(font);
		editorPane.setFont(font);
	}
}
