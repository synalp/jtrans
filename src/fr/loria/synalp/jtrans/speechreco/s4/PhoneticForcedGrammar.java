/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package fr.loria.synalp.jtrans.speechreco.s4;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import fr.loria.synalp.jtrans.speechreco.grammaire.Grammatiseur;
import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.dictionary.WordClassification;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import fr.loria.synalp.jtrans.utils.ProgressDisplay;

public class PhoneticForcedGrammar extends JSGFGrammar {
	List<String> words0 = null;
	List<String> wordRule = new ArrayList<String>();
	
	protected Grammatiseur grammatiseur = null;
	
	GrammarNode[] finsDesMots;

	public List<String>[] getWordsAndRules() {
		List[] res = {words0,wordRule};
		return res;
	}
	
	public PhoneticForcedGrammar() throws MalformedURLException, ClassNotFoundException {
		super(".",HMMModels.getLogMath(),null,false,true,false,false,null);
		
		class MyPronunc extends Pronunciation {
			public MyPronunc(Unit[] units, String tag, WordClassification wc, float prob) {
				super(units,tag,wc,prob);
			}
			public void setWord(Word w) {
				super.setWord(w);
			}
		}
		
		dictionary=new Dictionary() {
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
				if (text.startsWith("XZ")) {
					int u = text.indexOf('Y');
					assert u>0;
					text2=text.substring(u+1);
				}
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
		
//		// on cree automatiquement une grammaire dummy avec un seul noeud pour que l'allocation en plante pas
//		initialNode = createGrammarNode("SIL");
//		initialNode.setFinalNode(true);
	}

	/**
	 * attention: ne pas utiliser normalement !
	 * permet de definir directement des grammaires en phonemes
	 */
	public void setGram(GrammarNode n) {
		initialNode=n;
	}
	
	public void setWords(List<String> words, ProgressDisplay progress) {
		words0 = words;
		wordRule.clear();
		if (grammatiseur==null) {
			progress.setIndeterminateProgress("Initializing grammar...");
			grammatiseur = Grammatiseur.getGrammatiseur();
		}
		
//		// on commence toujours par un silence !
//		n = createGrammarNode("SIL");
//		n.setFinalNode(false);
//		initialNode = n;
		
		StringBuilder gramstring = new StringBuilder();

		for (int wi=0;wi<words.size();wi++) {
			String w = words.get(wi);
			String rule = grammatiseur.getGrammar(w);
			// on recupere toujours un silence optionnel au debut et a la fin, que je supprime:
//			rule = rule.substring(4,rule.length()-8).trim();
			System.out.println("rule for word "+w+" "+rule);
			if (rule==null || rule.length()==0) {
				System.out.println("ERROR PHONETISEUR mot "+w);
				continue;
			}
			if (rule.charAt(0)=='#') {
				// le phonetiseur n'a pas marche: on suppose que c'est un bruit
				rule = "xx "+rule;
			}
			// conversion des phonemes
			StringBuilder rule2 = new StringBuilder();
			StringTokenizer st = new StringTokenizer(rule);
			while (st.hasMoreTokens()) {
				String s = st.nextToken();
				if (s.startsWith("#")) {
				} else if (s.equals("(") || s.equals("[") || s.equals(")") || s.equals("]") || s.equals("|")) {
					rule2.append(s+" ");
				} else {
					s=convertPhone(s);
					rule2.append(s+" ");
				}
			}
			// regle pour _un_ mot:
			rule = rule2.toString().trim();
			// j'aoute un prefixe pour reperer les 1ers phones de chaque mot
			rule = annoteFirstPhones(rule,wi);
			rule = rule.replaceAll("\\[ *\\]", "").trim();
			if (rule.length()>0)
				gramstring.append("[ sil ] "+rule+" ");
			wordRule.add(""+rule);
		}
		gramstring.append("[ sil ]");

		System.out.println("gramstring "+gramstring);
		
		// create JSGF file and load it
		{
			try {
				
				// bugfix:
				PrintWriter f = new PrintWriter(new FileWriter("detgrammar.gram"));
				f.println("#JSGF V1.0;");
				f.println("grammar detgrammar;");
				f.println("public <a> = "+gramstring.toString()+";");
				f.close();
				
				loadJSGF("detgrammar");
//				System.out.println("GRAMMAR JSGF");
//				getInitialNode().dump();
				
				System.out.println("nb of grammar nodes "+getGrammarNodes().size());
				System.out.println("final nodes:");
				for (GrammarNode n : getGrammarNodes()) {
					if (n.isFinalNode()) {
						System.out.println("\t"+n);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSGFGrammarParseException e) {
				e.printStackTrace();
			} catch (JSGFGrammarException e) {
				e.printStackTrace();
			}
		}
	}
	
	// wi est utilise only pour ajouter l'indice du mot dans le prefixe du mot
	protected String annoteFirstPhones(String rule, int wi) {
		String[] rules = rule.split(" ");
		chercheFirstPhones(rules,0,wi);
		StringBuilder sb = new StringBuilder();
		for (String r : rules) {
			sb.append(r+" ");
		}
		return sb.toString().trim();
	}
	private void chercheFirstPhones(String[] rules, int i, int wi) {
		if (i>=rules.length) return;
		if (rules[i].equals("(")) {
			chercheFirstPhones(rules, i+1, wi);
			int co=0;
			int fin = i+1;
			for (;;fin++) {
				if (rules[fin].equals("(")) co++;
				else if (rules[fin].equals(")")) {
					if (co==0) break;
					co--;
				} else if (rules[fin].equals("|")) {
					if (co==0) {
						chercheFirstPhones(rules, fin+1, wi);
					}
				}
			}
		} else if (rules[i].equals("[")) {
			chercheFirstPhones(rules, i+1, wi);
			int co=0;
			int fin=i+1;
			for (;;fin++) {
				if (rules[fin].equals("[")) co++;
				else if (rules[fin].equals("]")) {
					if (co==0) break;
					co--;
				}
			}
			chercheFirstPhones(rules, fin+1, wi);
		} else {
			String x = "XZ"+wi+"Y"+rules[i];
			rules[i]=x;
		}
	}
	
	HashMap<String, String> phmap = null;
	protected String convertPhone(String ph) {
		if (phmap==null) {
			// TODO: je ne suis pas sur de cette conversion: la verifier !
			final String[] from = {"xx",  "J", "euf","H","a","an","in","b","d","e","E","eh" ,"eu","f","g","i","j","k","l","m","n","o","O", "oh","on","p","R","s","sil","SIL","swa","t","u","v","y","z","Z", "S", "w"};
			final String[] to =   {"SIL", "gn","euf","y","a","an","in","b","d","e","eh","eh","eu","f","g","i","j","k","l","m","n","o","oh","oh","on","p","r","s","SIL","SIL","swa","t","u","v","y","z","ge","ch","w"};
			
//			// on verifie que tous les phones des HMMs sont connus
//			AcousticModel mods = HMMModels.getAcousticModels();
//			Iterator<HMM> it = mods.getHMMIterator();
//			while (it.hasNext()) {
//				HMM hmm = it.next();
//				String nom = hmm.getBaseUnit().getName();
//				assert Arrays.binarySearch(to, nom)<0;
//			}
			phmap = new HashMap<String, String>();
			for (int i=0;i<from.length;i++) {
				phmap.put(from[i], to[i]);
			}
		}
		String phh = phmap.get(ph);
		if (phh==null) {
			System.out.println("ERROR phonecticgram convertPhones UNKNOWN PHONE "+ph);
			return ph;
		}
		return phh;
	}
	
	final GrammarNode[] tmpnodes = {null,null};
	/**
	 * @return (1) 1er noeud (2) dernier noeud
	 * Le 1er noeud est TOUJOURS un silence obligatoire, car cela est impose par
	 * FlatLinguist.allocate() !!
	 * Pour pouvoir aligner des grammaires sans silences, il faut ajouter
	 * 3 trames de silence automatiquement avant les trames relles...
	 */
	public GrammarNode[] convertRule2S4(String rule, LogMath logMath) {
		
		System.out.println("convert rule "+rule);
		
		StringTokenizer st = new StringTokenizer(rule);
		GrammarNode n = createGrammarNode("SIL");
		tmpnodes[0]=n;
		while (st.hasMoreTokens()) {
			String s = st.nextToken();
			if (s.charAt(0)=='#') continue;
			if (s.equals("(")) {
				GrammarNode[] nn = convertRuleAlt(st,logMath);
				n.add(nn[0], logMath.getLogOne());
				n=nn[1];
			} else if (s.equals("[")) {
					GrammarNode[] nn = convertRuleOpt(st,logMath);
					n.add(nn[0], logMath.getLogOne());
					n=nn[1];
			} else {
				String ss = convertPhone(s);
				GrammarNode nn = createGrammarNode(ss);
				n.add(nn, logMath.getLogOne());
				n=nn;
			}
		}
		// ajoute un silence a la fin ?
		// il semble que ce soit obligatoire pour avoir un resultat avec decode() !
		String ss = convertPhone("SIL");
		GrammarNode nn = createGrammarNode(ss);
		n.add(nn, logMath.getLogOne());
		n=nn;
		
		tmpnodes[1]=n;
		return tmpnodes;
	}
	GrammarNode[] convertRuleAlt(StringTokenizer st, LogMath logMath) {
		GrammarNode[] ns = new GrammarNode[2];
		GrammarNode n0 = createGrammarNode(false);
		ns[0]=n0;
		GrammarNode lastn = createGrammarNode(false);
		ns[1]=lastn;
		// 1er noeud de la 1ere alternative
		GrammarNode n = createGrammarNode(false);
		n0.add(n,logMath.getLogOne());
		// n contient ensuite le dernier noeud de l'alternative courante
		while (st.hasMoreTokens()) {
			String s = st.nextToken();
			if (s.charAt(0)=='#') continue;
			if (s.equals("(")) {
				GrammarNode[] nn = convertRuleAlt(st,logMath);
				n.add(nn[0], logMath.getLogOne());
				n=nn[1];
			} else if (s.equals("|")) {
				n.add(lastn, logMath.getLogOne());
				// 1er noeud de la Neme alternative
				n = createGrammarNode(false);
				n0.add(n,logMath.getLogOne());
			} else if (s.equals("[")) {
				GrammarNode[] nn = convertRuleOpt(st,logMath);
				n.add(nn[0], logMath.getLogOne());
				n=nn[1];
			} else if (s.equals("]")) {
				System.out.println("ERROR ] dans ( ");
				return null;
			} else if (s.equals(")")) {
				// c'est la fin !
				n.add(lastn, logMath.getLogOne());
				// TODO: recalculer les transitions des alternatives !
				return ns;
			} else {
				String ss = convertPhone(s);
				GrammarNode nn = createGrammarNode(ss);
				n.add(nn, logMath.getLogOne());
				n=nn;
			}
		}
		System.out.println("ERROR pas de fin !");
		return null;
	}

	GrammarNode[] convertRuleOpt(StringTokenizer st, LogMath logMath) {
		GrammarNode[] ns = new GrammarNode[2];
		GrammarNode n0 = createGrammarNode(false);
		ns[0]=n0;
		GrammarNode lastn = createGrammarNode(false);
		ns[1]=lastn;

		// la 1ere alternative passe direct du premier au dernier noeud
        float branchScore = logMath.linearToLog(0.5);
		n0.add(lastn, branchScore);
		
		// 1er noeud de la seconde alternative
		GrammarNode n = createGrammarNode(false);
		n0.add(n,branchScore);
		
		// n contient ensuite le dernier noeud de l'alternative courante
		while (st.hasMoreTokens()) {
			String s = st.nextToken();
			if (s.charAt(0)=='#') continue;
			if (s.equals("(")) {
				GrammarNode[] nn = convertRuleAlt(st,logMath);
				n.add(nn[0], logMath.getLogOne());
				n=nn[1];
			} else if (s.equals("|")) {
				System.out.println("ERROR | dans [ ");
				return null;
			} else if (s.equals("[")) {
				GrammarNode[] nn = convertRuleOpt(st,logMath);
				n.add(nn[0], logMath.getLogOne());
				n=nn[1];
			} else if (s.equals(")")) {
				System.out.println("ERROR ) dans [ ");
				return null;
			} else if (s.equals("]")) {
				// c'est la fin !
				n.add(lastn, logMath.getLogOne());
				return ns;
			} else {
				String ss = convertPhone(s);
				GrammarNode nn = createGrammarNode(ss);
				n.add(nn, logMath.getLogOne());
				n=nn;
			}
		}
		System.out.println("ERROR pas de fin !");
		return null;
	}

	public GrammarNode getGram() {
		return initialNode;
	}

}
