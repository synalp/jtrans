package fr.loria.synalp.jtrans.viterbi;

import fr.loria.synalp.jtrans.elements.Word;
import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
		Assert.assertEquals(3*(1+1+1), sg.getNodeCount());

		sg = bogusSG("a [ a ]");
		Assert.assertEquals(3*(1+1+1+1), sg.getNodeCount());

		sg = bogusSG("a [ a ] [ a ]");
		Assert.assertEquals(3*(1+1+1+1+1), sg.getNodeCount());

		sg = bogusSG("a ( a | i )");
		Assert.assertEquals(3*(1+1+2+1), sg.getNodeCount());

		sg = bogusSG("a ( a | [ i ] )");
		Assert.assertEquals(3*(1+1+2+1), sg.getNodeCount());
	}


	@Test
	public void testInterWordSilences() {
		StateGraph sg;

		sg = bogusSG("a", "a");
		Assert.assertEquals(3*(1+1+2+1), sg.getNodeCount());

		sg = bogusSG("a", "[ a ]");
		Assert.assertEquals(3*(1+1+2+1), sg.getNodeCount());

		sg = bogusSG("a", "( a | i )");
		Assert.assertEquals(3*(1+1+3+1), sg.getNodeCount());
	}


	@Test
	public void testInitialEmptyRule() {
		StateGraph sg = bogusSG(null, "a");
		Assert.assertEquals(3*3, sg.getNodeCount());
	}


	@Test
	public void testFinalEmptyRule() {
		StateGraph sg;

		sg = bogusSG("a", null);
		Assert.assertEquals(3*3, sg.getNodeCount());

		sg = bogusSG("a", "b", null);
		Assert.assertEquals(3*(1+1+2+1), sg.getNodeCount());

		sg = bogusSG("a", "b", "a", null);
		Assert.assertEquals(3*(1+1+2+2+1), sg.getNodeCount());
	}


	@Test
	public void testConsecutiveEmptyRules() {
		StateGraph sg = bogusSG(null, null);
		Assert.assertEquals(3*2, sg.getNodeCount());

		sg = bogusSG(null, null, null, null, null);
		Assert.assertEquals(3*2, sg.getNodeCount());

		sg = bogusSG(null, null, "a", null, null, null);
		Assert.assertEquals(3*3, sg.getNodeCount());
	}


	@Test
	public void testSandwichedEmptyRules() {
		final int expected = 3 * (1 + 1 + 2 + 1);

		StateGraph sg = bogusSG("a", null, "b");
		Assert.assertEquals(expected, sg.getNodeCount());

		sg = bogusSG("a", null, null, null, null, "b");
		Assert.assertEquals(expected, sg.getNodeCount());

		sg = bogusSG("a", null, null, null, null, "b", null);
		Assert.assertEquals(expected, sg.getNodeCount());

		sg = bogusSG(null, "a", null, null, null, null, "b", null);
		Assert.assertEquals(expected, sg.getNodeCount());
	}


	@Test
	public void testEmptyRulesWithRealGrammar() {
		final int expected = 3 * (1 + 1 + 1);
		StateGraph sg;

		sg = new StateGraph("a");
		Assert.assertEquals(expected, sg.getNodeCount());

		sg = new StateGraph("() a");
		Assert.assertEquals(expected, sg.getNodeCount());

		sg = new StateGraph("a ()");
		Assert.assertEquals(expected, sg.getNodeCount());

		sg = new StateGraph("() () () () a () () () ()");
		Assert.assertEquals(expected, sg.getNodeCount());
	}


	@Test
	public void testManyPredecessors() {
		// This test may throw an array out of bounds exception if
		// MAX_TRANSITIONS is set to a small value
		// TODO: add a new test here every time we bump MAX_TRANSITIONS

		new StateGraph("5 à 6 zones c' est 51 10 euros");
		new StateGraph("10 10 10 10");
	}


	@Test
	public void testWordAlignments() {
		Word ah = new Word("ah");
		Word meh = new Word("meh");

		List<Word> alignable = new ArrayList<Word>();
		alignable.add(ah);
		alignable.add(meh);

		StateGraph sg = bogusSG("a", "m ( e | i )");

		Assert.assertEquals(
				3 * (1 + /*a*/(1) + 1 + (/*m*/1 + /*e|i*/2) + 1),
				sg.getNodeCount());

		int[] timeline = new int[] {
				// initial SIL
				0, 0, 0,
				1,
				2, 2, 2, 2, 2,

				// a (offset: 9)
				3, 3, 3,
				4, 4,
				5, 5, 5, 5, 5, 5, 5,

				// inter-word SIL (offset: 21)
				6,
				7, 7, 7, 7,
				8,

				// m (offset: 27)
				9, 9, 9, 9, 9,
				10, 10, 10,
				11, 11, 11, 11,

				// e (offset: 39)
				12, 12, 12,
				13,
				14, 14, 14,

				// skip i: states 15, 16, 17 unused

				// final SIL (offset: 46)
				18, 18, 18,
				19, 19,
				20,
		};

		sg.setWordAlignments(alignable, timeline, 0);

		Assert.assertTrue(ah.isAligned());
		Assert.assertTrue(meh.isAligned());

		Assert.assertEquals(9, ah.getSegment().getStartFrame());
		Assert.assertEquals(27-1, ah.getSegment().getEndFrame());

		Assert.assertEquals(27, meh.getSegment().getStartFrame());
		Assert.assertEquals(timeline.length-1, meh.getSegment().getEndFrame());

		{
			List<Word.Phone> p = ah.getPhones();
			Assert.assertEquals(2,    p.size());

			Assert.assertEquals("SIL", p.get(0).toString());
			Assert.assertEquals(0,     p.get(0).getSegment().getStartFrame());
			Assert.assertEquals(3+1+5, p.get(0).getSegment().getLengthFrames());

			Assert.assertEquals("a",   p.get(1).toString());
			Assert.assertEquals(9,     p.get(1).getSegment().getStartFrame());
			Assert.assertEquals(21-1,  p.get(1).getSegment().getEndFrame());

		}

		{
			List<Word.Phone> p = meh.getPhones();
			Assert.assertEquals(4, p.size());

			Assert.assertEquals("SIL", p.get(0).toString());
			Assert.assertEquals(21,    p.get(0).getSegment().getStartFrame());
			Assert.assertEquals(1+4+1, p.get(0).getSegment().getLengthFrames());

			Assert.assertEquals("m",   p.get(1).toString());
			Assert.assertEquals(27,    p.get(1).getSegment().getStartFrame());
			Assert.assertEquals(5+3+4, p.get(1).getSegment().getLengthFrames());

			Assert.assertEquals("e",   p.get(2).toString());
			Assert.assertEquals(39,    p.get(2).getSegment().getStartFrame());
			Assert.assertEquals(3+1+3, p.get(2).getSegment().getLengthFrames());
		}
	}

}
