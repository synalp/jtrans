/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package fr.loria.synalp.jtrans.speechreco.s4;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.dictionary.WordClassification;
import edu.cmu.sphinx.linguist.flat.FlatLinguist;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import junit.framework.TestCase;

public class JSGFtest extends TestCase {
	
	class MyPronunc extends Pronunciation {
		public MyPronunc(Unit[] units, String tag, WordClassification wc, float prob) {
			super(units,tag,wc,prob);
		}
		public void setWord(Word w) {
			super.setWord(w);
		}
	}
	
	Dictionary dictionary=new Dictionary() {
		HashMap<String, Word> voc = new HashMap<String, Word>();
		
		@Override
		public void newProperties(PropertySheet ps) throws PropertyException {}
		
		@Override
		public Word getWord(String text) {
			if (text.equals("<sil>") || text.equals("</sil>") || text.equals("sil"))
				text="SIL";
			if (voc.containsKey(text)) return voc.get(text);
			boolean isFiller = text.equals("SIL");
			UnitManager umgr = HMMModels.getUnitManager();
			String text2 = text;
			if (text.startsWith("XZ")) text2=text.substring(2);
			Unit[] units = {umgr.getUnit(text2, isFiller)};
			MyPronunc[] pr = {new MyPronunc(units, null, null, 1)};
			Word w = new Word(text, pr, isFiller);
			pr[0].setWord(w);
			return w;
		}
		
		@Override
		public Word getSilenceWord() {
			return null;
		}
		
		@Override
		public Word getSentenceStartWord() {
			return null;
		}
		
		@Override
		public Word getSentenceEndWord() {
			return null;
		}
		
		@Override
		public WordClassification[] getPossibleWordClassifications() {
			return null;
		}
		
		@Override
		public Word[] getFillerWords() {
			return null;
		}
		
		@Override
		public void deallocate() {}
		
		@Override
		public void allocate() throws IOException {}
	};
	
//	// on cree automatiquement une grammaire dummy avec un seul noeud pour que l'allocation en plante pas
//	initialNode = createGrammarNode("SIL");
//	initialNode.setFinalNode(true);

	HashSet<SearchState> dejavu = new HashSet<SearchState>();
	
	private void recurs(String pref, SearchState s) {
		if (dejavu.contains(s)) {
			System.out.println(pref+"trans_to: "+s);
			return;
		}
		dejavu.add(s);
		System.out.println(pref+"trans_from: "+s);
		SearchStateArc[] arcs = s.getSuccessors();
		for (SearchStateArc arc : arcs) {
			recurs(pref+" ",arc.getState());
		}
	}
	
	/**
	 * probleme: le silence optionnel force ainsi peut se deplacer devant le swa optionnel !
	 */
	public void test() {
		try {
			PrintWriter f = new PrintWriter(new FileWriter("detgrammar.gram"));
			f.println("#JSGF V1.0;");
			f.println("grammar detgrammar;");
//			f.println("public <a> = ( l a | l );");
//			f.println("public <a> = l [ a ] [ b ] swa;");
			f.println("public <a> = l [ a ];");
			f.close();
			
			JSGFGrammar g = new JSGFGrammar(".",HMMModels.getLogMath(),null,false,true,false,false,dictionary);
			AcousticModel mods = HMMModels.getAcousticModels();
			
			g.loadJSGF("detgrammar");
			
			FlatLinguist linguist = new FlatLinguist(mods, HMMModels.getLogMath(), g, HMMModels.getUnitManager(), 1, 1, 1, 1, 1, true, true, true, false, 0, 1, mods);
			linguist.allocate();
			
			System.out.println("debug "+linguist.getSearchGraph().getInitialState());
			
			g.dumpGrammar("xx.gdl");
			
			System.out.println("PARCOURS");
			
			SearchState s = linguist.getSearchGraph().getInitialState();
			recurs("",s);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (JSGFGrammarParseException e) {
			e.printStackTrace();
		} catch (JSGFGrammarException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		JSGFtest m = new JSGFtest();
		m.test();
	}
}
