package fr.loria.synalp.jtrans.markup.in;

import fr.loria.synalp.jtrans.elements.*;
import fr.loria.synalp.jtrans.facade.Project;
import fr.loria.synalp.jtrans.facade.Track;
import fr.loria.synalp.jtrans.utils.FileUtils;

import java.io.*;
import java.util.*;
import java.util.regex.*;


/**
 * Parser for the TextGrid file format.
 */
public class TextGridLoader implements MarkupLoader {
	public Project parse(File file)
			throws ParsingException, IOException
	{
		Project project = new Project();
		BufferedReader reader = FileUtils.openFileAutoCharset(file);
		TextGridStateMachine machine = new TextGridStateMachine(reader);
		reader.close();

		for (int i = 0; i < machine.tiers.size(); i++) {
			Track track = new Track(machine.tierNames.get(i));
			float prevXmax = -1;

			for (TextGridStateMachine.Interval interval: machine.tiers.get(i)) {
				// Avoid redundant anchors - only create start anchor if
				// different from previous anchor

				if (prevXmax != interval.xmin) {
					track.elts.add(Anchor.timedAnchor(interval.xmin));
				}

				String normalized = RawTextLoader.normalizeText(interval.text);
				track.elts.addAll(RawTextLoader.parseString(
						normalized, RawTextLoader.DEFAULT_PATTERNS));

				track.elts.add(Anchor.timedAnchor(interval.xmax));

				prevXmax = interval.xmax;
			}

			project.tracks.add(track);
		}

		return project;
	}


	public String getFormat() {
		return "Praat TextGrid";
	}


	public String getExt() {
		return ".textgrid";
	}

}


class TextGridStateMachine {
	List<List<Interval>> tiers = new ArrayList<List<Interval>>();
	List<String> tierNames = new ArrayList<String>();


	private static enum State {
		FILE_HEADER_1,
		FILE_HEADER_2,
		FILE_DESCRIPTION,
		TIER_HEADER,
		TIER_DESCRIPTION,
		INTERVAL_HEADER,
		INTERVAL_DESCRIPTION,
		DONE
	}


	private static final String
			HALFPAT_STR   = "\\s*=\\s*\"(.*)\"",
			HALFPAT_FLOAT = "\\s*=\\s*(\\d+(|\\.\\d*))",
			HALFPAT_INT   = "\\s*=\\s*(\\d+)",
			HALFPAT_INDEX = "\\s*\\[\\s*(\\d+)\\s*\\]\\s*:";


	private static final Pattern
			PAT_SIZE = Pattern.compile("size" + HALFPAT_INT),
			PAT_ITEM = Pattern.compile("item" + HALFPAT_INDEX),
			PAT_NAME = Pattern.compile("name" + HALFPAT_STR, Pattern.DOTALL),
			PAT_TEXT = Pattern.compile("text" + HALFPAT_STR, Pattern.DOTALL),
			PAT_XMIN = Pattern.compile("xmin" + HALFPAT_FLOAT),
			PAT_XMAX = Pattern.compile("xmax" + HALFPAT_FLOAT),
			PAT_INTERVAL_COUNT = Pattern.compile("intervals: size" + HALFPAT_INT),
			PAT_INTERVAL_INDEX = Pattern.compile("intervals" + HALFPAT_INDEX);


	class Interval {
		float xmin = -1;
		float xmax = -1;
		String text = null;

		boolean isComplete() {
			return xmin >= 0 && xmax >= 0 && text != null;
		}

		boolean findMatch(String line) {
			Matcher m;

			m = PAT_TEXT.matcher(line);
			if (m.matches()) {
				text = m.group(1).trim();
				return true;
			}

			m = PAT_XMIN.matcher(line);
			if (m.matches()) {
				xmin = Float.parseFloat(m.group(1));
				return true;
			}

			m = PAT_XMAX.matcher(line);
			if (m.matches()) {
				xmax = Float.parseFloat(m.group(1));
				return true;
			}

			return false;
		}
	}


