package fr.loria.synalp.jtrans.markup.in;

import fr.loria.synalp.jtrans.elements.*;
import fr.loria.synalp.jtrans.facade.Project;
import fr.loria.synalp.jtrans.facade.Track;
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
				String line = normedText.substring(prevEnd, start);
				parserListeMot(line, prevEnd, elList, normedText);
			}

			// Create the actual element
			String sub = normedText.substring(start, end).trim();
			elList.add(new Comment(sub, seg.type));

			prevEnd = end;
		}

		// Line after the last element
		if (normedText.length() > prevEnd) {
			String line = normedText.substring(prevEnd);
			parserListeMot(line, prevEnd, elList, normedText);
		}

		return elList;
	}


	private static void parserListeMot(String ligne, int precfin, List<Element> listeElts, String text) {
		int index = 0;
		int debutMot;
		//on parcourt toute la ligne
		while(index < ligne.length()){

			//on saute les espaces
			while(index < ligne.length() &&
					Character.isWhitespace(ligne.charAt(index))){
				index++;
			}

			debutMot =  index;
			//on avance jusqu'au prochain espace

			while((index < ligne.length()) && (!Character.isWhitespace(ligne.charAt(index)))){
				index++;
			}

			if (index > debutMot){
				listeElts.add(new Word(text.substring(debutMot + precfin, index + precfin)));
			}
		}
	}


	public RawTextLoader(Map<Comment.Type, String> commentPatterns) {
		this.commentPatterns = commentPatterns;
	}


	public RawTextLoader() {
		this(DEFAULT_PATTERNS);
	}


	@Override
	public Project parse(File file) throws ParsingException, IOException {
		Project project = new Project();
		BufferedReader reader = FileUtils.openFileAutoCharset(file);

		int order = 0;
		boolean ongoingOverlap = false;

		// Add default speaker
		Track currentTrack = new Track("Unknown");
		project.tracks.add(currentTrack);
		currentTrack.elts.add(Anchor.orderedTimelessAnchor(order));

		Map<String, Track> trackMap = new HashMap<String, Track>();

		for (int lineNo = 1; true; lineNo++) {
			String line = reader.readLine();
			if (line == null)
				break;
			line = normalizeText(line).trim();

			for (Element el: parseString(line, commentPatterns)) {
				Comment.Type ctype = el instanceof Comment?
						((Comment) el).getType(): null;

				if (ctype == Comment.Type.SPEAKER_MARK) {
					/*
					Cordon off current speaker's speech by adding an ordered
					anchor in the current track. Warning: If an overlap is
					ongoing, `order` is the order ID that *starts* the overlap.
					*/
					currentTrack.elts.add(Anchor.orderedTimelessAnchor(
							ongoingOverlap? order+1: order));

					String speaker = el.toString().trim();
					currentTrack = trackMap.get(speaker);
					if (currentTrack == null) {
						currentTrack = new Track(speaker);
						project.tracks.add(currentTrack);
						trackMap.put(speaker, currentTrack);
					}
					currentTrack.elts.add(Anchor.orderedTimelessAnchor(order));
					order++;
				}

				else if (ctype == Comment.Type.OVERLAP_START_MARK) {
					if (ongoingOverlap) {
						throw new ParsingException(lineNo, line,
								"An overlap is already ongoing");
					}

					currentTrack.elts.add(Anchor.orderedTimelessAnchor(order));
					currentTrack.elts.add(el);
					ongoingOverlap = true;
					// Don't increment order; next SPEAKER_MARK will use the
					// current order and appear simultaneously
				}

				else if (ctype == Comment.Type.OVERLAP_END_MARK) {
					if (!ongoingOverlap) {
						throw new ParsingException(lineNo, line,
								"Trying to end non-existant overlap");
					}
					currentTrack.elts.add(el);
					currentTrack.elts.add(Anchor.orderedTimelessAnchor(order));
					order++;
					ongoingOverlap = false;
				}

				else {
					currentTrack.elts.add(el);
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
