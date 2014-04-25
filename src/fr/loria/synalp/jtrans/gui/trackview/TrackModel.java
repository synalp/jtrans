package fr.loria.synalp.jtrans.gui.trackview;

import fr.loria.synalp.jtrans.elements.Anchor;
import fr.loria.synalp.jtrans.elements.Element;
import fr.loria.synalp.jtrans.project.AnchorSandwich;
import fr.loria.synalp.jtrans.project.LinearBridge;
import fr.loria.synalp.jtrans.project.TrackProject;
import fr.loria.synalp.jtrans.utils.spantable.Span;

import java.util.*;


public class TrackModel extends ProjectModel<TrackProject> {

	private int nonEmptyRowCount;


	private class TrackColumn extends Column {
		Object[] cells;

		// Variables used when building table data
		int lastRow = 0;
		boolean lastCellWasText = false;

		private TrackColumn(int spkID) {
			super(spkID);
		}

		void addRowSpan(int currentRow) {
			int rowSpan = currentRow - lastRow;
			if (rowSpan > 1 && lastCellWasText)
				spanModel.addSpan(new Span(lastRow, spkID, rowSpan, 1));
		}

		int ontoNextCell(int currentRow, AnchorSandwich sandwich) {
			if (cells == null)
				return 0;

			cells[currentRow] = sandwich.getInitialAnchor();

			if (!sandwich.isEmpty()) {
				cells[currentRow+1] = new TextCell(spkID, sandwich);
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
	public Object getValueAt(int rowIndex, int columnIndex) {
		return ((TrackColumn)columns.get(columnIndex)).cells[rowIndex];
	}




	public TrackModel(TrackProject p) {
		project = p;

		spanModel.clear();
		columns = new ArrayList<>();

		int rows = 0;

		for (int i = 0; i < project.speakerCount(); i++) {
			columns.add(new TrackColumn(i));

			for (Element el: project.tracks.get(i).elts) {
				if (el instanceof Anchor) {
					rows += 2;
				}
			}
		}

		for (Column c: columns) {
			((TrackColumn)c).cells = new Object[rows];
		}

		nonEmptyRowCount = populateRows();
	}


	private int populateRows() {
		int row = 0;

		LinearBridge lb = project.linearBridge();

		while (lb.hasNext()) {
			int maxDelta = 0;
			AnchorSandwich[] sandwiches = lb.next();
			assert sandwiches.length == project.speakerCount();

			for (int i = 0; i < project.speakerCount(); i++) {
				if (sandwiches[i] == null) {
					continue;
				}

				TrackColumn col = (TrackColumn)columns.get(i);

				for (Element el: sandwiches[i]) {
					// row+1 because a row must be left for the anchor
					col.elementRowMap.put(el, row+1);
				}

				col.addRowSpan(row);
				int d = col.ontoNextCell(row, sandwiches[i]);
				maxDelta = Math.max(d, maxDelta);
			}
			row += maxDelta;
		}

		return row;
	}

}
