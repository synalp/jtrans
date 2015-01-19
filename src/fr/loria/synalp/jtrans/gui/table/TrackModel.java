package fr.loria.synalp.jtrans.gui.table;

import fr.loria.synalp.jtrans.project.Phrase;
import fr.loria.synalp.jtrans.project.Project;
import fr.loria.synalp.jtrans.project.Token;
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

		int ontoNextCell(int currentRow, Phrase phrase) {
			if (cells == null)
				return 0;

			cells[currentRow] = phrase.getInitialAnchor();

			if (!phrase.isEmpty()) {
				cells[currentRow+1] = new TextCell(spkID, phrase);
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
			rows += 3 * project.getTrackSize(i);
//			rows += 3 * project.tracks.get(i).size();
		}

		for (Column c: columns) {
			((TrackColumn)c).cells = new Object[rows];
		}

		nonEmptyRowCount = populateRows();
	}


	private int populateRows() {
		int row = 0;

		LinearBridge lb = new LinearBridge(project);

		while (lb.hasNext()) {
			int maxDelta = 0;
			Phrase[] phrases = lb.next();
			assert phrases.length == project.speakerCount();

			for (int i = 0; i < project.speakerCount(); i++) {
				if (phrases[i] == null) {
					continue;
				}

				TrackColumn col = (TrackColumn)columns.get(i);

				for (Token token: phrases[i]) {
					// row+1 because a row must be left for the anchor
					col.tokenRowMap.put(token, row+1);
				}

				col.addRowSpan(row);
				int d = col.ontoNextCell(row, phrases[i]);
				maxDelta = Math.max(d, maxDelta);
			}
			row += maxDelta;
		}

		return row;
	}


	public static class LinearBridge
			implements Iterator<Phrase[]>
	{
		private final int nTracks;
		private final Iterator<Phrase>[] phraseIterators;
		private final Phrase[] currentPhrases;


		public LinearBridge(Project p) {
			nTracks = p.speakerCount();
			phraseIterators = new Iterator[nTracks];
			currentPhrases = new Phrase[nTracks];

			for (int i = 0; i < nTracks; i++) {
				Iterator<Phrase> iter = p.phraseIterator(i);
				phraseIterators[i] = iter;
				if (iter.hasNext()) {
					currentPhrases[i] = iter.next();
				}
			}
		}


		@Override
		public boolean hasNext() {
			if (nTracks == 0) {
				return false;
			}

			for (Phrase s: currentPhrases) {
				if (s != null) {
					return true;
				}
			}

			return false;
		}


		@Override
		public Phrase[] next() {
			Phrase[] simultaneous = new Phrase[nTracks];
			Phrase earliest = null;

			// Find track containing the earliest upcoming anchor
			for (int i = 0; i < nTracks; i++) {
				Phrase phrase = currentPhrases[i];

				if (phrase == null) {
					continue;
				}

				if (earliest == null) {
					simultaneous[i] = phrase;
					earliest = phrase;
				} else {
					int cmp = phrase.compareTo(earliest);

					if (cmp < 0) {
						Arrays.fill(simultaneous, 0, i, null);
						simultaneous[i] = phrase;
						earliest = phrase;
					} else if (cmp == 0) {
						simultaneous[i] = phrase;
					}
				}
			}

			for (int i = 0; i < nTracks; i++) {
				if (simultaneous[i] == null) {
					;
				} else if (!phraseIterators[i].hasNext()) {
					currentPhrases[i] = null;
				} else {
					currentPhrases[i] = phraseIterators[i].next();
				}
			}

			return simultaneous;
		}


		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}


}
