package fr.loria.synalp.jtrans.facade;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import fr.loria.synalp.jtrans.facade.BinarySegmentation.Segment;

import static org.junit.Assert.*;
import static java.util.Arrays.asList;

public class BinarySegmentationTest {

	/**
	 * Unions segments in all possible combinations and compares the resulting
	 * segmentations against the reference segmentation.
	 * @param expected ordered list of segments
	 * @param inserted set of segments that will be "unioned" together
	 */
	private static void torture(
			List<Segment> expected,
			List<Segment> inserted)
	{
		recursiveTorture(expected, new ArrayList<Segment>(), inserted);
	}


	private static void recursiveTorture(
			List<Segment> expected,
			List<Segment> fixed,
			List<Segment> remaining)
	{
		if (remaining.isEmpty()) {
			BinarySegmentation binseg = new BinarySegmentation();
			for (Segment seg: fixed) {
				binseg.union(seg.off, seg.len);
				System.out.print(seg.off + "/" + seg.len + " ");
			}
			System.out.println();

			assertEquals(expected.size(), binseg.size());
			for (int i = 0; i < expected.size(); i++) {
				assertEquals(expected.get(i), binseg.get(i));
			}
		} else {
			for (Segment r: remaining) {
				List<Segment> fix2 = new ArrayList<>(fixed);
				List<Segment> rem2 = new ArrayList<>(remaining);
				fix2.add(r);
				rem2.remove(r);
				recursiveTorture(expected, fix2, rem2);
			}
		}
	}


	/**
	 * Shorthand for new Segment(off, len)
	 */
	private static Segment S(float off, float len) {
		return new Segment(off, len);
	}


	/**
	 * Creates a segmentation from an ASCII one-dimensional diagram of segments.
	 * Each non-space character in the diagram represents a segment of length 1.
	 * Example format: {@code "  XXX X   XX "}
	 */
	private static BinarySegmentation fromString(String diagram) {
		BinarySegmentation binseg = new BinarySegmentation();
		for (int i = 0; i < diagram.length(); i++) {
			if (' ' != diagram.charAt(i)) {
				binseg.union(i, 1);
			}
		}
		return binseg;
	}


	@Test
	public void testSeparateUnion() {
		torture(
				asList(S(1, 1), S(3, 1)),
				asList(S(1, 1), S(3, 1))
		);
	}


	@Test
	public void testSeparateUnionx3() {
		torture(
				asList(S(1, .5f), S(3, 1), S(5, 1)),
				asList(S(1, .5f), S(3, 1), S(5, 1))
		);
	}


	@Test
	public void testSame() {
		torture(
				asList(S(3, 1)),
				asList(S(3, 1), S(3, 1))
		);
	}


	@Test
	public void testUnionTouching() {
		torture(
				asList(S(1, 3)),
				asList(S(3, 1), S(1,2))
		);
	}


	@Test
	public void testUnionOverlapLeft() {
		torture(
				asList(S(2.5f, 1.5f)),
				asList(S(3,1), S(2.5f,1))
		);
	}


	@Test
	public void testUnionOverlapRight() {
		torture(
				asList(S(3, 1.5f)),
				asList(S(3,1), S(3.5f,1))
		);
	}


	@Test
	public void testUnionFullyContainedOverlap() {
		torture(
				asList(S(3, 1)),
				asList(S(3, 1), S(3.5f, 0.1f))
		);
	}


	@Test
	public void testUnionFullyEncompassingOverlap() {
		torture(
				asList(S(2, 3)),
				asList(S(3, 1), S(2, 3))
		);
	}


	@Test
	public void testMiddleJoin() {
		torture(
				asList(S(1, 3)),
				asList(S(1, 1), S(2, 1), S(3, 1))
		);
	}


	@Test
	public void testEncompassSeveral() {
		torture(
				asList(S(0, 6)),
				asList(S(1, 1), S(3, 1), S(0, 6))
		);
	}


	@Test
	public void testNegate() {
		BinarySegmentation
				bs   = fromString("   XXXX     XX      "),
				exp  = fromString("XXX    XXXXX  XXXXXX"),
				copy = new BinarySegmentation(bs);

		bs.negate(20);
		assertEquals(exp, bs);

		// Fall back to original segmentation
		bs.negate(20);
		assertEquals(copy, bs);
	}


	@Test
	public void testNegateStartAt0() {
		BinarySegmentation
				bs   = fromString("XXXX      "),
				exp  = fromString("    XXXXXX"),
				copy = new BinarySegmentation(bs);

		bs.negate(10);
		assertEquals(exp, bs);

		// Fall back to original segmentation
		bs.negate(10);
		assertEquals(copy, bs);
	}


	@Test
	public void testNegateEmpty() {
		BinarySegmentation bs = new BinarySegmentation();

		bs.negate(10);
		assertEquals(fromString("XXXXXXXXXX"), bs);

		bs.negate(10);
		assertEquals(new BinarySegmentation(), bs);
	}


	@Test
	public void testIntersect() {
		BinarySegmentation
				bs1 = fromString("   XXXX      XXX   "),
				bs2 = fromString("X XXX    X   X   X "),
				exp = fromString("   XX        X     ");

		bs1.intersect(bs2);
		assertEquals(exp, bs1);
	}


	@Test
	public void testIntersectNothing() {
		BinarySegmentation
				empty = new BinarySegmentation(),
				bs = fromString(" X X X X X X ");

		bs.intersect(empty);
		assertEquals(empty, bs);
	}

}
