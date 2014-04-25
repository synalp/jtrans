package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.elements.Anchor;
import fr.loria.synalp.jtrans.elements.Element;
import fr.loria.synalp.jtrans.elements.Word;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

public class LinearBridgeTest {

	private static LinearBridge lb(List<Track> trackList) {
		TrackProject p = new TrackProject();
		for (Track t: trackList) {
			p.addTrack(t.toString(), t);
		}
		return new LinearBridge(p);
	}


	@Test
	public void testEmptyTrackList() {
		LinearBridge bridge = lb(new ArrayList<Track>());
		assertFalse(bridge.hasNext());
	}


	@Test
	public void testEmptyTracks() {
		List<Track> trackList = new ArrayList<Track>();

		trackList.add(new Track());
		trackList.add(new Track());
		trackList.add(new Track());

		LinearBridge bridge = lb(trackList);
		assertFalse(bridge.hasNext());
	}


	@Test
	public void testWellFormed() {
		List<Track> trackList = new ArrayList<Track>();

		Track trackA = new Track();
		trackA.elts.add(new Anchor(5));
		trackA.elts.add(new Word("abc"));
		trackA.elts.add(new Anchor(10));
		trackList.add(trackA);

		Track trackB = new Track();
		trackB.elts.add(new Anchor(10));
		trackB.elts.add(new Word("def"));
		trackB.elts.add(new Anchor(15));
		trackList.add(trackB);

		LinearBridge bridge = lb(trackList);
		AnchorSandwich[] sl;

		assertTrue(bridge.hasNext());
		sl = bridge.next();
		assertEquals(2, sl.length);
		assertEquals(trackA.elts.get(0), sl[0].getInitialAnchor());
		assertEquals(trackA.elts.get(2), sl[0].getFinalAnchor());
		assertEquals(trackA.elts.subList(1, 2), sl[0]);
		assertNull(sl[1]);

		assertTrue(bridge.hasNext());
		sl = bridge.next();
		assertEquals(2, sl.length);
		assertNull(sl[0]);
		assertEquals(trackB.elts.get(0), sl[1].getInitialAnchor());
		assertEquals(trackB.elts.get(2), sl[1].getFinalAnchor());
		assertEquals(trackB.elts.subList(1, 2), sl[1]);

		assertFalse(bridge.hasNext());
	}


	@Test
	public void testSimultaneous() {
		List<Track> trackList = new ArrayList<Track>();

		Track trackA = new Track();
		trackA.elts.add(new Anchor(5));
		trackA.elts.add(new Word("abc"));
		trackA.elts.add(new Anchor(10));
		trackList.add(trackA);

		Track trackB = new Track();
		trackB.elts.add(new Anchor(5));
		trackB.elts.add(new Word("def"));
		trackB.elts.add(new Anchor(15));
		trackList.add(trackB);

		LinearBridge bridge = lb(trackList);
		AnchorSandwich[] sl;

		assertTrue(bridge.hasNext());
		sl = bridge.next();
		assertEquals(2, sl.length);
		assertEquals(trackA.elts.get(0), sl[0].getInitialAnchor());
		assertEquals(trackA.elts.get(2), sl[0].getFinalAnchor());
		assertEquals(trackA.elts.subList(1, 2), sl[0]);
		assertEquals(trackB.elts.get(0), sl[1].getInitialAnchor());
		assertEquals(trackB.elts.get(2), sl[1].getFinalAnchor());
		assertEquals(trackB.elts.subList(1, 2), sl[1]);

		assertFalse(bridge.hasNext());
	}


	private static boolean nullOrEmpty(AnchorSandwich s) {
		return s == null || s.isEmpty();
	}

