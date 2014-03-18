package fr.loria.synalp.jtrans.markup.out;

import fr.loria.synalp.jtrans.elements.Word;
import fr.loria.synalp.jtrans.facade.Project;
import fr.loria.synalp.jtrans.facade.Track;
import fr.loria.synalp.jtrans.utils.TimeConverter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

class TextGridSaverHelper {

	public static void savePraat(Project p, File f, boolean withWords, boolean withPhons)
			throws IOException
	{
		FileWriter w = new FileWriter(f);

		final int frameCount = (int) p.audioSourceTotalFrames;

		w.append("File type = \"ooTextFile\"")
				.append("\nObject class = \"TextGrid\"")
				.append("\n")
				.append("\nxmin = 0")
				.append("\nxmax = ")
				.append(Float.toString(TimeConverter.frame2sec(frameCount)))
				.append("\ntiers? <exists>")
				.append("\nsize = ")
				.append(Integer.toString(p.tracks.size() * ((withWords?1:0) + (withPhons?1:0))))
				.append("\nitem []:");

		int id = 1;
		for (Track t: p.tracks) {
			StringBuilder wordSB = new StringBuilder();
			StringBuilder phoneSB = new StringBuilder();

			List<Word> words = t.getWords();

			int wordCount = 0;
			int phoneCount = 0;

			for (Word word: words) {
				if (!word.isAligned()) {
					continue;
				}

				praatInterval(
						wordSB,
						wordCount+1,
						word.getSegment().getStartFrame(),
						word.getSegment().getEndFrame(),
						word.toString());
				wordCount++;

				for (Word.Phone phone: word.getPhones()) {
					praatInterval(
							phoneSB,
							phoneCount+1,
							phone.getSegment().getStartFrame(),
							phone.getSegment().getEndFrame(),
							phone.toString());
					phoneCount++;
				}
			}

			if (withWords) {
				praatTierHeader(w, id++, t.speakerName + " words", wordCount, frameCount);
				w.write(wordSB.toString());
			}

			if (withPhons) {
				praatTierHeader(w, id++, t.speakerName + " phons", phoneCount, frameCount);
				w.write(phoneSB.toString());
			}
		}

		w.close();
	}


	/**
	 * Appends a Praat tier header.
	 * @param w Append text to this writer
	 * @param id Tier ID (Praat tier numbering starts at 1 and is contiguous!)
	 * @param name Tier name
	 * @param intervalCount Number of intervals in the tier
	 */
	private static void praatTierHeader(
			Appendable w, int id, String name, int intervalCount, int frameCount)
			throws IOException
	{
		assert id > 0;
		w.append("\n\titem [").append(Integer.toString(id)).append("]:")
				.append("\n\t\tclass = \"IntervalTier\"")
				.append("\n\t\tname = \"").append(name).append('"') // TODO escape strings
				.append("\n\t\txmin = 0")
				.append("\n\t\txmax = ")
				.append(Float.toString(TimeConverter.frame2sec(frameCount)))
				.append("\n\t\tintervals: size = ")
				.append(Integer.toString(intervalCount));
	}


	/**
	 * Appends a Praat interval.
	 * @param w Append text to this writer
	 * @param id Interval ID (Interval numbering starts at 1 and is contiguous!)
	 */
	private static void praatInterval(
			Appendable w, int id, int xminFrame, int xmaxFrame, String content)
			throws IOException
	{
		w.append("\n\t\tintervals [").append(Integer.toString(id)).append("]:")
				.append("\n\t\t\txmin = ")
				.append(Float.toString(TimeConverter.frame2sec(xminFrame)))
				.append("\n\t\t\txmax = ")
				.append(Float.toString(TimeConverter.frame2sec(xmaxFrame)))
				.append("\n\t\t\ttext = \"")
				.append(content).append('"'); // TODO escape strings
	}

}
