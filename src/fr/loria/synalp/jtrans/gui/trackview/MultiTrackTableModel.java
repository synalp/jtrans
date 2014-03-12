package fr.loria.synalp.jtrans.gui.trackview;

import fr.loria.synalp.jtrans.elements.Anchor;
import fr.loria.synalp.jtrans.elements.Element;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.facade.AnchorSandwich;
import fr.loria.synalp.jtrans.facade.LinearBridge;
import fr.loria.synalp.jtrans.facade.Project;
import fr.loria.synalp.jtrans.facade.Track;
import fr.loria.synalp.jtrans.utils.spantable.DefaultSpanModel;
import fr.loria.synalp.jtrans.utils.spantable.Span;
import fr.loria.synalp.jtrans.utils.spantable.SpanModel;
import fr.loria.synalp.jtrans.utils.spantable.SpanTableModel;

import javax.swing.table.AbstractTableModel;
import java.util.*;


/**
 * Non-editable model.
 */
class MultiTrackTableModel extends AbstractTableModel implements SpanTableModel {

	private Project project;
	private SpanModel spanModel = new DefaultSpanModel();
	private int[] trackToColumn;
	private int nonEmptyRowCount;
	private List<Column> columns;

	/** Used to highlight words */
	Map<Word, int[]> wordMap = new HashMap<Word, int[]>();


	private class Column {
		final int columnNo;
		final Track track;
		final int trackNo;
		Object[] cells;

		// Highlight variables
		Word highlightedWord;
		int highlightedRow = -1;

		// Variables used when building table data
		int lastRow = 0;
		boolean lastCellWasText = false;

		private Column(int columnNo, int trackNo) {
			this.columnNo = columnNo;
			this.trackNo = trackNo;
			this.track = project.tracks.get(trackNo);
		}

		void highlightWord(Word word) {
			int oldHLRow = highlightedRow;
			int[] tc = wordMap.get(word);
			int newHLRow = tc==null? -1: tc[0];

			// if newHLRow>=0: don't un-highlight cell if null word
			if (oldHLRow != newHLRow && newHLRow >= 0) {
				if (oldHLRow >= 0)
					fireTableCellUpdated(oldHLRow, columnNo);
				highlightedRow = newHLRow;
			}

			highlightedWord = word;
			if (newHLRow >= 0)
				fireTableCellUpdated(newHLRow, columns.indexOf(this));
		}

		void addRowSpan(int currentRow) {
			int rowSpan = currentRow - lastRow;
			if (rowSpan > 1 && lastCellWasText)
				spanModel.addSpan(new Span(lastRow, columnNo, rowSpan, 1));
		}

		int ontoNextCell(int currentRow, AnchorSandwich sandwich) {
			if (cells == null)
				return 0;

			cells[currentRow] = sandwich.getInitialAnchor();

			if (!sandwich.isEmpty()) {
				cells[currentRow+1] = new TextCell(trackNo,
						sandwich.getInitialAnchor(), sandwich);
				lastCellWasText = true;
				lastRow = currentRow + 1;
				return 2;
			} else {
				lastCellWasText = false;
				lastRow = currentRow;
				return 1;
			}
		}
	}



	@Override
	public int getRowCount() {
		return nonEmptyRowCount;
	}


	@Override
	public int getColumnCount() {
		return columns.size();
	}


	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return columns.get(columnIndex).cells[rowIndex];
	}


	@Override
	public String getColumnName(int column) {
		return columns.get(column).track.speakerName;
	}


	public MultiTrackTableModel(Project p, boolean[] visibility) {
		super();
		project = p;
		refresh(visibility);
	}


	@Override
	public SpanModel getSpanModel() {
		return spanModel;
	}


	public void refresh(boolean[] visibility) {
		assert visibility.length == project.tracks.size();

		spanModel.clear();
		columns = new ArrayList<Column>();
		trackToColumn = new int[project.tracks.size()];

		// Count visible tracks and initialize track metadata
		for (int i = 0; i < visibility.length; i++) {
			if (visibility[i]) {
				int newColID = columns.size();
				columns.add(new Column(newColID, i));
				trackToColumn[i] = newColID;
			} else {
				trackToColumn[i] = -1;
			}
		}

		// Count rows
		int rows = 0;
		for (Column c: columns) {
			for (Element el: c.track.elts) {
				if (el instanceof Anchor) {
					rows += 2;
				}
			}
		}

		for (Column c: columns)
			c.cells = new Object[rows];

		nonEmptyRowCount = populateRows();
	}


	private int populateRows() {
		int row = 0;

		LinearBridge lb = new LinearBridge(project.tracks);

		while (lb.hasNext()) {
			int maxDelta = 0;
			AnchorSandwich[] sandwiches = lb.next();
			assert sandwiches.length == project.tracks.size();

			for (int i = 0; i < project.tracks.size(); i++) {
				int c = trackToColumn[i];
				if (c < 0 || sandwiches[i] == null)
					continue;

				for (Element el: sandwiches[i]) {
					if (el instanceof Word) {
						// row+1 because a row must be left for the anchor
						wordMap.put((Word)el, new int[]{row+1, c});
					}
				}

				columns.get(c).addRowSpan(row);
				int d = columns.get(c).ontoNextCell(row, sandwiches[i]);
				maxDelta = Math.max(d, maxDelta);
			}
			row += maxDelta;
		}

		return row;
	}


	public void highlightWord(int trackIdx, Word word) {
		int col = trackToColumn[trackIdx];
		if (col < 0) // hidden column
			return;
		columns.get(col).highlightWord(word);
	}


	public int getHighlightedRow(int col) {
		return columns.get(col).highlightedRow;
	}


	public Word getHighlightedWord(int col) {
		return columns.get(col).highlightedWord;
	}


	public int getTrackForColumn(int col) {
		return columns.get(col).trackNo;
	}

}
