package fr.loria.synalp.jtrans.markup.out;

import fr.loria.synalp.jtrans.project.*;

import java.io.*;
import java.util.Iterator;

import static fr.loria.synalp.jtrans.utils.FileUtils.getUTF8Writer;
import static fr.loria.synalp.jtrans.utils.TimeConverter.frame2sec;

public class TextGridSaverHelper {

	/**
	 * If true, anonymized words will be replaced with "*ANON*" and their
	 * phones will not be shown.
	 */
	public static boolean censorAnonWords = true;


	protected TextGridSaverHelper() {
		// Don't let anyone but subclasses instantiate
	}

	public static void savePraat(
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

			Iterator<Phrase> itr = p.phraseIterator(i);
			while (itr.hasNext()) {
				Phrase phrase = itr.next();

				// frame onto which to tack 0-length elements
				if (phrase.getInitialAnchor() != null) {
					lastFrame = phrase.getInitialAnchor().getFrame();
				}

				for (Element e: phrase) {
					Word word = e instanceof Word ? (Word) e : null;
					Comment comment = e instanceof Comment ? (Comment) e : null;

					if (word != null && word.isAligned()) {
						boolean censored = censorAnonWords && word.shouldBeAnonymized();

						praatInterval(
								wordSB,
								wordCount + 1,
								word.getSegment().getStartFrame(),
								word.getSegment().getEndFrame(),
								censored? "*ANON*": word.toString());
						wordCount++;

						if (!censored) {
							for (Word.Phone phone : word.getPhones()) {
								praatInterval(
										phoneSB,
										phoneCount + 1,
										phone.getSegment().getStartFrame(),
										phone.getSegment().getEndFrame(),
										phone.toString());
								phoneCount++;
							}
						}

						lastFrame = word.getSegment().getEndFrame();
					} else if (comment != null || word != null) {
						praatInterval(
								wordSB,
								wordCount + 1,
								lastFrame,
								lastFrame,
								e.toString());
						wordCount++;
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
				.append(Float.toString(frame2sec(frameCount)))
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
				.append(Float.toString(frame2sec(frameCount)))
				.append("\n\t\tintervals: size = ")
				.append(Integer.toString(intervalCount));
	}


	/**
	 * Appends a Praat interval.
	 * @param w Append text to this writer
	 * @param id Interval ID (Interval numbering starts at 1 and is contiguous!)
	 */
	public static void praatInterval(
			Appendable w, int id, int xminFrame, int xmaxFrame, String content)
			throws IOException
	{
		w.append("\n\t\tintervals [").append(Integer.toString(id)).append("]:")
				.append("\n\t\t\txmin = ")
				.append(Float.toString(frame2sec(xminFrame)))
				.append("\n\t\t\txmax = ")
				.append(Float.toString(frame2sec(xmaxFrame)))
				.append("\n\t\t\ttext = \"").append(content).append('"'); // TODO escape strings
	}

}
