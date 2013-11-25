package markup;

import facade.Project;
import org.w3c.dom.*;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import plugins.text.elements.*;

import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

/**
 * Parser for the Transcriber file format.
 */
public class TRSLoader implements MarkupLoader {

	public Project parse(File file)
			throws ParsingException, IOException
	{
		Project project = new Project();
		Document doc;

		// Map of Transcriber's speaker IDs to Locuteur_Info objects
		// (Locuteur_Info's IDs have nothing to do with Transcriber's)
		Map<String, Locuteur_Info> speakerIDMap = new HashMap<String, Locuteur_Info>();

		try {
			doc = newXMLDocumentBuilder().parse(file);
		} catch (ParserConfigurationException ex) {
			ex.printStackTrace();
			throw new ParsingException(ex.toString());
		} catch (SAXException ex) {
			throw new ParsingException(ex.toString());
		}

		// end time of last turn
		float lastEnd = -1f;

		// Add speakers and build speaker ID map
		for (Node spk = doc.getElementsByTagName("Speakers").item(0).getFirstChild();
			 null != spk;
			 spk = spk.getNextSibling())
		{
			if (!spk.getNodeName().equals("Speaker"))
				continue;

			Element el = (Element)spk;
			String trsID   = el.getAttribute("id");
			String name    = el.getAttribute("name");
			boolean check  = el.getAttribute("check").toLowerCase().equals("yes");
			boolean type   = el.getAttribute("type").toLowerCase().equals("female");
			String dialect = el.getAttribute("dialect");
			String accent  = el.getAttribute("accent");
			String scope   = el.getAttribute("scope");

			byte internalID = (byte)project.speakers.size();
			Locuteur_Info newSpeaker = new Locuteur_Info(
					internalID, name, check, type, dialect, accent, scope);
			project.speakers.add(newSpeaker);
			speakerIDMap.put(trsID, newSpeaker);
		}

		// Extract relevant information (speech text, Sync tags...) from Turn tags.
		NodeList turnList = doc.getElementsByTagName("Turn");
		for (int i = 0; i < turnList.getLength(); i++) {
			Element turn = (Element)turnList.item(i);
			Node child = turn.getFirstChild();
			Locuteur_Info currentSpeaker = speakerIDMap.get(turn.getAttribute("speaker"));
			boolean currentSpeakerIntroduced = false;

			float endTime = Float.parseFloat(turn.getAttribute("endTime"));
			if (endTime > lastEnd)
				lastEnd = endTime;

			while (null != child) {
				String name = child.getNodeName();

				// Speech text
				if (name.equals("#text")) {
					String text = RawTextLoader.normalizeText(child.getTextContent().trim());
					if (!text.isEmpty()) {
						// Introduce current speaker
						if (!currentSpeakerIntroduced) {
							currentSpeakerIntroduced = true;
							project.elts.add(new Element_Locuteur(currentSpeaker));
						}

						project.elts.addAll(RawTextLoader.parseString(text, project.types));
					}
				}

				// Anchor. Placed on the last character in the word *PRECEDING* the sync point
				else if (name.equals("Sync")) {
					project.elts.add(new Element_Ancre(
							Float.parseFloat(((Element) child).getAttribute("time"))));
				}

				else if (name.equals("Comment")) {
					project.elts.add(new Element_Commentaire(
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

		project.elts.add(new Element_Ancre(lastEnd));
		return project;
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


	public String getFormat() {
		return "Transcriber (.TRS)";
	}
}
