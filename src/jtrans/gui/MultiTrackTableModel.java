package jtrans.gui;

import jtrans.elements.Anchor;
import jtrans.elements.Element;
import jtrans.facade.Project;
import jtrans.facade.Track;

import javax.swing.table.DefaultTableModel;


class MultiTrackTableModel extends DefaultTableModel {
	public MultiTrackTableModel(Project p) {
		super(getData(p), getColumnNames(p));
	}


	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}


	private static String[] getColumnNames(Project project) {
		String[] t = new String[project.tracks.size()];

		for (int i = 0; i < t.length; i++)
			t[i] = project.tracks.get(i).speakerName;

		return t;
	}


	private static Object[][] getData(Project project) {
		String[][] data;

		// Index of next anchor, by track. -1 means no more anchors.
		int[] upNext = new int[project.tracks.size()];

		// Initialize upNext
		for (int i = 0; i < project.tracks.size(); i++) {
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

		data = new String[rows][project.tracks.size()];

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
		}

		return data;
	}
}
