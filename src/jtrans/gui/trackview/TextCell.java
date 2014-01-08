package jtrans.gui.trackview;

import jtrans.elements.Anchor;
import jtrans.elements.Element;

import java.util.List;


/**
 * Distilled information about a track portion between two anchors.
 * Ready for use in a MultiTrackTableModel.
 */
class TextCell {
	final int track;
	/** Anchor before the text */
	final Anchor anchor;
	final String text;

	final List<Element> elts;
	final int[] elStart;
	final int[] elEnd;

	public TextCell(int t, Anchor a, List<Element> elList) {
		track = t;
		anchor = a;

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