	TextGridStateMachine(BufferedReader reader)
			throws IOException, ParsingException
	{
		State state = State.FILE_HEADER_1;
		int lineNumber = 1;
		Interval currentInterval = new Interval();
		List<Interval> currentTier = null;
		int remainingIntervals = -1;

		while (state != State.DONE) {
			// Read a 'line' that may span multiple \n's within quotes
			StringBuilder lineSB = new StringBuilder();
			boolean inQuotes = false;
			int c;
			while (-1 != (c = reader.read())) {
				if (c == '\n')
					lineNumber++;

				if (!inQuotes) {
					if (c == '"')
						inQuotes = true;
					else if (c == '\n')
						break;
				} else {
					if (c == '"')
						inQuotes = false;
				}

				lineSB.append((char)c);
			}

			if (c == -1) {
				break;
			}
			String line = lineSB.toString();
			line = line.trim();
			String lcline = line.toLowerCase();
			
			switch (state) {
				case FILE_HEADER_1:
					if (!lcline.equals("file type = \"ootextfile\""))
						throw new ParsingException(lineNumber, line, "not an ooTextFile?");
					state = State.FILE_HEADER_2;
					break;

				case FILE_HEADER_2:
					if (!lcline.equals("object class = \"textgrid\""))
						throw new ParsingException(lineNumber, line, "not a TextGrid?");
					state = State.FILE_DESCRIPTION;
					break;
				
				case FILE_DESCRIPTION:
					if (lcline.equals("item []:")) {
						state = State.TIER_HEADER;
					} else if (line.isEmpty() ||
							PAT_XMIN.matcher(lcline).matches() ||
							PAT_XMAX.matcher(lcline).matches() ||
							PAT_SIZE.matcher(lcline).matches() ||
							lcline.equals("tiers? <exists>")) {
						;
					} else {
						throw new ParsingException(lineNumber, line,
								"unknown header line");
					}
					break;
				
				case TIER_HEADER:
					if (PAT_ITEM.matcher(lcline).matches()) {
						currentTier = new ArrayList<Interval>();
						tiers.add(currentTier);
						state = State.TIER_DESCRIPTION;
					} else {
						throw new ParsingException(lineNumber, line, "expecting tier header here");
					}
					break;
				
				case TIER_DESCRIPTION: {
					Matcher m;

					m = PAT_NAME.matcher(line);
					if (m.matches()) {
						tierNames.add(m.group(1));
						break;
					}

					m = PAT_INTERVAL_COUNT.matcher(line);
					if (m.matches()) {
						remainingIntervals = Integer.parseInt(m.group(1));
						state = State.INTERVAL_HEADER;
						break;
					}

					if (PAT_XMIN.matcher(lcline).matches() ||
							PAT_XMAX.matcher(lcline).matches() ||
							lcline.equals("class = \"intervaltier\"")) {
						;
					} else {
						throw new ParsingException(lineNumber, line, "unknown tier description line");
					}
					break;
				}

				case INTERVAL_HEADER:
					if (!PAT_INTERVAL_INDEX.matcher(lcline).matches()) {
						throw new ParsingException(lineNumber, line, "interval definition expected here");
					}
					state = State.INTERVAL_DESCRIPTION;
					remainingIntervals--;
					if (remainingIntervals < 0) {
						throw new ParsingException(lineNumber, line, "too many intervals");
					}
					break;

				case INTERVAL_DESCRIPTION:
					if (currentInterval.findMatch(line)) {
						if (currentInterval.isComplete()) {
							if (!currentInterval.text.isEmpty() &&
									currentInterval.xmin < currentInterval.xmax) {
								currentTier.add(currentInterval);
							}
							currentInterval = new Interval();
							if (remainingIntervals == 0)
								state = State.TIER_HEADER;
							else
								state = State.INTERVAL_HEADER;
						}
					} else {
						throw new ParsingException(lineNumber, line, "unknown interval description line");
					}
					break;

				case DONE:
					break;
				default:
					throw new ParsingException("unknown state " + state);
			}
		}
	}
}
