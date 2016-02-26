package fr.loria.synalp.jtrans.markup.in;

import fr.loria.synalp.jtrans.project.Anchor;
import fr.loria.synalp.jtrans.project.Token;
import fr.loria.synalp.jtrans.project.TurnProject;
import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for the TransICOR file format.
 */
public class TricoLoader extends TRSLoader {


    public static Pattern DTD_PATTERN =
	Pattern.compile("^.*(transicor[0-9a-z]*\\.dtd)$");


    public TurnProject parse(File file)
	throws ParsingException, IOException
    {
	System.out.println("TricoLoader parse");
	return parse(parseXML(file));
    }


    public TurnProject parse(Document doc) throws ParsingException {
	TurnProject project = new TurnProject();
	project.document = doc;

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

	// MQ : HACK for empty turns that still have a duration
	String trsID   = "spkHackNobody";
	int hackNobodyID = project.newSpeaker( TurnProject.hackNobody );
	spkIDMap.put(trsID, hackNobodyID);

	// Extract relevant information (speech text, Sync tags...) from Turn tags.
	NodeList turnList = doc.getElementsByTagName("Turn");


	for (int i = 0; i < turnList.getLength(); i++) {
	    Element turn = (Element)turnList.item(i);
	    Node child = turn.getFirstChild();

	    // Map IDs of speakers active in this turn to tracks
	    String speakerAttr = turn.getAttribute("speaker");
	    if (speakerAttr.isEmpty()) {
		//System.err.println("TRS WARNING: skipping turn without any speakers");
		// Si, on va quand même creer un turn
		// Et on va même ajouter un mot = "f" pour que le Turn ait une durée
		TurnProject.Turn pTurn = project.newTurn();
		List<Integer> turnTracks = new ArrayList<>();
		turnTracks.add(hackNobodyID);
		pTurn.add(hackNobodyID, new Token("f"));
		continue;
	    }

	    List<Integer> turnTracks = new ArrayList<>();
	    for (String turnSpeaker: speakerAttr.split(" "))
		turnTracks.add(spkIDMap.get(turnSpeaker));

	    // Start with the first speaker in case the first "Who" tag is missing
	    int spkID = turnTracks.get(0);

	    TurnProject.Turn pTurn = project.newTurn();

	    while (null != child) {
		String name = child.getNodeName();
		// Speech text
		if (name.equals("#text")) {
		    pTurn.addAll(spkID, RawTextLoader.tokenize(
							       RawTextLoader.normalizeText(child.getTextContent().trim()),
							       RawTextLoader.DEFAULT_PATTERNS));
		}

		// Change speakers in a multi-speaker turn
		else if (name.equals("Who")) {
		    // Speaker numbering starts at 1 in the XML file
		    int nb = Integer.parseInt(((Element)child).getAttribute("nb"))-1;
		    if (nb>=turnTracks.size()) {
			System.err.println("WARNING error in TRS file: undefined speakers "+nb+" "+turnTracks.size());
			nb=0;
		    }
		    spkID = turnTracks.get(nb);
		}

		else {
		    Token token = transformNode(child);
		    if (null != token) {
			pTurn.add(spkID, token);
		    }
		}

		// Onto next Turn child
		child = child.getNextSibling();
	    }
	}

	// @author Matthieu Quignard (MQ)
	// Adding minimal Anchors from Section 
	try {
	    NodeList sectionList = doc.getElementsByTagName("Section");
	    int lastSectionNumber = sectionList.getLength() - 1;

	    Element firstSection = (Element)sectionList.item( 0 );
	    float sectionStartTime = Float.parseFloat(firstSection.getAttribute("startTime"));

	    Element lastSection = (Element)sectionList.item( lastSectionNumber );
	    float sectionEndTime = Float.parseFloat(lastSection.getAttribute("endTime"));

	    int nTurns = project.turns.size();
	    TurnProject.Turn firstTurn = project.turns.get(0);
	    firstTurn.start=new Anchor(sectionStartTime);

	    TurnProject.Turn lastTurn = project.turns.get(nTurns - 1);
	    lastTurn.end=new Anchor(sectionEndTime);

	} catch (Exception e) {
	    System.err.println("Error while trying to extract timestamps from Section elements");
	    e.printStackTrace();
	}

	return project;
    }


    
	public static Document parseXML(File file)
			throws ParsingException, IOException
	{
	    System.err.println("TricoLoader parseXML");
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
				System.out.println("DTD = "+dtdName);
				InputStream dtd = TricoLoader.class.getResourceAsStream(dtdName);
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
		return "TransICOR";
	}


	public String getExt() {
		return ".trico";
	}

}
