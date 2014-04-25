package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.elements.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class Track {

	public List<Element> elts = new ArrayList<>();


	// TODO: replace this with accessor for elts
	public void setSpeakerOnWords(int id) {
		for (Word word: getWords()) {
			word.setSpeaker(id);
		}
	}


	public List<Word> getWords() {
		ArrayList<Word> res = new ArrayList<>();
		for (Element element: elts) {
			if (element instanceof Word) {
				res.add((Word)element);
			}
		}
		return res;
	}


	public List<Word> getAlignedWords() {
		List<Word> res = new ArrayList<>();
		for (Element element: elts) {
			if (element instanceof Word && ((Word) element).isAligned()) {
				res.add((Word)element);
			}
		}
		return res;
	}


	public ElementListAnchorSandwichIterator sandwichIterator() {
		return new ElementListAnchorSandwichIterator(elts);
	}


	/**
	 * Neighbors of an Element in the element list.
	 * @param <T> subclass of Element
	 */
	public class Neighborhood<T extends Element> {
		public final T prev;
		public final T next;
		public final int prevIdx;
		public final int nextIdx;

		private Neighborhood(int p, int n) {
			prevIdx = p;
			nextIdx = n;

			prev = prevIdx>=0? (T)elts.get(prevIdx): null;
			next = nextIdx>=0? (T)elts.get(nextIdx): null;
		}
	}


	/**
	 * Returns the surrounding neighbors (of a specific class) of an element.
	 * @param central central element
	 * @param surroundClass class of the surrounding neighbors
	 * @param <T> should be the same type as surroundClass
	 */
	public <T extends Element> Neighborhood<T> getNeighbors(
			Element central, Class<T> surroundClass)
	{
		int prev = -1;
		int curr = -1;
		int next = -1;

		for (int i = 0; i < elts.size(); i++) {
			Element el = elts.get(i);

			if (el == central) {
				curr = i;
			} else if (!surroundClass.isInstance(el)) {
				;
			} else if (curr < 0) {
				prev = i;
			} else {
				next = i;
				break;
			}
		}

		return new Neighborhood<T>(prev, next);
	}


	/**
	 * Clears alignment between two anchors surrounding the given anchor.
	 */
	public void clearAlignmentAround(Anchor anchor) {
		Neighborhood<Anchor> range =
				getNeighbors(anchor, Anchor.class);
		clearAlignmentBetween(range.prev, range.next);
	}


	/**
	 * Clears alignment between two anchors.
	 */
	public void clearAlignmentBetween(Anchor a, Anchor b) {
		int from = a!=null? elts.indexOf(a): 0;
		int to   = b!=null? elts.indexOf(b): elts.size()-1;
		assert from < to;

		// Unalign the affected words
		for (int i = from; i <= to; i++) {
			Element el = elts.get(i);
			if (el instanceof Word) {
				((Word) el).clearAlignment();
			}
		}
	}


	public static class ElementListAnchorSandwichIterator implements Iterator<AnchorSandwich> {

		private final List<Element> baseList;
		private final ListIterator<Element> baseIterator;
		private int currentSandwichEl0;


		ElementListAnchorSandwichIterator(List<Element> elementList) {
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

}
