package fr.loria.synalp.jtrans.io;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Test;

import fr.loria.synalp.jtrans.markup.in.JTRLoader;
import fr.loria.synalp.jtrans.markup.out.JTRSaver;
import fr.loria.synalp.jtrans.project.Anchor;
import fr.loria.synalp.jtrans.project.Phrase;
import fr.loria.synalp.jtrans.project.Token;
import fr.loria.synalp.jtrans.project.TrackProject;
import static org.junit.Assert.*;

public class IoTest {

	private String saveJtr2() {
		final String fname="tmp_test1.jtr";
		try {
			TrackProject p = new TrackProject();
			ArrayList<Phrase> t1 = new ArrayList<Phrase>();
			Anchor deb = new Anchor(10);
			Anchor fin = new Anchor(15);
			ArrayList<Token> toks = new ArrayList<Token>();
			toks.add(new Token("il"));
			toks.add(new Token("mange"));
			t1.add(new Phrase(deb, fin, toks));
			p.addTrack("toto", t1);
			JTRSaver f = new JTRSaver();
			f.save(p, new File(fname));
		} catch (Exception e) {e.printStackTrace();fail();}
		return fname;
	}
	
	@Test
	public void testJTRTrackLoading() {
		try {
			String f=saveJtr2();
			JTRLoader l = new JTRLoader();
			TrackProject p=(TrackProject)l.parse(new File(f));
			Iterator<Phrase> i = p.phraseIterator(0);
			while (i.hasNext()) {
				Phrase pp = i.next();
				System.out.println(pp);
			}
		} catch (Exception e) {e.printStackTrace();fail();}
	}
}
