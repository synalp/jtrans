package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.elements.Element;
import fr.loria.synalp.jtrans.elements.Word;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import static java.util.Arrays.asList;

public class LinearBridgeTest {

	private Phrase phr(float start, float end, String... words) {
		List<Element> wordList = new ArrayList<>();
		for (String w: words) {
			wordList.add(new Word(w));
		}
		return new Phrase(new Anchor(start), new Anchor(end), wordList);
	}


	@Test
	public void testEmptyTrackList() {
		LinearBridge bridge = new LinearBridge(new TrackProject());
		assertFalse(bridge.hasNext());
	}


	@Test
	public void testEmptyTracks() {
		TrackProject p = new TrackProject();

		p.addTrack("A", new ArrayList<Phrase>());
		p.addTrack("B", new ArrayList<Phrase>());
		p.addTrack("C", new ArrayList<Phrase>());

		LinearBridge bridge = new LinearBridge(p);
		assertFalse(bridge.hasNext());
	}


	@Test
	public void testWellFormed() {
		TrackProject p = new TrackProject();
		p.addTrack("A", asList(phr(5, 10, "abc")));
		p.addTrack("B", asList(phr(10, 15, "def")));
		LinearBridge bridge = new LinearBridge(p);
		Phrase[] sl;

		assertTrue(bridge.hasNext());
		sl = bridge.next();
		assertEquals(2, sl.length);
		assertEquals(p.tracks.get(0).get(0), sl[0]);
		assertNull(sl[1]);

		assertTrue(bridge.hasNext());
		sl = bridge.next();
		assertEquals(2, sl.length);
		assertNull(sl[0]);
		assertEquals(p.tracks.get(1).get(0), sl[1]);

		assertFalse(bridge.hasNext());
	}


	@Test
	public void testSimultaneous() {
		TrackProject p = new TrackProject();
		p.addTrack("A", asList(phr(5, 10, "abc")));
		p.addTrack("B", asList(phr(5, 15, "def")));
		LinearBridge bridge = new LinearBridge(p);
		Phrase[] sl;

		assertTrue(bridge.hasNext());
		sl = bridge.next();
		assertEquals(2, sl.length);
		assertEquals(p.tracks.get(0).get(0), sl[0]);
		assertEquals(p.tracks.get(1).get(0), sl[1]);
		assertFalse(bridge.hasNext());
	}


	private static boolean nullOrEmpty(Phrase s) {
		return s == null || s.isEmpty();
	}

	@Test
	public void testSimultaneous2() {
		TrackProject p = new TrackProject();

		p.addTrack("A", asList(
				phr(0, 1, "abc"),
				// B says "def" here
				phr(2, 3, "ghi", "jkl", "mno")
		));

		p.addTrack("B", asList(
				// A says "abc" here
				phr(1, 2, "def"),
				phr(2, 3, "pqr", "stu"),
				phr(3, 4, "vwx")
		));

		LinearBridge bridge = new LinearBridge(p);
		Phrase[] sl;

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
		List<Phrase> trackA = asList(phr(5, 10, "abc"));
		List<Phrase> trackB = new ArrayList<>();

		{
			TrackProject p = new TrackProject();
			p.addTrack("A", trackA);
			p.addTrack("B", trackB);
			assertTrue(new LinearBridge(p).hasNext());
		}

		{
			TrackProject p = new TrackProject();
			p.addTrack("B", trackB);
			p.addTrack("A", trackA);
			assertTrue(new LinearBridge(p).hasNext());
		}

		{
			TrackProject p = new TrackProject();
			p.addTrack("B1", trackB);
			p.addTrack("B2", trackB);
			assertFalse(new LinearBridge(p).hasNext());
		}
	}


	@Test
	public void testEmptyPhrases() {
		TrackProject p = new TrackProject();
		p.addTrack("A", asList(phr(5, 10, "abc"), phr(15, 20, "def")));
		p.addTrack("B", asList(phr(10, 12, "ghi")));
		LinearBridge bridge = new LinearBridge(p);
		Phrase[] sl;

		assertTrue(bridge.hasNext());
		sl = bridge.next();
		assertEquals(2, sl.length);
		assertEquals(p.tracks.get(0).get(0), sl[0]);
		assertNull(sl[1]);

		assertTrue(bridge.hasNext());
		sl = bridge.next();
		assertEquals(2, sl.length);
		assertNull(sl[0]);
		assertEquals(p.tracks.get(1).get(0), sl[1]);

		assertTrue(bridge.hasNext());
		sl = bridge.next();
		assertEquals(2, sl.length);
		assertEquals(p.tracks.get(0).get(1), sl[0]);
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
