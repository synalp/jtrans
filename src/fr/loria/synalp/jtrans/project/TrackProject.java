package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.elements.Element;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.facade.AutoAligner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TrackProject extends Project {
	public List<List<Phrase>> tracks = new ArrayList<>();

	@Override
	public List<Word> getWords(int speaker) {
		ArrayList<Word> res = new ArrayList<>();
		for (Phrase phrase: tracks.get(speaker)) {
			for (Element element: phrase) {
				if (element instanceof Word) {
					res.add((Word) element);
				}
			}
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
							phrase.getWords());
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
