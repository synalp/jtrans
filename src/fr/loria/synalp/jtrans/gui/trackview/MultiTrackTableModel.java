package fr.loria.synalp.jtrans.gui.trackview;

import fr.loria.synalp.jtrans.elements.Anchor;
import fr.loria.synalp.jtrans.elements.Element;
import fr.loria.synalp.jtrans.elements.Word;
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
	private String[] columnNames;
	private Object[][] cells;
	private int[] highlightedRows;
	private Word[] highlightedWords;
	private int[] trackToColumn;
	private int[] columnToTrack;
	private int visibleColumns;
	private int nonEmptyRowCount;

	/** Used to highlight words */
	Map<Word, int[]> wordMap = new HashMap<Word, int[]>();


	@Override
	public int getRowCount() {
		return nonEmptyRowCount;
	}

	@Override
	public int getColumnCount() {
		return visibleColumns;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
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
		boolean lastCellWasText = false;

		MetaTrack(int trackNo, int colNo) {
			this.trackNo = trackNo;
			column = colNo;
			track = project.tracks.get(trackNo);
			iter = track.elts.listIterator();

			// Skip past first anchor and add initial row span if needed
			ontoNextCell(0);
		}

		/**
		 * Adds the current row span to the spanModel if needed.
		 * Only TextCells may span multiple rows.
		 */
		void addRowSpan(int currentRow) {
			int rowSpan = currentRow - lastRow;
			if (rowSpan > 1 && lastCellWasText)
				spanModel.addSpan(new Span(lastRow, column, rowSpan, 1));
		}

		/**
		 * Adjusts lastRow and anchor, updates wordMap.
		 */
		int ontoNextCell(int currentRow) {
			Anchor cellStartAnchor = anchor;

			if (!iter.hasNext())
				anchor = null;

			List<Element> elts = new ArrayList<Element>();
			while (iter.hasNext()) {
				Element next = iter.next();
				if (next instanceof Anchor) {
					anchor = (Anchor)next;
					break;
				} else {
					elts.add(next);
					if (next instanceof Word)
						wordMap.put((Word)next, new int[]{currentRow+1, column});
				}
			}

			if (cells != null) {
				cells[currentRow][column] = cellStartAnchor;

				if (!elts.isEmpty()) {
					cells[currentRow+1][column] = new TextCell(
							trackNo, cellStartAnchor, elts);
					lastCellWasText = true;
					lastRow = currentRow + 1;
					return 2;
				} else {
					lastCellWasText = false;
					lastRow = currentRow;
					return 1;
				}
			}
			return 0;
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
					rows += 2;

		cells = new Object[rows][visibleColumns];

		nonEmptyRowCount = populateRows(metaTracks);
	}


	private int populateRows(List<MetaTrack> metaTracks) {
		int row = 0;

		// For tracks with simultaneous anchors,
		// cells will be inserted at the same row
		List<MetaTrack> simultaneous = new ArrayList<MetaTrack>();

		while (row < cells.length) {
			simultaneous.clear();

			// Find track containing the earliest upcoming anchor
			for (MetaTrack m: metaTracks) {
				if (m.anchor == null)
					continue;

				if (simultaneous.isEmpty()) {
					simultaneous.add(m);
				} else {
					int cmp = m.anchor.compareTo(simultaneous.get(0).anchor);

					if (cmp < 0) {
						simultaneous.clear();
						simultaneous.add(m);
					} else if (cmp == 0) {
						simultaneous.add(m);
					}
				}
			}

			// No more anchors in all tracks
			if (simultaneous.isEmpty())
				break;

			int maxDelta = 0;
			for (MetaTrack m: simultaneous) {
				m.addRowSpan(row);
				int d = m.ontoNextCell(row);
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
