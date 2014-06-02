package fr.loria.synalp.jtrans.viterbi;

import fr.loria.synalp.jtrans.elements.Word;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class StateGraphTest {

	/**
	 * Constructs a StateGraph with arbitrary rules. The actual "human-readable"
	 * words don't matter (bogus words are generated for each rule).
	 * @param wordRules Rules for each separate word. Rules must be given as
	 *                  strings of whitespace-separated rule tokens.
	 * @param interWordSilences insert optional silences between words
	 */
	public static StateGraph bogusSG(
			boolean interWordSilences,
			String... wordRules)
	{
		final int n = wordRules.length;

		List<Word> phrase = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			phrase.add(new Word("Bogus" + i));
		}

		String[][] rules = new String[n][];
		for (int i = 0; i < n; i++) {
			if (wordRules[i] != null)
				rules[i] = StateGraph.trimSplit(wordRules[i]);
		}

		return new StateGraph(
				rules,
				phrase,
				interWordSilences);
	}


	/**
	 * Constructs a StateGraph with arbitrary rules, inserting optional silences
	 * between each word.
	 * @see #bogusSG(boolean, String...)
	 */
	public static StateGraph bogusSG(String... wordRules) {
		return bogusSG(true, wordRules);
	}


	public static String[][] multiTrimSplit(String... wordRules) {
		String[][] trimmed = new String[wordRules.length][];

		for (int i = 0; i < wordRules.length; i++) {
			trimmed[i] = StateGraph.trimSplit(wordRules[i]);
		}

		return trimmed;
	}


	@Test
	public void testPhoneCountsWithBranching() {
		// Within the same word, so no extra silences

		StateGraph sg;

		sg = bogusSG("a");
		assertTrue(sg.isLinear());
		assertEquals(3*(1+1+1), sg.getNodeCount());

		sg = bogusSG("a [ a ]");
		assertFalse(sg.isLinear());
		assertEquals(3*(1+1+1+1), sg.getNodeCount());

		sg = bogusSG("a [ a ] [ a ]");
		assertFalse(sg.isLinear());
		assertEquals(3*(1+1+1+1+1), sg.getNodeCount());

		sg = bogusSG("a ( a | i )");
		assertFalse(sg.isLinear());
		assertEquals(3*(1+1+2+1), sg.getNodeCount());

		sg = bogusSG("a ( a | [ i ] )");
		assertFalse(sg.isLinear());
		assertEquals(3*(1+1+2+1), sg.getNodeCount());
	}


	@Test
	public void testInterWordSilences() {
		StateGraph sg;

		sg = bogusSG("a", "a");
		assertFalse(sg.isLinear());
		assertEquals(3*(1+1+2+1), sg.getNodeCount());

		sg = bogusSG("a", "[ a ]");
		assertFalse(sg.isLinear());
		assertEquals(3*(1+1+2+1), sg.getNodeCount());

		sg = bogusSG("a", "( a | i )");
		assertFalse(sg.isLinear());
		assertEquals(3*(1+1+3+1), sg.getNodeCount());
	}


	@Test
	public void testNoInterWordSilences() {
		StateGraph sg;

		sg = bogusSG(false, "a", "a");
		assertTrue(sg.isLinear());
		assertEquals(3*(1+1+1+1), sg.getNodeCount());

		sg = bogusSG(false, "a", "[ a ]");
		assertFalse(sg.isLinear());
		assertEquals(3*(1+1+1+1), sg.getNodeCount());

		sg = bogusSG(false, "a", "( a | i )");
		assertFalse(sg.isLinear());
		assertEquals(3*(1+1+2+1), sg.getNodeCount());
	}


	@Test
	public void testInitialEmptyRule() {
		StateGraph sg = bogusSG(null, "a");
		assertTrue(sg.isLinear());
		assertEquals(3*3, sg.getNodeCount());
	}


	@Test
	public void testFinalEmptyRule() {
		StateGraph sg;

		sg = bogusSG("a", null);
		assertTrue(sg.isLinear());
		assertEquals(3*3, sg.getNodeCount());

		sg = bogusSG("a", "b", null);
		assertFalse(sg.isLinear());
		assertEquals(3*(1+1+2+1), sg.getNodeCount());

		sg = bogusSG("a", "b", "a", null);
		assertFalse(sg.isLinear());
		assertEquals(3*(1+1+2+2+1), sg.getNodeCount());
	}


	@Test
	public void testConsecutiveEmptyRules() {
		StateGraph sg = bogusSG(null, null);
		assertTrue(sg.isLinear());
		assertEquals(3*2, sg.getNodeCount());

		sg = bogusSG(null, null, null, null, null);
		assertTrue(sg.isLinear());
		assertEquals(3*2, sg.getNodeCount());

		sg = bogusSG(null, null, "a", null, null, null);
		assertTrue(sg.isLinear());
		assertEquals(3*3, sg.getNodeCount());
	}


	@Test
	public void testSandwichedEmptyRules() {
		final int expected = 3 * (1 + 1 + 2 + 1);

		StateGraph sg = bogusSG("a", null, "b");
		assertFalse(sg.isLinear());
		assertEquals(expected, sg.getNodeCount());

		sg = bogusSG("a", null, null, null, null, "b");
		assertFalse(sg.isLinear());
		assertEquals(expected, sg.getNodeCount());

		sg = bogusSG("a", null, null, null, null, "b", null);
		assertFalse(sg.isLinear());
		assertEquals(expected, sg.getNodeCount());

		sg = bogusSG(null, "a", null, null, null, null, "b", null);
		assertFalse(sg.isLinear());
		assertEquals(expected, sg.getNodeCount());
	}


	@Test
	public void testEmptyRulesWithRealGrammar() {
		final int expected = 3 * (1 + 1 + 1);
		StateGraph sg;

		sg = StateGraph.quick("a");
		assertTrue(sg.isLinear());
		assertEquals(expected, sg.getNodeCount());

		sg = StateGraph.quick("() a");
		assertTrue(sg.isLinear());
		assertEquals(expected, sg.getNodeCount());

		sg = StateGraph.quick("a ()");
		assertTrue(sg.isLinear());
		assertEquals(expected, sg.getNodeCount());

		sg = StateGraph.quick("() () () () a () () () ()");
		assertTrue(sg.isLinear());
		assertEquals(expected, sg.getNodeCount());
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
		String[][] rules = multiTrimSplit(
				"( t e y | t y )",
				"p eu",
				"( ( p a s | p e [ SIL ] a [ SIL ] eh s ) [ z ] | p a [ z ] )",
				"( s a v u a r | s a v w a r )");

		Word tu, peux, pas, savoir;

		tu = new Word("tu");
		peux = new Word("peux");
		pas = new Word("pas");
		savoir = new Word("savoir");

		StateGraph sg = new StateGraph(
				rules,
				Arrays.asList(tu, peux, pas, savoir),
				true);

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

		sg.alignmentFromNodeTimeline(timeline, F).commitToWords();

		assertEquals(F+3, tu.getSegment().getStartFrame());
		assertEquals(F+8, tu.getSegment().getEndFrame());
		assertEquals(2, tu.getPhones().size());
		assertEquals("t", tu.getPhones().get(0).toString());
		assertEquals(F+3, tu.getPhones().get(0).getSegment().getStartFrame());
		assertEquals(F+5, tu.getPhones().get(0).getSegment().getEndFrame());
		assertEquals(F+6, tu.getPhones().get(1).getSegment().getStartFrame());
		assertEquals(F+8, tu.getPhones().get(1).getSegment().getEndFrame());
		assertEquals("y", tu.getPhones().get(1).toString());

		assertEquals(F+9, peux.getSegment().getStartFrame());
		assertEquals(F+14, peux.getSegment().getEndFrame());
		assertEquals(2, peux.getPhones().size());
		assertEquals("p", peux.getPhones().get(0).toString());
		assertEquals("eu", peux.getPhones().get(1).toString());

		assertEquals(F+15, pas.getSegment().getStartFrame());
		assertEquals(F+23, pas.getSegment().getEndFrame()); // (+ sil)
		assertEquals(3, pas.getPhones().size());  // (+ sil)
		assertEquals("p", pas.getPhones().get(0).toString());
		assertEquals("a", pas.getPhones().get(1).toString());
		assertEquals("SIL", pas.getPhones().get(2).toString());

		assertEquals(F+24, savoir.getSegment().getStartFrame());
		assertEquals(F+44, savoir.getSegment().getEndFrame());
		assertEquals(7, savoir.getPhones().size());  // + trailing sil
		assertEquals("s", savoir.getPhones().get(0).toString());
		assertEquals("a", savoir.getPhones().get(1).toString());
		assertEquals("v", savoir.getPhones().get(2).toString());
		assertEquals("w", savoir.getPhones().get(3).toString());
		assertEquals("a", savoir.getPhones().get(4).toString());
		assertEquals("r", savoir.getPhones().get(5).toString());
		assertEquals("SIL", savoir.getPhones().get(6).toString());
	}


	@Test
	public void testWordBreakdown2() {
		Word ah = new Word("ah");
		Word meh = new Word("meh");

		StateGraph sg = new StateGraph(
				multiTrimSplit("a", "m ( e | i )"),
				Arrays.asList(ah, meh),
				true);

		assertEquals(
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

		sg.alignmentFromNodeTimeline(timeline, 0).commitToWords();

		assertTrue(ah.isAligned());
		assertTrue(meh.isAligned());

		assertEquals(9, ah.getSegment().getStartFrame());
		assertEquals(27-1, ah.getSegment().getEndFrame());

		assertEquals(27, meh.getSegment().getStartFrame());
		assertEquals(timeline.length-1, meh.getSegment().getEndFrame());

		{
			List<Word.Phone> p = ah.getPhones();
			assertEquals(2,    p.size());

			/*
			assertEquals("SIL", p.get(0).toString());
			assertEquals(0,     p.get(0).getSegment().getStartFrame());
			assertEquals(3+1+5, p.get(0).getSegment().getLengthFrames());
			*/

			assertEquals("a",   p.get(0).toString());
			assertEquals(9,     p.get(0).getSegment().getStartFrame());
			assertEquals(21-1,  p.get(0).getSegment().getEndFrame());

			assertEquals("SIL", p.get(1).toString());
			assertEquals(21,    p.get(1).getSegment().getStartFrame());
			assertEquals(1+4+1, p.get(1).getSegment().getLengthFrames());
		}

		{
			List<Word.Phone> p = meh.getPhones();
			assertEquals(3, p.size());

			assertEquals("m",   p.get(0).toString());
			assertEquals(27,    p.get(0).getSegment().getStartFrame());
			assertEquals(5+3+4, p.get(0).getSegment().getLengthFrames());

			assertEquals("e",   p.get(1).toString());
			assertEquals(39,    p.get(1).getSegment().getStartFrame());
			assertEquals(3+1+3, p.get(1).getSegment().getLengthFrames());

			assertEquals("SIL", p.get(2).toString());
			assertEquals(46,    p.get(2).getSegment().getStartFrame());
			assertEquals(3+2+1, p.get(2).getSegment().getLengthFrames());
		}
	}


	@Test
	public void testWordIdx() {
		StateGraph sg = bogusSG(
				"a a a a a",
				"a a a a a",
				"a a a a a",
				"a a a a a");

		final int stateCountPerWord = 3 * (5+1);

		assertEquals(-1, sg.getWordIdxAt(0));
		assertEquals(-1, sg.getWordIdxAt(1));
		assertEquals(-1, sg.getWordIdxAt(2));

		int node = 3;
		int fastW = -1;

		for (int w = 0; w < 4; w++) {
			int lastNode = node + stateCountPerWord;

			for (; node < lastNode; node++) {
				// slow version (starts from the beginning)
				int slowW = sg.getWordIdxAt(node);

				// fast version (picks up where we left off)
				fastW = sg.getWordIdxAt(node, fastW);

				assertEquals(w, slowW);
				assertEquals(w, fastW);
				assertTrue(slowW == fastW);
			}
		}

		assertEquals(-1, sg.getWordIdxAt(9999));
		assertEquals(-1, sg.getWordIdxAt(9999, fastW));
	}


	@Test
	public void testWordIdxWithEmptyWords() {
		String[][] rules = multiTrimSplit(
				"a a a a a",
				"",
				"a a a a a"
		);

		// Cancel second word
		rules[1] = null;

		Word bogus = new Word("aaaaa");
		Word dropped = new Word("dropped");

		StateGraph sg = new StateGraph(
				rules,
				Arrays.asList(bogus, dropped, bogus),
				true);

		int i = 0;

		for (; i < 3; i++) {
			assertEquals(-1, sg.getWordIdxAt(i));
			assertEquals(-1, sg.getWordIdxAt(i, -1));
		}

		for (; i < 3 + 6*3; i++) {
			assertEquals(0, sg.getWordIdxAt(i));
			assertEquals(0, sg.getWordIdxAt(i, -1));
		}

		// this is the tricky part (after the dropped word)
		for (; i < sg.getNodeCount(); i++) {
			assertEquals(2, sg.getWordIdxAt(i));
			assertEquals(2, sg.getWordIdxAt(i, 0));
		}
	}


	@Test
	public void testCopyConstructor() {
		StateGraph sg = bogusSG("a", "e", "i", "o", "u");
		StateGraph copy = new StateGraph(sg);

		assertEquals(sg.getNodeCount(), copy.getNodeCount());
		assertEquals(sg.nWords, copy.nWords);
		assertEquals(sg.words, copy.words);
		assertArrayEquals(sg.outCount, copy.outCount);
		assertArrayEquals(sg.nodeStates, copy.nodeStates);
		assertArrayEquals(sg.wordBoundaries, copy.wordBoundaries);

		for (int i = 0; i < sg.getNodeCount(); i++) {
			assertEquals(sg.getPhoneAt(i), copy.getPhoneAt(i));
			assertEquals(sg.getStateAt(i), copy.getStateAt(i));
			assertEquals(sg.getWordIdxAt(i), copy.getWordIdxAt(i));
			assertArrayEquals(sg.outNode[i], copy.outNode[i]);
			assertArrayEquals(sg.outProb[i], copy.outProb[i], 0);
		}
	}


	@Test
	public void testInboundTransitionBridge() {
		StateGraph sg = bogusSG(false, "[ a ]", "( e | i )");

		StateGraph.InboundTransitionBridge itb = sg.new InboundTransitionBridge();

		int[] inCount = {
				1, 2, 2, // sil
				2, 2, 2, // a
				3, 2, 2, // e
				3, 2, 2, // i
				3, 2, 2, // sil
		};

		int[][] inNode = {
				{    0   },  { 1, 0}, { 2, 1}, // sil
				{ 3, 2   },  { 4, 3}, { 5, 4}, // a
				{ 6, 2, 5},  { 7, 6}, { 8, 7}, // e
				{ 9, 2, 5},  {10, 9}, {11,10}, // i
				{12, 8,11},  {13,12}, {14,13}, // sil
		};

		assertArrayEquals(itb.inCount, inCount);
		for (int n = 0; n < sg.getNodeCount(); n++) {
			for (int t = 0; t < inCount[n]; t++) {
				assertEquals(inNode[n][t], itb.inNode[n][t]);
			}
		}
	}

}
