package fr.loria.synalp.jtrans.viterbi;

import junit.framework.Assert;
import org.junit.Test;

public class StateGraphTest {

	/**
	 * Constructs a StateGraph with arbitrary rules. The actual "human-readable"
	 * words don't matter (bogus words are generated for each rule).
	 * @param wordRules Rules for each separate word. Rules must be given as
	 *                  strings of whitespace-separated rule tokens.
	 */
	private static StateGraph bogusSG(String... wordRules) {
		final int n = wordRules.length;

		String[] phrase = new String[n];
		for (int i = 0; i < n; i++) {
			phrase[i] = "Bogus" + i;
		}

		String[][] rules = new String[n][];
		for (int i = 0; i < n; i++) {
			if (wordRules[i] != null)
				rules[i] = StateGraph.trimSplit(wordRules[i]);
		}

		return new StateGraph(phrase, rules);
	}


	@Test
	public void testPhoneCountsWithBranching() {
		// Within the same word, so no extra silences

		StateGraph sg;

		sg = bogusSG("a");
		Assert.assertEquals(3*(1+1+1), sg.getStateCount());

		sg = bogusSG("a [ a ]");
		Assert.assertEquals(3*(1+1+1+1), sg.getStateCount());

		sg = bogusSG("a [ a ] [ a ]");
		Assert.assertEquals(3*(1+1+1+1+1), sg.getStateCount());

		sg = bogusSG("a ( a | i )");
		Assert.assertEquals(3*(1+1+2+1), sg.getStateCount());

		sg = bogusSG("a ( a | [ i ] )");
		Assert.assertEquals(3*(1+1+2+1), sg.getStateCount());
	}


	@Test
	public void testInterWordSilences() {
		StateGraph sg;

		sg = bogusSG("a", "a");
		Assert.assertEquals(3*(1+1+2+1), sg.getStateCount());

		sg = bogusSG("a", "[ a ]");
		Assert.assertEquals(3*(1+1+2+1), sg.getStateCount());

		sg = bogusSG("a", "( a | i )");
		Assert.assertEquals(3*(1+1+3+1), sg.getStateCount());
	}


	@Test
	public void testInitialEmptyRule() {
		StateGraph sg = bogusSG(null, "a");
		Assert.assertEquals(3*3, sg.getStateCount());
	}


	@Test
	public void testFinalEmptyRule() {
		StateGraph sg;

		sg = bogusSG("a", null);
		Assert.assertEquals(3*3, sg.getStateCount());

		sg = bogusSG("a", "b", null);
		Assert.assertEquals(3*(1+1+2+1), sg.getStateCount());

		sg = bogusSG("a", "b", "a", null);
		Assert.assertEquals(3*(1+1+2+2+1), sg.getStateCount());
	}


	@Test
	public void testConsecutiveEmptyRules() {
		StateGraph sg = bogusSG(null, null);
		Assert.assertEquals(3*2, sg.getStateCount());

		sg = bogusSG(null, null, null, null, null);
		Assert.assertEquals(3*2, sg.getStateCount());

		sg = bogusSG(null, null, "a", null, null, null);
		Assert.assertEquals(3*3, sg.getStateCount());
	}


	@Test
	public void testSandwichedEmptyRules() {
		final int expected = 3 * (1 + 1 + 2 + 1);

		StateGraph sg = bogusSG("a", null, "b");
		Assert.assertEquals(expected, sg.getStateCount());

		sg = bogusSG("a", null, null, null, null, "b");
		Assert.assertEquals(expected, sg.getStateCount());

		sg = bogusSG("a", null, null, null, null, "b", null);
		Assert.assertEquals(expected, sg.getStateCount());

		sg = bogusSG(null, "a", null, null, null, null, "b", null);
		Assert.assertEquals(expected, sg.getStateCount());
	}


	@Test
	public void testEmptyRulesWithRealGrammar() {
		final int expected = 3 * (1 + 1 + 1);
		StateGraph sg;

		sg = new StateGraph("a");
		Assert.assertEquals(expected, sg.getStateCount());

		sg = new StateGraph("() a");
		Assert.assertEquals(expected, sg.getStateCount());

		sg = new StateGraph("a ()");
		Assert.assertEquals(expected, sg.getStateCount());

		sg = new StateGraph("() () () () a () () () ()");
		Assert.assertEquals(expected, sg.getStateCount());
	}


	@Test
	public void testManyPredecessors() {
		// This test may throw an array out of bounds exception if
		// MAX_TRANSITIONS is set to a small value
		// TODO: add a new test here every time we bump MAX_TRANSITIONS

		new StateGraph("5 à 6 zones c' est 51 10 euros");
		new StateGraph("10 10 10 10");
	}
}
