package facade;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
import plugins.text.elements.Locuteur_Info;

import javax.xml.parsers.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * TRS Loader.
 *
 * The constructor parses a TRS file. If parsing was successful, text and anchor info
 * can be retrieved through the public final attributes.
 */
class TRSLoader {

	/**
	 * Time anchor. Matches a "Sync" tag in TRS files.
	 */
	class Anchor {
		/** Position of the anchor in the text. */
		int character;

		/** Time */
		float seconds;

		private Anchor(int c, float s) {
			character = c;
			seconds = s;
		}
	}


	/**
	 * Raw text contained in the Turn tags.
	 */
	public final String text;


	/**
	 * Time anchors ("Sync" tags) contained in the Turn tags.
	 */
	public final ArrayList<Anchor> anchors;


	/**
	 * Mapping of TRS speaker IDs to speaker objects. This mapping is necessary
	 * because Locuteur_Info.id does not match TRS speaker IDs.
	 */
	public final HashMap<String, Locuteur_Info> speakers;


	/**
	 * Parse a TRS file.
	 */
	public TRSLoader(String path) throws ParserConfigurationException, IOException, SAXException {
		Document doc = newXMLDocumentBuilder().parse(path);
		StringBuffer buffer = new StringBuffer();
		ArrayList<Anchor> anchorList = new ArrayList<Anchor>();

		// end time of last turn
		float lastEnd = -1f;

		speakers = loadSpeakers(doc.getElementsByTagName("Speakers").item(0));

		// Extract relevant information (speech text, Sync tags...) from Turn tags.
		NodeList turnList = doc.getElementsByTagName("Turn");

		for (int i = 0; i < turnList.getLength(); i++) {
			Element turn = (Element)turnList.item(i);
			Node child = turn.getFirstChild();

			Locuteur_Info currentSpeaker = speakers.get(turn.getAttribute("speaker"));
			boolean currentSpeakerIntroduced = false;

			float endTime = Float.parseFloat(turn.getAttribute("endTime"));
			if (endTime > lastEnd)
				lastEnd = endTime;

			while (null != child) {
				String name = child.getNodeName();

				// Speech text
				if (name.equals("#text")) {
					String text = child.getTextContent().trim();
					if (!text.isEmpty()) {
						// Introduce current speaker with a line break and their
						// name so that reparse() can pick it up
						if (!currentSpeakerIntroduced) {
							if (buffer.length() > 0)
								buffer.append("\n");
							buffer.append(currentSpeaker.getName());
							currentSpeakerIntroduced = true;
						}
						buffer.append(" ").append(text);
					}
				}

				// Anchor. Placed on the last character in the word *PRECEDING* the sync point
				else if (name.equals("Sync")) {
					int character = buffer.length();
					float second = Float.parseFloat(((Element)child).getAttribute("time"));
					anchorList.add(new Anchor(character, second));
				}

				else if (name.equals("Comment")) {
					buffer.append(" {")
							.append(((Element)child).getAttribute("desc"))
							.append("}");
				}

				// Ignore unknown tag
				else {
					System.out.println("TRS WARNING: Ignoring inknown tag " + name);
				}

				// Onto next Turn child
				child = child.getNextSibling();
			}
		}

		// Fake anchor after last turn so that the whole speech gets aligned
		anchorList.add(new Anchor(buffer.length(), lastEnd));

		text = buffer.toString();
		anchors = anchorList;
	}


	private HashMap<String, Locuteur_Info> loadSpeakers(Node speakersNode) {
		HashMap<String, Locuteur_Info> info = new HashMap<String, Locuteur_Info>();

		Node spk = speakersNode.getFirstChild();

		for (; null != spk; spk = spk.getNextSibling()) {
			if (!spk.getNodeName().equals("Speaker"))
				continue;

			Element el = (Element)spk;
			String id      = el.getAttribute("id");
			String name    = el.getAttribute("name");
			boolean check  = el.getAttribute("check").toLowerCase().equals("yes");
			boolean type   = el.getAttribute("type").toLowerCase().equals("female");
			String dialect = el.getAttribute("dialect");
			String accent  = el.getAttribute("accent");
			String scope   = el.getAttribute("scope");

			info.put(id, new Locuteur_Info(
					(byte) info.size(), name, check, type, dialect, accent, scope));
		}

		return info;
	}


	/**
	 * Return a DocumentBuilder suitable to parsing a TRS file.
	 */
	private static DocumentBuilder newXMLDocumentBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);
		dbf.setNamespaceAware(true);
		dbf.setFeature("http://xml.org/sax/features/namespaces", false);
		dbf.setFeature("http://xml.org/sax/features/validation", false);
		dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		return dbf.newDocumentBuilder();
	}
}
