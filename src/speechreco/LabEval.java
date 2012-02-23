package speechreco;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.sphinx.util.NISTAlign;

import plugins.utils.FileUtils;

public class LabEval {
	private ArrayList<String> recursDir(File d) {
		ArrayList<String> res = new ArrayList<String>();
		File[] ff = d.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".wav")) return true;
				return false;
			}
		});
		if (ff!=null) {
			for (int i=0;i<ff.length;i++) res.add(ff[i].getAbsolutePath());
		}
		ff = d.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (dir.isDirectory()) return true;
				return false;
			}
		});
		if (ff!=null)
			for (int i=0;i<ff.length;i++) {
				ArrayList<String> tmp = recursDir(ff[i]);
				for (int j=0;j<tmp.size();j++) res.add(tmp.get(j));
			}
		return res;
	}

	public static void main(String args[]) {
		NISTAlign aligner = new NISTAlign(true, true);
		LabEval m = new LabEval();
		List<String> wavfiles = m.recursDir(new File("/home/xtof/svn/aligne/corpusSyntaxe/"));
		for (String wav : wavfiles) {
			String recfile = FileUtils.noExt(wav)+".rec";
			RecoUtterance rec = new RecoUtterance();
			rec.loadLab(recfile);
			String reffile = FileUtils.noExt(wav)+".lab";
			RecoUtterance ref = new RecoUtterance();
			ref.loadLab(reffile);
			rec.calcWER(aligner, ref);
		}
		aligner.printTotalSummary();
	}
}
