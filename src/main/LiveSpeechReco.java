package main;

import plugins.speechreco.aligners.sphiinx4.S4ForceAlignBlocViterbi;

public class LiveSpeechReco {
	public static void doReco() {
		S4ForceAlignBlocViterbi s4 = S4ForceAlignBlocViterbi.getS4Aligner(null);
		// TODO: define the vocabulary here + create in S4ForceAV a loop+filler grammar from these words
		String[] voc = {};
		s4.setMots(voc);
		// starts live speech reco from mike:
		s4.setNewAudioFile(null);
	}
	
	/**
	 * synchronous (blocking !) function for live reco from mike
	 */
	public static void liveMikeReco() {
		// TODO
	}
}
