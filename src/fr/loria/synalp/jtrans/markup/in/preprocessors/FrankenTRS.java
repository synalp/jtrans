package fr.loria.synalp.jtrans.markup.in.preprocessors;

import fr.loria.synalp.jtrans.project.Comment;
import fr.loria.synalp.jtrans.project.Element;
import fr.loria.synalp.jtrans.markup.in.MarkupLoader;
import fr.loria.synalp.jtrans.markup.in.ParsingException;
import fr.loria.synalp.jtrans.markup.in.RawTextLoader;
import fr.loria.synalp.jtrans.markup.in.TRSLoader;
import fr.loria.synalp.jtrans.project.TurnProject;
import fr.loria.synalp.jtrans.utils.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Headerless TRS with inline speakers, TCOF overlap conventions
 */
public class FrankenTRS implements MarkupLoader {

	// TODO: do this without going through a temp file
	public File xmlize(File file)
			throws ParsingException, IOException
	{
		File tmpFile = FileUtils.createVanishingTempFile("trsinlinespeakers", ".trs");

		PrintWriter w = FileUtils.writeFileUTF(tmpFile.getAbsolutePath());

		w.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		w.println("<!DOCTYPE Trans SYSTEM \"trans-14.dtd\">");
		w.println("<Trans><Episode><Section><Turn>");

		BufferedReader r = FileUtils.openFileISO(file.getAbsolutePath());
		String line;
		while (null != (line = r.readLine())) {
			w.println(line);
		}

		w.println("</Turn></Section></Episode></Trans>");
		w.close();

		return tmpFile;
	}


	public TurnProject parse(File file)
			throws ParsingException, IOException
	{
		TurnProject project = new TurnProject();

		Document doc = TRSLoader.parseXML(xmlize(file));
		Node turnNode = doc.getElementsByTagName("Turn").item(0);

		Node child = turnNode.getFirstChild();

		TurnProject.Turn pTurn = null;
		int spkID = -1;

		boolean overlapOngoing = false;

		Map<String, Integer> speakers = new HashMap<>();

		while (null != child) {
			String name = child.getNodeName();

			switch (name) {
				case "#text": {
					for (String line: child.getTextContent().split("[\n\r]")) {
						String text = line.trim();
						if (text.isEmpty()) {
							continue;
						}

						String[] sp = TRSInlineSpeakers.speakerPattern(text);

						if (null != sp) {
							if (!speakers.containsKey(sp[0])) {
								speakers.put(sp[0], project.newSpeaker(sp[0]));
							}
							if (!overlapOngoing && (pTurn == null || !pTurn.isEmpty())) {
								pTurn = project.newTurn();
							}
							spkID = speakers.get(sp[0]);
							text = sp[1];
						}

						text = RawTextLoader.normalizeText(text);
						for (Element el: RawTextLoader.parseString(text, RawTextLoader.DEFAULT_PATTERNS)) {
							Comment c = el instanceof Comment? (Comment)el: null;
							if (c != null && c.getType() == Comment.Type.OVERLAP_START_MARK) {
								pTurn = project.newTurn();
								overlapOngoing = true;
								addToTurn(pTurn, spkID, el);
							} else if (c != null && c.getType() == Comment.Type.OVERLAP_END_MARK) {
								addToTurn(pTurn, spkID, el);
								pTurn = project.newTurn();
								overlapOngoing = false;
							} else {
								addToTurn(pTurn, spkID, el);
							}
						}
					}
					break;
				}

				default: {
					addToTurn(pTurn, spkID, TRSLoader.transformNode(child));
					break;
				}
			}

			child = child.getNextSibling();
		}

		return project;
	}


	private static void addToTurn(TurnProject.Turn turn, int spkID, Element el)
			throws ParsingException
	{
		if (el == null) {
			return;
		}

		if (turn == null || spkID < 0) {
			throw new ParsingException("no speaker defined yet!");
		}

		turn.add(spkID, el);
	}


	@Override
	public String getFormat() {
		return "FrankenTRS";
	}


	@Override
	public String getExt() {
		return ".txt";
	}

}
