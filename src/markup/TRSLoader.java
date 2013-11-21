package markup;

import org.w3c.dom.*;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import plugins.text.ListeElement;
import plugins.text.TexteEditor;
import plugins.text.elements.*;
import plugins.text.regexp.TypeElement;

import javax.xml.parsers.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Parser for the Transcriber file format.
 */
public class TRSLoader implements MarkupLoader {

	/**
	 * All elements generated by parsing the TRS file.
	 */
	private ListeElement elements;


	/**
	 * Parse a TRS file.
	 */
	public void parse(File file)
			throws ParsingException, IOException
	{
		Document doc;

		try {
			doc = newXMLDocumentBuilder().parse(file);
		} catch (ParserConfigurationException ex) {
			ex.printStackTrace();
			throw new ParsingException(ex.toString());
		} catch (SAXException ex) {
			throw new ParsingException(ex.toString());
		}

		elements  = new ListeElement();

		// end time of last turn
		float lastEnd = -1f;

		Map<String, Locuteur_Info> speakers =
				loadSpeakers(doc.getElementsByTagName("Speakers").item(0));

		List<TypeElement> types = Arrays.asList(TexteEditor.DEFAULT_TYPES);

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
					String text = TextParser.normalizeText(child.getTextContent().trim());
					if (!text.isEmpty()) {
						// Introduce current speaker
						if (!currentSpeakerIntroduced) {
							currentSpeakerIntroduced = true;
							elements.addLocuteurElement(currentSpeaker.getName());
						}

						elements.addAll(TextParser.parseString(text, types));
					}
				}

				// Anchor. Placed on the last character in the word *PRECEDING* the sync point
				else if (name.equals("Sync")) {
					elements.add(new Element_Ancre(
							Float.parseFloat(((Element) child).getAttribute("time"))));
				}

				else if (name.equals("Comment")) {
					elements.add(new Element_Commentaire(
							((Element)child).getAttribute("desc")));
				}

				// Ignore unknown tag
				else {
					System.out.println("TRS WARNING: Ignoring inknown tag " + name);
				}

				// Onto next Turn child
				child = child.getNextSibling();
			}
		}

		elements.add(new Element_Ancre(lastEnd));
	}


	/**
	 * Creates a mapping of TRS speaker IDs to speaker objects. This mapping is
	 * necessary because Locuteur_Info.id does not match TRS speaker IDs.
	 */
	private static Map<String, Locuteur_Info> loadSpeakers(Node speakersNode) {
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

	//==========================================================================
	// GETTERS
	//==========================================================================

	public ListeElement getElements() {
		return elements;
	}

	public String getFormat() {
		return "Transcriber (.TRS)";
	}
}