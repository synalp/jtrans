package jtrans.gui;

import jtrans.facade.Project;
import jtrans.utils.spantable.SpanTable;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;


/**
 * Represents tracks as columns.
 * Renders groups of words between two anchors as a JTextArea cell.
 */
public class MultiTrackTable extends SpanTable {
	private TextAreaCellRenderer renderer = new TextAreaCellRenderer();


	public MultiTrackTable(Project project) {
		super(new MultiTrackTableModel(project));
		setEnabled(false);
		setShowGrid(true);
		getTableHeader().setReorderingAllowed(false);
		//setIntercellSpacing(new Dimension(1, 1));

		for (int i = 0; i < getColumnModel().getColumnCount(); i++)
			getColumnModel().getColumn(i).setCellRenderer(renderer);

		setPreferredScrollableViewportSize(new Dimension(512, 512));
		doLayout();
	}


	/**
	 * TextArea cells may wrap lines. Row height is flexible.
	 */
	@Override
	public void doLayout() {
		TableColumnModel tcm = getColumnModel();

		for (int row = 0; row < getRowCount(); row++) {
			int newRowHeight = 1;
			for (int colN = 0; colN < getColumnCount(); colN++) {
				TableColumn tableCol = tcm.getColumn(colN);
				Component cell = prepareRenderer(tableCol.getCellRenderer(), row, colN);

				if (cell instanceof JTextArea) {
					cell.setSize(
							tableCol.getWidth() - getIntercellSpacing().width,
							cell.getPreferredSize().height);
					int h = cell.getPreferredSize().height + getIntercellSpacing().height;
					if (h > newRowHeight)
						newRowHeight = h;
				}
			}
			if (getRowHeight(row) != newRowHeight)
				setRowHeight(row, newRowHeight);
		}
		super.doLayout();
	}


	public void setViewFont(Font font) {
		renderer.setFont(font);
		doLayout();
	}
}


