package fr.loria.synalp.jtrans.viterbi;

import fr.loria.synalp.jtrans.elements.Word;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static fr.loria.synalp.jtrans.viterbi.StateGraphTest.*;
import static org.junit.Assert.*;

public class StatePathTest {

	@Test
	public void testFlattenConstructor() {
		String[][] rules = multiTrimSplit(
				// #0
				"( t e y | t y )",

				// #1
				"p eu",

				// #2 - DROP!
				"",

				// #3
				"( ( p a s | p e [ SIL ] a [ SIL ] eh s ) [ z ] | p a [ z ] )",

				// #4 - DROP!
				"",

				// #5
				"( s a v u a r | s a v w a r )",

				// #6 - DROP!
				""
		);

		// drop words
		rules[2] = null;
		rules[4] = null;
		rules[6] = null;

		Word tu, peux, pas, savoir, dropped;

		tu = new Word("tu");
		peux = new Word("peux");
		pas = new Word("pas");
		savoir = new Word("savoir");
		dropped = new Word("word_intentionally_dropped");

		StateGraph fullSG = new StateGraph(
				new StateSet(),
				rules,
				Arrays.asList(tu, peux, dropped, pas, dropped, savoir, dropped),
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

		Set<Integer> nodeSet = new HashSet<>();
		for (int i = 0; i < timeline.length; i++) {
			nodeSet.add(timeline[i]);
		}

		// Make sure the dropped words don't add any nodes
		assertEquals(3 * (1 + (5 + 1) + (2 + 1) + (14 + 1) + (12) + 1),
				fullSG.getNodeCount());

		//----------------------------------------------------------------------
		// Flatten

		StatePath flatSG = new StatePath(fullSG, timeline);

		assertEquals(4, flatSG.nWords);
		assertEquals(tu, flatSG.getWords().get(0));
		assertEquals(peux, flatSG.getWords().get(1));
		assertEquals(pas, flatSG.getWords().get(2));
		assertEquals(savoir, flatSG.getWords().get(3));

		assertFalse(fullSG.isLinear());
		assertTrue(flatSG.isLinear());

		assertEquals(nodeSet.size(), flatSG.getNodeCount());

		//----------------------------------------------------------------------
		// Expected word boundaries

		int[][] expectedWordBoundaries = new int[][] {
				{ -1, 0, 2 },  // { word idx, first node, last node (incl.) }
				{ 0, 3, 8 },
				{ 1, 9, 14 },
				{ 2, 15, 23 },
				{ 3, 24, timeline.length-1 }
		};

		for (int[] ewb: expectedWordBoundaries) {
			int w = ewb[0];
			int firstNode = ewb[1];
			int lastNode = ewb[2];

			for (int n = firstNode; n <= lastNode; n++) {
				assertEquals(w, flatSG.getWordIdxAt(n));
			}
		}
	}


	@Test(expected = IllegalArgumentException.class)
	public void testNonLinearAsPath() {
		StatePath.asPath(bogusSG("a [ m ] i"));
	}


	@Test
	public void testLinearAsPath() {
		StateGraph sg = bogusSG(false, "a", "a", "a");
		StatePath sp = StatePath.asPath(sg);
		translationTorture(sg, sp);
	}


	private static void translationTorture(StateGraph orig, StatePath path) {
		for (int o = 0; o < orig.getNodeCount(); o++) {
			int t = path.translateNode(orig, o);

			assertEquals(orig.getPhoneAt(o), path.getPhoneAt(t));
			assertEquals(orig.getStateAt(o), path.getStateAt(t));

			if (orig.getWordIdxAt(o) >= 0) {
				Word ow = orig.words.get(orig.getWordIdxAt(o));
				Word pw = path.words.get(path.getWordIdxAt(t));
				assertEquals(ow, pw);
			}
		}
	}


	@Test
	public void testConcatenation() {
		// TODO: test dropped words

		StateGraph sg1 = bogusSG(false, "a", "e");
		StateGraph sg2 = bogusSG(false, "i", "o");

		assertTrue(sg1.isLinear());
		assertTrue(sg2.isLinear());

		StatePath sp1 = StatePath.asPath(sg1);
		StatePath sp2 = StatePath.asPath(sg2);
		StatePath cat = new StatePath(sp1, sp2);

		assertEquals(4, cat.nWords);
		assertEquals(4, cat.words.size());

		assertEquals(3*2*(1+2+1), cat.nNodes);

		assertEquals("a", cat.getPhoneAt(3));
		assertEquals("e", cat.getPhoneAt(6));
		// 9-11: sp1 final sil
		// 12-15: sp2 initial sil
		assertEquals("i", cat.getPhoneAt(16));
		assertEquals("o", cat.getPhoneAt(19));

		translationTorture(sp1, cat);
		translationTorture(sp2, cat);
		translationTorture(sg1, cat);
		translationTorture(sg2, cat);
	}

}
