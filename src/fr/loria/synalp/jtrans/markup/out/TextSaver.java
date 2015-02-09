package fr.loria.synalp.jtrans.markup.out;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import fr.loria.synalp.jtrans.project.Phrase;
import fr.loria.synalp.jtrans.project.Project;
import fr.loria.synalp.jtrans.project.Token;
import fr.loria.synalp.jtrans.utils.QuickSort;

public class TextSaver implements MarkupSaver {

	@Override
	public String getFormat() {
		return "raw text";
	}

	@Override
	public String getExt() {
		return ".txt";
	}

	@Override
	public void save(Project project, File file) throws IOException {
		saveText(project, file);
	}

	public static void saveText(Project p, File f) throws IOException {
		int lastend=0;
		ArrayList<int[]> wsegs = new ArrayList<int[]>();
		ArrayList<String> wwords = new ArrayList<String>();
		for (int i = 0; i < p.speakerCount(); i++) {
			Iterator<Phrase> itr = p.phraseIterator(i);
			while (itr.hasNext()) {
				Phrase phrase = itr.next();
				for (Token token: phrase) {
					if (token.isAligned()) {
						int end = token.getSegment().getEndFrame();
						if (end>lastend) lastend=end;
						int[] seg = {token.getSegment().getStartFrame(),end};
						wsegs.add(seg);
						wwords.add(token.toString());
					}
				}
			}
		}
		// sort segments 
		float[] debs = new float[wsegs.size()];
		int[] ids = new int[wsegs.size()];
		for (int i=0;i<ids.length;i++) {ids[i]=i;debs[i]=wsegs.get(i)[0];}
		QuickSort qsort = new QuickSort();
		qsort.sort(debs, ids);

		// rebuild the segments in order and merge overlapping ones
		ArrayList<int[]> asegs = new ArrayList<int[]>();
		ArrayList<String> awords = new ArrayList<String>();
		for (int i=0;i<ids.length;i++) {
			int deb=wsegs.get(ids[i])[0];
			int end=wsegs.get(ids[i])[1];
			// standard case: 2 anon segs separated by a non-anon seg
			int[] limits={deb,end};
			asegs.add(limits);
			awords.add(wwords.get(ids[i]));
		}
		
		PrintWriter ff = new PrintWriter(new FileWriter(f));
		for (int i=0;i<awords.size();i++)
			ff.println(awords.get(i));
		ff.close();
	}
}
