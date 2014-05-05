package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.linguist.acoustic.HMMState;

import java.util.Arrays;

public abstract class StatePool {

	public static final String SILENCE_PHONE = "SIL";


	public static String getPhone(HMMState state) {
		return null == state
				? "null"
				: state.getHMM().getBaseUnit().getName();
	}


	public abstract int size();
	public abstract void clear();
	public abstract HMMState get(int idx);
	public abstract int indexOf(HMMState state);
	public abstract int add(HMMState state);


	public String getPhone(int idx) {
		return getPhone(get(idx));
	}


	/**
	 * Adds states from another StatePool.
	 * @return translation map of state indices in the other StatePool to state
	 * indices in this StatePool
	 */
	public int[] addAll(StatePool p) {
		int[] translations = new int[p.size()];
		Arrays.fill(translations, -1);

		for (int i = 0; i < p.size(); i++) {
			translations[i] = add(p.get(i));
		}

		return translations;
	}

}
