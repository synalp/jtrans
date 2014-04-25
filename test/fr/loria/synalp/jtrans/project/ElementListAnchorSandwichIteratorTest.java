package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.elements.Anchor;
import fr.loria.synalp.jtrans.elements.Element;
import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.project.Track.ElementListAnchorSandwichIterator;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

public class ElementListAnchorSandwichIteratorTest {

	@Test
	public void testEmptyElementList() {
		ElementListAnchorSandwichIterator i = new ElementListAnchorSandwichIterator(
				new ArrayList<Element>());
		assertFalse(i.hasNext());
	}


	@Test
	public void testNoAnchorsAtExtremities() {
		List<Element> L = new ArrayList<Element>();
		L.add(new Word("a"));
		L.add(new Word("b"));
		L.add(new Word("c"));
		L.add(new Anchor(10)); // #3
		L.add(new Word("d"));
		L.add(new Word("e"));
		L.add(new Anchor(15)); // #6
		L.add(new Word("f"));
		L.add(new Word("g"));

		ElementListAnchorSandwichIterator i = new ElementListAnchorSandwichIterator(L);

		assertTrue(i.hasNext());
		assertEquals(L.subList(0, 3), i.next());
		assertEquals(L.subList(4, 6), i.next());
		assertEquals(L.subList(7, 9), i.next());
		assertFalse(i.hasNext());
	}


	@Test
	public void testLeadingAnchor() {
		List<Element> L = new ArrayList<Element>();
		L.add(new Anchor(10));
		L.add(new Word("d"));
		L.add(new Word("e"));
		L.add(new Word("f"));
		L.add(new Word("g"));

		ElementListAnchorSandwichIterator i = new ElementListAnchorSandwichIterator(L);

		assertTrue(i.hasNext());
		assertEquals(L.subList(1, L.size()), i.next());
		assertFalse(i.hasNext());
	}


	@Test
	public void testTrailingAnchor() {
		List<Element> L = new ArrayList<Element>();
		L.add(new Word("d"));
		L.add(new Word("e"));
		L.add(new Word("f"));
		L.add(new Word("g"));
		L.add(new Anchor(15));

		ElementListAnchorSandwichIterator i = new ElementListAnchorSandwichIterator(L);

		assertTrue(i.hasNext());
		assertEquals(L.subList(0, 4), i.next());
		assertTrue(i.next().isEmpty());
		assertFalse(i.hasNext());
	}


	@Test
	public void testEmpty() {
		List<Element> L = new ArrayList<Element>();
		ElementListAnchorSandwichIterator i = new ElementListAnchorSandwichIterator(L);
		assertFalse(i.hasNext());
	}


	@Test
	public void testAnchorsOnly() {
		List<Element> L = new ArrayList<Element>();
		L.add(new Anchor(5));
		L.add(new Anchor(10));
		L.add(new Anchor(15));
		L.add(new Anchor(20));

		ElementListAnchorSandwichIterator iter = new ElementListAnchorSandwichIterator(L);

		for (int i = 0; i < L.size()-1; i++) {
			AnchorSandwich s = iter.next();
			assertTrue(s.isEmpty());
			assertEquals(L.get(i), s.getInitialAnchor());
			assertEquals(L.get(i+1), s.getFinalAnchor());
		}

		assertFalse(iter.hasNext());
	}


	@Test
	public void testEmptySandwich() {
		List<Element> L = new ArrayList<Element>();

		L.add(new Anchor(5));
		L.add(new Word("abc"));
		L.add(new Anchor(10));
		L.add(new Anchor(15));
		L.add(new Word("def"));
		L.add(new Anchor(20));

		ElementListAnchorSandwichIterator iter = new ElementListAnchorSandwichIterator(L);
		AnchorSandwich s;

		assertTrue(iter.hasNext());
		s = iter.next();
		assertEquals(L.get(0), s.getInitialAnchor());
		assertEquals(L.get(2), s.getFinalAnchor());
		assertEquals(L.subList(1, 2), s);

		assertTrue(iter.hasNext());
		s = iter.next();
		assertEquals(L.get(2), s.getInitialAnchor());
		assertEquals(L.get(3), s.getFinalAnchor());
		assertTrue(s.isEmpty());

		assertTrue(iter.hasNext());
		s = iter.next();
		assertEquals(L.get(3), s.getInitialAnchor());
		assertEquals(L.get(5), s.getFinalAnchor());
		assertEquals(L.subList(4, 5), s);

		assertFalse(iter.hasNext());
	}

}
