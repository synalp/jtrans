package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.elements.Anchor;
import fr.loria.synalp.jtrans.elements.Element;
import fr.loria.synalp.jtrans.elements.Word;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public class AnchorSandwich
		extends AbstractList<Element>
		implements Comparable<AnchorSandwich>
{
	private final List<Element> elements;
	private final Anchor initialAnchor;
	private final Anchor finalAnchor;

	AnchorSandwich(List<Element> baseList) {
		final int subListStart, subListEnd;

		Element first = baseList.get(0);
		if (first instanceof Anchor) {
			initialAnchor = (Anchor)first;
			subListStart = 1;
		} else {
			initialAnchor = null;
			subListStart = 0;
		}

		Element last = baseList.get(baseList.size()-1);
		if (last instanceof Anchor) {
			finalAnchor = (Anchor)last;
			subListEnd = baseList.size()-1;
		} else {
			finalAnchor = null;
			subListEnd = baseList.size();
		}

		if (first == last && initialAnchor != null) {
			elements = baseList.subList(0, 0);
		} else {
			elements = baseList.subList(subListStart, subListEnd);
		}
	}

	AnchorSandwich(List<Element> elements, Anchor initialAnchor, Anchor finalAnchor) {
		this.elements = elements;
		this.initialAnchor = initialAnchor;
		this.finalAnchor = finalAnchor;
	}

	public Anchor getInitialAnchor() {
		return initialAnchor;
	}

	public Anchor getFinalAnchor() {
		return finalAnchor;
	}

	public List<Word> getWords() {
		List<Word> words = new ArrayList<Word>();

		for (Element el: elements) {
			if (el instanceof Word) {
				words.add((Word)el);
			}
		}

		return words;
	}

	public String getSpaceSeparatedWords() {
		StringBuilder sb = new StringBuilder();
		String prefix = "";

		for (Element el: elements) {
			if (el instanceof Word) {
				sb.append(prefix).append(el.toString());
				prefix = " ";
			}
		}

		return sb.toString();
	}


	public boolean isFullyAligned() {
		for (Element el: elements) {
			if (el instanceof Word && !((Word) el).isAligned()) {
				return false;
			}
		}

		return true;
	}


	@Override
	public Element get(int index) {
		return elements.get(index);
	}

	@Override
	public int size() {
		return elements.size();
	}

	@Override
	public int compareTo(AnchorSandwich o) {
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
