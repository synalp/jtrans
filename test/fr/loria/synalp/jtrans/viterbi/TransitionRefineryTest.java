package fr.loria.synalp.jtrans.viterbi;

import org.junit.Test;
import static org.junit.Assert.*;

public class TransitionRefineryTest {

	@Test
	public void testNextTransition() {
		int[] tl = new int[] {
				0, 0, 0, 1, 1,
				2, 3, 4, 4, 4,
				4, 4, 4, 5, 5,
				6, 6, 6, 7, 8,
				8, 8, 8, 8, 8,
		};

		assertEquals(2, TransitionRefinery.nextTransition(0, tl));
		assertEquals(2, TransitionRefinery.nextTransition(1, tl));
		assertEquals(2, TransitionRefinery.nextTransition(2, tl));
		assertEquals(6, TransitionRefinery.nextTransition(3, tl));
		assertEquals(6, TransitionRefinery.nextTransition(4, tl));

		assertEquals(6, TransitionRefinery.nextTransition(5, tl));
		assertEquals(6, TransitionRefinery.nextTransition(6, tl));
		assertEquals(12, TransitionRefinery.nextTransition(7, tl));
		assertEquals(12, TransitionRefinery.nextTransition(8, tl));
		assertEquals(12, TransitionRefinery.nextTransition(9, tl));

		assertEquals(12, TransitionRefinery.nextTransition(10, tl));
		assertEquals(12, TransitionRefinery.nextTransition(11, tl));
		assertEquals(12, TransitionRefinery.nextTransition(12, tl));
		assertEquals(14, TransitionRefinery.nextTransition(13, tl));
		assertEquals(14, TransitionRefinery.nextTransition(14, tl));

		assertEquals(18, TransitionRefinery.nextTransition(15, tl));
		assertEquals(18, TransitionRefinery.nextTransition(16, tl));
		assertEquals(18, TransitionRefinery.nextTransition(17, tl));
		assertEquals(18, TransitionRefinery.nextTransition(18, tl));
		assertEquals(-1, TransitionRefinery.nextTransition(19, tl));

		assertEquals(-1, TransitionRefinery.nextTransition(20, tl));
		assertEquals(-1, TransitionRefinery.nextTransition(21, tl));
		assertEquals(-1, TransitionRefinery.nextTransition(22, tl));
		assertEquals(-1, TransitionRefinery.nextTransition(23, tl));
		assertEquals(-1, TransitionRefinery.nextTransition(24, tl));
	}

}
