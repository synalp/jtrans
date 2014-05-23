package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.linguist.acoustic.HMMState;
import fr.loria.synalp.jtrans.elements.Word;

import java.util.ArrayList;
import java.util.List;

public class StateTimeline {

	List<HMMState> states;
	List<Word> words;
	List<Word> uniqueWords;


	public StateTimeline() {
		states = new ArrayList<>();
		words = new ArrayList<>();
		uniqueWords = new ArrayList<>();
	}


	public StateTimeline(StateTimeline other) {
		states = new ArrayList<>(other.states);
		words = new ArrayList<>(other.words);
		uniqueWords = new ArrayList<>(other.uniqueWords);
	}


	public void newFrame(HMMState state, Word word) {
		states.add(state);
		words.add(word);
		if (uniqueWords.isEmpty() || uniqueWords.get(uniqueWords.size()-1) != word) {
			uniqueWords.add(word);
		}
	}


	public int getLength() {
		return states.size();
	}


	public List<Word> getUniqueWords() {
		return uniqueWords;
	}


	public HMMState getStateAtFrame(int frame) {
		return states.get(frame);
	}


	public void setWordAlignments(int offset) {
		for (Word w: uniqueWords) {
			if (w != null) {
				w.clearAlignment();
			}
		}

		Word pWord = null;        // previous word
		Word.Phone phone = null;  // current phone
		String pUnit = null;

		for (int f = 0; f < getLength(); f++) {
			int now = offset+f; // absolute frame number

			Word word = words.get(f);
			if (word != pWord) {
				word.setSegment(now, now);
				pWord = word;
			} else if (null != word) {
				word.getSegment().setEndFrame(now);
			}

			String unit = states.get(f).getHMM().getBaseUnit().getName();
			if (f == 0 || pUnit == null || !unit.equals(pUnit)) {
				if (null != word) {
					phone = new Word.Phone(unit, new Word.Segment(now, now));
					word.addPhone(phone);
				}
				pUnit = unit;
			} else if (null != phone) {
				phone.getSegment().setEndFrame(now);
			}
		}
	}

}
