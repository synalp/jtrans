package fr.loria.synalp.jtrans.viterbi;

import edu.cmu.sphinx.linguist.acoustic.*;
import fr.loria.synalp.jtrans.speechreco.s4.HMMModels;

import java.util.*;

public class StatePool {

	/**
	 * Maps a phone to the index of the first unique HMM state in the
	 * uniqueStates array.
	 */
	private Map<String, Integer> phoneUStates = new HashMap<>();

	/** Pool of unique HMM states. */
	private List<HMMState> uniqueStates = new ArrayList<>();

	private AcousticModel acMod = HMMModels.getAcousticModels();
	private UnitManager unitMgr = new UnitManager();


	public static final String SILENCE_PHONE = "SIL";


	private int add(String phone) {
		assert !phoneUStates.containsKey(phone);

		int idx = uniqueStates.size();

		phoneUStates.put(phone, idx);

		// find HMM for this phone
		HMM hmm = acMod.lookupNearestHMM(
				unitMgr.getUnit(phone), HMMPosition.UNDEFINED, false);

		for (int i = 0; i < 3; i++) {
			HMMState state = hmm.getState(i);
			uniqueStates.add(state);

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

		return idx;
	}


	public void check(String phone) {
		if (!phoneUStates.containsKey(phone)) {
			add(phone);
		}
	}


	public int getId(String phone, int stateNo) {
		if (stateNo < 0 || stateNo > 2) {
			throw new IllegalArgumentException("illegal state number " +
					"(valid state numbers are 0, 1, 2)");
		}

		return stateNo + phoneUStates.get(phone);
	}


	public HMMState get(int id) {
		return uniqueStates.get(id);
	}


	public HMMState get(String phone, int stateNo) {
		return get(getId(phone, stateNo));
	}


	public int size() {
		return uniqueStates.size();
	}

}