	@Test
	public void testSimultaneous2() {
		List<Track> trackList = new ArrayList<Track>();
		Track trackA = new Track();
		Track trackB = new Track();
		trackList.add(trackA);
		trackList.add(trackB);

		{
			final List<Element> e = trackA.elts;

			e.add(new Anchor(0));
			e.add(new Word("abc"));
			e.add(new Anchor(1));

			// B: "def"

			e.add(new Anchor(2));
			e.add(new Word("ghi"));
			e.add(new Word("jkl"));
			e.add(new Word("mno"));
			e.add(new Anchor(3));
		}

		{
			final List<Element> e = trackB.elts;

			// A: "abc"

			e.add(new Anchor(1));
			e.add(new Word("def"));
//			e.add(new Anchor(2));

			e.add(new Anchor(2));
			e.add(new Word("pqr"));
			e.add(new Word("stu"));
			e.add(new Anchor(3));

//			e.add(new Anchor(3));
			e.add(new Word("vwx"));
			e.add(new Anchor(4));
		}

		LinearBridge bridge = lb(trackList);
		AnchorSandwich[] sl;

		assertTrue(bridge.hasNext());

		sl = bridge.next();
		assertFalse(nullOrEmpty(sl[0]));
		assertTrue(nullOrEmpty(sl[1]));

		sl = bridge.next();
		assertTrue(nullOrEmpty(sl[0]));
		assertFalse(nullOrEmpty(sl[1]));

		sl = bridge.next();
		assertFalse(nullOrEmpty(sl[0]));
		assertFalse(nullOrEmpty(sl[1]));

		sl = bridge.next();
		assertTrue(nullOrEmpty(sl[0]));
		assertFalse(nullOrEmpty(sl[1]));

		assertFalse(bridge.hasNext());
	}


	@Test
	public void testHasNext() {
		Track trackA = new Track();
		trackA.elts.add(new Anchor(5));
		trackA.elts.add(new Word("abc"));
		trackA.elts.add(new Anchor(10));

		Track trackB = new Track();

		{
			List<Track> trackList1 = new ArrayList<Track>();
			trackList1.add(trackA);
			trackList1.add(trackB);
			assertTrue(lb(trackList1).hasNext());
		}

		{
			List<Track> trackList2 = new ArrayList<Track>();
			trackList2.add(trackB);
			trackList2.add(trackA);
			assertTrue(lb(trackList2).hasNext());
		}

		{
			List<Track> trackList3 = new ArrayList<Track>();
			trackList3.add(trackB);
			trackList3.add(trackB);
			assertFalse(lb(trackList3).hasNext());
		}
	}


