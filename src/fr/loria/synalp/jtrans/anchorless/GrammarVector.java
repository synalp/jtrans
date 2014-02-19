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

	private List<Cell<HMMState>> stateCells = new ArrayList<Cell<HMMState>>();


	/**
	 * Build up the vector by traversing the grammar graph recursively.
	 * @param node node of the graph to visit
	 * @param seen set of visited nodes
	 */
	static Cell<String> traversePhoneGraph(
			GrammarNode node, Map<GrammarNode, Cell<String>> seen)
	{
		Cell<String> cell = seen.get(node);

		if (null != cell)
			return cell;

		if (!node.isEmpty()) {
			String w = node.getWord().getSpelling();
			// word boundary hack
			if (w.startsWith("XZ"))
				w = w.substring(1+w.indexOf('Y'));
			cell = new Cell<String>(w);

//			phoneCells.add(cell);
		} else {
			cell = new Cell<String>(null);
		}

		seen.put(node, cell);

		for (GrammarArc arc: node.getSuccessors()) {
			GrammarNode sucNode = arc.getGrammarNode();
			Cell<String> sucCell = traversePhoneGraph(sucNode, seen);

			if (!sucNode.isEmpty())
				cell.transitions.add(sucCell);
			else
				cell.transitions.addAll(sucCell.transitions);
		}

		return cell;
	}

	/**
	 * Inserts the emitting states of the phone in the vector,
	 * recursively inserts the states of the next phone in the vector,
	 * and links the exiting states with the next phone's first state.
	 * @param seenPhones set of visited phone cells; maps phone cells to
	 *                   their first state cell
	 * @param seenState1s set of visited first states
	 * @return cell of the first state in the phone
	 */
	private Cell<HMMState> traversePhoneHMMGraph(
			Cell<String> phone,
			HashMap<Cell<String>, Cell<HMMState>> seenPhones,
			HashMap<HMMState, Cell<HMMState>> seenState1s,
			AcousticModel acMod,
			UnitManager unitMgr)
	{
		Cell<HMMState> state1Cell = seenPhones.get(phone);

		if (null != state1Cell)
			return state1Cell;

		HMM hmm = acMod.lookupNearestHMM(
				unitMgr.getUnit(phone.item), HMMPosition.UNDEFINED, false);

		state1Cell = traverseHMMStateGraph(hmm.getInitialState(), seenState1s);

		for (Cell<String> sucPhone: phone.transitions) {
			Cell<HMMState> nextState1Cell = traversePhoneHMMGraph(
					sucPhone, seenPhones, seenState1s, acMod, unitMgr);
			recursiveBindExit(state1Cell, nextState1Cell, new HashSet<Cell<HMMState>>());
		}

		return state1Cell;
	}

	/**
	 * Inserts the emitting states of the HMM in the vector.
	 * @param seen set of visited states
	 */
	private Cell<HMMState> traverseHMMStateGraph(
			HMMState hmmState, Map<HMMState, Cell<HMMState>> seen)
	{
		Cell<HMMState> cell = seen.get(hmmState);

		if (null != cell)
			return cell;

		cell = new Cell<HMMState>(hmmState);
		if (hmmState.isEmitting())
			stateCells.add(cell);
		seen.put(hmmState, cell);

		for (HMMStateArc arc: hmmState.getSuccessors()) {
			HMMState sucState = arc.getHMMState();

			System.out.println(cell.item + " --> " + sucState + " " +
					(sucState.isEmitting() ? "emit": " -- ") + " " +
					(sucState.isExitState()? "exit": " -- ") + " " +
					HMMModels.getLogMath().logToLinear(arc.getLogProbability()));

			Cell<HMMState> sucCell = traverseHMMStateGraph(sucState, seen);

			if (!sucState.isEmitting())
				cell.transitions.add(sucCell);
			else
				cell.transitions.addAll(sucCell.transitions);
		}

		assert cell.transitions.size() <= 1;
		return cell;
	}


	private void recursiveBindExit(Cell<HMMState> stateCell, Cell<HMMState> successor,
								   Set<Cell<HMMState>> seen)
	{
		if (seen.contains(stateCell))
			return;
		else
			seen.add(stateCell);

		for (Cell<HMMState> sucState: stateCell.transitions)
			recursiveBindExit(sucState, successor, seen);

		if (stateCell.item.isExitState())
			stateCell.transitions.add(successor);
	}


	/**
	 * Constructs a grammar vector from an initial grammar node.
	 * @param node initial node of a Sphinx4 Grammar
	 * @see edu.cmu.sphinx.linguist.language.grammar
	 */
	public GrammarVector(GrammarNode node, AcousticModel acMod, UnitManager unitMgr) {
		traversePhoneHMMGraph(
				traversePhoneGraph(node, new HashMap<GrammarNode, Cell<String>>()),
				new HashMap<Cell<String>, Cell<HMMState>>(),
				new HashMap<HMMState, Cell<HMMState>>(),
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
		for (Cell<HMMState> cell: stateCells) {
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
	}

}
