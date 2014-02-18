package fr.loria.synalp.jtrans.anchorless;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.linguist.acoustic.*;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.linguist.language.grammar.*;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import fr.loria.synalp.jtrans.speechreco.s4.*;
import fr.loria.synalp.jtrans.utils.StdoutProgressDisplay;

import java.io.File;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Sweeping grammar vector for the experimental anchorless algorithm.
 */
public class GrammarVector implements Iterable<Cell> {

	private List<Cell> cells = new ArrayList<Cell>();


	@Override
	public Iterator<Cell> iterator() {
		return cells.iterator();
	}


	public Cell getRoot() {
		return cells.get(0);
	}


	/**
	 * Build up the vector by traversing the grammar graph recursively.
	 * @param node node of the graph to visit
	 * @param seen set of visited nodes
	 */
	private Cell recursiveTraversal(GrammarNode node, Map<GrammarNode, Cell> seen) {
		Cell cell = seen.get(node);

		if (null != cell)
			return cell;

		if (!node.isEmpty()) {
			String w = node.getWord().getSpelling();
			// word boundary hack
			if (w.startsWith("XZ"))
				w = w.substring(1+w.indexOf('Y'));
			cell = new Cell(w);

			cells.add(cell);
		} else {
			cell = new Cell(null);
		}

		seen.put(node, cell);

		for (GrammarArc suc: node.getSuccessors()) {
			GrammarNode sucNode = suc.getGrammarNode();
			Cell sucCell = recursiveTraversal(sucNode, seen);

			if (!sucNode.isEmpty())
				cell.transitions.add(sucCell);
			else
				cell.transitions.addAll(sucCell.transitions);
		}

		return cell;
	}


	/**
	 * Constructs a grammar vector from an initial grammar node.
	 * @param node initial node of a Sphinx4 Grammar
	 * @see edu.cmu.sphinx.linguist.language.grammar
	 */
	public GrammarVector(GrammarNode node) {
		recursiveTraversal(node, new HashMap<GrammarNode, Cell>());
	}


	/**
	 * Creates a Sphinx4 grammar from a piece of text.
	 * @param text words separated by spaces
	 * @return initial node
	 */
	public static GrammarNode createGrammarGraph(String text)
			throws MalformedURLException, ClassNotFoundException
	{
		PhoneticForcedGrammar g = new PhoneticForcedGrammar();
		g.setWords(Arrays.asList(text.split(" ")), new StdoutProgressDisplay());
		g.getInitialNode().dumpDot("grammar_graph.dot");
		return g.getInitialNode();
	}


	/**
	 * Constructs a grammar vector from a piece of text.
	 * @param text words separated by spaces
	 */
	public GrammarVector(String text)
			throws MalformedURLException, ClassNotFoundException
	{
		this(createGrammarGraph(text));
	}


	/**
	 * Scores a frame according to every state in the vector.
	 */
	public void scoreFrame(Data frame, TiedStateAcousticModel acMod, UnitManager unitMgr) {
		for (Cell cell: this) {
			String w = cell.name;
			assert w != null;

			HMM hmm = acMod.lookupNearestHMM(
					unitMgr.getUnit(w), HMMPosition.UNDEFINED, false);

			HMMState state = hmm.getInitialState();

			// TODO: score beyond initial state
			// TODO: fill the vector with (emitting) HMM states

			if (state.isEmitting()) {
				float score = state.getScore(frame);
				System.out.println(w + "\t" + score);
			} else {
				System.out.println(w + "\t <NON-EMITTING>");
			}
		}
	}


	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("USAGE: GrammarVector <SOUNDFILE.WAV> <\"transcription\">");
			System.exit(1);
		}

		final String wavpath = args[0];
		final String words   = args[1];

		GrammarVector gv = new GrammarVector(words);

		ConfigurationManager cm = new ConfigurationManager("sr.cfg");
		UnitManager unitmgr = (UnitManager)cm.lookup("unitManager");
		assert unitmgr != null;

		AudioFileDataSource afds = (AudioFileDataSource)cm.lookup("audioFileDataSource");
		afds.setAudioFile(new File(wavpath), null);

		S4mfccBuffer mfcc = new S4mfccBuffer();
		mfcc.setSource(S4ForceAlignBlocViterbi.getFrontEnd(afds));

		TiedStateAcousticModel acmod = (TiedStateAcousticModel) HMMModels.getAcousticModels();

		while (!mfcc.noMoreFramesAvailable) {
			Data data = mfcc.getData();
			if (data instanceof DataStartSignal || data instanceof DataEndSignal)
				continue;
			gv.scoreFrame(data, acmod, unitmgr);
		}

		System.out.println("done");
	}

}
