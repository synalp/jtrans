package fr.loria.synalp.jtrans.markup.in;

import fr.loria.synalp.jtrans.project.*;
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
				.replace('-', ' ')					// delete all '-'; TODO: keep the dash attached to the second word ? keep special words attached "week-end" ?
		;
	}


	public static final Pattern ANONYMOUS_WORD_PATTERN =
			Pattern.compile("\\*([^\\*\\s]+)\\*");


	public static final Map<Token.Type, String> DEFAULT_PATTERNS =
			new HashMap<Token.Type, String>()
	{{
		put(Token.Type.COMMENT,            "(\\{[^\\}]*\\}|\\[[^\\]]*\\]|\\+)");
		put(Token.Type.NOISE,              "XXX");
		put(Token.Type.OVERLAP_START_MARK, "<");
		put(Token.Type.OVERLAP_END_MARK,   ">");
		put(Token.Type.PUNCTUATION,        "(\\?|\\:|\\;|\\,|\\.|\\!)");
		put(Token.Type.SPEAKER_MARK,       "(^|\\n)(\\s)*L\\d+\\s");
	}};


	private Map<Token.Type, String> commentPatterns;


	/**
	 * Creates tokens from a string according to the regular expressions
	 * defined in commentPatterns.
	 */
	public static List<Token> tokenize(
			String normedText,
			Map<Token.Type, String> commentPatterns)
	{
		class NonTextSegment implements Comparable<NonTextSegment> {
			public int start, end;
			public Token.Type type;

			public NonTextSegment(int start, int end, Token.Type type) {
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

		List<Token> elList = new ArrayList<>();
		ArrayList<NonTextSegment> nonText0 = new ArrayList<NonTextSegment>();

		for (Map.Entry<Token.Type, String> entry: commentPatterns.entrySet()) {
			Pattern pat = Pattern.compile(entry.getValue());
			Matcher mat = pat.matcher(normedText);
			while (mat.find())
				nonText0.add(new NonTextSegment(mat.start(), mat.end(), entry.getKey()));
		}

		// remove nontext segments that are inside an anonymous segment
		ArrayList<NonTextSegment> nonText = new ArrayList<NonTextSegment>();
		// get anonymous limits
		ArrayList<int[]> anonlimits = new ArrayList<int[]>();
		int i=0;
		boolean inAnon=false;
		int deb=0;
		for (;;) {
			int j=normedText.indexOf('*',i);
			if (j<0) break;
			if (inAnon) {
				int[] ll = {deb,j};
				anonlimits.add(ll);
				inAnon=false;
			} else {
				deb=j+1;
				inAnon=true;
			}
			i=j+1;
		}
		if (inAnon) {
			System.out.println("ERROR tracking anonymous segments0 "+normedText);
		}
		for (NonTextSegment seg : nonText0) {
			boolean isin=false;
			for (int[] aseg : anonlimits) {
				if (seg.start>=aseg[0]&&seg.start<aseg[1]) {isin=true; break;}
				if (seg.end>=aseg[0]&&seg.end<aseg[1]) {isin=true; break;}
			}
			if (!isin) nonText.add(seg);
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
			elList.add(new Token(sub, seg.type));

			prevEnd = end;
		}

		// Line after the last element
		if (normedText.length() > prevEnd) {
			elList.addAll(parseWords(normedText.substring(prevEnd)));
		}

		return elList;
	}

	private static List<Token> parseWords(String text) {
		List<Token> list = new ArrayList<>();
		
		// get anonymous limits
		ArrayList<int[]> anonlimits = new ArrayList<int[]>();
		int i=0;
		boolean inAnon=false;
		int deb=0;
		for (;;) {
			int j=text.indexOf('*',i);
			if (j<0) break;
			if (inAnon) {
				int[] ll = {deb,j};
				anonlimits.add(ll);
				inAnon=false;
			} else {
				deb=j+1;
				inAnon=true;
			}
			i=j+1;
		}
		if (inAnon) {
			System.out.println("ERROR tracking anonymous segments "+text);
		}

		deb=0;
		final int fin=text.length();
		for (i=0;i<anonlimits.size();i++) {
			if (anonlimits.get(i)[0]-deb>1) { // les limites sont apres l'etoile. -1 pour eviter l'etoile
				for (String w: text.substring(0,anonlimits.get(i)[0]-1).trim().split("\\s+")) {
					w=w.trim();
					if (w.length()>0) {
						Token word = new Token(w);
						list.add(word);
					}
				}
			}
			{
				String w = text.substring(anonlimits.get(i)[0], anonlimits.get(i)[1]).trim();
				if (w.length()>0) {
					Token word = new Token(w);
					word.setAnonymize(true);
					list.add(word);
				}
			}
			deb=anonlimits.get(i)[1]+1;
		}
		// treat what remains:
		for (String w: text.substring(deb,fin).trim().split("\\s+")) {
			w=w.trim();
			if (w.length()>0) {
				Token word = new Token(w);
				list.add(word);
			}
		}
		
		return list;
	}


	public RawTextLoader(Map<Token.Type, String> commentPatterns) {
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

			for (Token token: tokenize(line, commentPatterns)) {
				Token.Type type = token.getType();

				if (type == Token.Type.SPEAKER_MARK) {
					turn = project.newTurn();

					String spkName = token.toString().trim();
					if (!spkIDMap.containsKey(spkName)) {
						spkID = project.newSpeaker(spkName);
						spkIDMap.put(spkName, spkID);
					} else {
						spkID = spkIDMap.get(spkName);
					}
				}

				else if (type == Token.Type.OVERLAP_START_MARK) {
					if (ongoingOverlap) {
						throw new ParsingException(lineNo, line,
								"An overlap is already ongoing");
					}

					turn = project.newTurn();
					ongoingOverlap = true;
				}

				else if (type == Token.Type.OVERLAP_END_MARK) {
					if (!ongoingOverlap) {
						throw new ParsingException(lineNo, line,
								"Trying to end non-existant overlap");
					}

					turn = project.newTurn();
					ongoingOverlap = false;
				}

				else {
					turn.add(spkID, token);
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
