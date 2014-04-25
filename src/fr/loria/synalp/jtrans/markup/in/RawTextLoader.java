package fr.loria.synalp.jtrans.markup.in;

import fr.loria.synalp.jtrans.elements.*;
import fr.loria.synalp.jtrans.project.TurnProject;
import fr.loria.synalp.jtrans.project.Project;
import fr.loria.synalp.jtrans.utils.FileUtils;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Parser for raw transcription text.
 */
public class RawTextLoader implements MarkupLoader {
	/**
	 * Substitute junk characters with ones that JTrans can handle.
	 */
	public static String normalizeText(String text) {
		return text
				.replace('\u2019', '\'')            // smart quotes
				.replace("\r\n", "\n")              // Windows CRLF
				.replace('\r', '\n')                // remaining non-Unix linebreaks
				.replace('\u00a0', ' ')             // non-breaking spaces
				.replaceAll("[\"=/]", " ")          // junk punctuation marks
				.replaceAll("\'(\\S)", "\' $1")     // add space after apostrophes glued to a word
		;
	}


	public static final Map<Comment.Type, String> DEFAULT_PATTERNS =
			new HashMap<Comment.Type, String>()
	{{
		put(Comment.Type.FREEFORM,     "(\\{[^\\}]*\\}|\\[[^\\]]*\\]|\\+)");
		put(Comment.Type.NOISE,        "\\*+");
		put(Comment.Type.OVERLAP_START_MARK, "<");
		put(Comment.Type.OVERLAP_END_MARK,   ">");
		put(Comment.Type.PUNCTUATION,  "(\\?|\\:|\\;|\\,|\\.|\\!)");
		put(Comment.Type.BEEP,         "\\*[^\\*\\s]+\\*");
		put(Comment.Type.SPEAKER_MARK, "(^|\\n)(\\s)*L\\d+\\s");
	}};


	private Map<Comment.Type, String> commentPatterns;


	/**
	 * Creates elements from a string according to the regular expressions
	 * defined in commentPatterns.
	 */
	public static List<Element> parseString(
			String normedText,
			Map<Comment.Type, String> commentPatterns)
	{
		class NonTextSegment implements Comparable<NonTextSegment> {
			public int start, end;
			public Comment.Type type;

			public NonTextSegment(int start, int end, Comment.Type type) {
				this.start = start;
				this.end = end;
				this.type = type;
			}

			public int compareTo(NonTextSegment other) {
				if (start > other.start) return 1;
				if (start < other.start) return -1;
				return 0;
			}
		}

		List<Element> elList = new ArrayList<Element>();
		ArrayList<NonTextSegment> nonText = new ArrayList<NonTextSegment>();


		for (Map.Entry<Comment.Type, String> entry: commentPatterns.entrySet()) {
			Pattern pat = Pattern.compile(entry.getValue());
			Matcher mat = pat.matcher(normedText);
			while (mat.find())
				nonText.add(new NonTextSegment(mat.start(), mat.end(), entry.getKey()));
		}
		Collections.sort(nonText);

		// Turn the non-text segments into Elements
		int prevEnd = 0;
		for (NonTextSegment seg: nonText) {
			int start = seg.start;
			int end = seg.end;
			if (prevEnd > start) {
				//cas entrecroisé : {-----------[---}-------]
				//on deplace de façon à avoir : {--------------}[-------]
				if (end > prevEnd) start = prevEnd;

				//cas imbriqué : {------[---]----------}
				//on ne parse pas l'imbriqué
				else continue;
			}

			// Line right before
			if (start > prevEnd) {
				elList.addAll(parseWords(normedText.substring(prevEnd, start)));
			}

			// Create the actual element
			String sub = normedText.substring(start, end).trim();
			elList.add(new Comment(sub, seg.type));

			prevEnd = end;
		}

		// Line after the last element
		if (normedText.length() > prevEnd) {
			elList.addAll(parseWords(normedText.substring(prevEnd)));
		}

		return elList;
	}


	private static List<Word> parseWords(String text) {
		List<Word> list = new ArrayList<>();
		for (String w: text.trim().split("\\s+")) {
			list.add(new Word(w));
		}
		return list;
	}


	public RawTextLoader(Map<Comment.Type, String> commentPatterns) {
		this.commentPatterns = commentPatterns;
	}


	public RawTextLoader() {
		this(DEFAULT_PATTERNS);
	}


	@Override
	public Project parse(File file) throws ParsingException, IOException {
		TurnProject project = new TurnProject();
		BufferedReader reader = FileUtils.openFileAutoCharset(file);

		boolean ongoingOverlap = false;

		// Add default speaker
		int spkID = project.newSpeaker("Unknown");
		TurnProject.Turn turn = project.newTurn();

		Map<String, Integer> spkIDMap = new HashMap<>();

		for (int lineNo = 1; true; lineNo++) {
			String line = reader.readLine();
			if (line == null)
				break;
			line = normalizeText(line).trim();

			for (Element el: parseString(line, commentPatterns)) {
				Comment.Type ctype = el instanceof Comment?
						((Comment) el).getType(): null;

				if (ctype == Comment.Type.SPEAKER_MARK) {
					turn = project.newTurn();

					String spkName = el.toString().trim();
					if (!spkIDMap.containsKey(spkName)) {
						spkID = project.newSpeaker(spkName);
						spkIDMap.put(spkName, spkID);
					} else {
						spkID = spkIDMap.get(spkName);
					}
				}

				else if (ctype == Comment.Type.OVERLAP_START_MARK) {
					if (ongoingOverlap) {
						throw new ParsingException(lineNo, line,
								"An overlap is already ongoing");
					}

					turn = project.newTurn();
					ongoingOverlap = true;
				}

				else if (ctype == Comment.Type.OVERLAP_END_MARK) {
					if (!ongoingOverlap) {
						throw new ParsingException(lineNo, line,
								"Trying to end non-existant overlap");
					}

					turn = project.newTurn();
					ongoingOverlap = false;
				}

				else {
					turn.add(spkID, el);
				}
			}
		}

		reader.close();

		return project;
	}


	@Override
	public String getFormat() {
		return "Raw Text";
	}


	@Override
	public String getExt() {
		return ".txt";
	}

}
