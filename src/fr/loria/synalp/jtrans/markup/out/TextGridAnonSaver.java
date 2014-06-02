package fr.loria.synalp.jtrans.markup.out;

import fr.loria.synalp.jtrans.project.Token;
import fr.loria.synalp.jtrans.project.Phrase;
import fr.loria.synalp.jtrans.project.Project;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import static fr.loria.synalp.jtrans.utils.FileUtils.getUTF8Writer;
import static fr.loria.synalp.jtrans.markup.out.TextGridSaverHelper.*;

/**
 * To interface with: http://sldr.org/voir_depot.php?id=526
 */
public class TextGridAnonSaver implements MarkupSaver {

	public static void savePraatAnonTier(Project p, File f) throws IOException {
		final int frameCount = (int) p.audioSourceTotalFrames;

		StringBuilder anonSB = new StringBuilder();
		int anonCount = 0;

		for (int i = 0; i < p.speakerCount(); i++) {
			Iterator<Phrase> itr = p.phraseIterator(i);
			while (itr.hasNext()) {
				Phrase phrase = itr.next();

				for (Token token: phrase) {
					if (token.isAligned() && token.shouldBeAnonymized()) {
						praatInterval(
								anonSB,
								anonCount + 1,
								token.getSegment().getStartFrame(),
								token.getSegment().getEndFrame(),
								"buzz");
						anonCount++;
					}
				}
			}
		}

		Writer w = getUTF8Writer(f);
		praatFileHeader(w, frameCount, 1);
		praatTierHeader(w, 1, "ANON", anonCount, frameCount);
		w.write(anonSB.toString());
		w.close();
	}



	public void save(Project project, File file) throws IOException {
		savePraatAnonTier(project, file);
	}

	public String getFormat() {
		return "Praat TextGrid anonymization tier for " +
				"http://sldr.org/voir_depot.php?id=526";
	}

	public String getExt() {
		return ".anon.textgrid";
	}
}
