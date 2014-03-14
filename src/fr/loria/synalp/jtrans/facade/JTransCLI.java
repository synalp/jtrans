package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.markup.*;
import fr.loria.synalp.jtrans.utils.StdoutProgressDisplay;

import java.io.File;

public class JTransCLI {
	public MarkupLoader loader;
	public String markupFileName;
	public String audioFileName;
	public String outputFileName;


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
		project.align(new StdoutProgressDisplay());

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
