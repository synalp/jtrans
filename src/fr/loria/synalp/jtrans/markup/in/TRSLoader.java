package fr.loria.synalp.jtrans.markup.in;

import fr.loria.synalp.jtrans.elements.Comment;
import fr.loria.synalp.jtrans.project.Anchor;
import fr.loria.synalp.jtrans.project.TurnProject;
import org.w3c.dom.*;
import org.w3c.dom.Element;
import org.xml.sax.*;

import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for the Transcriber file format.
 */
public class TRSLoader implements MarkupLoader {

	public static Pattern DTD_PATTERN =
			Pattern.compile("^.*(trans-[0-9a-z]*\\.dtd)$");


	public static Document parseXML(File file)
			throws ParsingException, IOException
	{
		try {
			return newXMLDocumentBuilder().parse(file);
		} catch (ParserConfigurationException ex) {
			ex.printStackTrace();
			throw new ParsingException(ex.toString());
		} catch (SAXException ex) {
			ex.printStackTrace();
			throw new ParsingException(ex.toString());
		}
	}


	public TurnProject parse(File file)
			throws ParsingException, IOException
	{
		return parse(parseXML(file));
	}


	public TurnProject parse(Document doc) throws ParsingException {
		TurnProject project = new TurnProject();

		// Map of Transcriber's speaker IDs to JTrans tracks
		Map<String, Integer> spkIDMap = new HashMap<>();

		// Last sync times (to detect unordered sync times)
		float lastSyncTime = -1f;

		// end time of last turn
		float lastEnd = -1f;

		NodeList speakerList = doc.getElementsByTagName("Speakers");
		if (1 != speakerList.getLength()) {
			throw new ParsingException("TRS error: expected 1 Speakers tag " +
					"but found " + speakerList.getLength() + "!");
		}

		// Create speaker tracks and build ID map
		for (Node spk = speakerList.item(0).getFirstChild();
			 null != spk;
			 spk = spk.getNextSibling())
		{
			if (!spk.getNodeName().equals("Speaker"))
				continue;

			Element el = (Element)spk;
			String trsID   = el.getAttribute("id");
			String name    = el.getAttribute("name");

			int newSpkID = project.newSpeaker(name);
			spkIDMap.put(trsID, newSpkID);
		}

		// Extract relevant information (speech text, Sync tags...) from Turn tags.
		NodeList turnList = doc.getElementsByTagName("Turn");
		for (int i = 0; i < turnList.getLength(); i++) {
			Element turn = (Element)turnList.item(i);
			Node child = turn.getFirstChild();

			// Map IDs of speakers active in this turn to tracks
			String speakerAttr = turn.getAttribute("speaker");
			if (speakerAttr.isEmpty()) {
				System.out.println("TRS WARNING: skipping turn without any speakers");
				continue;
			}

			List<Integer> turnTracks = new ArrayList<>();
			for (String turnSpeaker: speakerAttr.split(" "))
				turnTracks.add(spkIDMap.get(turnSpeaker));

			// Start with the first speaker in case the first "Who" tag is missing
			int spkID = turnTracks.get(0);

			TurnProject.Turn pTurn = project.newTurn();

			float endTime = Float.parseFloat(turn.getAttribute("endTime"));
			if (endTime > lastEnd)
				lastEnd = endTime;

			while (null != child) {
				String name = child.getNodeName();

				// Speech text
				if (name.equals("#text")) {
					pTurn.addAll(spkID, RawTextLoader.parseString(
							RawTextLoader.normalizeText(child.getTextContent().trim()),
							RawTextLoader.DEFAULT_PATTERNS));
				}

				// Anchor
				else if (name.equals("Sync")) {
					float time = Float.parseFloat(((Element) child).getAttribute("time"));

					if (time > endTime) {
						throw new ParsingException(String.format("TRS error: " +
								"Sync time (%f) exceeds Turn endTime (%f)!",
								time, endTime));
					}

					if (lastSyncTime > time) {
						throw new ParsingException(String.format(
								"TRS error: Sync times in non-" +
								"chronological order! (%f after %f)",
								lastSyncTime, time));
					}

					if (null != pTurn.start) {
						assert null == pTurn.end;
						pTurn.end = new Anchor(time);
						pTurn = project.newTurn();
					}

					pTurn.start = new Anchor(time);
				}

				// Change speakers in a multi-speaker turn
				else if (name.equals("Who")) {
					// Speaker numbering starts at 1 in the XML file
					int nb = Integer.parseInt(((Element)child).getAttribute("nb"));
					spkID = turnTracks.get(nb - 1);
				}

				else {
					fr.loria.synalp.jtrans.elements.Element el = transformNode(child);
					if (null != el) {
						pTurn.add(spkID, el);
					}
				}

				// Onto next Turn child
				child = child.getNextSibling();
			}

			pTurn.end = new Anchor(endTime);
		}

		return project;
	}


	public static fr.loria.synalp.jtrans.elements.Element transformNode(Node n) {
		String name = n.getNodeName();

		switch (name) {
			case "Comment":
				return new Comment(
						((Element) n).getAttribute("desc"),
						Comment.Type.FREEFORM);
			case "Event":
				return new Comment(
						((Element) n).getAttribute("desc"),
						Comment.Type.NOISE);
			default:
				System.out.println("TRS WARNING: Ignoring inknown tag " + name);
				break;
		}

		return null;
	}


	/**
	 * Return a DocumentBuilder suitable to parsing a TRS file.
	 */
	protected static DocumentBuilder newXMLDocumentBuilder()
			throws ParserConfigurationException
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(true);
		dbf.setNamespaceAware(true);
		DocumentBuilder builder = dbf.newDocumentBuilder();

		builder.setEntityResolver(new EntityResolver() {
			@Override
			public InputSource resolveEntity(String publicId, String systemId)
					throws IOException, SAXException
			{
				Matcher m = DTD_PATTERN.matcher(systemId);
				if (!m.matches()) {
					return null;
				}

				String dtdName = m.group(1);

				InputStream dtd = TRSLoader.class.getResourceAsStream(dtdName);
				if (dtd == null) {
					throw new SAXException("Lacking DTD: " + dtdName);
				}

				return new InputSource(dtd);
			}
		});

		builder.setErrorHandler(new ErrorHandler() {
			@Override
			public void warning(SAXParseException e) {
				System.err.println("XML Warning: " + e);
			}

			@Override
			public void error(SAXParseException e) {
				System.err.println("XML Error: " + e);
			}

			@Override
			public void fatalError(SAXParseException e) throws SAXException {
				System.err.println("XML Fatal Error: " + e);
				throw e;
			}
		});

		return builder;
	}


	public String getFormat() {
		return "Transcriber (.TRS)";
	}


	public String getExt() {
		return ".trs";
	}

}
