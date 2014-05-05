package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.linguist.acoustic.HMMState;

import java.util.ArrayList;
import java.util.List;

/**
 * StatePool that allows duplicate states
 * (e.g. for speaker-dependent Gaussians).
 */
public class StateList extends StatePool {

	private List<HMMState> states = new ArrayList<>();


	@Override
	public int size() {
		return states.size();
	}

	@Override
	public void clear() {
		states.clear();
	}

	@Override
	public HMMState get(int idx) {
		return states.get(idx);
	}

	@Override
	public int indexOf(HMMState state) {
		return states.indexOf(state);
	}

	@Override
	public int add(HMMState state) {
		states.add(state);
		return states.size()-1;
	}
}
