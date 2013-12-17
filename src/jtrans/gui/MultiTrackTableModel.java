package jtrans.gui;

import jtrans.elements.Anchor;
import jtrans.elements.Element;
import jtrans.facade.Project;
import jtrans.facade.Track;
import jtrans.utils.spantable.DefaultSpanModel;
import jtrans.utils.spantable.Span;
import jtrans.utils.spantable.SpanModel;
import jtrans.utils.spantable.SpanTableModel;

import javax.swing.table.AbstractTableModel;
import java.util.ListIterator;


class MultiTrackTableModel extends AbstractTableModel implements SpanTableModel {
	private Project project;
	private SpanModel spanModel = new DefaultSpanModel();
	private String[][] data;

	@Override
	public int getRowCount() {
		return data.length;
	}

	@Override
	public int getColumnCount() {
		return project.tracks.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return data[rowIndex][columnIndex];
	}

	@Override
	public String getColumnName(int column) {
		return project.tracks.get(column).speakerName;
	}

	public MultiTrackTableModel(Project p) {
		super();
		project = p;
		refresh();
	}


	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
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
		final int column;
		final ListIterator<Element> iter;
		float currentTime;
		int lastRow = 0;

		MetaTrack(int c) {
			column = c;
			track = project.tracks.get(c);
			iter = track.elts.listIterator();

			// Skip past first anchor and add initial row span if needed
			ontoNextCell(0);
		}

		/**
		 * Adjusts lastRow and currentTime.
		 * Adds the current row span to the spanModel if needed.
		 * @return contents of the current cell
		 */
		String ontoNextCell(int currentRow) {
			int rowSpan = currentRow - lastRow;
			if (rowSpan > 1)
				spanModel.addSpan(new Span(lastRow, column, rowSpan, 1));
			lastRow = currentRow;

			StringBuilder sb = new StringBuilder()
					.append("[").append(currentTime).append("] ");

			// Invalidate currentTime
			if (!iter.hasNext())
				currentTime = Float.MAX_VALUE;

			while (iter.hasNext()) {
				Element next = iter.next();
				if (next instanceof Anchor) {
					currentTime = ((Anchor) next).seconds;
					break;
				}
				sb.append(next.toString()).append(' ');
			}

			return sb.toString();
		}
	}


	public void refresh() {
		spanModel.clear();
		final int trackCount = project.tracks.size();

		// Initialize track metadata
		MetaTrack[] metaTracks = new MetaTrack[trackCount];
		for (int i = 0; i < trackCount; i++)
			metaTracks[i] = new MetaTrack(i);

		// Count rows
		int rows = 0;
		for (Track t: project.tracks)
			for (Element el: t.elts)
				if (el instanceof Anchor)
					rows++;

		data = new String[rows][trackCount];

		for (int row = 0; row < rows; row++) {
			MetaTrack meta = null;

			// Find track containing the earliest upcoming anchor
			for (MetaTrack m: metaTracks)
				if (null == meta || m.currentTime < meta.currentTime)
					meta = m;

			// No more anchors in all tracks
			if (null == meta)
				break;

			data[row][meta.column] = meta.ontoNextCell(row);
		}
	}
}
