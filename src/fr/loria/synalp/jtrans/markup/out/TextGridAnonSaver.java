package fr.loria.synalp.jtrans.markup.out;

import fr.loria.synalp.jtrans.project.Token;
import fr.loria.synalp.jtrans.project.Phrase;
import fr.loria.synalp.jtrans.project.Project;
import fr.loria.synalp.jtrans.utils.QuickSort;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

import static fr.loria.synalp.jtrans.utils.FileUtils.getUTF8Writer;
import static fr.loria.synalp.jtrans.markup.out.TextGridSaverHelper.*;

/**
 * To interface with: http://sldr.org/voir_depot.php?id=526
 */
public class TextGridAnonSaver implements MarkupSaver {

	public static void savePraatAnonTier(Project p, File f) throws IOException {
		final int frameCount = (int) p.audioSourceTotalFrames;

		int lastend=0;
		ArrayList<int[]> anonsegs = new ArrayList<int[]>();
		for (int i = 0; i < p.speakerCount(); i++) {
			Iterator<Phrase> itr = p.phraseIterator(i);
			while (itr.hasNext()) {
				Phrase phrase = itr.next();
				for (Token token: phrase) {
					if (token.isAligned()) {
						int end = token.getSegment().getEndFrame();
						if (end>lastend) lastend=end;
						if (token.shouldBeAnonymized()) {
							int[] seg = {token.getSegment().getStartFrame(),end};
							anonsegs.add(seg);
						}
					}
				}
			}
		}
		// sort segments to anonymize
		float[] debs = new float[anonsegs.size()];
		int[] ids = new int[anonsegs.size()];
		for (int i=0;i<ids.length;i++) {ids[i]=i;debs[i]=anonsegs.get(i)[0];}
		QuickSort qsort = new QuickSort();
		qsort.sort(debs, ids);
		// rebuild the segments in order and merge overlapping ones
		ArrayList<int[]> asegs = new ArrayList<int[]>();
		int prevdeb=-1, prevend=-1;
		for (int i=0;i<ids.length;i++) {
			int deb=anonsegs.get(ids[i])[0];
			int end=anonsegs.get(ids[i])[1];
			if (deb==prevdeb) {
				if (end>prevend) {
					// remove the previous segment and put this one instead
					asegs.remove(asegs.size()-1);
					int[] limits={deb,end};
					asegs.add(limits);
					prevend=end;
					continue;
				} else {
					// remove the current segment
					continue;
				}
			} else if (end<=prevend) {
				// remove the current segment
				continue;
			} else if (deb<=prevend) {
				// overlapping segs: replace the end of the previous one with the new end
				int[] limits = asegs.get(asegs.size()-1);
				limits[1]=end;
				prevend=end;
				continue;
			} else {
				// standard case: 2 anon segs separated by a non-anon seg
				int[] limits={deb,end};
				asegs.add(limits);
				prevend=end;
			}
		}

		StringBuilder anonSB = new StringBuilder();
		int anonCount = 0;
		prevend=0;
		float prevendsec=0;
		for (int i=0;i<asegs.size();i++) {
			int deb=asegs.get(i)[0];
			int end=asegs.get(i)[1];
			if (deb>prevend) {
				// insert a nonanon seg
				prevendsec=praatInterval(
						anonSB,
						anonCount + 1,
						prevendsec,
						// end-1 is a hack because when later converting frames to seconds, a +1 is added to the end of the segment
						// this is because in the main (non-anon) code for segments, segments ends one frame before the beginning of the new segments,
						// while in this code, segments end = next segment start
						deb-1,
						"x");
				anonCount++;
			}
			prevendsec=praatInterval(
					anonSB,
					anonCount + 1,
					prevendsec,
					end-1,
					"buzz");
			prevend=end;
			anonCount++;
		}
		if (prevend<lastend) {
			praatInterval(
					anonSB,
					anonCount + 1,
					prevendsec,
					lastend-1,
					"x");
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
