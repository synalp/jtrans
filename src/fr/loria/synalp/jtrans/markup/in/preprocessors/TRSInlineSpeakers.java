package fr.loria.synalp.jtrans.markup.in.preprocessors;

import fr.loria.synalp.jtrans.markup.in.ParsingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TRSInlineSpeakers extends TRSPreprocessor {

	public final static Pattern INLINE_SPEAKER_PATTERN =
			Pattern.compile("^\\s*(L[0-9])(|\\s+.*)$",
					Pattern.MULTILINE | Pattern.DOTALL);


	public static void main(String[] args) throws Exception {
		new TRSInlineSpeakers().transform(args);
	}


	@Override
	public String getFormat() {
		return "Transcriber (TCOF conventions, no formal speaker turns)";
	}


	public void preprocess(Document doc) throws ParsingException {
		Set<String> speakers = new HashSet<String>();
		String currentSpeaker = null;
		Element currentSyntheticTurn = null;
		String lastSyncTime = null;
		Element speakerMetadataTag;

		//----------------------------------------------------------------------
		// Create speaker metadata tag

		if (0 != doc.getElementsByTagName("Speakers").getLength())
			throw new ParsingException("Speaker metadata already specified");
		speakerMetadataTag = doc.createElement("Speakers");
		Node epTag = doc.getElementsByTagName("Episode").item(0);
		epTag.getParentNode().insertBefore(speakerMetadataTag, epTag);

		//----------------------------------------------------------------------
		// Modify turns

		if (1 != doc.getElementsByTagName("Turn").getLength())
			throw new ParsingException("Can only preprocess a single giant Turn");

		Element turn = (Element)doc.getElementsByTagName("Turn").item(0);
		Node turnContainer = turn.getParentNode();
		turnContainer.removeChild(turn);

		for (Node child = turn.getFirstChild();
			 null != child;
			 child = child.getNextSibling())
		{
			final String name = child.getNodeName();
			final String text = child.getTextContent().trim();

			if (name.equals("Sync")) {
				lastSyncTime = ((Element)child).getAttribute("time");
			}

			else if (name.equals("#text")) {
				if (text.isEmpty())
					continue;

				Matcher m = INLINE_SPEAKER_PATTERN.matcher(text);
				if (m.matches()) {
					String newSpeaker = m.group(1);

					if (!speakers.contains(newSpeaker)) {
						speakers.add(newSpeaker);
						Element speakerTag = doc.createElement("Speaker");
						speakerTag.setAttribute("id", newSpeaker);
						speakerTag.setAttribute("name", newSpeaker);
						speakerTag.setAttribute("scope", "local");
						speakerMetadataTag.appendChild(speakerTag);
					}

					if (!newSpeaker.equals(currentSpeaker)) {
						currentSpeaker = newSpeaker;

						if (lastSyncTime == null) {
							throw new ParsingException(
									"Missing Sync before new speaker turn");
						}

						if (currentSyntheticTurn != null) {
							currentSyntheticTurn.setAttribute("endTime", lastSyncTime);
							turnContainer.appendChild(currentSyntheticTurn);
						}

						currentSyntheticTurn = doc.createElement("Turn");
						currentSyntheticTurn.setAttribute("startTime", lastSyncTime);
						currentSyntheticTurn.setAttribute("speaker", newSpeaker);

						// Consume sync time
						lastSyncTime = null;

						String remainder = m.group(2).trim();
						if (!remainder.isEmpty()) {
							currentSyntheticTurn.appendChild(
									doc.createTextNode(remainder));
						}

						// Don't add child a second time
						continue;
					}
				}
			}

			if (currentSyntheticTurn == null) {
				System.err.println("WARNING: an element named " + name
						+ " was lost because no speaker is defined yet");
			} else {
				currentSyntheticTurn.appendChild(child.cloneNode(true));
			}
		}

		// Add who tags
		new TCOFWhoifier().preprocess(doc);
	}

}