	@Test
	public void testEmptySandwiches() {
		List<Track> trackList = new ArrayList<Track>();

		Track trackA = new Track();
		trackA.elts.add(new Anchor(5));
		trackA.elts.add(new Word("abc"));
		trackA.elts.add(new Anchor(10));
		trackA.elts.add(new Anchor(15));
		trackA.elts.add(new Word("def"));
		trackA.elts.add(new Anchor(20));
		trackList.add(trackA);

		Track trackB = new Track();
		trackB.elts.add(new Anchor(10));
		trackB.elts.add(new Word("ghi"));
		trackB.elts.add(new Anchor(12));
		trackList.add(trackB);

		LinearBridge bridge = lb(trackList);
		AnchorSandwich[] sl;

		assertTrue(bridge.hasNext());
		sl = bridge.next();
		assertEquals(2, sl.length);
		assertEquals(trackA.elts.get(0), sl[0].getInitialAnchor());
		assertEquals(trackA.elts.get(2), sl[0].getFinalAnchor());
		assertEquals(trackA.elts.subList(1, 2), sl[0]);
		assertNull(sl[1]);

		assertTrue(bridge.hasNext());
		sl = bridge.next();
		assertEquals(2, sl.length);
		assertEquals(trackA.elts.get(2), sl[0].getInitialAnchor());
		assertEquals(trackA.elts.get(3), sl[0].getFinalAnchor());
		assertTrue(sl[0].isEmpty());
		assertEquals(trackB.elts.get(0), sl[1].getInitialAnchor());
		assertEquals(trackB.elts.get(2), sl[1].getFinalAnchor());
		assertEquals(trackB.elts.subList(1, 2), sl[1]);

		assertTrue(bridge.hasNext());
		sl = bridge.next();
		assertEquals(2, sl.length);
		assertEquals(trackA.elts.get(3), sl[0].getInitialAnchor());
		assertEquals(trackA.elts.get(5), sl[0].getFinalAnchor());
		assertEquals(trackA.elts.subList(4, 5), sl[0]);
		assertNull(sl[1]);

		assertFalse(bridge.hasNext());
	}

/*
	@Test
	public void testInterleavedWordSequence() {
		List<Track> trackList = new ArrayList<Track>();
		Track trackA = new Track("A");
		Track trackB = new Track("B");
		trackList.add(trackA);
		trackList.add(trackB);

		trackA.elts.add(Anchor.orderedTimelessAnchor(0));
		trackA.elts.add(new Word("abc"));
		trackA.elts.add(Anchor.orderedTimelessAnchor(1));

		trackB.elts.add(Anchor.orderedTimelessAnchor(1));
		trackB.elts.add(new Word("def"));
		trackB.elts.add(Anchor.orderedTimelessAnchor(2));

		trackA.elts.add(Anchor.orderedTimelessAnchor(2));
		trackA.elts.add(new Word("ghi"));
		trackA.elts.add(Anchor.orderedTimelessAnchor(3));

		trackB.elts.add(Anchor.orderedTimelessAnchor(4));
		trackB.elts.add(new Word("jkl"));

		LinearBridge lb = lb(trackList);
		AnchorSandwich wordSeq = lb.nextInterleavedElementSequence();
		assertEquals(4, wordSeq.size());
		assertEquals("abc", wordSeq.get(0).toString());
		assertEquals("def", wordSeq.get(1).toString());
		assertEquals("ghi", wordSeq.get(2).toString());
		assertEquals("jkl", wordSeq.get(3).toString());
		assertEquals(Anchor.orderedTimelessAnchor(0),
				wordSeq.getInitialAnchor());
		assertNull(wordSeq.getFinalAnchor());
	}


	@Test
	public void nextSingle() {
		List<Track> trackList = new ArrayList<Track>();
		Track trackA = new Track("A");
		Track trackB = new Track("B");
		trackList.add(trackA);
		trackList.add(trackB);

		{
			final List<Element> e = trackA.elts;

			e.add(Anchor.orderedTimelessAnchor(0));
			e.add(new Word("abc"));
			e.add(Anchor.orderedTimelessAnchor(1));

			// B: "def"

			e.add(Anchor.orderedTimelessAnchor(2));
			e.add(new Word("ghi"));
			e.add(new Word("jkl"));
			e.add(new Word("mno"));
			e.add(Anchor.orderedTimelessAnchor(3));
		}

		{
			final List<Element> e = trackB.elts;

			// A: "abc"

			e.add(Anchor.orderedTimelessAnchor(1));
			e.add(new Word("def"));
//			e.add(Anchor.orderedTimelessAnchor(2));

			e.add(Anchor.orderedTimelessAnchor(2));
			e.add(new Word("pqr"));
			e.add(new Word("stu"));
//			e.add(Anchor.orderedTimelessAnchor(3));

			e.add(Anchor.orderedTimelessAnchor(3));
			e.add(new Word("vwx"));
			e.add(Anchor.orderedTimelessAnchor(4));
		}

		LinearBridge lb = lb(trackList);
		AnchorSandwich s;

		s = lb.nextSingle();
		assertEquals(1, s.size());
		assertEquals("abc", s.get(0).toString());

		s = lb.nextSingle();
		assertEquals(1, s.size());
		assertEquals("def", s.get(0).toString());

		s = lb.nextSingle();
		assertEquals(3, s.size());
		assertEquals("ghi", s.get(0).toString());
		assertEquals("jkl", s.get(1).toString());
		assertEquals("mno", s.get(2).toString());

		s = lb.nextSingle();
		assertEquals(1, s.size());
		assertEquals("vwx", s.get(0).toString());

		assertFalse(lb.hasNext());
	}
*/

}
