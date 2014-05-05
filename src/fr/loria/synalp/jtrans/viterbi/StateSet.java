package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.linguist.acoustic.*;
import fr.loria.synalp.jtrans.speechreco.s4.HMMModels;

import java.util.*;

/**
 * StatePool that does not allow duplicate states.
 */
public class StateSet extends StatePool {

	/**
	 * Maps a phone to the index of the first unique HMM state in the
	 * uniqueStates array.
	 */
	private Map<String, int[]> phoneUStates = new HashMap<>();

	/** Pool of unique HMM states. */
	private List<HMMState> uniqueStates = new ArrayList<>();

	private AcousticModel acMod = HMMModels.getAcousticModels();
	private UnitManager unitMgr = new UnitManager();


	public StateSet() {
		clear();
	}


	@Override
	public void clear() {
		phoneUStates.clear();
		uniqueStates.clear();

		addPhone(SILENCE_PHONE);
		assert 0 == getId(SILENCE_PHONE, 0);
		assert 1 == getId(SILENCE_PHONE, 1);
		assert 2 == getId(SILENCE_PHONE, 2);
	}


	public int add(HMMState state) {
		int stateNo = state.getState();
		String phone = getPhone(state);
		int[] phoneIdxes = phoneUStates.get(phone);

		assert stateNo >= 0 && stateNo < 3;

		if (null == phoneIdxes) {
			phoneIdxes = new int[]{-1, -1, -1};
			phoneUStates.put(phone, phoneIdxes);
		} else if (phoneIdxes[stateNo] >= 0) {
			// already added
			return phoneIdxes[stateNo];
		}

		assert !uniqueStates.contains(state);
		int idx = uniqueStates.size();

		phoneIdxes[stateNo] = idx;
		uniqueStates.add(state);

		return idx;
	}


	public void check(String phone) {
		if (!phoneUStates.containsKey(phone)) {
			addPhone(phone);
		}
	}


	private void addPhone(String phone) {
		assert !phoneUStates.containsKey(phone);

		// find HMM for this phone
		HMM hmm = acMod.lookupNearestHMM(
				unitMgr.getUnit(phone), HMMPosition.UNDEFINED, false);

		for (int i = 0; i < 3; i++) {
			HMMState state = hmm.getState(i);
			add(state);

			assert state.isEmitting();
			assert !state.isExitState();
			assert state.getSuccessors().length == 2;

			for (HMMStateArc arc: state.getSuccessors()) {
				HMMState arcState = arc.getHMMState();
				if (i == 2 && arcState.isExitState()) {
					continue;
				}

				if (arcState != state) {
					assert i != 2;
					assert !arcState.isExitState();
					assert arcState == hmm.getState(i+1);
				}
			}
		}
	}


	/**
	 * The silence phone is guaranteed to occupy IDs 0 through 2 inclusive.
	 */
	public int getId(String phone, int stateNo) {
		if (stateNo < 0 || stateNo > 2) {
			throw new IllegalArgumentException("illegal state number " +
					"(valid state numbers are 0, 1, 2)");
		}

		return phoneUStates.get(phone)[stateNo];
	}


	@Override
	public HMMState get(int id) {
		return uniqueStates.get(id);
	}


	@Override
	public int indexOf(HMMState state) {
		return uniqueStates.indexOf(state);
	}


	@Override
	public int size() {
		return uniqueStates.size();
	}


	public boolean isSilent(int id) {
		assert id < 0 || id > 2 || getPhone(get(id)).equals(SILENCE_PHONE);
		return id >= 0 && id <= 2;
	}

}
