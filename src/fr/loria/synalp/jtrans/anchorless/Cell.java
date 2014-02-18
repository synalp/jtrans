package fr.loria.synalp.jtrans.anchorless;

import java.util.*;

/**
 * A cell in the GrammarVector
 */
public class Cell {
	String name = null;
	Set<Cell> transitions = new HashSet<Cell>();


	public Cell(String s) {
		name = s;
	}


	public Cell link(Cell c) {
		transitions.add(c);
		return c;
	}


	public String toString() {
		return name;
	}


	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Cell))
			return false;

		Cell other = (Cell)o;

		if (!name.equals(other.name))
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