package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.elements.*;
import fr.loria.synalp.jtrans.speechreco.s4.Alignment;

public class Track {
	public String speakerName;
	public ElementList elts = new ElementList();
	public Alignment words = new Alignment();
	public Alignment phons = new Alignment();

	public Track(String speakerName) {
		this.speakerName = speakerName;
	}

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
	 * Clears alignment between two anchors surrounding the given anchor.
	 */
	public void clearAlignmentAround(Anchor anchor) {
		ElementList.Neighborhood<Anchor> range =
				elts.getNeighbors(anchor, Anchor.class);
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
			if (el instanceof Word)
				((Word) el).posInAlign = -1;
		}

		// Remove segments
		int beforeRemoval = words.getNbSegments();
		words.clearInterval(
				a!=null? a.getFrame(): 0,
				b!=null? b.getFrame(): Integer.MAX_VALUE);
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
	}

	@Override
	public String toString() {
		return speakerName;
	}
}
