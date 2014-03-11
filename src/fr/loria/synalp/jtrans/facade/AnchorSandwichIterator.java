package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.elements.Anchor;
import fr.loria.synalp.jtrans.elements.Element;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class AnchorSandwichIterator implements Iterator<AnchorSandwich> {

	private final List<Element> baseList;
	private final ListIterator<Element> baseIterator;
	private int currentSandwichEl0;


	AnchorSandwichIterator(List<Element> elementList) {
		baseList = elementList;
		baseIterator = baseList.listIterator();
		currentSandwichEl0 = 0;
	}


	@Override
	public boolean hasNext() {
		return baseIterator.hasNext();
	}


	@Override
	public AnchorSandwich next() {
		Element e = null;
		int newSandwichEl0 = baseIterator.previousIndex();
		AnchorSandwich sandwich;

		while (baseIterator.hasNext() && !(e instanceof Anchor)) {
			newSandwichEl0++;
			e = baseIterator.next();
		}

		if (!(e instanceof Anchor)) {
			newSandwichEl0 = baseList.size()-1;
		}

		sandwich = new AnchorSandwich(
				baseList.subList(currentSandwichEl0, newSandwichEl0+1));
		currentSandwichEl0 = newSandwichEl0;

		if (currentSandwichEl0 == 0 && hasNext()) {
			assert sandwich.isEmpty();
			return next();
		}

		return sandwich;
	}


	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
