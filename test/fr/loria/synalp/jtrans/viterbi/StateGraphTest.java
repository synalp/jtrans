package fr.loria.synalp.jtrans.viterbi;

import fr.loria.synalp.jtrans.elements.Word;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
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

		int[] speakers = new int[phrase.length];

		return new StateGraph(new StatePool(), rules, phrase, speakers);
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

		sg = StateGraph.quick("a");
		Assert.assertEquals(expected, sg.getNodeCount());

		sg = StateGraph.quick("() a");
		Assert.assertEquals(expected, sg.getNodeCount());

		sg = StateGraph.quick("a ()");
		Assert.assertEquals(expected, sg.getNodeCount());

		sg = StateGraph.quick("() () () () a () () () ()");
		Assert.assertEquals(expected, sg.getNodeCount());
	}


	@Test
	public void testManyPredecessors() {
		// This test may throw an array out of bounds exception if
		// MAX_TRANSITIONS is set to a small value
		// TODO: add a new test here every time we bump MAX_TRANSITIONS

		StateGraph.quick("5 à 6 zones c' est 51 10 euros");
		StateGraph.quick("10 10 10 10");
	}


	@Test
	public void testWordBreakdown1() {
		StateGraph sg = bogusSG(
				"( t e y | t y )",
				"p eu",
				"( ( p a s | p e [ SIL ] a [ SIL ] eh s ) [ z ] | p a [ z ] )",
				"( s a v u a r | s a v w a r )"
		);


		Word tu, peux, pas, savoir;

		tu = new Word("tu");
		peux = new Word("peux");
		pas = new Word("pas");
		savoir = new Word("savoir");

		List<Word> phrase = Arrays.asList(tu, peux, pas, savoir);

		int[] timeline = {
				// frame 0: silence
				0, 1, 2,

				// frame 3: "tu"
				12, 13, 14, 15, 16, 17,

				// frame 9: "peux"
				21, 22, 23, 24, 25, 26,

				// frame 15: "pas"
				63, 64, 65, 66, 67, 68,

				// frame 21: silence
				72, 73, 74,

				// frame 24: "sav--"
				93, 94, 95, 96, 97, 98, 99, 100, 101,

				// frame 33: "--oir"
				102, 103, 104, 105, 106, 107, 108, 109, 110,

				// frame 42: silence
				111, 112, 113,
		};

		// spice things up with some offset
		final int F = 1000;

		sg.setWordAlignments(phrase, timeline, F);

		Assert.assertEquals(F+3, tu.getSegment().getStartFrame());
		Assert.assertEquals(F+8, tu.getSegment().getEndFrame());
		Assert.assertEquals(2, tu.getPhones().size());
		Assert.assertEquals("t", tu.getPhones().get(0).toString());
		Assert.assertEquals(F+3, tu.getPhones().get(0).getSegment().getStartFrame());
		Assert.assertEquals(F+5, tu.getPhones().get(0).getSegment().getEndFrame());
		Assert.assertEquals(F+6, tu.getPhones().get(1).getSegment().getStartFrame());
		Assert.assertEquals(F+8, tu.getPhones().get(1).getSegment().getEndFrame());
		Assert.assertEquals("y", tu.getPhones().get(1).toString());

		Assert.assertEquals(F+9, peux.getSegment().getStartFrame());
		Assert.assertEquals(F+14, peux.getSegment().getEndFrame());
		Assert.assertEquals(2, peux.getPhones().size());
		Assert.assertEquals("p", peux.getPhones().get(0).toString());
		Assert.assertEquals("eu", peux.getPhones().get(1).toString());

		Assert.assertEquals(F+15, pas.getSegment().getStartFrame());
		Assert.assertEquals(F+23, pas.getSegment().getEndFrame()); // (+ sil)
		Assert.assertEquals(3, pas.getPhones().size());  // (+ sil)
		Assert.assertEquals("p", pas.getPhones().get(0).toString());
		Assert.assertEquals("a", pas.getPhones().get(1).toString());
		Assert.assertEquals("SIL", pas.getPhones().get(2).toString());

		Assert.assertEquals(F+24, savoir.getSegment().getStartFrame());
		Assert.assertEquals(F+44, savoir.getSegment().getEndFrame());
		Assert.assertEquals(7, savoir.getPhones().size());  // + trailing sil
		Assert.assertEquals("s", savoir.getPhones().get(0).toString());
		Assert.assertEquals("a", savoir.getPhones().get(1).toString());
		Assert.assertEquals("v", savoir.getPhones().get(2).toString());
		Assert.assertEquals("w", savoir.getPhones().get(3).toString());
		Assert.assertEquals("a", savoir.getPhones().get(4).toString());
		Assert.assertEquals("r", savoir.getPhones().get(5).toString());
		Assert.assertEquals("SIL", savoir.getPhones().get(6).toString());
	}


	@Test
	public void testWordBreakdown2() {
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

			/*
			Assert.assertEquals("SIL", p.get(0).toString());
			Assert.assertEquals(0,     p.get(0).getSegment().getStartFrame());
			Assert.assertEquals(3+1+5, p.get(0).getSegment().getLengthFrames());
			*/

			Assert.assertEquals("a",   p.get(0).toString());
			Assert.assertEquals(9,     p.get(0).getSegment().getStartFrame());
			Assert.assertEquals(21-1,  p.get(0).getSegment().getEndFrame());

			Assert.assertEquals("SIL", p.get(1).toString());
			Assert.assertEquals(21,    p.get(1).getSegment().getStartFrame());
			Assert.assertEquals(1+4+1, p.get(1).getSegment().getLengthFrames());
		}

		{
			List<Word.Phone> p = meh.getPhones();
			Assert.assertEquals(3, p.size());

			Assert.assertEquals("m",   p.get(0).toString());
			Assert.assertEquals(27,    p.get(0).getSegment().getStartFrame());
			Assert.assertEquals(5+3+4, p.get(0).getSegment().getLengthFrames());

			Assert.assertEquals("e",   p.get(1).toString());
			Assert.assertEquals(39,    p.get(1).getSegment().getStartFrame());
			Assert.assertEquals(3+1+3, p.get(1).getSegment().getLengthFrames());

			Assert.assertEquals("SIL", p.get(2).toString());
			Assert.assertEquals(46,    p.get(2).getSegment().getStartFrame());
			Assert.assertEquals(3+2+1, p.get(2).getSegment().getLengthFrames());
		}
	}

}
