package jtrans.gui.trackview;

import jtrans.elements.Anchor;
import jtrans.elements.Word;

import java.util.List;


/**
 * Distilled information about a track portion between two anchors.
 * Ready for use in a MultiTrackTableModel.
 */
class Cell {
	final int track;
	final Anchor anchor;
	final String text;
	final List<Word> words;
	final int[] wordStart;


	public Cell(int t, Anchor a, List<Word> wordList) {
		track = t;
		anchor = a;

		words = wordList;
		wordStart = new int[words.size()];

		StringBuilder sb = new StringBuilder();
		sb.append('[').append(anchor.seconds).append(']');
		for (int i = 0; i < words.size(); i++) {
			sb.append(' ');
			wordStart[i] = sb.length();
			sb.append(words.get(i).getWordString());
		}
		text = sb.toString();
	}


	@Override
	public String toString() {
		return text;
	}


	public Word getWordAtCaret(int caret) {
		for (int i = 0; i < wordStart.length; i++) {
			if (caret < wordStart[i])
				continue;
			Word w = words.get(i);
			if (caret < wordStart[i] + w.getWordString().length())
				return w;
		}
		return null;
	}
}