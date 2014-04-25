package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.facade.AutoAligner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TrackProject extends Project {
	public List<Track> tracks = new ArrayList<>();

	@Override
	public List<Word> getWords(int speaker) {
		return tracks.get(speaker).getWords();
	}

	@Override
	public List<Word> getAlignedWords(int speaker) {
		return tracks.get(speaker).getAlignedWords();
	}

	@Override
	public Iterator<AnchorSandwich> sandwichIterator(int speaker) {
		return tracks.get(speaker).sandwichIterator();
	}

	@Override
	public void align(AutoAligner aligner)
			throws IOException, InterruptedException
	{
		clearAlignment();

		for (int i = 0; i < speakerCount(); i++) {
			Iterator<AnchorSandwich> itr = sandwichIterator(i);

			while (itr.hasNext()) {
				AnchorSandwich sandwich = itr.next();

				if (/*clear || */ !sandwich.isFullyAligned()) {
					align(aligner,
							sandwich.getInitialAnchor(),
							sandwich.getFinalAnchor(),
							sandwich.getWords());
				}
			}
		}
	}

	public List<Word> getWords() {
		List<Word> words = new ArrayList<>();
		for (Track t: tracks) {
			words.addAll(t.getWords());
		}
		return words;
	}

	public void addTrack(String name, Track t) {
		speakerNames.add(name);
		tracks.add(t);
		assert tracks.size() == speakerNames.size();
	}
}
