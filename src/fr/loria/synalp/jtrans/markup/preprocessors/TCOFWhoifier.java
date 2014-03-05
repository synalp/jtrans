package fr.loria.synalp.jtrans.markup.preprocessors;

import fr.loria.synalp.jtrans.facade.Project;
import fr.loria.synalp.jtrans.markup.ParsingException;
import fr.loria.synalp.jtrans.markup.TRSLoader;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Adds Who tags to Transcriber files that use the TCOF convention for
 * overlapping speech.
 *
 * The TCOF corpus does not use Who tags, which is the standard Transcriber way
 * of denoting overlapping speech. Instead, TCOF denotes overlaps with angle
 * brackets across two Turns. In the following example, the words "four five
 * six" and "seven eight nine" are spoken simultaneously.
 *
 * <pre>
 * {@code
 * <Turn speaker="spk1" ...>
 *     <Sync time="1.234"/> one two three &lt; four five six
 * </Turn>
 *
 * <Turn speaker="spk2" ...>
 *     <Sync time="5.678"/> seven eight nine &gt; ten eleven twelve
 * </Turn>
 * }</pre>
 *
 * This tool would transform the example above into multi-speaker Turns, which,
 * as far as JTrans is concerned, is more manageable Transcriber markup.
 *
 * <pre>
 * {@code
 * <Turn speaker="spk1" ...>
 *     <Sync time="1.234"/> one two three
 * </Turn>
 *
 * <Turn speaker="spk1 spk2" ...>
 *     <Sync time="5.678"/>
 *     <Who nb="1"/> &lt; four five six
 *     <Who nb="2"/> seven eight nine &gt; ten eleven twelve
 * </Turn>
 * }</pre>
 */
public class TCOFWhoifier extends TRSLoader {

	@Override
	public Project parse(File file) throws ParsingException, IOException {
		try {
			return parse(whoify(file));
		} catch (ParserConfigurationException ex) {
			ex.printStackTrace();
			throw new ParsingException(ex.toString());
		} catch (SAXException ex) {
			ex.printStackTrace();
			throw new ParsingException(ex.toString());
		}
	}


	@Override
	public String getFormat() {
		return "Transcriber (TCOF conventions)";
	}


	private static List<Node> scrap(Node node) {
		List<Node> scrapped = new ArrayList<Node>();
		while (null != node) {
			scrapped.add(node);
			Node n = node;
			node = n.getNextSibling();
			n.getParentNode().removeChild(n);
		}
		return scrapped;
	}


	public static Document whoify(File file)
			throws IOException, ParserConfigurationException, SAXException
	{
		Document doc = newXMLDocumentBuilder().parse(file);

		String previousSpeaker = null;
		Element reportScrapped = null;
		List<Node> reportOverlap = null;

		NodeList turnList = doc.getElementsByTagName("Turn");
		for (int turnNo = 0; turnNo < turnList.getLength(); turnNo++) {
			Element turn = (Element)turnList.item(turnNo);

			boolean hangingOverlap = reportOverlap != null;
			final boolean multiSpeakerTurn = hangingOverlap;

			// "Empty" means the turn does not contain any significant children.
			// Sync tags and whitespace #text are not significant by themselves.
			boolean emptyTurn = reportOverlap == null && reportScrapped == null;

			String singleSpeaker = turn.getAttribute("speaker");
			if (singleSpeaker.isEmpty() || 1 != singleSpeaker.split(" ").length)
				throw new Error("speaker count != 1. If >0, already whoified");

			assert reportOverlap == null || reportScrapped == null;

			if (reportOverlap != null) {
				turn.setAttribute("speaker", previousSpeaker + " " + singleSpeaker);
			}

			if (reportScrapped != null) {
				if (singleSpeaker.equals(previousSpeaker)) {
					NodeList rscn = reportScrapped.getChildNodes();
					for (int j = rscn.getLength()-1; j >= 0; j--)
						turn.insertBefore(rscn.item(j), turn.getFirstChild());

					turn.setAttribute("startTime", reportScrapped.getAttribute("startTime"));
				} else {
					turn = (Element)turn.getParentNode().insertBefore(reportScrapped, turn);
					singleSpeaker = turn.getAttribute("speaker");
				}
				reportScrapped = null;
			}

			// We may have changed our turn

			Node child = turn.getFirstChild();
			previousSpeaker = singleSpeaker;

			while (null != child) {
				String name = child.getNodeName();
				String text = child.getTextContent();

				if (name.equals("Sync") && reportOverlap != null) {
					Node nextChild = child.getNextSibling();

					Element who1 = doc.createElement("Who");
					who1.setAttribute("nb", ""+1);
					Element who2 = doc.createElement("Who");
					who2.setAttribute("nb", ""+2);

					turn.insertBefore(who1, nextChild);
					for (Node n: reportOverlap)
						turn.insertBefore(n, nextChild);
					turn.insertBefore(who2, nextChild);

					reportOverlap = null;
					hangingOverlap = false;
					child = nextChild;
				}

				else if (name.equals("Sync") && reportOverlap == null && multiSpeakerTurn) {
					String syncTime =  ((Element)child).getAttribute("time");

					reportScrapped = doc.createElement("Turn");
					reportScrapped.setAttribute("endTime", turn.getAttribute("endTime"));
					reportScrapped.setAttribute("startTime", syncTime);
					reportScrapped.setAttribute("speaker",   singleSpeaker);
					for (Node n: scrap(child))
						reportScrapped.appendChild(n);

					turn.setAttribute("endTime", syncTime);
					break;
				}

				else if (name.equals("#text") && text.contains("<")) {
					if (reportOverlap != null)
						throw new Error("overlap already ongoing before '" + text + "'");

					int chevronPos = text.indexOf('<');
					String beforeChevron = text.substring(0, chevronPos);
					child.setTextContent(beforeChevron);

					reportOverlap = scrap(child.getNextSibling());
					reportOverlap.add(0, doc.createTextNode(text.substring(chevronPos)));

					// Don't leave an empty turn
					if (emptyTurn && beforeChevron.trim().isEmpty()) {
						turn.getParentNode().removeChild(turn);
						turnNo--;
					}

					break;
				}

				else {
					if (!name.equals("Sync") && (!name.equals("#text") || !text.trim().isEmpty()))
						emptyTurn = false;

					child = child.getNextSibling();
				}
			}

			if (hangingOverlap)
				throw new Error("hanging overlap - missing Sync tag?");
		}

		return doc;
	}


	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("USAGE:\n" +
					"TCOFWhoifier <INPUT.trs> <OUTPUT.trs>");
			System.exit(1);
		}

		Document doc = whoify(new File(args[0]));

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, doc.getXmlEncoding());
		transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doc.getDoctype().getSystemId());

		transformer.transform(
				new DOMSource(doc),
				new StreamResult(new File(args[1])));
	}

}
