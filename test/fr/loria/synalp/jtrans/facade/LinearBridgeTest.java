package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.elements.Anchor;
import fr.loria.synalp.jtrans.elements.Word;
import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class LinearBridgeTest {

	@Test
	public void testEmptyTrackList() {
		LinearBridge bridge = new LinearBridge(new ArrayList<Track>());
		Assert.assertFalse(bridge.hasNext());
	}


	@Test
	public void testEmptyTracks() {
		List<Track> trackList = new ArrayList<Track>();

		trackList.add(new Track("A"));
		trackList.add(new Track("B"));
		trackList.add(new Track("C"));

		LinearBridge bridge = new LinearBridge(trackList);
		Assert.assertFalse(bridge.hasNext());
	}


	@Test
	public void testWellFormed() {
		List<Track> trackList = new ArrayList<Track>();

		Track trackA = new Track("A");
		trackA.elts.add(Anchor.timedAnchor(5));
		trackA.elts.add(new Word("abc"));
		trackA.elts.add(Anchor.timedAnchor(10));
		trackList.add(trackA);

		Track trackB = new Track("B");
		trackB.elts.add(Anchor.timedAnchor(10));
		trackB.elts.add(new Word("def"));
		trackB.elts.add(Anchor.timedAnchor(15));
		trackList.add(trackB);

		LinearBridge bridge = new LinearBridge(trackList);
		AnchorSandwich[] sl;

		Assert.assertTrue(bridge.hasNext());
		sl = bridge.next();
		Assert.assertEquals(2, sl.length);
		Assert.assertEquals(trackA.elts.get(0), sl[0].getInitialAnchor());
		Assert.assertEquals(trackA.elts.get(2), sl[0].getFinalAnchor());
		Assert.assertEquals(trackA.elts.subList(1, 2), sl[0]);
		Assert.assertNull(sl[1]);

		Assert.assertTrue(bridge.hasNext());
		sl = bridge.next();
		Assert.assertEquals(2, sl.length);
		Assert.assertNull(sl[0]);
		Assert.assertEquals(trackB.elts.get(0), sl[1].getInitialAnchor());
		Assert.assertEquals(trackB.elts.get(2), sl[1].getFinalAnchor());
		Assert.assertEquals(trackB.elts.subList(1, 2), sl[1]);

		Assert.assertFalse(bridge.hasNext());
	}


	@Test
	public void testSimultaneous() {
		List<Track> trackList = new ArrayList<Track>();

		Track trackA = new Track("A");
		trackA.elts.add(Anchor.timedAnchor(5));
		trackA.elts.add(new Word("abc"));
		trackA.elts.add(Anchor.timedAnchor(10));
		trackList.add(trackA);

		Track trackB = new Track("B");
		trackB.elts.add(Anchor.timedAnchor(5));
		trackB.elts.add(new Word("def"));
		trackB.elts.add(Anchor.timedAnchor(15));
		trackList.add(trackB);

		LinearBridge bridge = new LinearBridge(trackList);
		AnchorSandwich[] sl;

		Assert.assertTrue(bridge.hasNext());
		sl = bridge.next();
		Assert.assertEquals(2, sl.length);
		Assert.assertEquals(trackA.elts.get(0), sl[0].getInitialAnchor());
		Assert.assertEquals(trackA.elts.get(2), sl[0].getFinalAnchor());
		Assert.assertEquals(trackA.elts.subList(1, 2), sl[0]);
		Assert.assertEquals(trackB.elts.get(0), sl[1].getInitialAnchor());
		Assert.assertEquals(trackB.elts.get(2), sl[1].getFinalAnchor());
		Assert.assertEquals(trackB.elts.subList(1, 2), sl[1]);

		Assert.assertFalse(bridge.hasNext());
	}


	@Test
	public void testHasNext() {
		Track trackA = new Track("A");
		trackA.elts.add(Anchor.timedAnchor(5));
		trackA.elts.add(new Word("abc"));
		trackA.elts.add(Anchor.timedAnchor(10));

		Track trackB = new Track("B");

		{
			List<Track> trackList1 = new ArrayList<Track>();
			trackList1.add(trackA);
			trackList1.add(trackB);
			Assert.assertTrue(new LinearBridge(trackList1).hasNext());
		}

		{
			List<Track> trackList2 = new ArrayList<Track>();
			trackList2.add(trackB);
			trackList2.add(trackA);
			Assert.assertTrue(new LinearBridge(trackList2).hasNext());
		}

		{
			List<Track> trackList3 = new ArrayList<Track>();
			trackList3.add(trackB);
			trackList3.add(trackB);
			Assert.assertFalse(new LinearBridge(trackList3).hasNext());
		}
	}


	@Test
	public void testEmptySandwiches() {
		List<Track> trackList = new ArrayList<Track>();

		Track trackA = new Track("A");
		trackA.elts.add(Anchor.timedAnchor(5));
		trackA.elts.add(new Word("abc"));
		trackA.elts.add(Anchor.timedAnchor(10));
		trackA.elts.add(Anchor.timedAnchor(15));
		trackA.elts.add(new Word("def"));
		trackA.elts.add(Anchor.timedAnchor(20));
		trackList.add(trackA);

		Track trackB = new Track("B");
		trackB.elts.add(Anchor.timedAnchor(10));
		trackB.elts.add(new Word("ghi"));
		trackB.elts.add(Anchor.timedAnchor(12));
		trackList.add(trackB);

		LinearBridge bridge = new LinearBridge(trackList);
		AnchorSandwich[] sl;

		Assert.assertTrue(bridge.hasNext());
		sl = bridge.next();
		Assert.assertEquals(2, sl.length);
		Assert.assertEquals(trackA.elts.get(0), sl[0].getInitialAnchor());
		Assert.assertEquals(trackA.elts.get(2), sl[0].getFinalAnchor());
		Assert.assertEquals(trackA.elts.subList(1, 2), sl[0]);
		Assert.assertNull(sl[1]);

		Assert.assertTrue(bridge.hasNext());
		sl = bridge.next();
		Assert.assertEquals(2, sl.length);
		Assert.assertEquals(trackA.elts.get(2), sl[0].getInitialAnchor());
		Assert.assertEquals(trackA.elts.get(3), sl[0].getFinalAnchor());
		Assert.assertTrue(sl[0].isEmpty());
		Assert.assertEquals(trackB.elts.get(0), sl[1].getInitialAnchor());
		Assert.assertEquals(trackB.elts.get(2), sl[1].getFinalAnchor());
		Assert.assertEquals(trackB.elts.subList(1, 2), sl[1]);

		Assert.assertTrue(bridge.hasNext());
		sl = bridge.next();
		Assert.assertEquals(2, sl.length);
		Assert.assertEquals(trackA.elts.get(3), sl[0].getInitialAnchor());
		Assert.assertEquals(trackA.elts.get(5), sl[0].getFinalAnchor());
		Assert.assertEquals(trackA.elts.subList(4, 5), sl[0]);
		Assert.assertNull(sl[1]);

		Assert.assertFalse(bridge.hasNext());
	}

}
