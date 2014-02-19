package fr.loria.synalp.jtrans.anchorless;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.linguist.acoustic.*;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.linguist.language.grammar.*;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import fr.loria.synalp.jtrans.speechreco.s4.*;
import fr.loria.synalp.jtrans.utils.StdoutProgressDisplay;

import java.io.File;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Sweeping grammar vector for the experimental anchorless algorithm.
 */
public class GrammarVector {

	private List<Cell> stateCells = new ArrayList<Cell>();


	/**
	 * Build up the vector by traversing the grammar graph recursively.
	 * @param node node of the graph to visit
	 * @param seenNodes set of visited nodes
	 * @return 1st state of the phoneme
	 */
	private Cell traversePhoneGraph(
			GrammarNode node,
			Map<GrammarNode, Cell> seenNodes,
			AcousticModel acMod,
			UnitManager unitMgr)
	{
		Cell firstStateCell = seenNodes.get(node);
		if (null != firstStateCell)
			return firstStateCell;

		if (!node.isEmpty()) {
			String w = node.getWord().getSpelling();

			// word boundary hack
			if (w.startsWith("XZ"))
				w = w.substring(1+w.indexOf('Y'));

			// find HMM for this phone
			HMM hmm = acMod.lookupNearestHMM(
					unitMgr.getUnit(w), HMMPosition.UNDEFINED, false);

			// add phone states
			firstStateCell = traverseHMMStateGraph(
					hmm.getInitialState(),
					new HashMap<HMMState, Cell>());
		} else {
			firstStateCell = new Cell(null);
		}

		seenNodes.put(node, firstStateCell);

		for (GrammarArc arc: node.getSuccessors()) {
			GrammarNode sucNode = arc.getGrammarNode();
			Cell sucCell = traversePhoneGraph(sucNode, seenNodes, acMod, unitMgr);

			// link our exiting states with the first state of each succeeding phone
			if (!sucNode.isEmpty()) {
				recursiveBindExit(firstStateCell, sucCell, new HashSet<Cell>());
			} else {
				for (Cell t: sucCell.transitions)
					recursiveBindExit(firstStateCell, t, new HashSet<Cell>());
			}
		}

		return firstStateCell;
	}


	/**
	 * Inserts the emitting states of the HMM in the vector.
	 * (Usually, this is a mini graph with three nodes)
	 * @param seen set of visited states
	 */
	private Cell traverseHMMStateGraph(
			HMMState hmmState, Map<HMMState, Cell> seen)
	{
		Cell cell = seen.get(hmmState);

		if (null != cell)
			return cell;

		cell = new Cell(hmmState);
		if (hmmState.isEmitting())
			stateCells.add(cell);
		seen.put(hmmState, cell);

		for (HMMStateArc arc: hmmState.getSuccessors()) {
			HMMState sucState = arc.getHMMState();

			System.out.println(cell.item + " --> " + sucState + " " +
					(sucState.isEmitting() ? "emit": " -- ") + " " +
					(sucState.isExitState()? "exit": " -- ") + " " +
					HMMModels.getLogMath().logToLinear(arc.getLogProbability()));

			Cell sucCell = traverseHMMStateGraph(sucState, seen);

			if (!sucState.isEmitting())
				cell.transitions.add(sucCell);
			else
				cell.transitions.addAll(sucCell.transitions);
		}

		assert cell.transitions.size() <= 1;
		return cell;
	}


	private void recursiveBindExit(Cell stateCell, Cell successor,
								   Set<Cell> seen)
	{
		if (seen.contains(stateCell))
			return;
		else
			seen.add(stateCell);

		for (Cell sucState: stateCell.transitions)
			recursiveBindExit(sucState, successor, seen);

		if (null == stateCell.item || stateCell.item.isExitState())
			stateCell.transitions.add(successor);
	}


	/**
	 * Constructs a grammar vector from an initial grammar node.
	 * @param node initial node of a Sphinx4 Grammar
	 * @see edu.cmu.sphinx.linguist.language.grammar
	 */
	public GrammarVector(GrammarNode node, AcousticModel acMod, UnitManager unitMgr) {
		traversePhoneGraph(
				node,
				new HashMap<GrammarNode, Cell>(),
				acMod,
				unitMgr);
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
	public GrammarVector(String text, AcousticModel acMod, UnitManager unitMgr)
			throws MalformedURLException, ClassNotFoundException
	{
		this(createGrammarGraph(text), acMod, unitMgr);
	}


	/**
	 * Scores a frame according to every state in the vector.
	 */
	public void scoreFrame(Data frame, TiedStateAcousticModel acMod, UnitManager unitMgr) {
		for (Cell cell: stateCells) {
			float score = cell.item.getScore(frame);
			System.out.println(cell + "\t" + score);
		}
	}


	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("USAGE: GrammarVector <SOUNDFILE.WAV> <\"transcription\">");
			System.exit(1);
		}

		final String wavpath = args[0];
		final String words   = args[1];

		ConfigurationManager cm = new ConfigurationManager("sr.cfg");
		UnitManager unitmgr = (UnitManager)cm.lookup("unitManager");
		assert unitmgr != null;

		TiedStateAcousticModel acmod = (TiedStateAcousticModel) HMMModels.getAcousticModels();

		GrammarVector gv = new GrammarVector(words, acmod, unitmgr);

		AudioFileDataSource afds = (AudioFileDataSource)cm.lookup("audioFileDataSource");
		afds.setAudioFile(new File(wavpath), null);

		S4mfccBuffer mfcc = new S4mfccBuffer();
		mfcc.setSource(S4ForceAlignBlocViterbi.getFrontEnd(afds));

		int f = 0;
		while (!mfcc.noMoreFramesAvailable) {
			Data data = mfcc.getData();
			if (data instanceof DataStartSignal || data instanceof DataEndSignal)
				continue;
			System.out.println("Scoring Frame: " + (f++));
			gv.scoreFrame(data, acmod, unitmgr);
		}

		System.out.println("done");
		System.out.println("GRAPH SIZE: " + gv.stateCells.size());
	}

}
