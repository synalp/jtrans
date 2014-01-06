package jtrans.facade;

import jtrans.markup.*;
import jtrans.utils.StdoutProgressDisplay;

import java.io.File;

public class JTransCLI {
	public MarkupLoader loader;
	public String markupFileName;
	public String audioFileName;
	public String outputFileName;
	public boolean useAnchors = true;


	public JTransCLI(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			String lcarg = arg.toLowerCase();

			if (lcarg.endsWith(".wav")) {
				audioFileName = arg;
			}

			else if (lcarg.endsWith(".jtr")) {
				loader = new JTRLoader();
				markupFileName = arg;
			}

			else if (lcarg.endsWith(".trs")) {
				loader = new TRSLoader();
				markupFileName = arg;
			}

			else if (lcarg.endsWith(".textgrid")) {
				loader = new TextGridLoader();
				markupFileName = arg;
			}

			else if (lcarg.endsWith(".txt")) {
				loader = new RawTextLoader();
				markupFileName = arg;
			}

			else if (lcarg.equals("-o")) {
				outputFileName = args[++i];
			}

			else if (lcarg.equals("-noanchors")) {
				useAnchors = false;
			}

			else {
				System.err.println("args error: dont know what to do with " + arg);
				return;
			}
		}
	}

	public static void main(String args[]) throws Exception {
		if (!new File("res").exists()) {
			System.err.println("Resources missing. Please launch GUI to install" +
					" them, and then you can use the CLI.");
			System.exit(1);
		}

		JTransCLI cli = new JTransCLI(args);

		Project project = cli.loader.parse(new File(cli.markupFileName));
		project.setAudio(cli.audioFileName);

		for (int i = 0; i < project.tracks.size(); i++) {
			AutoAligner aa = new AutoAligner(project,
					project.tracks.get(i),
					new StdoutProgressDisplay(),
					null);
			if (cli.useAnchors)
				aa.alignBetweenAnchors();
			else
				aa.alignRaw();
		}

		System.out.println("Done!");

		System.out.println("Writing to " + cli.outputFileName);
		String lcout = cli.outputFileName.toLowerCase();
		if (lcout.endsWith(".jtr")) {
			project.saveJson(new File(cli.outputFileName));
		} else if (lcout.endsWith(".textgrid")) {
			project.savePraat(new File(cli.outputFileName), true, false);
		} else {
			System.err.println("Unknown output format");
			System.exit(1);
		}

		System.exit(0);
	}
}
