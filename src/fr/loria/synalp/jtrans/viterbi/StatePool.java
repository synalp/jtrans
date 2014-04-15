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


	public StatePool() {
		check(SILENCE_PHONE);
		assert 0 == getId(SILENCE_PHONE, 0);
		assert 1 == getId(SILENCE_PHONE, 1);
		assert 2 == getId(SILENCE_PHONE, 2);
	}


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


	/**
	 * Adds states from another StatePool.
	 * @return translation map of state indices in the other StatePool to state
	 * indices in this StatePool
	 */
	public int[] addAll(StatePool p) {
		int[] translations = new int[p.size()];
		Arrays.fill(translations, -1);

		for (Map.Entry<String, Integer> e: p.phoneUStates.entrySet()) {
			String phone = e.getKey();
			int pPhoneStateId = e.getValue();

			Integer thisPhoneStateId = phoneUStates.get(phone);

			if (thisPhoneStateId == null) {
				thisPhoneStateId = uniqueStates.size();
				phoneUStates.put(phone, thisPhoneStateId);

				uniqueStates.add(p.uniqueStates.get(pPhoneStateId));
				uniqueStates.add(p.uniqueStates.get(pPhoneStateId+1));
				uniqueStates.add(p.uniqueStates.get(pPhoneStateId+2));
			}

			translations[pPhoneStateId] = thisPhoneStateId;
			translations[pPhoneStateId+1] = thisPhoneStateId+1;
			translations[pPhoneStateId+2] = thisPhoneStateId+2;
		}

		return translations;
	}


	public void check(String phone) {
		if (!phoneUStates.containsKey(phone)) {
			add(phone);
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
