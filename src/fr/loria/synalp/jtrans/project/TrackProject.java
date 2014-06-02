package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.align.AutoAligner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TrackProject extends Project {
	public List<List<Phrase>> tracks = new ArrayList<>();

	@Override
	public List<Token> getAlignableWords(int speaker) {
		ArrayList<Token> res = new ArrayList<>();
		for (Phrase phrase: tracks.get(speaker)) {
			res.addAll(phrase.getAlignableWords());
		}
		return res;
	}

	@Override
	public Iterator<Phrase> phraseIterator(int speaker) {
		return tracks.get(speaker).iterator();
	}

	@Override
	public void align(AutoAligner aligner)
			throws IOException, InterruptedException
	{
		clearAlignment();

		for (int i = 0; i < speakerCount(); i++) {
			Iterator<Phrase> itr = phraseIterator(i);

			while (itr.hasNext()) {
				Phrase phrase = itr.next();

				if (!phrase.isFullyAligned()) {
					align(aligner,
							phrase.getInitialAnchor(),
							phrase.getFinalAnchor(),
							phrase.getAlignableWords(),
							false); // never concatenate.
					// If we did concatenate, we'd have to start over a new
					// concatenated timeline on the next for iteration anyway.
				}
			}
		}
	}

	public void addTrack(String name, List<Phrase> t) {
		speakerNames.add(name);
		tracks.add(t);
		assert tracks.size() == speakerNames.size();
	}
}
