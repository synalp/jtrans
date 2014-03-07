package fr.loria.synalp.jtrans.markup;

import fr.loria.synalp.jtrans.elements.*;
import fr.loria.synalp.jtrans.elements.Comment;
import fr.loria.synalp.jtrans.facade.Project;
import fr.loria.synalp.jtrans.facade.Track;
import org.w3c.dom.*;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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


	public Project parse(File file)
			throws ParsingException, IOException
	{
		return parse(parseXML(file));
	}


	public Project parse(Document doc) {
		Project project = new Project();

		// Map of Transcriber's speaker IDs to JTrans tracks
		Map<String, Track> trackIDMap = new HashMap<String, Track>();

		// end time of last turn
		float lastEnd = -1f;

		// Create speaker tracks and build ID map
		for (Node spk = doc.getElementsByTagName("Speakers").item(0).getFirstChild();
			 null != spk;
			 spk = spk.getNextSibling())
		{
			if (!spk.getNodeName().equals("Speaker"))
				continue;

			Element el = (Element)spk;
			String trsID   = el.getAttribute("id");
			String name    = el.getAttribute("name");

			Track newTrack = new Track(name);
			project.tracks.add(newTrack);
			trackIDMap.put(trsID, newTrack);
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

			List<Track> turnTracks = new ArrayList<Track>();
			for (String turnSpeaker: speakerAttr.split(" "))
				turnTracks.add(trackIDMap.get(turnSpeaker));

			// Start with the first speaker in case the first "Who" tag is missing
			Track currentTrack = turnTracks.get(0);

			// The same speaker may be repeated in the speaker attribute
			Set<Track> uniqueTurnTracks = new HashSet<Track>(turnTracks);

			float endTime = Float.parseFloat(turn.getAttribute("endTime"));
			if (endTime > lastEnd)
				lastEnd = endTime;

			while (null != child) {
				String name = child.getNodeName();

				// Speech text
				if (name.equals("#text")) {
					String text = RawTextLoader.normalizeText(child.getTextContent().trim());
					if (!text.isEmpty()) {
						currentTrack.elts.addAll(RawTextLoader.parseString(
								text,
								RawTextLoader.DEFAULT_PATTERNS));
					}
				}

				// Anchor. Placed on the last character in the word *PRECEDING* the sync point
				else if (name.equals("Sync")) {
					float time = Float.parseFloat(((Element)child).getAttribute("time"));
					for (Track t: uniqueTurnTracks)
						addUniqueAnchor(t, time);
				}

				// Change speakers in a multi-speaker turn
				else if (name.equals("Who")) {
					// Speaker numbering starts at 1 in the XML file
					int nb = Integer.parseInt(((Element)child).getAttribute("nb"));
					currentTrack = turnTracks.get(nb - 1);
				}

				else if (name.equals("Comment")) {
					currentTrack.elts.add(new Comment(
							((Element)child).getAttribute("desc"),
							Comment.Type.FREEFORM));
				}

				else if (name.equals("Event")) {
					currentTrack.elts.add(new Comment(
							((Element)child).getAttribute("desc"),
							Comment.Type.NOISE));
				}

				// Ignore unknown tag
				else {
					System.out.println("TRS WARNING: Ignoring inknown tag " + name);
				}

				// Onto next Turn child
				child = child.getNextSibling();
			}

			for (Track t: uniqueTurnTracks)
				addUniqueAnchor(t, endTime);
		}

		return project;
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

		return builder;
	}


	/**
	 * Adds an anchor to a track only if it hasn't been added to it already.
	 */
	private static void addUniqueAnchor(Track t, float seconds) {
		Anchor anchor = Anchor.timedAnchor(seconds);

		if (!t.elts.isEmpty()) {
			fr.loria.synalp.jtrans.elements.Element lastEl = t.elts.get(t.elts.size()-1);
			if (anchor.equals(lastEl))
				return;
		}

		t.elts.add(anchor);
	}


	public String getFormat() {
		return "Transcriber (.TRS)";
	}
}
