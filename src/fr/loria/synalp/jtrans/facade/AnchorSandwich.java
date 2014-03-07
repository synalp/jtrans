package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.elements.Anchor;
import fr.loria.synalp.jtrans.elements.Element;

import java.util.AbstractList;
import java.util.List;

public class AnchorSandwich
		extends AbstractList<Element>
		implements Comparable<AnchorSandwich>
{
	private final List<Element> baseList;

	AnchorSandwich(List<Element> baseList) {
		this.baseList = baseList;
	}

	public Anchor getInitialAnchor() {
		if (isEmpty()) {
			return null;
		}

		Element first = baseList.get(0);
		return first instanceof Anchor? (Anchor)first: null;
	}

	@Override
	public Element get(int index) {
		return baseList.get(index);
	}

	@Override
	public int size() {
		return baseList.size();
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
