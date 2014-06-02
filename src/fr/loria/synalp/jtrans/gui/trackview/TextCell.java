package fr.loria.synalp.jtrans.gui.trackview;

import fr.loria.synalp.jtrans.project.Token;

import java.util.List;


/**
 * Distilled information about a track portion between two anchors.
 * Ready for use in a MultiTrackTableModel.
 */
class TextCell {
	final int spkID;
	final String text;

	final List<Token> tokens;
	final int[] tokStart;
	final int[] tokEnd;

	public TextCell(int spkID, List<Token> tokenList) {
		this.spkID = spkID;

		tokens   = tokenList;
		tokStart = new int[tokenList.size()];
		tokEnd   = new int[tokenList.size()];

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < tokens.size(); i++) {
			tokStart[i] = sb.length();
			sb.append(tokens.get(i)).append(' ');
			tokEnd[i] = sb.length();
		}

		text = sb.toString();
	}


	@Override
	public String toString() {
		return text;
	}


	public Token getElementAtCaret(int caret) {
		for (int i = 0; i < tokens.size(); i++) {
			if (caret < tokStart[i])
				continue;
			if (caret < tokEnd[i])
				return tokens.get(i);
		}
		return null;
	}
}