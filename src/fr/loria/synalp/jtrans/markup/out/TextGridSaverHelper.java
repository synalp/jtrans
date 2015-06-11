package fr.loria.synalp.jtrans.markup.out;

import fr.loria.synalp.jtrans.graph.StatePool;
import fr.loria.synalp.jtrans.project.*;
import fr.loria.synalp.jtrans.utils.FileUtils;

import java.io.*;
import java.util.Iterator;

import static fr.loria.synalp.jtrans.utils.FileUtils.getUTF8Writer;
import static fr.loria.synalp.jtrans.speechreco.s4.S4mfccBuffer.frame2second;

public class TextGridSaverHelper {

	/**
	 * If true, anonymized words will be replaced with "*ANON*" and their
	 * phones will not be shown.
	 */
	public static enum docensort {withNPs, anonymous, both};
	public static docensort censorAnonWords = docensort.both;

	protected TextGridSaverHelper() {
		// Don't let anyone but subclasses instantiate
	}

	public static void savePraat(Project p,File f,boolean withWords,boolean withPhons) throws IOException {
		if (censorAnonWords==docensort.both) {
			String ext="";
			String s=f.getAbsolutePath();
			int i=s.lastIndexOf('.');
			if (i>=0) ext=s.substring(i);
			File fanon = new File(FileUtils.noExt(s)+"_anon"+ext);
			savePraat(true, p, fanon, withWords, withPhons);
			savePraat(false, p, f, withWords, withPhons);
		} else if (censorAnonWords==docensort.withNPs) {
			savePraat(false, p, f, withWords, withPhons);
		} else {
			savePraat(true,p, f, withWords, withPhons);
		}
	}
	private static void savePraat(
			boolean anonymous,
			Project p,
			File f,
			boolean withWords,
			boolean withPhons)
			throws IOException
	{
		Writer w = getUTF8Writer(f);

		final int frameCount = (int) p.audioSourceTotalFrames;

		praatFileHeader(w,
				frameCount,
				p.speakerCount() * ((withWords?1:0) + (withPhons?1:0)));

		int id = 1;
		for (int i = 0; i < p.speakerCount(); i++) {
			StringBuilder wordSB = new StringBuilder();
			StringBuilder phoneSB = new StringBuilder();

			int wordCount = 0;
			int phoneCount = 0;

			// frame onto which to tack 0-length elements
			int lastFrame = 0;

			int textidStard2add = -1; String textid2add = "BUG";

			Iterator<Phrase> itr = p.phraseIterator(i);
			while (itr.hasNext()) {
				Phrase phrase = itr.next();

                // frame onto which to tack 0-length elements
				if (phrase.getInitialAnchor() != null) {
					lastFrame = phrase.getInitialAnchor().getFrame();
				}

				for (Token token: phrase) {
					if (token.isAlignable() && token.isAligned()) {
						boolean censored = anonymous && token.shouldBeAnonymized();
						String tok = token.shouldBeAnonymized()?"*"+token.toString()+"*":token.toString();

                        int startWfr = token.getFirstNonSilenceFrame();
                        int endWfr = token.getLastNonSilenceFrame();
                        lastFrame = token.getSegment().getEndFrame();
						if (startWfr<0) {
                            // ce n'est que un SIL: j'ajoute un mot pour le SIL
                            // et bien non, en fait, j'ai decide de ne pas faire apparaitre du tout les SIL dans la tier des mots...
//                            praatInterval(
//                                    wordSB,
//                                    wordCount + 1,
//                                    token.getSegment().getStartFrame(),
//                                    token.getSegment().getEndFrame(),
//                                    censored ? "*ANON*" : tok);
//                            wordCount++;
                        } else {
//                            if (startWfr>token.getSegment().getStartFrame()) {
//                                // il y a un SIL devant: j'ajoute un mot "SIL" devant
//                                praatInterval(
//                                    wordSB,
//                                    wordCount + 1,
//                                    token.getSegment().getStartFrame(),
//                                    startWfr,
//                                    StatePool.SILENCE_PHONE);
//                                wordCount++;
//                            }
                            if (textidStard2add>=0) {
                                // first add a START textid comment with the same frame as the next word
                                praatInterval(
                                        wordSB,
                                        ++wordCount,
                                        startWfr,
                                        startWfr-1,
                                        textid2add);
                                textidStard2add=-1;
                            }
                            praatInterval(
                                    wordSB,
                                    ++wordCount,
                                    startWfr,
                                    endWfr,
                                    censored ? "*ANON*" : tok);
                            lastFrame=endWfr;
//                            if (endWfr<token.getSegment().getEndFrame()) {
//                                // il y a un SIL derriere; j'ajoute un mot "SIL" a la fin
//                                praatInterval(
//                                        wordSB,
//                                        wordCount + 1,
//                                        endWfr,
//                                        token.getSegment().getEndFrame(),
//                                        StatePool.SILENCE_PHONE);
//                                wordCount++;
//                            }
                        }
                        // dans tous les cas, j'ajoute les phonemes
                        if (!censored) {
                            for (Token.Phone phone : token.getPhones()) {
                                praatInterval(
                                        phoneSB,
                                        phoneCount + 1,
                                        phone.getSegment().getStartFrame(),
                                        phone.getSegment().getEndFrame(),
                                        phone.toString());
                                phoneCount++;
                            }
                        }

					} else if (null != token) {
                        if (token.toString().charAt(0)=='[') {
							// on traite le cas des commentaires textid
                            if (token.toString().endsWith("start]")) {
                                textid2add=token.toString();
                                textidStard2add=lastFrame; // this value is actually not used; the only important thing is that it is >=0
                            } else if (token.toString().endsWith("end]")) {
                                if (textidStard2add<0) {
                                    // only add the textid comment iff there are at least 1 real word in the interval
                                    praatInterval(
                                            wordSB,
                                            ++wordCount,
                                            lastFrame+1,
                                            lastFrame,
                                            token.toString());
                                } else {
                                    // cancels the previously started segment, because it is empty
                                    textidStard2add=-1;
                                }
                            } else {
                                // autre commentaire
                                praatInterval(
                                    wordSB,
                                    ++wordCount,
                                        lastFrame+1,
                                        lastFrame,
                                    token.toString());
                            }
                        } else {
                            // mot prononcé
                            praatInterval(
                                    wordSB,
                                    ++wordCount,
                                    lastFrame,
                                    lastFrame - 1,
                                    token.toString());
                        }
                    }
                }
            }

			if (withWords) {
				praatTierHeader(w, id++, p.getSpeakerName(i) + " words", wordCount, frameCount);
				w.write(wordSB.toString());
			}

			if (withPhons) {
				praatTierHeader(w, id++, p.getSpeakerName(i) + " phons", phoneCount, frameCount);
				w.write(phoneSB.toString());
			}
		}

		w.close();
	}


