package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.align.Aligner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TrackProject extends Project {
	public List<List<Phrase>> tracks = new ArrayList<List<Phrase>>();

	public int getTrackSize(int i) {return tracks.get(i).size();}
	
	@Override
	public List<Token> getTokens(int speaker) {
		ArrayList<Token> res = new ArrayList<>();
		for (Phrase phrase: tracks.get(speaker)) {
			res.addAll(phrase);
		}
		return res;
	}

	public void fixTracks() {
		for (int i=0;i<tracks.size();i++) {
			List<Phrase> x = tracks.get(i);
			for (int j=0;j<x.size();j++) {
				System.out.println(x.get(j).getClass().getName());
			}
		}
	}
	
	@Override
	public Iterator<Phrase> phraseIterator(int speaker) {
		return tracks.get(speaker).iterator();
	}

	@Override
	public void align(Aligner aligner, Aligner reference)
			throws IOException, InterruptedException
	{
		clearAlignment();

		for (int i = 0; i < speakerCount(); i++) {
			Iterator<Phrase> itr = phraseIterator(i);

			while (itr.hasNext()) {
				Phrase phrase = itr.next();

				if (!phrase.isFullyAligned()) {
					aligner.align(
							phrase.getInitialAnchor(),
							phrase.getFinalAnchor(),
							phrase,
							reference);
				}
			}
		}
	}

	public void addTrack(String name, List<Phrase> newTrack) {
		final int speakerID = tracks.size();

		speakerNames.add(name);
		tracks.add(newTrack);
		assert tracks.size() == speakerNames.size();

		for (Phrase phrase: newTrack) {
			for (Token token: phrase) {
				token.setSpeaker(speakerID);
			}
		}
	}
}
