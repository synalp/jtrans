package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.elements.Anchor;
import fr.loria.synalp.jtrans.elements.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class LinearBridge
		implements Iterator<AnchorSandwich[]>
{
	private final int nTracks;
	private final AnchorSandwichIterator[] sandwichIterators;
	private final AnchorSandwich[] currentSandwiches;


	public LinearBridge(List<Track> tracks) {
		nTracks = tracks.size();
		sandwichIterators = new AnchorSandwichIterator[nTracks];
		currentSandwiches = new AnchorSandwich[nTracks];

		for (int i = 0; i < nTracks; i++) {
			AnchorSandwichIterator iter = tracks.get(i).sandwichIterator();
			sandwichIterators[i] = iter;
			if (iter.hasNext()) {
				currentSandwiches[i] = iter.next();
			}
		}
	}


	@Override
	public boolean hasNext() {
		if (nTracks == 0) {
			return false;
		}

		for (AnchorSandwich s: currentSandwiches) {
			if (s != null) {
				return true;
			}
		}

		return false;
	}


	@Override
	public AnchorSandwich[] next() {
		AnchorSandwich[] simultaneous = new AnchorSandwich[nTracks];
		AnchorSandwich earliest = null;

		// Find track containing the earliest upcoming anchor
		for (int i = 0; i < nTracks; i++) {
			AnchorSandwich sandwich = currentSandwiches[i];

			if (sandwich == null) {
				continue;
			}

			if (earliest == null) {
				simultaneous[i] = sandwich;
				earliest = sandwich;
			} else {
				int cmp = sandwich.compareTo(earliest);

				if (cmp < 0) {
					Arrays.fill(simultaneous, 0, i, null);
					simultaneous[i] = sandwich;
					earliest = sandwich;
				} else if (cmp == 0) {
					simultaneous[i] = sandwich;
				}
			}
		}

		for (int i = 0; i < nTracks; i++) {
			if (simultaneous[i] == null) {
				;
			} else if (!sandwichIterators[i].hasNext()) {
				currentSandwiches[i] = null;
			} else {
				currentSandwiches[i] = sandwichIterators[i].next();
			}
		}

		return simultaneous;
	}


	/**
	 * Returns a linear sequence of elements in chronological order,
	 * from all tracks (hence "interleaved").
	 *
	 * In the case of simultaneous anchors in several tracks, the anchor
	 * sandwich containing the most words is kept in the linear sequence; all
	 * other simultaneous sandwiches are ignored.
	 *
	 * TODO: overlapping non-simultaneous sandwiches (e.g. OFROM)
	 *
	 * @return a sequence of elements contained in an AnchorSandwich whose
	 * initial and final anchors delimit the sequence
	 */
	public AnchorSandwich nextInterleavedElementSequence() {
		List<Element> seq = new ArrayList<Element>();

		Anchor initialAnchor = null;
		Anchor finalAnchor = null;

		while (hasNext()) {
			AnchorSandwich longest = nextSingle();

			if (longest == null) {
				continue;
			}

			// build sequence sandwich

			seq.addAll(longest);

			if (initialAnchor == null) {
				initialAnchor = longest.getInitialAnchor();
			}

			// Finish this sequence if we have a timed anchor
			Anchor fa = longest.getFinalAnchor();
			if (fa != null && fa.hasTime()) {
				finalAnchor = fa;
				break;
			}
		}

		return new AnchorSandwich(seq, initialAnchor, finalAnchor);
	}


	/**
	 * Returns a single upcoming sandwich. In the case of simultaneous
	 * sandwiches, returns the longest one (in terms of word count).
	 *
	 * TODO: overlapping non-simultaneous sandwiches (e.g. OFROM)
	 */
	public AnchorSandwich nextSingle() {
		AnchorSandwich longest = null;
		int longestLength = 0;

		// find longest simultaneous sandwich
		for (AnchorSandwich sandwich: next()) {
			if (sandwich == null || sandwich.isEmpty()) {
				continue;
			}

			int length = sandwich.getWords().size();
			if (length > longestLength) {
				longest = sandwich;
				longestLength = length;
			}
		}

		return longest;
	}


	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
