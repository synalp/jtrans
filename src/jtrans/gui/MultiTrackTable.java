package jtrans.gui;

import jtrans.elements.Word;
import jtrans.facade.Project;
import jtrans.utils.spantable.SpanTable;

import java.awt.*;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.table.*;


/**
 * Represents tracks as columns.
 * Renders groups of words between two anchors as a JTextArea cell.
 */
public class MultiTrackTable extends SpanTable {
	private Project project;
	private TextAreaCellRenderer renderer = new TextAreaCellRenderer();
	private boolean[] visibility;
	private int visibleCount;


	public MultiTrackTable(Project project) {
		this.project = project;

		visibility = new boolean[project.tracks.size()];
		Arrays.fill(visibility, true);
		visibleCount = visibility.length;

		refreshModel();
		setEnabled(false);
		setShowGrid(true);
		getTableHeader().setReorderingAllowed(false);
		//setIntercellSpacing(new Dimension(1, 1));
		setPreferredScrollableViewportSize(new Dimension(512, 512));
		doLayout();
	}


	/**
	 * Refreshes the MultiTrackTableModel. Should be called after a column is
	 * hidden or shown.
	 */
	private void refreshModel() {
		setModel(new MultiTrackTableModel(project, visibility));
	}


	/**
	 * Sets our custom cell renderer for all cells.
	 */
	@Override
	public void setModel(TableModel tm) {
		super.setModel(tm);
		for (int i = 0; i < getColumnModel().getColumnCount(); i++)
			getColumnModel().getColumn(i).setCellRenderer(renderer);
	}


	/**
	 * TextArea cells may wrap lines. Row height is flexible.
	 */
	@Override
	public void doLayout() {
		TableColumnModel tcm = getColumnModel();

		for (int row = 0; row < getRowCount(); row++) {
			int newRowHeight = 1;
			int col = 0;
			for (int i = 0; i < project.tracks.size(); i++) {
				if (!visibility[i])
					continue;

				TableColumn tableCol = tcm.getColumn(col);
				Component cell = prepareRenderer(tableCol.getCellRenderer(), row, col);

				if (cell instanceof JTextPane) {
					cell.setSize(
							tableCol.getWidth() - getIntercellSpacing().width,
							cell.getPreferredSize().height);
					int h = cell.getPreferredSize().height + getIntercellSpacing().height;
					if (h > newRowHeight)
						newRowHeight = h;
				}

				col++;
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


	public void setTrackVisible(int index, boolean v) {
		assert index >= 0 && index < project.tracks.size();

		if (v && !visibility[index]) {
			visibleCount++;
		} else if (!v && visibility[index]) {
			visibleCount--;
		} else {
			return;
		}

		visibility[index] = v;
		refreshModel();
		doLayout();
	}


	public int getVisibleCount() {
		return visibleCount;
	}


	public void highlightWord(int trackIdx, Word word) {
		((MultiTrackTableModel)getModel()).highlightWord(trackIdx, word);
	}
}