	public static void praatFileHeader(Appendable w, int frameCount, int tierCount)
		throws IOException
	{
		w.append("File type = \"ooTextFile\"")
				.append("\nObject class = \"TextGrid\"")
				.append("\n")
				.append("\nxmin = 0")
				.append("\nxmax = ")
				.append(Float.toString(frame2second(frameCount)))
				.append("\ntiers? <exists>")
				.append("\nsize = ")
				.append(Integer.toString(tierCount))
				.append("\nitem []:");
	}


	/**
	 * Appends a Praat tier header.
	 * @param w Append text to this writer
	 * @param id Tier ID (Praat tier numbering starts at 1 and is contiguous!)
	 * @param name Tier name
	 * @param intervalCount Number of intervals in the tier
	 */
	public static void praatTierHeader(
			Appendable w, int id, String name, int intervalCount, int frameCount)
			throws IOException
	{
		assert id > 0;
		w.append("\n\titem [").append(Integer.toString(id)).append("]:")
				.append("\n\t\tclass = \"IntervalTier\"")
				.append("\n\t\tname = \"").append(name).append('"') // TODO escape strings
				.append("\n\t\txmin = 0")
				.append("\n\t\txmax = ")
				.append(Float.toString(frame2second(frameCount)))
				.append("\n\t\tintervals: size = ")
				.append(Integer.toString(intervalCount));
	}


	/**
	 * Appends a Praat interval.
	 * @param w Append text to this writer
	 * @param id Interval ID (Interval numbering starts at 1 and is contiguous!)
	 */
	public static float praatInterval(
			Appendable w, int id, int xminFrame, int xmaxFrame, String content)
			throws IOException
	{
		float endSec=frame2second(xmaxFrame,false);
		w.append("\n\t\tintervals [").append(Integer.toString(id)).append("]:")
				.append("\n\t\t\txmin = ")
				.append(Float.toString(frame2second(xminFrame)))
				.append("\n\t\t\txmax = ")
				.append(Float.toString(endSec))
				.append("\n\t\t\ttext = \"").append(content).append('"'); // TODO escape strings
		return endSec;
	}
	// added this second function because in some cases, we really want the beginning of the next segment to match the end of the previous,
	// at the millisecond level !
	public static float praatInterval(
			Appendable w, int id, float xminSec, int xmaxFrame, String content)
			throws IOException
	{
		float endSec=frame2second(xmaxFrame,false);
		w.append("\n\t\tintervals [").append(Integer.toString(id)).append("]:")
				.append("\n\t\t\txmin = ")
				.append(Float.toString(xminSec))
				.append("\n\t\t\txmax = ")
				.append(Float.toString(endSec))
				.append("\n\t\t\ttext = \"").append(content).append('"'); // TODO escape strings
		return endSec;
	}
	public static float praatInterval(
			Appendable w, int id, float xminSec, float xmaxSec, String content)
			throws IOException
	{
		w.append("\n\t\tintervals [").append(Integer.toString(id)).append("]:")
				.append("\n\t\t\txmin = ")
				.append(Float.toString(xminSec))
				.append("\n\t\t\txmax = ")
				.append(Float.toString(xmaxSec))
				.append("\n\t\t\ttext = \"").append(content).append('"'); // TODO escape strings
		return xmaxSec;
	}

}
