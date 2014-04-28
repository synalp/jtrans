package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.elements.Element;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.facade.AutoAligner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TrackProject extends Project {
	public List<List<AnchorSandwich>> tracks = new ArrayList<>();

	@Override
	public List<Word> getWords(int speaker) {
		ArrayList<Word> res = new ArrayList<>();
		for (AnchorSandwich phrase: tracks.get(speaker)) {
			for (Element element: phrase) {
				if (element instanceof Word) {
					res.add((Word) element);
				}
			}
		}
		return res;
	}

	@Override
	public Iterator<AnchorSandwich> sandwichIterator(int speaker) {
		return tracks.get(speaker).iterator();
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

	public void addTrack(String name, List<AnchorSandwich> t) {
		speakerNames.add(name);
		tracks.add(t);
		assert tracks.size() == speakerNames.size();
	}
}
