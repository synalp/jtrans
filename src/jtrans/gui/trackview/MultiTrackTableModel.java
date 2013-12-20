package jtrans.gui.trackview;

import jtrans.elements.Anchor;
import jtrans.elements.Element;
import jtrans.elements.Word;
import jtrans.facade.Project;
import jtrans.facade.Track;
import jtrans.utils.spantable.DefaultSpanModel;
import jtrans.utils.spantable.Span;
import jtrans.utils.spantable.SpanModel;
import jtrans.utils.spantable.SpanTableModel;

import javax.swing.table.AbstractTableModel;
import java.util.*;


class MultiTrackTableModel extends AbstractTableModel implements SpanTableModel {
	private Project project;
	private SpanModel spanModel = new DefaultSpanModel();
	private String[] columnNames;
	private Cell[][] cells;
	private int[] highlightedRows;
	private Word[] highlightedWords;
	private int[] trackToColumn;
	private int[] columnToTrack;
	Map<Word, int[]> wordMap = new HashMap<Word, int[]>();
	private int visibleColumns;


	@Override
	public int getRowCount() {
		return cells.length;
	}

	@Override
	public int getColumnCount() {
		return visibleColumns;
	}

	@Override
	public Cell getValueAt(int rowIndex, int columnIndex) {
		return cells[rowIndex][columnIndex];
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	public MultiTrackTableModel(Project p, boolean[] visibility) {
		super();
		project = p;
		refresh(visibility);
	}


	@Override
	public boolean isCellEditable(int row, int column) {
		return cells[row][column] != null;
	}


	@Override
	public SpanModel getSpanModel() {
		return spanModel;
	}


	/**
	 * Keeps track of various variables when building table data for a Track.
	 */
	private class MetaTrack {
		final Track track;
		final int trackNo;
		final int column;
		final ListIterator<Element> iter;
		Anchor anchor;
		int lastRow = 0;

		MetaTrack(int trackNo, int colNo) {
			this.trackNo = trackNo;
			column = colNo;
			track = project.tracks.get(trackNo);
			iter = track.elts.listIterator();

			// Skip past first anchor and add initial row span if needed
			ontoNextCell(0);
		}

		/**
		 * Adjusts lastRow and currentTime.
		 * Adds the current row span to the spanModel if needed.
		 * Updates wordMap.
		 * @return contents of the current cell
		 */
		void ontoNextCell(int currentRow) {
			int rowSpan = currentRow - lastRow;
			if (rowSpan > 1)
				spanModel.addSpan(new Span(lastRow, column, rowSpan, 1));
			lastRow = currentRow;

			Anchor cellStartAnchor = anchor;

			// Invalidate currentTime
			if (!iter.hasNext())
				anchor = null;

			List<Word> words = new ArrayList<Word>();
			while (iter.hasNext()) {
				Element next = iter.next();
				if (next instanceof Anchor) {
					anchor = (Anchor)next;
					break;
				} else if (next instanceof Word) {
					wordMap.put((Word)next, new int[]{currentRow, column});
					words.add((Word)next);
				}
			}

			if (cells != null)
				cells[currentRow][column] = new Cell(
						trackNo, cellStartAnchor, words);
		}
	}


	public void refresh(boolean[] visibility) {
		spanModel.clear();

		visibleColumns = 0;
		trackToColumn = new int[project.tracks.size()];
		List<MetaTrack> metaTracks = new ArrayList<MetaTrack>();

		// Count visible tracks and initialize track metadata
		for (int i = 0; i < visibility.length; i++) {
			if (visibility[i]) {
				trackToColumn[i] = visibleColumns;
				metaTracks.add(new MetaTrack(i, visibleColumns));
				visibleColumns++;
			} else {
				trackToColumn[i] = -1;
			}
		}

		columnNames = new String[visibleColumns];
		columnToTrack = new int[visibleColumns];
		for (int i = 0; i < visibleColumns; i++) {
			columnNames[i] = metaTracks.get(i).track.speakerName;
			columnToTrack[i] = metaTracks.get(i).column;
		}

		highlightedRows = new int[visibleColumns];
		Arrays.fill(highlightedRows, -1);
		highlightedWords = new Word[visibleColumns];

		// Count rows
		int rows = 0;
		for (MetaTrack mt: metaTracks)
			for (Element el: mt.track.elts)
				if (el instanceof Anchor)
					rows++;

		cells = new Cell[rows][visibleColumns];

		for (int row = 0; row < rows; row++) {
			MetaTrack meta = null;

			// Find track containing the earliest upcoming anchor
			for (MetaTrack m: metaTracks) {
				if (m.anchor != null &&
						(null == meta || m.anchor.seconds < meta.anchor.seconds))
				{
					meta = m;
				}
			}

			// No more anchors in all tracks
			if (null == meta)
				break;

			meta.ontoNextCell(row);
		}
	}


	public void highlightWord(int trackIdx, Word word) {
		int col = trackToColumn[trackIdx];
		if (col < 0) // hidden column
			return;

		int oldHLRow = highlightedRows[col];
		int[] tc = wordMap.get(word);
		int newHLRow = tc==null? -1: tc[0];

		// if newHLRow>=0: don't un-highlight cell if null word
		if (oldHLRow != newHLRow && newHLRow >= 0) {
			if (oldHLRow >= 0)
				fireTableCellUpdated(oldHLRow, col);
			highlightedRows[col] = newHLRow;
		}

		highlightedWords[col] = word;
		if (newHLRow >= 0)
			fireTableCellUpdated(newHLRow, col);

	}

	public int getHighlightedRow(int col) {
		return highlightedRows[col];
	}

	public Word getHighlightedWord(int col) {
		return highlightedWords[col];
	}

	public int getTrackForColumn(int col) {
		return columnToTrack[col];
	}
}
