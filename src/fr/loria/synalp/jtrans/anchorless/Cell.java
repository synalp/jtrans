package fr.loria.synalp.jtrans.anchorless;

import java.util.*;

/**
 * A cell in the GrammarVector
 */
public class Cell<T> {
	T item = null;
	Set<Cell<T>> transitions = new HashSet<Cell<T>>();


	public Cell(T s) {
		item = s;
	}


	public Cell<T> link(Cell<T> c) {
		transitions.add(c);
		return c;
	}


	public String toString() {
		return item.toString();
	}


	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		else if (!(o instanceof Cell))
			return false;

		Cell other = (Cell)o;

		if (!item.equals(other.item))
			return false;

		if (transitions.size() != other.transitions.size())
			return false;

		Set<Cell> remainingTransitions =
				new HashSet<Cell>(other.transitions);

		nextTransition:
		for (Cell myTransition: transitions) {
			for (Cell otherTransition: remainingTransitions) {
				if (myTransition.equals(otherTransition)) {
					remainingTransitions.remove(otherTransition);
					continue nextTransition;
				}
			}
			return false;
		}

		return true;
	}
}