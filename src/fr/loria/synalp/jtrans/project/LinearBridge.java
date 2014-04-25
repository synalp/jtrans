package fr.loria.synalp.jtrans.project;

import java.util.Arrays;
import java.util.Iterator;

public class LinearBridge
		implements Iterator<AnchorSandwich[]>
{
	private final int nTracks;
	private final Iterator<AnchorSandwich>[] sandwichIterators;
	private final AnchorSandwich[] currentSandwiches;


	public LinearBridge(Project p) {
		nTracks = p.speakerCount();
		sandwichIterators = new Iterator[nTracks];
		currentSandwiches = new AnchorSandwich[nTracks];

		for (int i = 0; i < nTracks; i++) {
			Iterator<AnchorSandwich> iter = p.sandwichIterator(i);
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


	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
