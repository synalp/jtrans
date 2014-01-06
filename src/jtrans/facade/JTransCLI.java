package jtrans.facade;

import jtrans.markup.MarkupLoader;
import jtrans.markup.TRSLoader;
import jtrans.utils.StdoutProgressDisplay;

import java.io.File;

public class JTransCLI {
	public static void main(String args[]) throws Exception {
		MarkupLoader loader = new TRSLoader();
		Project project = loader.parse(new File(args[0]));
		project.setAudio(args[1]);

		for (int i = 0; i < project.tracks.size(); i++) {
			AutoAligner aa = new AutoAligner(project,
					project.tracks.get(i),
					new StdoutProgressDisplay(),
					null);
			aa.alignBetweenAnchors();
		}

		System.out.println("Done!");
		System.exit(0);
	}
}
