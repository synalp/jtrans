package fr.loria.synalp.jtrans.markup.in;

import fr.loria.synalp.jtrans.elements.*;
import fr.loria.synalp.jtrans.elements.Comment;
import fr.loria.synalp.jtrans.facade.Project;
import fr.loria.synalp.jtrans.facade.Track;
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


	public Project parse(File file)
			throws ParsingException, IOException
	{
		return parse(parseXML(file));
	}


	public Project parse(Document doc) throws ParsingException {
		Project project = new Project();

		// Map of Transcriber's speaker IDs to JTrans tracks
		Map<String, Track> trackIDMap = new HashMap<String, Track>();

		// Last sync times per track (to detect unordered sync times)
		Map<Track, Float> lastSyncTimes = new HashMap<>();

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

			String orderedAnchorInWhoTag = null;

			while (null != child) {
				String name = child.getNodeName();

				// Speech text
				if (name.equals("#text")) {
					String text = RawTextLoader.normalizeText(child.getTextContent().trim());
					if (!text.isEmpty()) {
						currentTrack.elts.addAll(RawTextLoader.parseString(
								text,
								RawTextLoader.DEFAULT_PATTERNS));
						if (orderedAnchorInWhoTag != null && orderedAnchorInWhoTag.equals("chevron")) {
							for (int j = currentTrack.elts.size()-1; j >= 0; j--) {
								fr.loria.synalp.jtrans.elements.Element el =
										currentTrack.elts.get(j);

								if (el instanceof Comment && ((Comment) el).getType() == Comment.Type.OVERLAP_END_MARK) {
									currentTrack.elts.add(j+1, Anchor.orderedTimelessAnchor(0));
									System.out.println("Adding in " + currentTrack);
									orderedAnchorInWhoTag = null;
									break;
								}
							}
						}
					}
				}

				// Anchor
				else if (name.equals("Sync")) {
					float time = Float.parseFloat(((Element) child).getAttribute("time"));

					if (time > endTime) {
						throw new ParsingException(String.format("TRS error: " +
								"Sync time (%f) exceeds Turn endTime (%f)!",
								time, endTime));
					}

					for (Track t: uniqueTurnTracks) {
						Float previousTime = lastSyncTimes.get(t);
						if (null != previousTime && previousTime > time) {
							throw new ParsingException(String.format(
									"TRS error: Sync times in non-" +
									"chronological order! (%f after %f)",
									previousTime, time));
						}

						addUniqueAnchor(t, time);
						lastSyncTimes.put(t, time);
					}
				}

				// Change speakers in a multi-speaker turn
				else if (name.equals("Who")) {
					if (orderedAnchorInWhoTag != null) {
						if (orderedAnchorInWhoTag.equals("end")) {
							currentTrack.elts.add(Anchor.orderedTimelessAnchor(0));
						}
					}

					// Speaker numbering starts at 1 in the XML file
					int nb = Integer.parseInt(((Element)child).getAttribute("nb"));
					currentTrack = turnTracks.get(nb - 1);

					orderedAnchorInWhoTag = ((Element)child).getAttribute("JTransOrderedAnchor");
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

		for (int i = 0; i < project.tracks.size(); i++) {
			project.tracks.get(i).setSpeakerOnWords(i);
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


	public String getExt() {
		return ".trs";
	}

}
