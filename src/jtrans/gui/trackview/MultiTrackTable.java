package jtrans.gui.trackview;

import jtrans.elements.Word;
import jtrans.facade.Project;
import jtrans.utils.spantable.SpanTable;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.table.*;


/**
 * Represents tracks as columns.
 * Renders groups of words between two anchors as a JTextArea cell.
 */
public class MultiTrackTable extends SpanTable {
	private Project project;
	private MultiTrackTableModel model;
	private CellRenderer renderer = new CellRenderer();
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

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Point p = e.getPoint();
				int row = rowAtPoint(p);
				int col = columnAtPoint(p);

				if (row < 0 || col < 0)
					return;

				TableCellRenderer tcr = getCellRenderer(row, col);
				Cell cell = model.getValueAt(row, col);
				Rectangle rect = getCellRect(row, col, false);

				CellRenderer renderer = (CellRenderer)tcr.getTableCellRendererComponent(
						MultiTrackTable.this, cell, true, true, row, col);

				renderer.setSize((int)rect.getWidth(), (int)rect.getHeight());

				Point fakeClick = new Point(p.x-rect.x, p.y-rect.y);
				int caret = renderer.viewToModel(fakeClick);
				highlightWord(model.getTrackForColumn(col), cell.getWordAtCaret(caret));
			}
		});
	}


	/**
	 * Refreshes the MultiTrackTableModel. Should be called after a column is
	 * hidden or shown.
	 */
	private void refreshModel() {
		model = new MultiTrackTableModel(project, visibility);
		setModel(model);
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
		model.highlightWord(trackIdx, word);
	}
}


