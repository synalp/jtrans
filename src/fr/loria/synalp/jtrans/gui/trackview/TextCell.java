package fr.loria.synalp.jtrans.gui.trackview;

import fr.loria.synalp.jtrans.project.Element;

import java.util.List;


/**
 * Distilled information about a track portion between two anchors.
 * Ready for use in a MultiTrackTableModel.
 */
class TextCell {
	final int spkID;
	final String text;

	final List<Element> elts;
	final int[] elStart;
	final int[] elEnd;

	public TextCell(int spkID, List<Element> elList) {
		this.spkID = spkID;

		elts = elList;
		elStart = new int[elList.size()];
		elEnd = new int[elList.size()];

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < elts.size(); i++) {
			elStart[i] = sb.length();
			sb.append(elts.get(i)).append(' ');
			elEnd[i] = sb.length();
		}

		text = sb.toString();
	}


	@Override
	public String toString() {
		return text;
	}


	public Element getElementAtCaret(int caret) {
		for (int i = 0; i < elts.size(); i++) {
			if (caret < elStart[i])
				continue;
			if (caret < elEnd[i])
				return elts.get(i);
		}
		return null;
	}
}