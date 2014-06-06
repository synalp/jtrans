package fr.loria.synalp.jtrans.gui;

import fr.loria.synalp.jtrans.project.Token;

import javax.swing.*;
import java.util.List;

public abstract class WordFinder {
	JTransGUI gui;
	int cSpk = 0;
	int cWord = 0;
	boolean found = false;

	public WordFinder(JTransGUI gui) {
		this.gui = gui;
	}

	public void next(int delta) {
		assert delta == 1 || delta == -1;

		if (found) {
			cWord += delta;
			found = false;
		}

		for (;;) {
			for (; cSpk < gui.project.speakerCount(); cSpk++) {
				List<Token> words = gui.project.getTokens(cSpk);
				for (; cWord >= 0 && cWord < words.size(); cWord += delta) {
					Token t = words.get(cWord);
					if (t.getType() == Token.Type.WORD && matches(t)) {
						found = true;
						gui.table.highlightWord(cSpk, t);
						return;
					}
				}

				reset(delta, false);
			}

			int rc = JOptionPane.showConfirmDialog(gui.jf,
					String.format("No more %s.\nSearch from %s?",
							getGoal(),
							delta > 0 ? "beginning" : "end"),
					getGoal(),
					JOptionPane.YES_NO_OPTION
			);

			if (rc != JOptionPane.YES_OPTION) {
				return;
			}

			reset(delta, true);
		}
	}

	public void next() {
		next(1);
	}

	public void previous() {
		next(-1);
	}

	/**
	 * @param delta forward search if >0, backward search if <0
	 */
	public void reset(int delta, boolean resetCurrentSpeaker) {
		if (resetCurrentSpeaker) {
			cSpk = 0;
		}

		if (delta > 0) {
			// search from beginning
			cWord = 0;
		} else {
			// search from end
			cWord = gui.project.getTokens(cSpk).size()-1;
		}
	}

	public void reset() {
		reset(1, true);
	}

	public abstract boolean matches(Token word);

	protected abstract String getGoal();

	public void prompt() {
		// no-op by default
	}


	////////////////////////////////////////////////////////////////////////////
	// IMPLEMENTATIONS


	public static class Anonymous extends WordFinder {
		public Anonymous(JTransGUI gui) {
			super(gui);
		}

		@Override
		public boolean matches(Token word) {
			return word.shouldBeAnonymized();
		}

		@Override
		public String getGoal() {
			return "Anonymous Words";
		}
	}


	public static class ByContent extends WordFinder {
		private String content;

		public ByContent(JTransGUI gui) {
			super(gui);
		}

		@Override
		public boolean matches(Token word) {
			if (null == content) {
				return false;
			}
			assert content.toLowerCase().equals(content);
			return word.toString().toLowerCase().equals(content);
		}

		@Override
		public void prompt() {
			String value = JOptionPane.showInputDialog(gui.jf,
					"Word to find",
					content);
			if (value == null) {
				return;
			}
			content = value.toLowerCase();
			reset(1, true);
			next();
		}

		@Override
		protected String getGoal() {
			return "Words matching \"" + content + "\"";
		}
	}

}
