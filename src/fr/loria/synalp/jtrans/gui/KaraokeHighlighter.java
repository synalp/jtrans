package fr.loria.synalp.jtrans.gui;

import fr.loria.synalp.jtrans.project.Token;
import static fr.loria.synalp.jtrans.speechreco.s4.S4mfccBuffer.second2frame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import static java.lang.System.currentTimeMillis;

/**
 * Highlights words in the GUI as they are uttered.
 */
class KaraokeHighlighter {

	public static final int KARAOKE_UPDATE_INTERVAL = 50; // milliseconds

	/* IMPORTANT: the timer *has* to be a Swing timer, not a java.util.Timer.
	This is to ensure that the callback is called from Swing's event dispatch
	thread. */
	Timer timer;
	JTransGUI gui;
	int[] hl;
	long t0ms;
	int initFrame = 0;


	public KaraokeHighlighter(JTransGUI gui) {
		this.gui = gui;
		hl = new int[gui.project.speakerCount()];
		timer = new Timer(KARAOKE_UPDATE_INTERVAL, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tick();
			}
		});
	}


	public void start() {
		timer.start();
		t0ms = System.currentTimeMillis();
		initFrame = second2frame(gui.getCurPosInSec());
	}


	public void stop() {
		timer.stop();
	}


	public void tick() {
		int frame = initFrame + second2frame(currentTimeMillis() - t0ms) / 1000;

		for (int i = 0; i < gui.project.speakerCount(); i++) {
			// todo: TrackProject.getTokens() is slow
			List<Token> wordList = gui.project.getTokens(i);
			if (wordList.isEmpty()) {
				continue;
			}

			int newHL;

			for (newHL = hl[i]; newHL < wordList.size(); newHL++) {
				Token t = wordList.get(newHL);
				if (t.isAligned() && t.getSegment().getEndFrame() >= frame) {
					break;
				}
			}

			if (newHL >= wordList.size()) {
				continue;
			}

			// Only update UI if the word wasn't already highlighted
			Token w = wordList.get(newHL);

			if (w.getSegment().getStartFrame() > frame) {
				gui.table.highlightWord(i, null);
				continue;
			}

			if (hl[i] != newHL) {
				gui.table.highlightWord(i, w);
			}

			hl[i] = newHL;
		}
	}

}