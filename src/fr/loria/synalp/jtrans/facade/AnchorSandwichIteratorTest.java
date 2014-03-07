package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.elements.Anchor;
import fr.loria.synalp.jtrans.elements.Element;
import fr.loria.synalp.jtrans.elements.Word;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AnchorSandwichIteratorTest {

	@Test
	public void testNoAnchorsAtExtremities() {
		List<Element> L = new ArrayList<Element>();
		L.add(new Word("a"));
		L.add(new Word("b"));
		L.add(new Word("c"));
		L.add(Anchor.timedAnchor(10)); // #3
		L.add(new Word("d"));
		L.add(new Word("e"));
		L.add(Anchor.timedAnchor(15)); // #6
		L.add(new Word("f"));
		L.add(new Word("g"));

		AnchorSandwichIterator i = new AnchorSandwichIterator(L);

		Assert.assertTrue(i.hasNext());
		Assert.assertEquals(L.subList(0, 3), i.next());
		Assert.assertEquals(L.subList(3, 6), i.next());
		Assert.assertEquals(L.subList(6, 9), i.next());
		Assert.assertFalse(i.hasNext());
	}


	@Test
	public void testLeadingAnchor() {
		List<Element> L = new ArrayList<Element>();
		L.add(Anchor.timedAnchor(10));
		L.add(new Word("d"));
		L.add(new Word("e"));
		L.add(new Word("f"));
		L.add(new Word("g"));

		AnchorSandwichIterator i = new AnchorSandwichIterator(L);

		Assert.assertTrue(i.hasNext());
		Assert.assertEquals(L, i.next());
		Assert.assertFalse(i.hasNext());
	}


	@Test
	public void testTrailingAnchor() {
		List<Element> L = new ArrayList<Element>();
		L.add(new Word("d"));
		L.add(new Word("e"));
		L.add(new Word("f"));
		L.add(new Word("g"));
		L.add(Anchor.timedAnchor(15));

		AnchorSandwichIterator i = new AnchorSandwichIterator(L);

		Assert.assertTrue(i.hasNext());
		Assert.assertEquals(L.subList(0, 4), i.next());
		Assert.assertEquals(L.subList(4, 5), i.next());
		Assert.assertFalse(i.hasNext());
	}


	@Test
	public void testEmpty() {
		List<Element> L = new ArrayList<Element>();
		AnchorSandwichIterator i = new AnchorSandwichIterator(L);
		Assert.assertFalse(i.hasNext());
	}


	@Test
	public void testAnchorsOnly() {
		List<Element> L = new ArrayList<Element>();
		L.add(Anchor.timedAnchor(5));
		L.add(Anchor.timedAnchor(10));
		L.add(Anchor.timedAnchor(15));
		L.add(Anchor.timedAnchor(20));
		AnchorSandwichIterator i = new AnchorSandwichIterator(L);
		Assert.assertEquals(L.subList(0, 1), i.next());
		Assert.assertEquals(L.subList(1, 2), i.next());
		Assert.assertEquals(L.subList(2, 3), i.next());
		Assert.assertEquals(L.subList(3, 4), i.next());
		Assert.assertFalse(i.hasNext());
	}

}
