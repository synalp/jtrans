package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.elements.*;

import java.util.ArrayList;
import java.util.List;

public class Track {

	public String speakerName;
	public List<Element> elts = new ArrayList<Element>();


	public Track(String speakerName) {
		this.speakerName = speakerName;
	}


	public void clearAlignment() {
		for (Word word: getWords()) {
			word.clearAlignment();
		}
	}


	public List<Word> getWords() {
		ArrayList<Word> res = new ArrayList<Word>();
		for (Element element: elts) {
			if (element instanceof Word) {
				res.add((Word)element);
			}
		}
		return res;
	}


	public List<Word> getAlignedWords() {
		ArrayList<Word> res = new ArrayList<Word>();
		for (Element element: elts) {
			if (element instanceof Word && ((Word) element).isAligned()) {
				res.add((Word)element);
			}
		}
		return res;
	}


	public AnchorSandwichIterator sandwichIterator() {
		return new AnchorSandwichIterator(elts);
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


	@Override
	public String toString() {
		return speakerName;
	}

}
