package jtrans.gui.trackview;

import jtrans.elements.Anchor;
import jtrans.elements.Word;

import java.util.List;

class Cell {
	Anchor anchor;
	String text;
	List<Word> words;
	int[] wordStart;

	public Cell(Anchor a, List<Word> wordList) {
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