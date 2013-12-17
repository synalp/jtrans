package jtrans.gui;

import jtrans.facade.Project;
import spantable.SpanTable;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;


/**
 * Represents tracks as columns.
 * Renders groups of words between two anchors as a JTextArea cell.
 */
public class MultiTrackTable extends SpanTable {
	public MultiTrackTable(Project project) {
		super(new MultiTrackTableModel(project));
		setEnabled(true);
		setShowGrid(false);
		setIntercellSpacing(new Dimension(1, 0));

		TextAreaCellRenderer renderer = new TextAreaCellRenderer();
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


	public static JFrame createFrame(Project project) {
		JTable tbl = new MultiTrackTable(project);
		JScrollPane sp = new JScrollPane(tbl);
		sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		JFrame f = new JFrame("MultiTrackTable");
		f.setContentPane(sp);
		f.pack();
		return f;
	}
}


