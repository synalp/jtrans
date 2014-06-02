package fr.loria.synalp.jtrans.project;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * Piece of text sandwiched between two anchors.
 */
public class Phrase
		extends AbstractList<Token>
		implements Comparable<Phrase>
{
	private final List<Token> tokens;
	private final Anchor initialAnchor;
	private final Anchor finalAnchor;

	public Phrase(Anchor initialAnchor, Anchor finalAnchor, List<Token> tokens) {
		this.tokens = tokens;
		this.initialAnchor = initialAnchor;
		this.finalAnchor = finalAnchor;
	}

	public Anchor getInitialAnchor() {
		return initialAnchor;
	}

	public Anchor getFinalAnchor() {
		return finalAnchor;
	}

	public List<Token> getAlignableWords() {
		List<Token> words = new ArrayList<>();

		for (Token token: tokens) {
			if (token.isAlignable()) {
				words.add(token);
			}
		}

		return words;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String prefix = "";

		for (Token token: tokens) {
			sb.append(prefix).append(token);
			prefix = " ";
		}

		return sb.toString();
	}


	public boolean isFullyAligned() {
		for (Token token: tokens) {
			if (token.isAlignable() && !token.isAligned()) {
				return false;
			}
		}

		return true;
	}


	@Override
	public Token get(int index) {
		return tokens.get(index);
	}


	@Override
	public int size() {
		return tokens.size();
	}


	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Phrase)) {
			return false;
		}

		Phrase as = (Phrase)o;

		return as.initialAnchor.equals(initialAnchor) &&
				as.finalAnchor.equals(finalAnchor) &&
				super.equals(o);
	}


	@Override
	public int compareTo(Phrase o) {
		Anchor myIA = getInitialAnchor();
		Anchor theirIA = o.getInitialAnchor();

		if (myIA != null) {
			return myIA.compareTo(theirIA);
		}

		if (theirIA != null) {
			return theirIA.compareTo(null);
		}

		return 0;
	}
}
