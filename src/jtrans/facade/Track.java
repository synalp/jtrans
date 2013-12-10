package jtrans.facade;

import jtrans.elements.*;
import jtrans.speechreco.s4.Alignment;

public class Track {
	public ElementList elts = new ElementList();
	public Alignment words = new Alignment();
	public Alignment phons = new Alignment();

	public void clearAlignment() {
		words = new Alignment();
		phons = new Alignment();
		for (Word word: elts.getMots())
			word.posInAlign = -1;
		refreshIndex();
	}

	public void refreshIndex() {
		words.buildIndex();
		phons.buildIndex();
		elts.refreshIndex();
	}

	/**
	 * Clears alignment around an anchor.
	 * @return the anchor's neighborhood, i.e. the range of elements
	 * that got unaligned
	 */
	public ElementList.Neighborhood<Anchor> clearAlignmentAround(Anchor anchor) {
		ElementList.Neighborhood<Anchor> range =
				elts.getNeighbors(anchor, Anchor.class);

		int from = range.prev!=null? range.prevIdx: 0;
		int to   = range.next!=null? range.nextIdx: elts.size()-1;

		// Unalign the affected words
		for (int i = from; i <= to; i++) {
			Element el = elts.get(i);
			if (el instanceof Word)
				((Word) el).posInAlign = -1;
		}

		// Remove segments
		int beforeRemoval = words.getNbSegments();
		words.clearInterval(
				range.prev != null ? range.prev.getFrame() : 0,
				range.next != null ? range.next.getFrame() : Integer.MAX_VALUE);
		int removed = beforeRemoval - words.getNbSegments();

		// Adjust elements following the removal
		for (int i = to+1; i < elts.size(); i++) {
			Element el = elts.get(i);
			if (!(el instanceof Word))
				continue;
			((Word) el).posInAlign -= removed;
		}

		// TODO: unalign phonemes, clear affected overlaps...
		refreshIndex();

		return range;
	}
}
