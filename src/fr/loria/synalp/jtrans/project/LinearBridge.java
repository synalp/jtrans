package fr.loria.synalp.jtrans.project;

import java.util.Arrays;
import java.util.Iterator;

public class LinearBridge
		implements Iterator<Phrase[]>
{
	private final int nTracks;
	private final Iterator<Phrase>[] phraseIterators;
	private final Phrase[] currentPhrases;


	public LinearBridge(Project p) {
		nTracks = p.speakerCount();
		phraseIterators = new Iterator[nTracks];
		currentPhrases = new Phrase[nTracks];

		for (int i = 0; i < nTracks; i++) {
			Iterator<Phrase> iter = p.phraseIterator(i);
			phraseIterators[i] = iter;
			if (iter.hasNext()) {
				currentPhrases[i] = iter.next();
			}
		}
	}


	@Override
	public boolean hasNext() {
		if (nTracks == 0) {
			return false;
		}

		for (Phrase s: currentPhrases) {
			if (s != null) {
				return true;
			}
		}

		return false;
	}


	@Override
	public Phrase[] next() {
		Phrase[] simultaneous = new Phrase[nTracks];
		Phrase earliest = null;

		// Find track containing the earliest upcoming anchor
		for (int i = 0; i < nTracks; i++) {
			Phrase phrase = currentPhrases[i];

			if (phrase == null) {
				continue;
			}

			if (earliest == null) {
				simultaneous[i] = phrase;
				earliest = phrase;
			} else {
				int cmp = phrase.compareTo(earliest);

				if (cmp < 0) {
					Arrays.fill(simultaneous, 0, i, null);
					simultaneous[i] = phrase;
					earliest = phrase;
				} else if (cmp == 0) {
					simultaneous[i] = phrase;
				}
			}
		}

		for (int i = 0; i < nTracks; i++) {
			if (simultaneous[i] == null) {
				;
			} else if (!phraseIterators[i].hasNext()) {
				currentPhrases[i] = null;
			} else {
				currentPhrases[i] = phraseIterators[i].next();
			}
		}

		return simultaneous;
	}


	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
