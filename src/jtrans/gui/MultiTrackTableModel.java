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


	public void refresh() {
		spanModel.clear();
		final int trackCount = project.tracks.size();

		// Index of next anchor, by track. -1 means no more anchors.
		int[] upNext = new int[trackCount];
		Span[] curSpans = new Span[trackCount];

		// Initialize upNext
		for (int i = 0; i < trackCount; i++) {
			Track track = project.tracks.get(i);
			upNext[i] = -1;
			for (int j = 0; j < track.elts.size(); j++) {
				if (track.elts.get(j) instanceof Anchor) {
					upNext[i] = j;
					break;
				}
			}
		}

		// Count rows
		int rows = 0;
		for (Track t: project.tracks)
			for (Element el: t.elts)
				if (el instanceof Anchor)
					rows++;

		data = new String[rows][trackCount];

		for (int row = 0; row < rows; row++) {
			float earliestSecond = Float.MAX_VALUE;
			int trackId = -1;

			// Find track containing the earliest upcoming anchor
			for (int i = 0; i < upNext.length; i++) {
				Track track = project.tracks.get(i);
				if (upNext[i] < 0)
					continue;
				Element next = track.elts.get(upNext[i]);
				if (next instanceof Anchor && ((Anchor) next).seconds < earliestSecond) {
					earliestSecond = ((Anchor) next).seconds;
					trackId = i;
				}
			}

			// No more anchors in all tracks
			if (trackId < 0)
				break;

			Track track = project.tracks.get(trackId);

			// Build cell contents
			StringBuilder sb = new StringBuilder()
					.append("[").append(earliestSecond).append("] ");

			// Add current span if needed
			if (curSpans[trackId] != null && curSpans[trackId].getHeight() > 1)
				spanModel.addSpan(curSpans[trackId]);

			// Create Span for this cell. For now it doesn't span any other
			// rows, but the row span can be lengthened in later iterations
			curSpans[trackId] = new Span(row, trackId, 1, 1);

			upNext[trackId]++;
			while (true) {
				if (upNext[trackId] >= track.elts.size()) {
					upNext[trackId] = -1;
					break;
				}
				Element next = track.elts.get(upNext[trackId]);
				if (next instanceof Anchor) {
					break;
				}
				sb.append(next.toString()).append(' ');
				upNext[trackId]++;
			}

			data[row][trackId] = sb.toString();

			// Adjust span heights
			for (int i = 0; i < trackCount; i++) {
				if (i == trackId)
					continue;
				Span oldSpan = curSpans[i];
				if (oldSpan == null)
					continue;
				curSpans[i] = new Span(
						oldSpan.getRow(),
						oldSpan.getColumn(),
						oldSpan.getHeight()+1,
						oldSpan.getWidth());
			}
		}
	}
}
